import { FondsFilterField, OperationCompareType } from "elza-api";
import { defineMessages } from "react-intl";

export const messages = defineMessages({
  [FondsFilterField.InstitutionCode]: {
    id: "fund_filter_institutionId",
    defaultMessage: "Instituce"
  },
  [FondsFilterField.FundNumber]: {
    id: "fund_filter_fundNumber",
    defaultMessage: "Číslo AS"
  },
  [FondsFilterField.Mark]: {
    id: "fund_filter_mark",
    defaultMessage: "Značka AS"
  },
  [FondsFilterField.InternalCode]: {
    id: "fund_filter_internalCode",
    defaultMessage: "Interní kód"
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
