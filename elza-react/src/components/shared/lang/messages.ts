import { defineMessages } from "react-intl";

/**
 * Společná slovní zásoba UI - jedno znění pro jeden pojem.
 *
 * Při migraci z legacy helperu sahej **nejdřív sem**. Legacy katalog měl pro
 * tytéž pojmy víc klíčů s různým zněním - `global.action.update` je dodnes
 * "Upravit", zatímco react-intl deskriptor téhož id říkal "Uložit" - a bez
 * společného místa by se ta roztříštěnost jen rozkopírovala dál.
 * **Před převzetím legacy id proto vždy porovnej český text s deskriptorem.**
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
      "Zkopírování do schránky se nezdařilo. Označte hodnotu a zkopírujte ji ručně (Ctrl+C).",
  },
  ok: {
    id: "global_ok",
    defaultMessage: "Ok",
  },
  cancel: {
    id: "global_cancel",
    defaultMessage: "Zrušit",
  },
  create: {
    id: "global.action.create",
    defaultMessage: "Vytvořit",
  },
  add: {
    id: "global.action.add",
    defaultMessage: "Přidat",
  },
  remove: {
    id: "global.action.remove",
    defaultMessage: "Odebrat",
  },
  replace: {
    id: "global.action.replace",
    defaultMessage: "Nahradit",
  },
  change: {
    id: "global.action.change",
    defaultMessage: "Změnit",
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
  delete: {
    id: "global.action.delete",
    defaultMessage: "Odstranit",
  },
  download: {
    id: "global.action.download",
    defaultMessage: "Stáhnout",
  },
  /** Pozor: "Exportovat" je jiná akce než download ("Stáhnout"). */
  export: {
    id: "global.action.export",
    defaultMessage: "Exportovat",
  },
  run: {
    id: "global.action.run",
    defaultMessage: "Spustit",
  },
  choose: {
    id: "global.action.choose",
    defaultMessage: "Vybrat",
  },
  merge: {
    id: "global.action.merge",
    defaultMessage: "Sloučit",
  },
  search: {
    id: "global.action.search",
    defaultMessage: "Vyhledat",
  },
  filterClean: {
    id: "global.action.filter.clean",
    defaultMessage: "Zrušit filtr",
  },
  use: {
    id: "global.action.use",
    defaultMessage: "Použít",
  },
  write: {
    id: "global.action.write",
    defaultMessage: "Zapsat",
  },
  // Nejčastější text v celé aplikaci (přes 60 volání) - proto sem.
  validationRequired: {
    id: "global.validation.required",
    defaultMessage: "Pole je povinné",
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
    confirm: { id: 'global.action.confirm', defaultMessage: 'Potvrdit' },
    /** Popisky kódování v importních dialozích. */
    encodingCentralEuropean: { id: 'global.encoding.centralEuropean', defaultMessage: 'Windows-1250 (středoevropské)' },
    encodingWesternEuropean: { id: 'global.encoding.westernEuropean', defaultMessage: 'Windows-1252 (západoevropské)' },
    selectAll: { id: 'global.action.selectAll', defaultMessage: 'Vybrat vše' },
    selectedCount: { id: 'global.title.selectedCount', defaultMessage: 'Vybráno: {count}' },
});
