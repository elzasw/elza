import * as types from 'actions/constants/ActionTypes.js';
import {combineReducers, createStore, applyMiddleware, compose} from 'redux'
import thunkMiddleware from 'redux-thunk'
import createLogger from 'redux-logger'
import {reducer as formReducer} from 'redux-form';
import {lenToBytesStr, roughSizeOfObject} from 'components/Utils.jsx';
import {splitterResize} from 'actions/global/splitter.jsx';
import {normalizeInt} from 'components/validate.jsx';

const normalizePacketSize = (value, previousValue, allValues, previousAllValues) => {
    var vv = normalizeInt(value, previousValue, allValues, previousAllValues)
    if (vv > 32) {
        return previousValue
    }
    return vv
}

//import devTools from 'remote-redux-devtools';

// Nastavení úrovně logování
const _logStoreState = true;
const _logStoreSize = false;
const _logActionDuration = false;
const _logCollapsed = true;

/**
 * Sestavení reducerů.
 */
import arrRegion from './arr/arrRegion.jsx';
import refTables from './refTables/refTables.jsx';
import registryRegion from './registry/registryRegion.jsx';
import registryRegionRecordTypes from './registry/registryRegionRecordTypes.jsx';
import toastr from '../../components/shared/toastr/ToastrStore.jsx';
import partyRegion from './party/partyRegion.jsx';
import fundRegion from './fund/fundRegion.jsx';
import contextMenu from './global/contextMenu.jsx';
import modalDialog from './global/modalDialog.jsx';
import webSocket from './global/webSocket.jsx';
import login from './global/login.jsx';
import splitter from './global/splitter.jsx';
import developer from './global/developer.jsx';
import focus from './global/focus.jsx';
import adminRegion from './admin/adminRegion.jsx';
import fundForm from './arr/form/fundForm.jsx';
import addPacketForm from './arr/form/addPacketForm.jsx';
import stateRegion from './state/stateRegion.jsx';
import userDetail from './user/userDetail.jsx';
import router from './router.jsx';

import addPartyForm from './party/form/addPartyForm.jsx';
import partyNameForm from './party/form/partyNameForm.jsx';
import partyIdentifierForm from './party/form/partyIdentifierForm.jsx';
import partyCreatorForm from './party/form/partyCreatorForm.jsx';
import relationForm from './party/form/relationForm.jsx';

import addRegistryForm from './registry/form/addRegistryForm.jsx';
import editRegistryForm from './registry/form/editRegistryForm.jsx';


let reducer = combineReducers({
    arrRegion,
    refTables,
    registryRegion,
    registryRegionRecordTypes,
    toastr,
    developer,
    partyRegion,
    fundRegion,
    contextMenu,
    modalDialog,
    webSocket,
    login,
    splitter,
    focus,
    adminRegion,
    stateRegion,
    router,
    userDetail,
    form: formReducer.plugin({
        fundForm: fundForm,
        addPacketForm: addPacketForm,
        addPartyForm: addPartyForm,
        partyNameForm: partyNameForm,
        partyIdentifierForm: partyIdentifierForm,
        partyCreatorForm: partyCreatorForm,
        relationForm: relationForm,
        addRegistryForm: addRegistryForm,
        editRegistryForm: editRegistryForm,
    }).normalize({
        addPacketForm: {
            'start': normalizeInt,
            'size': normalizePacketSize,
            'count': normalizeInt,
        }
    })
});

// Store a middleware
const loggerMiddleware = createLogger({
    collapsed: _logCollapsed,
    duration: _logActionDuration,
    predicate: (getState, action) => (action.type !== types.STORE_STATE_DATA && action.type !== types.GLOBAL_SPLITTER_RESIZE)
})


var createStoreWithMiddleware;
if (_logStoreState) {
    createStoreWithMiddleware = applyMiddleware(
        thunkMiddleware,
        loggerMiddleware
    )(createStore)
} else {
    createStoreWithMiddleware = applyMiddleware(
        thunkMiddleware
    )(createStore)
}
/* REDUX DEBUG
 var createStoreWithMiddleware;
 if (_logStoreState) {
 createStoreWithMiddleware = compose(applyMiddleware(
 thunkMiddleware,
 loggerMiddleware
 ), window.devToolsExtension ? window.devToolsExtension() : f => f)(createStore)
 } else {
 createStoreWithMiddleware = compose(applyMiddleware(
 thunkMiddleware
 ), window.devToolsExtension ? window.devToolsExtension() : f => f)(createStore)
 }
 */

var initialState = {
}
var store = function configureStore(initialState) {
    return createStoreWithMiddleware(reducer, initialState)
}(initialState);

/*
  const finalCreateStore = compose(
    applyMiddleware(thunkMiddleware),
    typeof window === 'object' && typeof window.devToolsExtension !== 'undefined' ? window.devToolsExtension() : f => f
  )(createStore);

  const _store = finalCreateStore(reducer, initialState);
*/
/*
import {selectFundTab} from 'actions/arr/fund.jsx'
var fund = Object.assign({id: 1, versionId: 1});
store.dispatch(selectFundTab(fund));
*/

// Resize
window.addEventListener("resize", () => {
    const state = store.getState()
    store.dispatch(splitterResize(state.splitter.leftSize, state.splitter.rightSize))
});

if (_logStoreSize) {
    let curr
    function handleChange() {
        let prev = curr
        curr = store.getState().arrRegion;

        if (curr !== prev) {
            var lenStr = lenToBytesStr(roughSizeOfObject(curr));
            console.log("Velikost store", lenStr);
            //console.log("@@@@@@@@@@@@@@@@@@@@@@CHANGE", prev, curr);
        }
    }

    store.subscribe(handleChange);
}

var save = function(store) {
    const action = {
        type: types.STORE_SAVE
    };

    //var rrd = registryRegionData(store.registryRegionData, action)
    //console.log(result.registryRegion);
    // result.registryRegion._info = result.registryRegion.registryRegionData._info
    // result.registryRegion.selectedId = result.registryRegion.registryRegionData.selectedId

    return {
        partyRegion: partyRegion(store.partyRegion, action),
        registryRegion: registryRegion(store.registryRegion, action),
        arrRegion: arrRegion(store.arrRegion, action),
        fundRegion: fundRegion(store.fundRegion, action),
        splitter: splitter(store.splitter, action)
    }
}

module.exports = {
    store,
    save
}
