import * as types from 'actions/constants/ActionTypes.js';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx';
import {WebApi} from 'actions/index.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {i18n, Utils} from 'components/shared';
import {registryListInvalidate} from 'actions/registry/registry.jsx';
import {partyListInvalidate} from 'actions/party/party.jsx';

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
        savingApiWrapper(dispatch, WebApi.xmlImport(data))
            .then(() => {
                dispatch(modalDialogHide());
                dispatch(addToastrSuccess(i18n('import.toast.success'), i18n('import.toast.success' + messageType)));
                switch (messageType) {
                    case 'Fund':
                        break;
                    case 'Record':
                        dispatch(registryListInvalidate);
                        break;
                    case 'Party':
                        dispatch(partyListInvalidate);
                        dispatch(registryListInvalidate);
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
