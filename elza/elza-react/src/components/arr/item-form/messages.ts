import { MessageDescriptor, defineMessages } from "react-intl";
import { RulDataTypeCodeEnum } from "api/RulDataTypeCodeEnum";

export const messages = defineMessages({
  copyFromPrev: {
    id: "desc_item_action_copyFromPrev",
    defaultMessage: "Kopírovat hodnoty PP z předchozí JP",
  },
  copyToggle: {
    id: "desc_item_action_copyToggle",
    defaultMessage: "Nastavení opakovaného kopírování hodnot PP",
  },
  openInDataGrid: {
    id: "descItemType.action.openInDataGrid",
    defaultMessage: "Zobrazit v tabulce",
  },
  copyValues: {
    id: "descItemType.action.copyValues",
    defaultMessage: "Kopírovat hodnoty PP pro vložení do jiné JP (Ctrl+kliknutí je přidá k již zkopírovaným)",
  },
  pasteValues: {
    id: "descItemType.action.pasteValues",
    defaultMessage: "Vložit zkopírované hodnoty PP",
  },
  addDescItem: {
    id: "node_action_addDescItem",
    defaultMessage: "Prvek popisu",
  },
  addDescItemTitle: {
    id: "subNodeForm.descItemType.title.add",
    defaultMessage: "Přidat prvek popisu",
  },
  calculateSwitchToAuto: {
    id: "itemForm.calculate.switchToAuto",
    defaultMessage: "Pole je vyplňováno uživatelsky, přepnout na automatické",
  },
  calculateSwitchToManual: {
    id: "itemForm.calculate.switchToManual",
    defaultMessage: "Pole je vyplňováno automaticky, přepnout na uživatelské",
  },
  calculatedPlaceholder: {
    id: "itemForm.calculate.placeholder",
    defaultMessage: "Hodnota počítána funkcí",
  },
});

export const dataTypeFormatMessages: Partial<Record<RulDataTypeCodeEnum, MessageDescriptor>> = defineMessages({
  [RulDataTypeCodeEnum.COORDINATES]: {
    id: "dataType.COORDINATES.format",
    defaultMessage:
      "<p><b>Načtení souřadnic ze souboru</b> ve formátu KML, GML nebo WKT</p>" +
      "<p><b>Systém WGS84</b> (např. Mapy.cz)</p>" +
      "<p><i>příklad: 49.5765442N, 14.3965617E</i></p>" +
      "<p><b>Značkovací jazyk WKT</b></p>" +
      "<p><i>příklady: POINT (14.3965617 49.5765442)</i></p>" +
      "<p><i>LINESTRING (14.3965528 49.5765909,14.4172300 49.5551484)</i></p>" +
      "<p><i>POLYGON ((14.3828494 49.5976066,14.3829031 49.5971094,14.3842817 49.5971546,14.3842281 49.5976379,14.3828494 49.5976066))</i></p>",
  },
  // the examples are placeholders filled from components/shared/unitdate/examples by UI language
  [RulDataTypeCodeEnum.UNITDATE]: {
    id: "dataType.UNITDATE.format",
    defaultMessage:
      "<p><b>Formát datace</b></p>" +
      "<p>Století: {century}</p>" +
      "<p>Rok: {year}</p>" +
      "<p>Měsíc: {month}</p>" +
      "<p>Den: {day}</p>" +
      "<p>Hodiny, minuty, sekundy: {time}</p>" +
      "<p><b>Intervaly</b></p>" +
      "<p>Roky: {intervalYears}</p>" +
      "<p>Kombinace: {intervalCombined}</p>" +
      "<p><b>Odhad</b></p>" +
      "<p>Definuje se uzavřením hodnoty do kulatých nebo hranatých závorek:</p>" +
      "<p>Např.: {estimateBrackets}</p>" +
      "<p>Při použití znaku '/' pro oddělení intervalu jsou od i do chápány jako odhad:</p>" +
      "<p>Např.: {estimateSlash}</p>",
  },
});
