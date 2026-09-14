/**
 * Akce pro výstupy - named output.
 */

import * as types from '../../actions/constants/ActionTypes';
import {WebApi} from '../../actions/index';
import {} from '../../components/shared';
import { FormattedMessage, defineMessages } from 'react-intl';
import { getIntl } from 'components/shared/lang/intlInstance';
import { nodeListMessages } from 'components/arr/nodeListMessages';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    usageEnded: { id: 'arr.output.title.usageEnded', defaultMessage: 'Bylo ukončeno používání výstupu' },
    nodesAdded: { id: 'arr.output.title.nodesAdded', defaultMessage: 'JP byly přidány' },
    deleted: { id: 'arr.output.title.deleted', defaultMessage: 'Výstup byl smazán' },
    revert: { id: 'arr.output.title.revert', defaultMessage: 'Vráceno do přípravy' },
    clone: { id: 'arr.output.title.clone', defaultMessage: 'Kopie vytvořena' },
    added: { id: 'arr.output.title.added', defaultMessage: 'Výstup byl vytvořen' },
    generateContinue: {
        id: 'ribbon.action.arr.output.generate.continue',
        defaultMessage: '{0}, chcete i přesto pokračovat?',
    },
    sendConfirm: { id: 'ribbon.action.arr.output.send.confirm', defaultMessage: 'Provést odeslání výstupu?' },
    sendSuccess: { id: 'ribbon.action.arr.output.send.success', defaultMessage: 'Výstup odeslán' },
});

/** Důvod, proč generování výstupu potřebuje potvrzení. */
const generateReasonMessages = defineMessages({
    DETECT_CHANGE: {
        id: 'ribbon.action.arr.output.generate.detectChange',
        defaultMessage:
            'Byly zjištěny změny v archivním popisu. Některé hodnoty spočtené funkcemi nemusí být aktuální',
    },
    RECOMMENDED_ACTION_NOT_RUN: {
        id: 'ribbon.action.arr.output.generate.recommendedAction',
        defaultMessage: 'Nebyly spuštěny všechny doporučené akce',
    },
});
import {indexById} from '../../stores/app/utils';
import {isFundOutputFilesAction} from './fundOutputFiles';
import {isFundOutputFunctionsAction} from './fundOutputFunctions';
import {addToastrSuccess} from '../../components/shared/toastr/ToastrActions';
import {modalDialogHide} from '../../actions/global/modalDialog';
import {savingApiWrapper} from '../../actions/global/status';
import { showConfirmDialog } from 'components/shared/dialog';

export function isFundOutput(action) {
    if (isFundOutputDetail(action)) {
        return true;
    }
    if (isFundOutputFilesAction(action)) {
        return true;
    }
    if (isFundOutputFunctionsAction(action)) {
        return true;
    }

    switch (action.type) {
        case types.FUND_OUTPUT_REQUEST:
        case types.FUND_OUTPUT_RECEIVE:
        case types.FUND_OUTPUT_FILTER_STATE:
            return true;
        default:
            return false;
    }
}

export function isFundOutputDetail(action) {
    switch (action.type) {
        case types.FUND_OUTPUT_DETAIL_REQUEST:
        case types.FUND_OUTPUT_DETAIL_RECEIVE:
        case types.FUND_OUTPUT_SELECT_OUTPUT:
        case types.FUND_OUTPUT_DETAIL_CLEAR:
        case types.OUTPUT_INCREASE_VERSION:
            return true;
        default:
            return false;
    }
}

function _fundOutputDataKey(fundOutput) {
    return '-FilterState=' + fundOutput.filterState;
}

function _fundOutputDetailDataKey(fundOutputDetail) {
    if (fundOutputDetail.id !== null) {
        return fundOutputDetail.id + '_';
    } else {
        return '';
    }
}

export function fundOutputUsageEnd(versionId, outputId) {
    return (dispatch, getState) => {
        WebApi.outputUsageEnd(versionId, outputId).then(json => {
            dispatch(addToastrSuccess(<FormattedMessage {...messages.usageEnded} />));
        });
    };
}

export function fundOutputAddNodes(versionId, outputId, nodeIds) {
    return (dispatch, getState) => {
        WebApi.fundOutputAddNodes(versionId, outputId, nodeIds).then(json => {
            dispatch(addToastrSuccess(<FormattedMessage {...messages.nodesAdded} />));
            dispatch(modalDialogHide());
        });
    };
}

export function fundOutputRemoveNodes(versionId, outputId, nodeIds) {
    return (dispatch, getState) => {
        WebApi.fundOutputRemoveNodes(versionId, outputId, nodeIds);
    };
}

export function fundOutputDelete(versionId, outputId) {
    return (dispatch, getState) => {
        WebApi.outputDelete(versionId, outputId).then(json => {
            dispatch(addToastrSuccess(<FormattedMessage {...messages.deleted} />));
            dispatch(fundOutputDetailClear(versionId));
        });
    };
}

export function fundOutputRevert(versionId, outputId) {
    return (dispatch, getState) => {
        WebApi.outputRevert(versionId, outputId).then(json => {
            dispatch(addToastrSuccess(<FormattedMessage {...messages.revert} />));
        });
    };
}

export function fundOutputClone(versionId, outputId) {
    return (dispatch, getState) => {
        WebApi.outputClone(versionId, outputId).then(json => {
            dispatch(fundOutputSelectOutput(versionId, json.id));
            dispatch(addToastrSuccess(<FormattedMessage {...messages.clone} />));
        });
    };
}

// const handleAddNodes = (fundOutputDetail, dispatch) => {
//     const fund = this.getActiveFund(this.props);
//
//     this.props.dispatch(modalDialogShow(this, getIntl().formatMessage(nodeListMessages.select),
//         <FundNodesSelectForm
//             onSubmitForm={(ids, nodes) => {
//                 dispatch(fundOutputAddNodes(fund.versionId, fundOutputDetail.id, ids))
//             }}
//         />))
// };

export function fundOutputCreate(versionId, data) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.createOutput(versionId, data)).then(json => {
            dispatch(addToastrSuccess(<FormattedMessage {...messages.added} />));
            dispatch(fundOutputSelectOutput(versionId, json.id));
            // handleAddNodes(json, dispatch);
            return json;
        });
    };
}

export function fundOutputEdit(versionId, outputId, data) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.updateOutput(versionId, outputId, data));
    };
}

export function fundOutputSelectOutput(versionId, id) {
    return {
        type: types.FUND_OUTPUT_SELECT_OUTPUT,
        versionId,
        id,
    };
}

function _getFundOutput(versionId, getState) {
    var state = getState();
    var index = indexById(state.arrRegion.funds, versionId, 'versionId');
    if (index != null) {
        const fund = state.arrRegion.funds[index];
        return fund.fundOutput;
    }

    return null;
}

/**
 * Fetch dat pro detail výstupu.
 */
export function fundOutputDetailFetchIfNeeded(versionId, outputId) {
    return (dispatch, getState) => {
        const fundOutput = _getFundOutput(versionId, getState);
        if (fundOutput == null) {
            return;
        }

        const fundOutputDetail = fundOutput.fundOutputDetail;
        const dataKey = _fundOutputDetailDataKey(fundOutputDetail);

        if (fundOutputDetail.currentDataKey !== dataKey) {
            dispatch(fundOutputDetailRequest(versionId, dataKey));
            WebApi.getFundOutputDetail(versionId, outputId).then(json => {
                const newFundOutput = _getFundOutput(versionId, getState);
                if (newFundOutput == null) {
                    return;
                }
                const newFundOutputDetail = newFundOutput.fundOutputDetail;
                const newDataKey = _fundOutputDetailDataKey(newFundOutputDetail);
                if (newDataKey === dataKey) {
                    dispatch(fundOutputDetailReceive(versionId, json));
                }
            });
        }
    };
}

/**
 * Fetch dat pro seznam výstupů.
 */
export function fundOutputFetchIfNeeded(versionId) {
    return (dispatch, getState) => {
        const fundOutput = _getFundOutput(versionId, getState);
        if (fundOutput == null) {
            return;
        }

        const dataKey = _fundOutputDataKey(fundOutput);
        if (fundOutput.currentDataKey !== dataKey) {
            dispatch(fundOutputRequest(versionId, dataKey));
            WebApi.getOutputs(versionId, fundOutput.filterState !== -1 ? fundOutput.filterState : null).then(json => {
                const newFundOutput = _getFundOutput(versionId, getState);
                if (newFundOutput == null) {
                    return;
                }
                const newDataKey = _fundOutputDataKey(newFundOutput);
                if (newDataKey === dataKey) {
                    dispatch(fundOutputReceive(versionId, json));
                }
            });
        }
    };
}

function fundOutputRequest(versionId, dataKey) {
    return {
        type: types.FUND_OUTPUT_REQUEST,
        versionId,
        dataKey,
    };
}

function fundOutputReceive(versionId, outputs) {
    return {
        type: types.FUND_OUTPUT_RECEIVE,
        versionId,
        outputs,
    };
}

function fundOutputDetailRequest(versionId, dataKey) {
    return {
        type: types.FUND_OUTPUT_DETAIL_REQUEST,
        versionId,
        dataKey,
    };
}

function fundOutputDetailReceive(versionId, data) {
    return {
        type: types.FUND_OUTPUT_DETAIL_RECEIVE,
        versionId,
        data,
    };
}

export function fundOutputDetailClear(versionId) {
    return {
        type: types.FUND_OUTPUT_DETAIL_CLEAR,
        versionId,
    };
}

export function fundOutputGenerate(outputId) {
    return (dispatch, getState) => {
        WebApi.outputGenerate(outputId).then(async (data) => {
            if (data && data.status !== 'OK') {
                const reason = generateReasonMessages[data.status];
                if (!reason) {
                    console.warn('Neznámý stav generování výstupu:', data.status);
                    return;
                }
                const response = await dispatch(
                    showConfirmDialog(
                        <FormattedMessage
                            {...messages.generateContinue}
                            values={{ 0: getIntl().formatMessage(reason) }}
                        />,
                    ),
                );
                if (response) {
                    WebApi.outputGenerate(outputId, true);
                }
            }
        });
    };
}

export function fundOutputSend(outputId) {
    return async (dispatch) => {
        const response = await dispatch(showConfirmDialog(<FormattedMessage {...messages.sendConfirm} />));
        if (response) {
            WebApi.outputSend(outputId).then(() => {
                dispatch(addToastrSuccess(<FormattedMessage {...messages.sendSuccess} />));
            });
        }
    };
}

export function fundOutputFilterByState(versionId, state) {
    return {
        type: types.FUND_OUTPUT_FILTER_STATE,
        versionId,
        state,
    };
}
