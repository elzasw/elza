import {AeState} from "./generated/model";
//import {IconProp} from "@fortawesome/fontawesome-svg-core";
/*import {
  faArrowRight, faArrowUp, faBan, faCheck,
  faFileDownload, faPlus,
  faQuestionCircle, faSyncAlt, faTimes,
} from "@fortawesome/free-solid-svg-icons";*/

export function getLabel(value: AeState) {
  switch (value) {
    case AeState.APSAPPROVED:
      return "Schválená";
    case AeState.APSINVALID:
      return "Neplatná";
    case AeState.APSNEW:
      return "Nová";
    case AeState.APSREPLACED:
      return "Nahrazená";
    case AeState.RLCSINVALID:
      return "Zrušená změna";
    case AeState.RLCSNEW:
      return "Nově zakládaná nebo upravovaná";
    case AeState.RLCSTOAMEND:
      return "Vrácená ze schvalování k doplnění";
    case AeState.RLCSTOAPPROVE:
      return "Upravovaná předávaná ke schválení";
    case AeState.RLCSSAVED:
      return "Uložená v jádru";
    default:
      console.warn("Nepřeložená hodnota", value);
      return "?";
  }
}

/**
 * Seřazené dle abecedy podle getLabel.
 */
export function getLocalValuesSorted(): Array<AeState> {
  return getLocalValues().sort((a, b) => getLabel(a).localeCompare(getLabel(b)));
}

export function getLocalValues(): Array<AeState> {
  return [
    AeState.RLCSINVALID,
    AeState.RLCSNEW,
    AeState.RLCSTOAMEND,
    AeState.RLCSTOAPPROVE,
    AeState.RLCSSAVED,
  ];
}

/**
 * Seřazené dle abecedy podle getLabel.
 */
export function getGlobalValuesSorted(): Array<AeState> {
  return getGlobalValues().sort((a, b) => getLabel(a).localeCompare(getLabel(b)));
}

export function getGlobalValues(): Array<AeState> {
  return [
    AeState.APSAPPROVED,
    AeState.APSINVALID,
    AeState.APSNEW,
    AeState.APSREPLACED
  ];
}

export function getIcon(value: AeState): string {
  switch (value) {
    case AeState.APSAPPROVED:
      return 'fa-check';
    case AeState.APSINVALID:
      return 'fa-ban';
    case AeState.APSNEW:
      return 'fa-plus';
    case AeState.APSREPLACED:
      return 'fa-sync-alt';
    case AeState.RLCSINVALID:
      return 'fa-times';
    case AeState.RLCSNEW:
      return 'fa-plus';
    case AeState.RLCSTOAMEND:
      return 'fa-arrow-right';
    case AeState.RLCSTOAPPROVE:
      return 'fa-arrow-up';
    case AeState.RLCSSAVED:
      return 'fa-file-download';
    default:
      console.warn("Nedefinovaná ikona hodnota", value);
      return 'fa-question-circle';
  }
}

export function getColor(value: AeState): string {
  switch (value) {
    case AeState.APSAPPROVED:
    case AeState.APSINVALID:
    case AeState.APSNEW:
    case AeState.APSREPLACED:
      return "#317E9F";
    case AeState.RLCSINVALID:
    case AeState.RLCSNEW:
    case AeState.RLCSTOAMEND:
    case AeState.RLCSTOAPPROVE:
    case AeState.RLCSSAVED:
      return "#EBA960";
    default:
      console.warn("Nedefinovaná barva", value);
      return "";
  }
}
