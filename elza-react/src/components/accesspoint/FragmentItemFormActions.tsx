import {WebApi} from '../../actions/index.jsx';
import {ItemFormActions} from './ItemFormActions';

interface BaseAction {
    area: string;
    type: string;
}

export class FragmentItemFormActions extends ItemFormActions {
    static AREA = 'AP_FRAGMENT';

    constructor() {
        super(FragmentItemFormActions.AREA);
    }

    /** Načtení server dat pro formulář pro aktuálně předané parametry BEZ využití cache. */
    //@Override
    _getItemFormData(getState, dispatch) {
        // není podpora kešování
        const state = getState();
        const {parent} = this._getItemFormStore(state);
        return WebApi.getFragment(parent.id).then(data => ({...data.form, parent: {id: data.id}}));
    }

    // @Override
    _getItemFormStore(state) {
        return state.ap.fragmentItemForm;
    }

    // @Override
    _getParentObjStore(state) {
        return state.ap;
    }

    // @Override
    _callCreateDescItem(parent, descItemTypeId, item) {
        return WebApi.changeFragmentItems(parent.id, [{updateOp: 'CREATE', item}]);
    }

    // @Override
    _callUpdateDescItem(parent, item) {
        return WebApi.changeFragmentItems(parent.id, [{updateOp: 'UPDATE', item}]);
    }

    // @Override
    _callDeleteDescItem(parent, item) {
        return WebApi.changeFragmentItems(parent.id, [{updateOp: 'DELETE', item}]);
    }

    // @Override
    _callDeleteDescItemType(parent, descItemTypeId) {
        return WebApi.deleteFragmentItemsByType(parent.id, descItemTypeId);
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

export const fragmentItemFormActions = new FragmentItemFormActions();
