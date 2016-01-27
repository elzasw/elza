/**
 * FA.
 */
export const FA_FA_FILE_TREE_REQUEST = 'FA_FA_FILE_TREE_REQUEST'
export const FA_FA_FILE_TREE_RECEIVE = 'FA_FA_FILE_TREE_RECEIVE'

export const FA_EXTENDED_VIEW = 'FA_EXTENDED_VIEW'

export const FA_FA_TREE_REQUEST = 'FA_FA_TREE_REQUEST'
export const FA_FA_TREE_RECEIVE = 'FA_FA_TREE_RECEIVE'
export const FA_FA_TREE_EXPAND_NODE = 'FA_FA_TREE_EXPAND_NODE'
export const FA_FA_TREE_COLLAPSE_NODE = 'FA_FA_TREE_COLLAPSE_NODE'
export const FA_FA_TREE_FOCUS_NODE = 'FA_FA_TREE_FOCUS_NODE'
export const FA_TREE_AREA_MAIN = 'FA_TREE_AREA_MAIN'
export const FA_TREE_AREA_MOVEMENTS_LEFT = 'FA_TREE_AREA_MOVEMENTS_LEFT'
export const FA_TREE_AREA_MOVEMENTS_RIGHT = 'FA_TREE_AREA_MOVEMENTS_RIGHT'
export const FA_FA_TREE_SELECT_NODE = 'FA_FA_TREE_SELECT_NODE'
export const FA_FA_TREE_FULLTEXT_CHANGE = 'FA_FA_TREE_FULLTEXT_CHANGE'
export const FA_FA_TREE_FULLTEXT_RESULT = 'FA_FA_TREE_FULLTEXT_RESULT'

export const FA_SUB_NODE_FORM_REQUEST = 'FA_SUB_NODE_FORM_REQUEST'
export const FA_SUB_NODE_FORM_RECEIVE = 'FA_SUB_NODE_FORM_RECEIVE'
export const FA_SUB_NODE_INFO_REQUEST = 'FA_SUB_NODE_INFO_REQUEST'
export const FA_SUB_NODE_INFO_RECEIVE = 'FA_SUB_NODE_INFO_RECEIVE'

export const FA_SUB_NODE_FORM_VALUE_CHANGE = 'FA_SUB_NODE_FORM_VALUE_CHANGE'
export const FA_SUB_NODE_FORM_VALUE_CHANGE_SPEC = 'FA_SUB_NODE_FORM_VALUE_CHANGE_SPEC'
export const FA_SUB_NODE_FORM_VALUE_BLUR = 'FA_SUB_NODE_FORM_VALUE_BLUR'
export const FA_SUB_NODE_FORM_VALUE_FOCUS = 'FA_SUB_NODE_FORM_VALUE_FOCUS'
export const FA_SUB_NODE_FORM_VALUE_ADD = 'FA_SUB_NODE_FORM_VALUE_ADD'
export const FA_SUB_NODE_FORM_VALUE_DELETE = 'FA_SUB_NODE_FORM_VALUE_DELETE'
export const FA_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE = 'FA_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE'
export const FA_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD = 'FA_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD'
export const FA_SUB_NODE_FORM_VALUE_RESPONSE = 'FA_SUB_NODE_FORM_VALUE_RESPONSE'

export const FA_NODE_INFO_REQUEST = 'FA_NODE_INFO_REQUEST'
export const FA_NODE_INFO_RECEIVE = 'FA_NODE_INFO_RECEIVE'

export const FA_SELECT_FA_TAB = 'FA_SELECT_FA_TAB'
export const FA_CLOSE_FA_TAB = 'FA_CLOSE_FA_TAB'

export const FA_FA_SELECT_NODE_TAB = 'FA_FA_SELECT_NODE_TAB'
export const FA_FA_CLOSE_NODE_TAB = 'FA_FA_CLOSE_NODE_TAB'
export const FA_FA_SELECT_SUBNODE = 'FA_FA_SELECT_SUBNODE'
export const FA_FA_SUBNODES_NEXT = 'FA_FA_SUBNODES_NEXT'
export const FA_FA_SUBNODES_PREV = 'FA_FA_SUBNODES_PREV'
export const FA_FA_SUBNODES_NEXT_PAGE = 'FA_FA_SUBNODES_NEXT_PAGE'
export const FA_FA_SUBNODES_PREV_PAGE = 'FA_FA_SUBNODES_PREV_PAGE'

/**
 * Uživatelské nastavení pro JP
 */
export const NODE_DESC_ITEM_TYPE_LOCK = 'NODE_DESC_ITEM_TYPE_LOCK'
export const NODE_DESC_ITEM_TYPE_UNLOCK = 'NODE_DESC_ITEM_TYPE_UNLOCK'
export const NODE_DESC_ITEM_TYPE_UNLOCK_ALL = 'NODE_DESC_ITEM_TYPE_UNLOCK_ALL'
export const NODE_DESC_ITEM_TYPE_COPY = 'NODE_DESC_ITEM_TYPE_COPY'
export const NODE_DESC_ITEM_TYPE_NOCOPY = 'NODE_DESC_ITEM_TYPE_NOCOPY'

/**
 * Obaly pro FA
 */
export const PACKETS_REQUEST = 'PACKETS_REQUEST'
export const PACKETS_RECEIVE = 'PACKETS_RECEIVE'


/**
 * Party.
 */
export const PARTY_FIND_PARTY_REQUEST = 'PARTY_FIND_PARTY_REQUEST'  // volaní hledání osob
export const PARTY_FIND_PARTY_RECEIVE = 'PARTY_FIND_PARTY_RECEIVE'  // dokončení hledání osob
export const PARTY_SELECT_PARTY = 'PARTY_SELECT_PARTY'              // dokončení hledání osob
export const PARTY_DETAIL_REQUEST = 'PARTY_DETAIL_REQUEST'          // pořadavek na načtení detailu osoby
export const PARTY_DETAIL_RECEIVE = 'PARTY_DETAIL_RECIVE'           // dokončení požadavku na hledání osoby
export const PARTY_DELETED = 'PARTY_DELETED'                        // smazani osoby

/**
 * Toastr
**/

export const TOASTR_TYPE_INFO = 'TOASTR_TYPE_INFO'
export const TOASTR_TYPE_SUCCESS = 'TOASTR_TYPE_SUCCESS'
export const TOASTR_TYPE_WARNING = 'TOASTR_TYPE_WARNING'
export const TOASTR_TYPE_CLEAR = 'TOASTR_TYPE_CLEAR'
export const TOASTR_TYPE_DANGER = 'TOASTR_TYPE_DANGER'

/**
 * Registry.
 */
export const REGISTRY_REQUEST_REGISTRY_LIST = 'REGISTRY_REQUEST_REGISTRY_LIST'
export const REGISTRY_RECEIVE_REGISTRY_LIST = 'REGISTRY_RECEIVE_REGISTRY_LIST'
export const REGISTRY_SELECT_REGISTRY = 'REGISTRY_SELECT_REGISTRY'
export const REGISTRY_RECEIVE_REGISTRY_DETAIL = 'REGISTRY_RECEIVE_REGISTRY_DETAIL'
export const REGISTRY_REQUEST_REGISTRY_DETAIL = 'REGISTRY_REQUEST_REGISTRY_DETAIL'
export const REGISTRY_CHANGE_REGISTRY_DETAIL = 'REGISTRY_CHANGE_REGISTRY_DETAIL'
export const REGISTRY_SEARCH_REGISTRY = 'REGISTRY_SEARCH_REGISTRY'
export const REGISTRY_CHANGED_PARENT_REGISTRY = 'REGISTRY_CHANGED_PARENT_REGISTRY'
export const REGISTRY_CHANGED_TYPES_ID = 'REGISTRY_CHANGED_TYPES_ID'
export const REGISTRY_REMOVE_REGISTRY = 'REGISTRY_REMOVE_REGISTRY'
export const REGISTRY_MOVE_REGISTRY_START = 'REGISTRY_MOVE_REGISTRY_START'
export const REGISTRY_MOVE_REGISTRY_FINISH = 'REGISTRY_MOVE_REGISTRY_FINISH'
export const REGISTRY_MOVE_REGISTRY_CANCEL = 'REGISTRY_MOVE_REGISTRY_CANCEL'
export const REGISTRY_UPDATED = 'REGISTRY_UPDATED'





/**
 * Global.
 */
export const GLOBAL_GET_OBJECT_INFO = 'GLOBAL_GET_OBJECT_INFO'
export const GLOBAL_CONTEXT_MENU_SHOW = 'GLOBAL_CONTEXT_MENU_SHOW'
export const GLOBAL_CONTEXT_MENU_HIDE = 'GLOBAL_CONTEXT_MENU_HIDE'
export const GLOBAL_MODAL_DIALOG_SHOW = 'GLOBAL_MODAL_DIALOG_SHOW'
export const GLOBAL_MODAL_DIALOG_HIDE = 'GLOBAL_MODAL_DIALOG_HIDE'
export const GLOBAL_INIT_FORM_DATA = 'GLOBAL_INIT_FORM_DATA'
export const GLOBAL_WEB_SOCKET_CONNECT = 'GLOBAL_WEB_SOCKET_CONNECT'
export const GLOBAL_WEB_SOCKET_DISCONNECT = 'GLOBAL_WEB_SOCKET_DISCONNECT'

/**
 * Admin - Packages
 */
export const ADMIN_PACKAGES_REQUEST = 'ADMIN_PACKAGES_REQUEST'                  // dotaz na načtení balíčků
export const ADMIN_PACKAGES_RECEIVE = 'ADMIN_PACKAGES_RECEIVE'                  // odpověď na načtení balíčků
export const ADMIN_PACKAGES_DELETE_REQUEST = 'ADMIN_PACKAGES_DELETE_REQUEST'    // dotaz na smazání balíčku
export const ADMIN_PACKAGES_DELETE_RECEIVE = 'ADMIN_PACKAGES_DELETE_RECEIVE'    // odpověď na smazání balíčku
export const ADMIN_PACKAGES_IMPORT_REQUEST = 'ADMIN_PACKAGES_IMPORT_REQUEST'    // dotaz na nahrání balíčku
export const ADMIN_PACKAGES_IMPORT_RECEIVE = 'ADMIN_PACKAGES_IMPORT_RECEIVE'    // odpověď na nahrání balíčku

/**
 * Admin - Fulltext
 */
export const ADMIN_FULLTEXT_REINDEXING_REQUEST = 'ADMIN_FULLTEXT_REINDEXING_REQUEST'    // požadavek na reindexace
export const ADMIN_FULLTEXT_REINDEXING_STATE_REQUEST = 'ADMIN_FULLTEXT_REINDEXING_STATE_REQUEST'    // dotaz na průběh indexace
export const ADMIN_FULLTEXT_REINDEXING_STATE_RECIEVE = 'ADMIN_FULLTEXT_REINDEXING_STATE_RECIEVE'    // odpověď na průběh indexace
		
/**
 * Ref tables.
 */
export const REF_RULE_SET_REQUEST = 'REF_RULE_SET_REQUEST'
export const REF_RULE_SET_RECEIVE = 'REF_RULE_SET_RECEIVE'
export const REF_RUL_DATA_TYPES_REQUEST = 'REF_RUL_DATA_TYPES_REQUEST'
export const REF_RUL_DATA_TYPES_RECEIVE = 'REF_PARTY_TYPE_RECEIVE'
export const REF_PARTY_NAME_FORM_TYPES_REQUEST = 'REF_PARTY_NAME_FORM_TYPES_REQUEST'
export const REF_PARTY_NAME_FORM_TYPES_RECEIVE = 'REF_PARTY_NAME_FORM_TYPES_RECEIVE'
export const REF_PARTY_TYPES_REQUEST = 'REF_PARTY_TYPES_REQUEST'
export const REF_PARTY_TYPES_RECEIVE = 'REF_PARTY_TYPES_RECEIVE'
export const REF_RECORD_TYPES_REQUEST = 'REF_RECORD_TYPES_REQUEST'
export const REF_RECORD_TYPES_RECEIVE = 'REF_RECORD_TYPES_RECEIVE'
export const REF_CALENDAR_TYPES_REQUEST = 'REF_CALENDAR_TYPES_REQUEST'
export const REF_CALENDAR_TYPES_RECEIVE = 'REF_CALENDAR_TYPES_RECEIVE'
export const REF_PACKET_TYPES_REQUEST = 'REF_PACKET_TYPES_REQUEST'
export const REF_PACKET_TYPES_RECEIVE = 'REF_PACKET_TYPES_RECEIVE'

/**
 * Akce od websocketů.
 */
export const CHANGE_CONFORMITY_INFO = 'CHANGE_CONFORMITY_INFO'
export const CHANGE_INDEXING_FINISHED = 'CHANGE_INDEXING_FINISHED'
export const CHANGE_PACKAGE = 'CHANGE_PACKAGE'