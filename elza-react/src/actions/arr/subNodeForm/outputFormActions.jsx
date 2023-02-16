/**
 * Akce pro formulař výstupů.
 */

import { WebApi } from 'actions/index';
import { indexById } from 'stores/app/utils';
import { outputIncreaseNodeVersion } from '../outputActions';
import { ItemFormActions } from './itemFormActions';

export class OutputFormActions extends ItemFormActions {
    static AREA = 'OUTPUT';

    constructor() {
        super(OutputFormActions.AREA);
    }

    /** Načtení server dat pro formulář pro aktuálně předané parametry BEZ využití cache. */
    //@Override
    _getItemFormData(getState, dispatch, versionId, nodeId, routingKey) {
        // není podpora kešování
        return WebApi.getOutputNodeForm(versionId, nodeId);
    }

    // @Override
    _getItemFormStore(state, versionId, routingKey) {
        const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
        if (fundIndex !== null) {
            return state.arrRegion.funds[fundIndex].fundOutput.fundOutputDetail.subNodeForm;
        } else {
            return null;
        }
    }

    // @Override
    _getParentObjStore(state, versionId, routingKey) {
        const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
        if (fundIndex !== null) {
            const fund = state.arrRegion.funds[fundIndex];
            return fund.fundOutput.fundOutputDetail;
        } else {
            return null;
        }
    }

    // @Override
    _callUpdateDescItem(dispatch, formState, fundVersionId, outputVersionId, outputId, descItem) {
        // increase output version
        dispatch(outputIncreaseNodeVersion(fundVersionId, outputId, outputVersionId));

        return WebApi.updateOutputItem(fundVersionId, outputVersionId, descItem);
    }

    // @Override
    _callDeleteDescItem(versionId, parentId, parentVersionId, descItem) {
        return WebApi.deleteOutputItem(versionId, parentId, parentVersionId, descItem.descItemObjectId);
    }

    // @Override
    _callCreateDescItem(versionId, parentId, nodeVersionId, descItemTypeId, descItem) {
        return WebApi.createOutputItem(versionId, parentId, nodeVersionId, descItemTypeId, descItem);
    }

    // @Override
    _callArrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        return WebApi.arrOutputCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDescItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        return WebApi.descOutputItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDeleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId) {
        return WebApi.deleteOutputItemType(versionId, parentId, parentVersionId, descItemTypeId);
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
        return WebApi.setNotIdentifiedOutputItem(
            versionId,
            nodeId,
            parentNodeVersion,
            descItemTypeId,
            descItemSpecId,
            descItemObjectId,
        );
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
        return WebApi.unsetNotIdentifiedOutputItem(
            versionId,
            nodeId,
            parentNodeVersion,
            outputItemTypeId,
            outputItemSpecId,
            outputItemObjectId,
        );
    }

    // @Override
    _getParentObjIdInfo(parentObjStore, routingKey) {
        return {parentId: parentObjStore.id, parentVersion: parentObjStore.version};
    }
}

export const outputFormActions = new OutputFormActions();
