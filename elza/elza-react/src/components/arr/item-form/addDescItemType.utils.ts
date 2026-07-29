import { FormItemType, MandatoryType } from "elza-api";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import { EditItem } from "./types";

type DescItemGroupsMap = Record<string, DescItemGroup> & { ids: string[] };

export type AvailableItemType = DescItemTypeRef & { className: string };

/**
 * Resolve which desc item types the "add item type" form offers in the autocomplete.
 *
 * The server omits impossible types from itemTypes (impossible is the default state, so it
 * isn't transferred). A type missing from itemTypes is therefore impossible for this node; in
 * non-strict mode it is still offered, but only when its code belongs to the rule set (i.e. it
 * is node-compatible rather than an output/structured/global-only type).
 *
 * Already-added types are excluded. Each returned type carries a className encoding its
 * mandatory type plus a `queued` marker when it is in the queue.
 */
export function resolveAvailableItemTypes(
  descItemTypes: DescItemTypeRef[],
  itemTypes: FormItemType[],
  descItems: EditItem[],
  ruleSetItemTypeCodes: string[] | undefined,
  strictMode: boolean,
  queuedTypeIds: Iterable<number>,
): AvailableItemType[] {
  const queuedIds = new Set(queuedTypeIds);
  const addedIds = new Set(descItems.map(({ itemTypeId }) => itemTypeId));
  const nodeCompatibleCodes = new Set(ruleSetItemTypeCodes);
  const itemTypeById = new Map(itemTypes.map((itemType) => [itemType.itemTypeId, itemType]));

  return descItemTypes
    .filter((item) => {
      if (addedIds.has(item.id)) {
        return false;
      }
      const itemType = itemTypeById.get(item.id);
      if (itemType && itemType.type !== MandatoryType.Impossible) {
        return true;
      }
      return !strictMode && nodeCompatibleCodes.has(item.code);
    })
    .map((item) => {
      const type = itemTypeById.get(item.id)?.type ?? MandatoryType.Impossible;
      // The queued class is part of className (which ListItem compares in shouldComponentUpdate)
      // so toggling the queue reliably re-renders the row and updates its checkmark.
      const isQueued = queuedIds.has(item.id);
      const className = `type-${type.toLowerCase()}${isQueued ? " queued" : ""}`;
      return { ...item, className };
    });
}

/**
 * Map each type id to the index of the group it belongs to, in the group order used by the form
 * (buildGroupsForm). Built in one pass so the sort comparator can look the index up in O(1).
 */
function buildGroupIndexByType(groups: DescItemGroupsMap): Map<number, number> {
  const groupIndexByType = new Map<number, number>();
  groups.ids.forEach((groupId, index) => {
    groups[groupId].itemTypes.forEach((itemType) => {
      if (!groupIndexByType.has(itemType.id)) {
        groupIndexByType.set(itemType.id, index);
      }
    });
  });
  return groupIndexByType;
}

/**
 * Index of the group a type belongs to, in the group order used by the form (buildGroupsForm).
 * Types without a group sort last so the composite (group, viewOrder) order matches the rendered form.
 */
export function groupIndexOfType(groups: DescItemGroupsMap, typeId: number): number {
  return buildGroupIndexByType(groups).get(typeId) ?? groups.ids.length;
}

/**
 * Sort types into the final form order (group order, then viewOrder within a group) so the
 * auto-focus, which targets the first submitted type, lands on the topmost new field.
 * Types without a group sort last so the composite (group, viewOrder) order matches the rendered form.
 */
export function sortTypesByFormOrder(
  types: DescItemTypeRef[],
  groups: DescItemGroupsMap,
): DescItemTypeRef[] {
  const groupIndexByType = buildGroupIndexByType(groups);
  const groupIndexOf = (typeId: number) => groupIndexByType.get(typeId) ?? groups.ids.length;

  return [...types].sort((a, b) => {
    const groupDiff = groupIndexOf(a.id) - groupIndexOf(b.id);
    return groupDiff !== 0 ? groupDiff : a.viewOrder - b.viewOrder;
  });
}
