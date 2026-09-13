import { defineMessages } from "react-intl";

/**
 * Společná slovní zásoba UI - jedno znění pro jeden pojem.
 *
 * Při migraci z legacy helperu sahej **nejdřív sem**. Legacy katalog má pro
 * tytéž pojmy víc klíčů s různým zněním (`global.action.cancel` = "Storno",
 * `global_cancel` = "Storno", ale react-intl deskriptor "Zrušit"), a bez
 * společného místa by se ta roztříštěnost jen rozkopírovala dál.
 *
 * Terminologie: držíme běžné termíny, které uživatel zná z ostatních aplikací
 * (Windows, Office, Android), ne interní žargon - UI má být srozumitelné bez
 * dokumentace. Proto "Zrušit", ne "Storno": *storno* v češtině znamená
 * účetní stornování dokladu, ne zavření dialogu.
 */
export const globalMessages = defineMessages({
  copyToClipboard: {
    id: "global_copyToClipboard",
    defaultMessage: "Zkopírovat do schránky",
  },
  copyToClipboardFinished: {
    id: "global_copyToClipboard_finished",
    defaultMessage: "Zkopírováno do schránky",
  },
  copyToClipboardUnavailable: {
    id: "global_copyToClipboard_unavailable",
    defaultMessage:
      "Zkopírování do schránky není dostupné. Aplikace pravděpodobně neběží v zabezpečeném režimu (https).",
  },
  ok: {
    id: "global_ok",
    defaultMessage: "Ok",
  },
  cancel: {
    id: "global_cancel",
    defaultMessage: "Zrušit",
  },
  save: {
    id: "global.action.save",
    defaultMessage: "Uložit",
  },
  // Záměrně oddělené od `save`: legacy klíč `global.action.update` ("Upravit")
  // se používá pro obojí - jako popisek submit tlačítka v režimu úprav i jako
  // tooltip ikony tužky. To jsou dvě různé akce a jedno slovo pro obě dělá UI
  // méně srozumitelné, takže se při migraci vybírá podle významu.
  edit: {
    id: "global.action.edit",
    defaultMessage: "Upravit",
  },
  yes: {
    id: "global_yes",
    defaultMessage: "Ano",
  },
  no: {
    id: "global_no",
    defaultMessage: "Ne",
  },
  undefined: {
    id: "global_undefined",
    defaultMessage: "Výjimka",
  },
  close: {
    id: "global.action.close",
    defaultMessage: "Zavřít",
  },
  showInMap: {
    id: "global.action.showInMap",
    defaultMessage: "Zobrazit v mapě",
  },
  editInMap: {
    id: "global.action.editInMap",
    defaultMessage: "Upravit v mapě",
  },
});
