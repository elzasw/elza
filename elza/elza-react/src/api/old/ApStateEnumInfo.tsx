import {ApState} from "../generated/model";
//import {IconProp} from "@fortawesome/fontawesome-svg-core";
/*import {
  faArrowRight, faArrowUp, faBan, faCheck,
  faFileDownload, faPlus,
  faQuestionCircle, faSyncAlt, faTimes,
} from "@fortawesome/free-solid-svg-icons";*/

export function getLabel(value: ApState) {
  switch (value) {
    case ApState.APSAPPROVED:
      return "Schválená";
    case ApState.APSINVALID:
      return "Neplatná";
    case ApState.APSNEW:
      return "Nová";
    case ApState.APSREPLACED:
      return "Nahrazená";
    case ApState.RLCSINVALID:
      return "Zrušená změna";
    case ApState.RLCSNEW:
      return "Nově zakládaná nebo upravovaná";
    case ApState.RLCSTOAMEND:
      return "Vrácená ze schvalování k doplnění";
    case ApState.RLCSTOAPPROVE:
      return "Upravovaná předávaná ke schválení";
    case ApState.RLCSSAVED:
      return "Uložená v jádru";
    default:
      console.warn("Nepřeložená hodnota", value);
      return "?";
  }
}

/**
 * Seřazené dle abecedy podle getLabel.
 */
export function getLocalValuesSorted(): Array<ApState> {
  return getLocalValues().sort((a, b) => getLabel(a).localeCompare(getLabel(b)));
}

export function getLocalValues(): Array<ApState> {
  return [
    ApState.RLCSINVALID,
    ApState.RLCSNEW,
    ApState.RLCSTOAMEND,
    ApState.RLCSTOAPPROVE,
    ApState.RLCSSAVED,
  ];
}

/**
 * Seřazené dle abecedy podle getLabel.
 */
export function getGlobalValuesSorted(): Array<ApState> {
  return getGlobalValues().sort((a, b) => getLabel(a).localeCompare(getLabel(b)));
}

export function getGlobalValues(): Array<ApState> {
  return [
    ApState.APSAPPROVED,
    ApState.APSINVALID,
    ApState.APSNEW,
    ApState.APSREPLACED
  ];
}

export function getIcon(value: ApState): string {
  switch (value) {
    case ApState.APSAPPROVED:
      return 'fa-check';
    case ApState.APSINVALID:
      return 'fa-ban';
    case ApState.APSNEW:
      return 'fa-plus';
    case ApState.APSREPLACED:
      return 'fa-sync-alt';
    case ApState.RLCSINVALID:
      return 'fa-times';
    case ApState.RLCSNEW:
      return 'fa-plus';
    case ApState.RLCSTOAMEND:
      return 'fa-arrow-right';
    case ApState.RLCSTOAPPROVE:
      return 'fa-arrow-up';
    case ApState.RLCSSAVED:
      return 'fa-file-download';
    default:
      console.warn("Nedefinovaná ikona hodnota", value);
      return 'fa-question-circle';
  }
}

export function getColor(value: ApState): string {
  switch (value) {
    case ApState.APSAPPROVED:
    case ApState.APSINVALID:
    case ApState.APSNEW:
    case ApState.APSREPLACED:
      return "#317E9F";
    case ApState.RLCSINVALID:
    case ApState.RLCSNEW:
    case ApState.RLCSTOAMEND:
    case ApState.RLCSTOAPPROVE:
    case ApState.RLCSSAVED:
      return "#EBA960";
    default:
      console.warn("Nedefinovaná barva", value);
      return "";
  }
}
