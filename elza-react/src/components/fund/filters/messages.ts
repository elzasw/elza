import { FondsFieldName, OperationCompareType } from "elza-api";
import { defineMessages } from "react-intl";

export const messages = defineMessages({
  [FondsFieldName.InstitutionCode]: {
    id: "fund_filter_institutionId",
    defaultMessage: "Instituce"
  },
  [FondsFieldName.InstitutionId]: {
    id: "fund_filter_institutionId",
    defaultMessage: "Instituce"
  },
  [FondsFieldName.FondsNumber]: {
    id: "fund_filter_fundNumber",
    defaultMessage: "Číslo AS"
  },
  [FondsFieldName.Mark]: {
    id: "fund_filter_mark",
    defaultMessage: "Značka AS"
  },
  [FondsFieldName.InternalCode]: {
    id: "fund_filter_internalCode",
    defaultMessage: "Interní kód"
  },
  [FondsFieldName.Name]: {
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
