import { FondsFilterField } from "elza-api";
import { defineMessages } from "react-intl";

export const messages = defineMessages({
  [FondsFilterField.InstitutionCode]: {
    id: "fund_filter_institutionId",
    defaultMessage: "Instituce"
  },
  [FondsFilterField.FundNumber]: {
    id: "fund_filter_fundNumber",
    defaultMessage: "Cislo AS"
  },
  [FondsFilterField.InternalCode]: {
    id: "fund_filter_internalCode",
    defaultMessage: "Interni kod"
  }
})
