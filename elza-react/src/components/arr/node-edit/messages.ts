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
  addDescItem: {
    id: "node_action_addDescItem",
    defaultMessage: "Prvek popisu",
  },
  addDescItemTitle: {
    id: "subNodeForm.descItemType.title.add",
    defaultMessage: "Přidat prvek popisu",
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
  [RulDataTypeCodeEnum.UNITDATE]: {
    id: "dataType.UNITDATE.format",
    defaultMessage:
      "<p><b>Formát datace</b></p>" +
      "<p>Století: 20. st. <i>nebo</i> 20.st. <i>nebo</i> 20st</p>" +
      "<p>Rok: 1968</p>" +
      "<p>Měsíc: 8.1968</p>" +
      "<p>Den: 21.8.1968</p>" +
      "<p>Hodiny, minuty, sekundy: 21.8.1968 2:43 <i>nebo</i> 21.8.1968 8:23:31</p>" +
      "<p><b>Intervaly</b></p>" +
      "<p>Roky: 1968-1969</p>" +
      "<p>Kombinace: 8.1968-1969 <i>nebo</i> 21.8.1968 2:43-27.6.1989</p>" +
      "<p><b>Odhad</b></p>" +
      "<p>Definuje se uzavřením hodnoty do kulatých nebo hranatých závorek:</p>" +
      "<p>Např.: [16.8.1977] <i>nebo</i> [1990]-1992</p>" +
      "<p>Při použití znaku '/' pro oddělení intervalu jsou od i do chápány jako odhad:</p>" +
      "<p>Např.: 1985/1990</p>",
  },
});
