import { FormItemType, NodeItem } from 'elza-api';
import { mergeItemsIntoNode } from '../templates/mergeItems';
import { ClipboardItem } from './itemClipboard';
import { planPaste } from './pasteRules';
import { GetItemTypeInfo, selectItemsForType } from './remap';

interface PreparePasteParams {
    clipboardItems: ClipboardItem[];
    sourceFundId: number;
    /** The target node's form items; inherited ones are left out here. */
    descItems: NodeItem[];
    itemTypes: FormItemType[];
    nodeId: number;
    targetFundId: number;
    /** Pastes only into this DescItemType (remapping STRING/TEXT when allowed); all items into their own types when omitted. */
    itemTypeId?: number;
    getItemTypeInfo: GetItemTypeInfo;
}

/** Works out what a paste would do: the rule outcome and the positioned items to create. */
export function preparePaste({
    clipboardItems,
    sourceFundId,
    descItems,
    itemTypes,
    nodeId,
    targetFundId,
    itemTypeId,
    getItemTypeInfo,
}: PreparePasteParams) {
    const incomingItems =
        itemTypeId == undefined ? clipboardItems : selectItemsForType(clipboardItems, itemTypeId, getItemTypeInfo);
    const ownItems = descItems.filter((item) => item.nodeId === nodeId);
    const plan = planPaste({ ownItems, incomingItems, itemTypes, sourceFundId, targetFundId });
    const { createItems } = mergeItemsIntoNode(ownItems, plan.items, { replace: false });
    return { plan, createItems };
}

/** The candidate types a paste into a single DescItemType would add at least one value to. */
export function getPastableTypeIds(
    params: Omit<PreparePasteParams, 'itemTypeId'> & { candidateTypeIds: number[] },
): number[] {
    const { candidateTypeIds, ...pasteParams } = params;
    return candidateTypeIds.filter(
        (itemTypeId) => preparePaste({ ...pasteParams, itemTypeId }).createItems.length > 0,
    );
}
