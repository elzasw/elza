import { FormItemType } from "elza-api";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import { FormItem } from "./formItems";
import { ViewDescItemGroupsLocal } from "./types";

export function buildGroupsForm(
  descItems: FormItem[],
  itemTypes: FormItemType[],
  groupRefs: Record<number, DescItemGroup> & { ids: string[] },
  itemTypeRefs: Record<number, DescItemTypeRef>,
) {
  if (!groupRefs) {
    return [];
  }
  const _descItems: FormItem[] = [...descItems];
  const descItemGroups: Array<ViewDescItemGroupsLocal> = [];

  _descItems.forEach((localItem) => {
    let groupRef: DescItemGroup = undefined;
    let typeWidth: number = undefined;

    // find group and item type width in group refs
    for (const id of groupRefs.ids) {
      const _group = groupRefs[id];
      for (const itemType of _group.itemTypes) {
        if (itemType.id === localItem.item.itemTypeId) {
          typeWidth = itemType.width;
          groupRef = _group;
          break;
        }
      }
      if (groupRef) {
        break;
      }
    }

    const typeRef = itemTypeRefs[localItem.item.itemTypeId];
    const typeForm = itemTypes.find(
      ({ itemTypeId }) => itemTypeId === localItem.item.itemTypeId,
    );

    const existingGroup = descItemGroups.find(
      (groupDef) => groupDef.group.code === groupRef.code,
    );
    if (!existingGroup) {
      descItemGroups.push({
        group: groupRef,
        descItemTypes: [
          {
            typeRef,
            typeForm,
            typeWidth,
            descItems: [localItem],
          },
        ],
      });
    } else {
      const existingType = existingGroup.descItemTypes.find(
        (typeDef) => typeDef.typeRef.id === localItem.item.itemTypeId,
      );
      if (!existingType) {
        existingGroup.descItemTypes.push({
          typeRef,
          typeForm,
          typeWidth,
          descItems: [localItem],
        });
      } else {
        existingType.descItems.push(localItem);
      }
    }
  });

  descItemGroups.sort(
    (a, b) =>
      groupRefs.ids.indexOf(a.group.code) - groupRefs.ids.indexOf(b.group.code),
  );
  descItemGroups.forEach(({ descItemTypes }) =>
    descItemTypes.sort((a, b) => a.typeRef.viewOrder - b.typeRef.viewOrder),
  );

  return descItemGroups;
}
