import { FormattedMessage, MessageDescriptor, defineMessages } from 'react-intl';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { BulkActionRunState, BulkActionType } from 'elza-api';
import * as types from 'actions/constants/ActionTypes';
import { changeFundAction } from 'actions/global/change';
import { fundTreeFetch, fundTreeInvalidate } from 'actions/arr/fundTree';
import { fundNodeSubNodeFulltextSearch } from 'actions/arr/node';
import { addToastrDanger, addToastrSuccess } from 'components/shared/toastr/ToastrActions';
import { ZP2015_INTRO_VYPOCET_EJ } from '../../constants';
import { AppState } from 'typings/store';
import { Fund } from 'typings/store/AppState.types';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    sortSuccess: { id: 'arr.functions.persistentSort.sortSuccess', defaultMessage: 'Seřazení proběhlo úspěšně' },
    sortInterrupted: { id: 'arr.functions.persistentSort.sortInterrupted', defaultMessage: 'Seřazení bylo přerušeno' },
    sortFailed: { id: 'arr.functions.persistentSort.sortFailed', defaultMessage: 'Seřazení selhalo' },
    ejSuccess: { id: 'arr.functions.computeAndVizualizeEJ.success', defaultMessage: 'Výpočet a vizualizace EJ proběhlo úspěšně' },
    ejInterrupted: { id: 'arr.functions.computeAndVizualizeEJ.interrupted', defaultMessage: 'Výpočet a vizualizace EJ bylo přerušeno' },
    ejError: { id: 'arr.functions.computeAndVizualizeEJ.error', defaultMessage: 'Výpočet a vizualizace EJ selhalo' },
});

/** The event BULK_ACTION_STATE_CHANGE (EventIdInVersion on the server). */
export interface BulkActionStateChangeMessage {
    versionId: number;
    /** Id of the run. */
    entityId: number;
    /** Code of the action. */
    code?: string;
    /** BulkActionRunState */
    state?: string;
    /** Kind of the action (BulkActionType). */
    actionType?: string;
}

type Dispatch = ThunkDispatch<AppState, void, AnyAction>;

interface StateMessages {
    success: MessageDescriptor;
    interrupted: MessageDescriptor;
    failed: MessageDescriptor;
}

function notifyEnd(dispatch: Dispatch, state: string | undefined, stateMessages: StateMessages) {
    if (state === BulkActionRunState.Finished) {
        dispatch(addToastrSuccess(<FormattedMessage {...stateMessages.success} />));
    } else if (state === BulkActionRunState.Interrupted) {
        dispatch(addToastrDanger(<FormattedMessage {...stateMessages.interrupted} />));
    } else if (state === BulkActionRunState.Error) {
        dispatch(addToastrDanger(<FormattedMessage {...stateMessages.failed} />));
    }
}

/** Reloads the form of the selected unit, when the fund is the one shown and has one. */
function reloadActiveNodeForm(dispatch: Dispatch, getState: () => AppState, fund: Fund) {
    const { arrRegion } = getState();
    const activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : undefined;
    if (activeFund === fund && fund.nodes?.activeIndex != null) {
        dispatch(fundNodeSubNodeFulltextSearch(undefined));
    }
}

/**
 * A run of a bulk action changed its state. The persistent sort is recognised by its kind (every
 * rule set names its own action), the computation of the storage units by its ZP2015 code. Both
 * change the tree of the fund of the run - updated only when the fund is open, as are the messages.
 */
export function bulkActionStateChanged(message: BulkActionStateChangeMessage) {
    return (dispatch: Dispatch, getState: () => AppState) => {
        const fund = getState().arrRegion.funds.find(f => f.versionId === message.versionId);
        if (fund) {
            if (message.actionType === BulkActionType.PersistentSort) {
                if (message.state === BulkActionRunState.Finished) {
                    dispatch(fundTreeInvalidate(fund.versionId));
                    reloadActiveNodeForm(dispatch, getState, fund);
                }
                notifyEnd(dispatch, message.state, {
                    success: messages.sortSuccess,
                    interrupted: messages.sortInterrupted,
                    failed: messages.sortFailed,
                });
            } else if (message.code === ZP2015_INTRO_VYPOCET_EJ) {
                if (message.state === BulkActionRunState.Finished) {
                    dispatch(fundTreeFetch(types.FUND_TREE_AREA_MAIN, fund.versionId, null, fund.fundTree.expandedIds));
                    reloadActiveNodeForm(dispatch, getState, fund);
                }
                notifyEnd(dispatch, message.state, {
                    success: messages.ejSuccess,
                    interrupted: messages.ejInterrupted,
                    failed: messages.ejError,
                });
            }
        }
        dispatch(changeFundAction(message.versionId, message.entityId));
    };
}
