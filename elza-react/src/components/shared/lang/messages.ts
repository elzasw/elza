import { defineMessages } from "react-intl";

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
    defaultMessage: "Storno",
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
});
