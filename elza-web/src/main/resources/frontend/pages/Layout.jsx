/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import { AppStore, ResizeStore } from 'stores/index.jsx';
import {AbstractReactComponent, ContextMenu, Toastr, ModalDialog, WebSocket, Login} from 'components/index.jsx';
var AppRouter = require ('./AppRouter')
import {ShortcutManager} from 'react-shortcuts';
import {Shortcuts} from 'react-shortcuts';
var keyModifier = Utils.getKeyModifier()
import {Utils} from 'components/index.jsx';
import {routerNavigate} from 'actions/router.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import Tetris from "components/game/Tetris.jsx";

require('./Layout.less');

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
        addDescItemType: [keyModifier + 'p',"n p enter"], //ADD_DESC_ITEM
        addNodeAfter: [keyModifier + '+',"n j down enter"],    //ADD_NODE_AFTER
        addNodeBefore: [keyModifier + '-',"n j up enter"],   //ADD_NODE_BEFORE
        addNodeChild: [keyModifier + '*',"n j right enter"],    //ADD_NODE_CHILD
        addNodeEnd: [keyModifier + '/','n j e enter']      //ADD_NODE_END
    },
    DescItemType: {
        deleteDescItemType: keyModifier + 'y',  //DELETE_DESC_ITEM
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
        area1: keyModifier + '1',               //FOCUS_AREA_1
        area2: keyModifier + '2'                //FOCUS_AREA_2
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
const shortcutManager = new ShortcutManager(keymap)

var _gameRunner = null;

class Layout extends AbstractReactComponent {

    state = {
        showGame: false,
        canStartGame: false,
    };

    componentWillUnmount() {
        if (_gameRunner) {
            clearTimeout(_gameRunner);
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleShortcuts = (action) => {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'home':
                this.dispatch(routerNavigate('/'));
                this.dispatch(setFocus('home', 1, 'list'));
                break;
            case 'arr':
                this.dispatch(routerNavigate('/arr'));
                this.dispatch(setFocus('arr', 1, 'tree'));
                break;
            case 'party':
                this.dispatch(routerNavigate('/party'));
                this.dispatch(setFocus('party', 1, 'tree'));
                break;
            case 'registry':
                this.dispatch(routerNavigate('/registry'));
                this.dispatch(setFocus('registry', 1, 'list'));
                break;
            case 'admin':
                this.dispatch(routerNavigate('/admin'));
                break;
        }
    };

    handleGameStartLeave = () => {
        if (_gameRunner) {
            clearTimeout(_gameRunner);
            _gameRunner = null;
        }
        this.setState({canStartGame: false});
    };

    handleGameStartOver = () => {
        if (_gameRunner) {
            clearTimeout(_gameRunner);
            _gameRunner = null;
        }
        _gameRunner = setTimeout(() => {
            this.setState({canStartGame: true});
        }, 1000);
    };
    componentWillMount(){
        this.dispatch({type:"SHORTCUTS_SAVE",shortcutManager:shortcutManager});
    }
    render() {
        const {canStartGame, showGame} = this.state;

        if (showGame) {
            return <Tetris onClose={() => { this.setState({showGame: false, canStartGame: false}) }} />;
        }

        return <Shortcuts name='Main' handler={this.handleShortcuts} global>
            <div className={versionNumber ? 'root-container with-version' : 'root-container'}>
                <div onClick={() => { canStartGame && this.setState({showGame: true}) }} onMouseEnter={this.handleGameStartOver} onMouseLeave={this.handleGameStartLeave} className={"game-placeholder " + (canStartGame ? "canStart" : "")}>
                    &nbsp;
                </div>
                {this.props.children}
                <div style={{overflow:'hidden'}}>
                    <Toastr.Toastr />
                </div>
                <ContextMenu {...this.props.contextMenu}/>
                <ModalDialog {...this.props.modalDialog}/>
                <WebSocket />
                <Login />
                <AppRouter/>
            </div>
            {typeof versionNumber != "undefined" && <div className="version-container">Verze sestavení aplikace: {versionNumber}</div>}
        </Shortcuts>
    }
}
Layout.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

function mapStateToProps(state) {
    const {contextMenu, modalDialog} = state
    return {
        contextMenu,
        modalDialog
    }
}

export default connect(mapStateToProps)(Layout);
