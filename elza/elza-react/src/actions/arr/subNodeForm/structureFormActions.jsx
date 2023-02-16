/**
 * Akce pro formulář strukturovaných typů.
 */

import { WebApi } from 'actions/index';
import { ItemFormActions } from './itemFormActions';

export class StructureFormActions extends ItemFormActions {
    static AREA = 'STRUCTURE';

    constructor() {
        super(StructureFormActions.AREA);
    }

    /** Načtení server dat pro formulář pro aktuálně předané parametry BEZ využití cache. */
    //@Override
    _getItemFormData(getState, dispatch, versionId, nodeId, routingKey) {
        // není podpora kešování
        return WebApi.getFormStructureItems(versionId, nodeId).then(i => ({...i, parent: {...i.parent, version: 0}}));
    }

    // @Override
    _getItemFormStore(state, versionId, routingKey) {
        const subStore = state.structures.stores[String(routingKey)];
        if (!!subStore) {
            return subStore.subNodeForm;
        } else {
            return null;
        }
    }

    // @Override
    _getParentObjStore(state, versionId, routingKey) {
        const subStore = state.structures.stores[String(routingKey)];
        if (!!subStore) {
            return subStore;
        } else {
            return null;
        }
    }

    // @Override
    _callCreateDescItem(versionId, parentId, nodeVersionId, descItemTypeId, descItem) {
        return WebApi.createStructureItem(versionId, parentId, descItemTypeId, descItem);
    }

    // @Override
    _callUpdateDescItem(dispatch, formState, versionId, parentVersionId, parentId, descItem) {
        return WebApi.updateStructureItem(versionId, descItem);
    }

    // @Override
    _callDeleteDescItem(versionId, parentId, parentVersionId, descItem) {
        return WebApi.deleteStructureItem(versionId, descItem);
    }

    // @Override
    _callArrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        // Not implemented
    }

    // @Override
    _callDescItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        // Not implemented
    }

    // @Override
    _callDeleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId) {
        return WebApi.deleteStructureItemsByType(versionId, parentId, descItemTypeId);
    }

    // @Override
    _callSetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        descItemTypeId,
        descItemSpecId,
        descItemObjectId,
    ) {
        // Not implemented
    }

    // @Override
    _callUnsetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        outputItemTypeId,
        outputItemSpecId,
        outputItemObjectId,
    ) {
        // Not implemented
    }

    // @Override
    _getParentObjIdInfo(parentObjStore, routingKey) {
        return {parentId: parentObjStore.id, parentVersion: parentObjStore.version};
    }
}

export const structureFormActions = new StructureFormActions();
