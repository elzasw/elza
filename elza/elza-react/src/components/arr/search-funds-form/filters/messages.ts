import { FundsFieldName, OperationCompareType } from "elza-api";
import { defineMessages } from "react-intl";

export const messages = defineMessages({
  [FundsFieldName.InstitutionCode]: {
    id: "fund_filter_institutionId",
    defaultMessage: "Instituce"
  },
  [FundsFieldName.FundNumber]: {
    id: "fund_filter_fundNumber",
    defaultMessage: "Číslo AS"
  },
  [FundsFieldName.Mark]: {
    id: "fund_filter_mark",
    defaultMessage: "Značka AS"
  },
  [FundsFieldName.InternalCode]: {
    id: "fund_filter_internalCode",
    defaultMessage: "Interní kód"
  },
  [FundsFieldName.Name]: {
    id: "fund_filter_name",
    defaultMessage: "Název"
  },
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
  }
})
