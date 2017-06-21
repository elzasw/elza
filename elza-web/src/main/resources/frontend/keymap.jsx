import {Utils} from 'components/index.jsx';
var keyModifier = Utils.getKeyModifier()

var keymap = {
    Main: {
        home: ['alt+1','g h'],  	//GOTO_HOME_PAGE
        arr: ['alt+2','g a'],       //GOTO_ARR_PAGE
        registry: ['alt+3','g r'],  //GOTO_REGISTRY_PAGE
        party: ['alt+4','g p'],     //GOTO_PARTY_PAGE
        admin: ['alt+5','g n'],     //GOTO_ADMIN_PAGE
    },
    Tree: {},
    Accordion: {
        prevItem: keyModifier + 'up',   //SELECT_PREVIOUS_ITEM
        nextItem: keyModifier + 'down', //SELECT_NEXT_ITEM
        toggleItem: 'shift+enter',       //TOGGLE_ITEM
        "ACCORDION_MOVE_UP": "up",
        "ACCORDION_MOVE_DOWN": "down",
        "ACCORDION_MOVE_TOP": "home",
        "ACCORDION_MOVE_END": "end"
    },
    NodePanel: {
        searchItem: keyModifier + 'f',      //SEARCH_ITEM
        addDescItemType: [keyModifier + 'p',"n p"], //ADD_DESC_ITEM
        addNodeAfter: [keyModifier + '+',"n j down"],    //ADD_NODE_AFTER
        addNodeBefore: [keyModifier + '-',"n j up"],   //ADD_NODE_BEFORE
        addNodeChild: [keyModifier + '*',"n j right"],    //ADD_NODE_CHILD
        addNodeEnd: [keyModifier + '/',"n j end"]      //ADD_NODE_END
    },
    DescItemType: {
        deleteDescItemType: [keyModifier + 'y'],  //DELETE_DESC_ITEM
    },
    DescItem: {
        addDescItem: keyModifier + 'i',     //ADD_DESC_ITEM_PART
        deleteDescItem: keyModifier + 'd',  //DELETE_DESC_ITEM_PART
    },
    ArrOutputDetail: {},
    ArrRequestDetail: {},
    PartyDetail: {},
    Registry: {
        addRegistry: keyModifier + 'n',         //ADD_REGISTRY
        registryMove: keyModifier + 'x',        //REGISTRY_MOVE
        registryMoveApply: keyModifier + 'v',   //REGISTRY_MOVE_APPLY
        registryMoveCancel: keyModifier + 'w',  //REGISTRY_MOVE_CANCEL
        area1: [keyModifier + '1', "+"],               //FOCUS_AREA_1
        area2: [keyModifier + '2', "ě"]                //FOCUS_AREA_2
    },
    RegistryDetail: {
        editRecord: keyModifier + 'e',          //EDIT_RECORD
        goToPartyPerson: keyModifier + 'b',     //GOTO_PARTY
        addRegistryVariant: keyModifier + 'i'   //ADD_REGISTRY_VARIANT
    },
    VariantRecord: {
        deleteRegistryVariant: keyModifier + 'd'    //DELETE_REGISTRY_VARIANT
    },
    Tabs: {
        prevTab: keyModifier + 'left',      //SELECT_PREVIOUS_TAB
        nextTab: keyModifier + 'right',     //SELECT_NEXT_TAB
    },
    ArrParent: {
        registerJp: keyModifier + 'j',  //MAP_REGISTER_TO_NODE_TOGGLE
        area1: keyModifier + '1',       //FOCUS_AREA_1
        area2: keyModifier + '2',       //FOCUS_AREA_2
        area3: keyModifier + '3',       //FOCUS_AREA_3
        back: keyModifier + 'z',        //BACK
        arr: keyModifier + 'a',         //GOTO_ARR_PAGE
        dataGrid: keyModifier + 't',    //GOTO_DATAGRID_PAGE
        movements: keyModifier + 'm',   //GOTO_MOVEMENTS_PAGE
        actions: keyModifier + 'h',     //GOTO_ACTIONS_PAGE
        output: keyModifier + 'o',      //GOTO_OUTPUT_PAGE
        newOutput: keyModifier + '+',   //CREATE_NEW
        newAction: keyModifier + '+'
    },
    AdminExtSystemPage: {},
    Party: {
        addParty: keyModifier + 'n',    //ADD_PARTY
        addRelation: keyModifier + 't', //ADD_RELATION
        area1: keyModifier + '1',       //FOCUS_AREA_1
        area2: keyModifier + '2',       //FOCUS_AREA_2
        area3: keyModifier + '3',       //FOCUS_AREA_3
    },
    FundTreeLazy:{
        "MOVE_UP": "up",
        "MOVE_DOWN": "down",
        "MOVE_TO_PARENT_OR_CLOSE": "left",
        "MOVE_TO_CHILD_OR_OPEN": "right"
    },
    DescItemJsonTableCellForm:{
        "FORM_CLOSE": "enter"
    },
    CollapsablePanel:{
        "PANEL_TOGGLE":"enter",
        "PANEL_PIN":"shift+enter"
    },
    ListBox:{
        "MOVE_UP":"up",
        "MOVE_DOWN":"down",
        "MOVE_PAGE_UP":"pageup",
        "MOVE_PAGE_DOWN":"pagedown",
        "MOVE_TOP":"home",
        "MOVE_END":"end",
        "ITEM_CHECK":"space",
        "ITEM_DELETE":"del",
        "ITEM_SELECT":"enter"
    },
    DataGrid:{
        "MOVE_UP":"up",
        "MOVE_DOWN":"down",
        "MOVE_LEFT":"left",
        "MOVE_RIGHT":"right",
        "ITEM_EDIT":["enter","f2"],
        "ITEM_ROW_CHECK":"space",
        "ITEM_DELETE":"del",
    },
    LazyListBox:{
        "MOVE_UP":"up",
        "MOVE_DOWN":"down",
        "MOVE_PAGE_UP":"pageup",
        "MOVE_PAGE_DOWN":"pagedown",
        "MOVE_TOP":"home",
        "MOVE_END":"end",
        "ITEM_DELETE":"del",
        "ITEM_CHECK":"space",
        "ITEM_SELECT":"enter"
    },
    DataGridPagination:{
        "CONFIRM":"enter"
    },
    Autocomplete: {
        "MOVE_UP": "up",
        "MOVE_DOWN": "down",
        "MOVE_TO_PARENT_OR_CLOSE": "left",
        "MOVE_TO_CHILD_OR_OPEN": "right",
        "SELECT_ITEM": "enter",
        "OPEN_MENU": "alt+down",
        "CLOSE_MENU": ["escape","alt+up"]
    }
}

export default keymap;