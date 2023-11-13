export enum MenuOptions {
    RIBBON_AP_EXT_SYNCS_HIDDEN = "ribbon.ap.ext-syncs.hidden",
    RIBBON_AP_REMOVEDUPLICITY_HIDDEN = "ribbon.ap.removeDuplicity.hidden",
}

export interface MenuOption {
    name: MenuOptions;
    value: string;
}