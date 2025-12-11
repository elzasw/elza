import { RulDataTypeVO } from "api/RulDataTypeVO";
import { NodeFormData, MandatoryType, DataType, NodeItem } from "elza-api";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import { createEmptyDescItem } from "./desc-items/utils";
import { ViewDescItemGroups } from "./types";

export function buildGroups(
  { descItems, itemTypes }: NodeFormData,
  groupRefs: Record<number, DescItemGroup> & { ids: string[] },
  itemTypeRefs: Record<number, DescItemTypeRef>,
  dataTypeRefs: Record<number, RulDataTypeVO>,
  nodeId: number,
  nodeVersionId: number,
  skipForcedItems?: boolean,
) {
  if (!groupRefs) {
    return [];
  }

  const _descItems: NodeItem[] = [...descItems];

  const forcedItemTypes = skipForcedItems
    ? []
    : itemTypes.filter(
        ({ type }) =>
          type === MandatoryType.Required || type === MandatoryType.Recommended,
      );

  forcedItemTypes.forEach(({ itemTypeId, specs, repeatable }) => {
    const itemTypeRef = itemTypeRefs[itemTypeId];
    const dataType = dataTypeRefs[itemTypeRef.dataTypeId];

    const forcedSpecs = specs.filter(
      ({ type }) =>
        type === MandatoryType.Required || type === MandatoryType.Recommended,
    );

    const _descItem = descItems.find(
      ({ itemTypeId: _itemTypeId }) => itemTypeId === _itemTypeId,
    );
    const addSpec =
      _descItem &&
      (itemTypeRef.useSpecification || repeatable) &&
      dataType.code !== DataType.Enum;

    const descItemTypeCount = descItems.filter(
      ({ itemTypeId: _itemTypeId }) => _itemTypeId === itemTypeId,
    );

    if (forcedSpecs.length > 0 && addSpec) {
      forcedSpecs.forEach(({ itemSpecId }) => {
        const descItem = descItems.find(
          ({ itemTypeId: _itemTypeId, itemSpecId: _itemSpecId }) =>
            _itemTypeId === itemTypeId && _itemSpecId === itemSpecId,
        );
        if (!descItem) {
          _descItems.push({
            ...createEmptyDescItem(
              itemTypeId,
              nodeId,
              nodeVersionId,
              descItemTypeCount.length,
              dataType.code,
            ),
            itemSpecId,
          });
        }
      });
    } else {
      const descItem = descItems.find(
        ({ itemTypeId: _itemTypeId }) => _itemTypeId === itemTypeId,
      );
      if (!descItem) {
        _descItems.push(
          createEmptyDescItem(
            itemTypeId,
            nodeId,
            nodeVersionId,
            descItemTypeCount.length,
            dataType.code,
          ),
        );
      }
    }
  });

  const descItemGroups: Array<ViewDescItemGroups> = [];

  _descItems.forEach((item) => {
    let groupRef: DescItemGroup = undefined;
    let typeWidth: number = undefined;

    // find group and item type width in group refs
    for (const id of groupRefs.ids) {
      const _group = groupRefs[id];
      for (const itemType of _group.itemTypes) {
        if (itemType.id === item.itemTypeId) {
          typeWidth = itemType.width;
          groupRef = _group;
          break;
        }
      }
      if (groupRef) {
        break;
      }
    }

    const typeRef = itemTypeRefs[item.itemTypeId];
    const typeForm = itemTypes.find(
      ({ itemTypeId }) => itemTypeId === item.itemTypeId,
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
            descItems: [item],
          },
        ],
      });
    } else {
      const existingType = existingGroup.descItemTypes.find(
        (typeDef) => typeDef.typeRef.id === item.itemTypeId,
      );
      if (!existingType) {
        existingGroup.descItemTypes.push({
          typeRef,
          typeForm,
          typeWidth,
          descItems: [item],
        });
      } else {
        existingType.descItems.push(item);
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
