import {IItemFormState} from "../../stores/app/accesspoint/itemForm";
import {valuesEquals} from '../Utils';
import {i18n} from '../shared';
import {ItemFormActions} from "./ItemFormActions";
import {WebApi} from '../../actions/index.jsx';
import * as types from '../../actions/constants/ActionTypes.js';
import {storeFromArea} from '../../shared/utils'


interface BaseAction {
    area: string;
    type: string;
}

export class AccessPointFormActions extends ItemFormActions {

    static AREA = "ACCESS_POINT";

    constructor() {
        super(AccessPointFormActions.AREA);
    }

    /** Načtení server dat pro formulář pro aktuálně předané parametry BEZ využití cache. */
    //@Override
    _getItemFormData(getState, dispatch) {
        // není podpora kešování
        const state = getState();
        return WebApi.getAccessPoint(state.app.registryDetail.id).then(data => ({...data.form, parent: {id: data.id}}));
    }

    // @Override
    _getItemFormStore(state) {
        return state.ap.form
    }

    // @Override
    _getParentObjStore(state) {
        return storeFromArea(state, "registryList");
    }

    // @Override
    _callCreateDescItem(parent, descItemTypeId, item) {
        return WebApi.changeAccessPointItems(parent.id, [{updateOp: "CREATE", item}])
    }

    // @Override
    _callUpdateDescItem(parent, item) {
        return WebApi.changeAccessPointItems(parent.id, [{updateOp: "UPDATE", item}]);
    }

    // @Override
    _callDeleteDescItem(parent, item) {
        return WebApi.changeAccessPointItems(parent.id, [{updateOp: "DELETE", item}]);
    }

    // @Override
    _callDeleteDescItemType(parent, descItemTypeId) {
        return WebApi.deleteAccessPointItemsByType(parent.id, descItemTypeId);
    }

    // @Override
    _callSetNotIdentifiedDescItem(descItemTypeId, descItemSpecId, descItemObjectId) {
        // Not implemented
    }

    _callUnsetNotIdentifiedDescItem(outputItemTypeId, outputItemSpecId, outputItemObjectId) {
        // Not implemented
    }
}

export const accessPointFormActions = new AccessPointFormActions();
