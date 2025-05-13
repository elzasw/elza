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
})
