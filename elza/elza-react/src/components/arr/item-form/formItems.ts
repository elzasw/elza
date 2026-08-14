import { RulDataTypeVO } from "api/RulDataTypeVO";
import {
  DataType,
  FormItemType,
  MandatoryType,
  NodeItem,
} from "elza-api";
import { useEffect } from "react";
import { DescItemTypeRef } from "typings/store";
import { createEmptyDescItem } from "./desc-items/utils";
import { EditItem } from "./types";

export interface FormItem {
  item: EditItem;
  localId: string;
  // When set, the field renders this string read-only instead of the item's value.
  forcedDisplayString?: string;
}

/**
 * Generates empty placeholder desc items so the form always shows input fields
 * for item types that the user is expected or allowed to fill in.
 *
 * Rules by mandatory level:
 *
 * Required / Recommended types:
 *   - With specification:
 *       - Repeatable:     empty item per Required/Recommended spec missing a value
 *       - Non-repeatable: empty item per Required/Recommended/Possible spec missing a value
 *   - Without specification: empty item if no value exists
 *
 * Possible types (non-repeatable only):
 *   - With or without specification: empty item only when inherited value is inhibited
 *
 * In all cases an inherited-and-inhibited value is treated as "effectively empty"
 * — the parent's value is suppressed, so the user needs a fresh input slot.
 */
export function getForcedItemTypes(
  descItems: NodeItem[] = [],
  itemTypes: FormItemType[],
  itemTypeRefs: Record<number, DescItemTypeRef>,
  dataTypeRefs: Record<number, RulDataTypeVO>,
  nodeId: number,
  nodeVersionId: number,
  { skipForcedItems }: { skipForcedItems: boolean },
) {
  const forcedDescItems: NodeItem[] = [];

  if (skipForcedItems) {
    return forcedDescItems;
  }

  // An item counts as "effectively empty" when it comes from a parent node
  // and its value has been inhibited (suppressed) on the current node
  const isInheritedAndInhibited = (item: NodeItem) =>
    item.nodeId !== nodeId && !!item.inhibited;

  itemTypes.forEach(({ itemTypeId, type, repeatable, specs = [] }) => {
    const itemTypeRef = itemTypeRefs[itemTypeId];
    const dataType = dataTypeRefs[itemTypeRef.dataTypeId];

    const isRequiredOrRecommended =
      type === MandatoryType.Required || type === MandatoryType.Recommended;
    const isPossible = type === MandatoryType.Possible;

    // Only process Required, Recommended, and Possible types
    if (!isRequiredOrRecommended && !isPossible) return;

    // Possible types only get forced items when non-repeatable
    if (isPossible && repeatable) return;

    const existingItemsOfType = descItems.filter(
      ({ itemTypeId: existingItemTypeId }) => existingItemTypeId === itemTypeId,
    );
    const existingItemCount = existingItemsOfType.length;

    // TODO Hotfix - enum types are excluded from spec-based prefilling because
    // the prefilled value appears saved in UI but is not actually persisted
    const useSpecification = itemTypeRef.useSpecification && dataType.code !== DataType.Enum;

    if (useSpecification) {
      // Determine which specs need a forced empty item:
      // - Required/Recommended type + repeatable:     only Required/Recommended specs
      // - Required/Recommended type + non-repeatable:  Required/Recommended/Possible specs
      // - Possible type (always non-repeatable here):  all specs
      const specsToProcess = isRequiredOrRecommended
        ? specs.filter(
            ({ type: specType }) =>
              specType === MandatoryType.Required ||
              specType === MandatoryType.Recommended ||
              (!repeatable && specType === MandatoryType.Possible),
          )
        : specs;

      specsToProcess.forEach(({ itemSpecId }) => {
        const existingItemsOfSpec = existingItemsOfType.filter(
          ({ itemSpecId: existingItemSpecId }) => existingItemSpecId === itemSpecId,
        );
        const specHasValue = existingItemsOfSpec.length > 0;
        const allSpecItemsInheritedAndInhibited =
          specHasValue && existingItemsOfSpec.every(isInheritedAndInhibited);

        // Required/Recommended: add when missing or effectively empty
        // Possible: add only when effectively empty (inherited + inhibited)
        const shouldAdd = isRequiredOrRecommended
          ? !specHasValue || allSpecItemsInheritedAndInhibited
          : allSpecItemsInheritedAndInhibited;

        if (shouldAdd) {
          forcedDescItems.push({
            ...createEmptyDescItem(itemTypeId, nodeId, nodeVersionId, existingItemCount, dataType.code),
            itemSpecId,
          });
        }
      });

      // Required/Recommended: if no specs qualified for processing, still add an empty item
      if (isRequiredOrRecommended && specsToProcess.length === 0 && existingItemCount === 0) {
        forcedDescItems.push(
          createEmptyDescItem(itemTypeId, nodeId, nodeVersionId, existingItemCount, dataType.code),
        );
      }
    } else {
      const typeHasValue = existingItemsOfType.length > 0;
      const allTypeItemsInheritedAndInhibited =
        typeHasValue && existingItemsOfType.every(isInheritedAndInhibited);

      // Required/Recommended: add when missing or effectively empty
      // Possible (non-repeatable): add only when effectively empty
      const shouldAdd =
        (isRequiredOrRecommended && (!typeHasValue || allTypeItemsInheritedAndInhibited)) ||
        (isPossible && allTypeItemsInheritedAndInhibited);

      if (shouldAdd) {
        forcedDescItems.push(
          createEmptyDescItem(itemTypeId, nodeId, nodeVersionId, existingItemCount, dataType.code),
        );
      }
    }
  });

  return forcedDescItems;
}

// Generator stabilnich klicu prvku popisu s moznosti naparovat existujici
// novy prvek popisu na prvek popisu vytvoreny na serveru po ulozeni
let counter = 0;

export function useKeyGen(nodeId: number) {
  useEffect(() => {
    counter = 0;
  }, [nodeId]);

  function getKey() {
    const key = `desc-item-${counter}`;
    counter++;
    return key;
  }

  return { getKey };
}
