import { FondsFieldName, NodeFieldName, OperationCompareType } from "elza-api";
import { defineMessages } from "react-intl";

export const messages = defineMessages({
  [OperationCompareType.Eq]: {
    id: "fund_filter_operation_eq",
    defaultMessage: "Je"
  },
  [OperationCompareType.Neq]: {
    id: "fund_filter_operation_neq",
    defaultMessage: "Není"
  },
  [OperationCompareType.Contains]: {
    id: "fund_filter_operation_contains",
    defaultMessage: "Obsahuje"
  },
  [OperationCompareType.Gt]: {
    id: "fund_filter_operation_greater_than",
    defaultMessage: "Větší než"
  },
  [OperationCompareType.Lt]: {
    id: "fund_filter_operation_lower_than",
    defaultMessage: "Menší než"
  },
  [OperationCompareType.Gte]: {
    id: "fund_filter_operation_greater_than_equal",
    defaultMessage: "Větší nebo rovno"
  },
  [OperationCompareType.Lte]: {
    id: "fund_filter_operation_lower_than_equal",
    defaultMessage: "Menší nebo rovno"
  },
  [OperationCompareType.IsNull]: {
    id: "fund_filter_operation_is_null",
    defaultMessage: "Nevyplněn"
  },
  [OperationCompareType.NotNull]: {
    id: "fund_filter_operation_not_null",
    defaultMessage: "Vyplněn"
  },
  [OperationCompareType.Intersect]: {
    id: "fund_filter_operation_intersect",
    defaultMessage: "Spadá částečně do"
  },
  [OperationCompareType.IsIn]: {
    id: "fund_filter_operation_is_in",
    defaultMessage: "Spadá zcela do"
  },
  "DescItem": {
    id: "search_funds_form_desc_item",
    defaultMessage: "Prvek popisu",
  },
  [NodeFieldName.Uuid]: {
    id: "search_funds_form_uuid",
    defaultMessage: "UUID",
  },
  [NodeFieldName.ConformityMissing]: {
    id: "search_funds_form_conformity_missing",
    defaultMessage: "Chybějící prvek",
  },
  [NodeFieldName.ConformityError]: {
    id: "search_funds_form_conformity_error",
    defaultMessage: "Chyba",
  },
  [FondsFieldName.InstitutionId]: {
    id: "search_funds_form_institution",
    defaultMessage: "Instituce",
  },
  [FondsFieldName.FondsId]: {
    id: "search_funds_form_fonds",
    defaultMessage: "Archivní soubor",
  },
  filter_confirm: {
    id: "fund_filter_confirm",
    defaultMessage: "Potvrdit",
  },
  filter_add_value: {
    id: "fund_filter_add_value",
    defaultMessage: "Nebo",
  },
  filter_or: {
    id: "fund_filter_or",
    defaultMessage: "NEBO",
  },
})
