import {valuesEquals} from '../Utils';
import {i18n} from '../shared';
import {ItemFormActions} from "./ItemFormActions";
import {WebApi} from '../../actions/index.jsx';


interface BaseAction {
    area: string;
    type: string;
}

export class ApNameFormActions extends ItemFormActions {

    static AREA = "AP_NAME";

    constructor() {
        super(ApNameFormActions.AREA);
    }

    /** Načtení server dat pro formulář pro aktuálně předané parametry BEZ využití cache. */
    //@Override
    _getItemFormData(getState, dispatch) {
        // není podpora kešování
        const state = getState();
        const {parent} = this._getItemFormStore(state);
        return WebApi.getAccessPointName(parent.accessPointId, parent.id).then(data => ({...data.form, parent: {id: data.objectId, accessPointId: data.accessPointId}}));
    }

    // @Override
    _getItemFormStore(state) {
        return state.ap.nameItemForm
    }

    // @Override
    _getParentObjStore(state) {
        return state.ap;
    }

    // @Override
    _callCreateDescItem(parent, descItemTypeId, item) {
        return WebApi.changeNameItems(parent.accessPointId, parent.id, [{updateOp: "CREATE", item}])
    }

    // @Override
    _callUpdateDescItem(parent, item) {
        return WebApi.changeNameItems(parent.accessPointId, parent.id, [{updateOp: "UPDATE", item}]);
    }

    // @Override
    _callDeleteDescItem(parent, item) {
        return WebApi.changeNameItems(parent.accessPointId, parent.id, [{updateOp: "DELETE", item}]);
    }

    // @Override
    _callDeleteDescItemType(parent, descItemTypeId) {
        return WebApi.deleteNameItemsByType(parent.accessPointId, parent.id, descItemTypeId);
    }

    // @Override
    _callSetNotIdentifiedDescItem(descItemTypeId, descItemSpecId, descItemObjectId) {
        // Not implemented
    }

    _callUnsetNotIdentifiedDescItem(outputItemTypeId, outputItemSpecId, outputItemObjectId) {
        // Not implemented
    }

    isSubNodeFormCacheAction(action: BaseAction) {
        // Not implemented
    }
}

export const apNameFormActions = new ApNameFormActions();
