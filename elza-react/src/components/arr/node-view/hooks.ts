import { DataStructureRef, DataType, NodeItem } from "elza-api";
import { useMemo } from "react";
import { FormItem } from "../item-form/formItems";

function isEmptyStructuredItem(item: NodeItem) {
  const isStructured = item.data?.dataType === DataType.Structured;
  if (!isStructured || item.undefined) {
    return false;
  }
  const { value, complement } = item.data as DataStructureRef;
  return !value && !complement;
}

/**
 * Read-only views leave out structured items that would render nothing: value and complement
 * arrive inline with the item, so emptiness is known without loading the structured object.
 * Filtering before the form is grouped also removes the item type and group labels left behind.
 */
export function useVisibleFormItems(formItems: FormItem[] | undefined) {
  return useMemo(
    () => formItems?.filter(({ item }) => !isEmptyStructuredItem(item)),
    [formItems],
  );
}
