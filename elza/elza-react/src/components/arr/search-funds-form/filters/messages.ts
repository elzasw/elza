import { NodeFieldName, OperationCompareType } from "elza-api";
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
    defaultMessage: "Chybějící",
  },
  [NodeFieldName.ConformityError]: {
    id: "search_funds_form_conformity_error",
    defaultMessage: "Chyba",
  },
})
