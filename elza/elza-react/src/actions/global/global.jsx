import * as types from 'actions/constants/ActionTypes';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx';
import {WebApi} from 'actions/index.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {trackImportBatch} from 'utils/pendingImportBatches';
import { Utils} from 'components/shared';
import { FormattedMessage, defineMessages } from 'react-intl';
import { messageFor } from 'components/shared/lang/dynamicMessage';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    success: { id: 'import.toast.success', defaultMessage: 'Import dokončen' },
});

/** Podrobnost hlášky podle typu importu; klíč se dřív skládal za běhu. */
const importDetailMessages = defineMessages({
    Fund: { id: 'import.toast.successFund', defaultMessage: 'Import archivního souboru byl úspěšně dokončen.' },
    Party: { id: 'import.toast.successParty', defaultMessage: 'Import osob byl úspěšně dokončen.' },
    Record: { id: 'import.toast.successRecord', defaultMessage: 'Import rejstříkových hesel byl úspěšně dokončen.' },
});
import {registryListInvalidate} from 'actions/registry/registry.jsx';

export const ObjectInfo = class ObjectInfo {
    constructor() {
        this.nodeIds = new Utils.StringSet();
        this.fundIds = new Utils.StringSet();

        this.addNode = this.addNode.bind(this);
        this.addFund = this.addFund.bind(this);
    }

    addNode(node) {
        console.log('addNode', node);
        this.nodeIds.add(node.id);
    }

    addFund(fund) {
        console.log('addFund', fund);
        this.fundIds.add(fund.id);
    }
};

export function getObjectInfo(objectInfo) {
    return {
        type: types.GLOBAL_GET_OBJECT_INFO,
        objectInfo,
    };
}

export function importForm(data, messageType) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.xmlImport(data))
            .then((result) => {
                dispatch(modalDialogHide());
                // 202 response with batchId means the import was wrapped into a batch and runs
                // asynchronously; both the enqueue and the final outcome are announced by
                // ImportBatchToaster, which has the intl context this action has not.
                if (result && result.batchId != null) {
                    trackImportBatch(result.batchId);
                } else {
                    dispatch(
                        addToastrSuccess(
                            <FormattedMessage {...messages.success} />,
                            <FormattedMessage {...messageFor(importDetailMessages, messageType, importDetailMessages.Fund)} />,
                        ),
                    );
                }
                switch (messageType) {
                    case 'Fund':
                        break;
                    case 'Record':
                        dispatch(registryListInvalidate());
                        break;
                    default:
                        return;
                }
            })
            .catch(() => {
                dispatch(modalDialogHide());
            });
    };
}
