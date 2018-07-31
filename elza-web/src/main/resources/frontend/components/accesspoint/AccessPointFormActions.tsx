import {IItemFormState} from "../../stores/app/accesspoint/itemForm";
import {valuesEquals} from '../Utils';
import {i18n} from '../shared';
import {ItemFormActions} from "./ItemFormActions";
import {WebApi} from '../../actions/index.jsx';
import * as types from '../../actions/constants/ActionTypes.js';

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
    _getItemFormData(getState, dispatch, versionId) {
        // není podpora kešování
        return new Promise((resolve) => {
            const state = getState();
            resolve({...state.app.registryDetail.data.form, parent: {id: state.app.registryDetail.data.id}})
        });
    }

    // @Override
    _getItemFormStore(state) {
        return state.ap.form
    }

    // @Override
    _getParentObjStore(state) {
        return state.ap
    }

    // @Override
    _callCreateDescItem(parentId, nodeVersionId, descItemTypeId, item) {
        return WebApi.changeAccessPointItems(parentId, [{updateOp: "CREATE", item}])
    }

    // @Override
    _callUpdateDescItem(parentVersionId, parentId, item) {
        return WebApi.changeAccessPointItems(parentId, [{updateOp: "UPDATE", item}]);
    }

    // @Override
    _callDeleteDescItem(parentVersionId, item, parentId) {
        return WebApi.changeAccessPointItems(parentId, [{updateOp: "DELETE", item}]);
    }

    // @Override
    _callDeleteDescItemType(parentId, parentVersionId, descItemTypeId) {
        return WebApi.deleteStructureItemsByType(parentId, descItemTypeId);
    }

    // @Override
    _callSetNotIdentifiedDescItem(parentNodeVersion, descItemTypeId, descItemSpecId, descItemObjectId) {
        // Not implemented
    }

    _callUnsetNotIdentifiedDescItem(parentNodeVersion, outputItemTypeId, outputItemSpecId, outputItemObjectId) {
        // Not implemented
    }

    isSubNodeFormCacheAction(action: BaseAction) {
        // Not implemented
    }
}

export const accessPointFormActions = new AccessPointFormActions();
