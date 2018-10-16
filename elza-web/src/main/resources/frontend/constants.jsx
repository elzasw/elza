export const DEFAULT_LIST_SIZE = 200;
export const AP_EXT_SYSTEM_TYPE = {
    INTERPI: 'INTERPI'
};



export const MODAL_DIALOG_VARIANT = {
    LARGE: 'dialog-lg',
    FULLSCREEN: 'dialog-fullscreen',
    NO_HEADER: 'dialog-no-header'
};


export const PARTY_TYPE_CODES = {
    GROUP_PARTY: 'GROUP_PARTY',
    PERSON: 'PERSON',
    DYNASTY: 'DYNASTY',
    EVENT: 'EVENT',
};

export const RELATION_CLASS_CODES = {
    RELATION: "R",
    BIRTH: "B",
    EXTINCTION: "E"
};

export const ActionState = {
    RUNNING: 'RUNNING',
    WAITING: 'WAITING',
    PLANNED: 'PLANNED',
    FINISHED: 'FINISHED',
    ERROR: 'ERROR',
    INTERRUPTED: 'INTERRUPTED',
    OUTDATED: 'OUTDATED'
};


export const FOCUS_KEYS = {
    NONE: null,
    ARR: 'arr',
    PARTY: 'party',
    REGISTRY: 'registry',
    HOME: 'home',
    FUND_OUTPUT: 'fund-output',
    FUND_ACTION: 'fund-action',
    FUND_REQUEST: 'fund-request',
    ADMIN_EXT_SYSTEM: 'admin-extSystem',
};

/**
 * Formát pro zobrazení typu Integer.
 * @type {{NUMBER: string, DURATION: string}}
 */
export const DisplayType = {
    NUMBER: 'NUMBER',
    DURATION: 'DURATION',
};

//konkrétrní kód akce pro perzistentní řazení v balíčku ZP2015
export const PERSISTENT_SORT_CODE = "PERZISTENTNI_RAZENI";
export const ZP2015_INTRO_VYPOCET_EJ = "ZP2015_INTRO_VYPOCET_EJ";