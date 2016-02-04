import * as types from 'actions/constants/actionTypes';
import {combineReducers, createStore, applyMiddleware, compose} from 'redux'
import thunkMiddleware from 'redux-thunk'
import createLogger from 'redux-logger'
import {reducer as formReducer} from 'redux-form';
import {lenToBytesStr, roughSizeOfObject} from 'components/Utils';
//import devTools from 'remote-redux-devtools';

// Nastavení úrovně logování
const _logStoreState = true;
const _logStoreSize = false;
const _logActionDuration = false;
const _logCollapsed = true;

/**
 * Sestavení reducerů.
 */
import arrRegion from './arr/arrRegion';
import refTables from './refTables/refTables';
import faFileTree from './arr/faFileTree';
import registry from './registry/registry';
import registryData from './registry/registryData';
import registryRecordTypes from './registry/registryRecordTypes';
import toastrs from './toastr/toastrs';
import partyRegion from './party/partyRegion';
import contextMenu from './global/contextMenu';
import modalDialog from './global/modalDialog';
import webSocket from './global/webSocket';
import splitter from './global/splitter';
import adminRegion from './admin/adminRegion';
import addFaForm from './arr/form/addFaForm';
import addPacketForm from './arr/form/addPacketForm';
import stateRegion from './state/stateRegion';
import router from './router';

import addPartyForm from './party/form/addPartyForm';
import partyNameForm from './party/form/partyNameForm';
import partyIdentifierForm from './party/form/partyIdentifierForm';
import partyCreatorForm from './party/form/partyCreatorForm';
import relationForm from './party/form/relationForm';

import addRegistryForm from './registry/form/addRegistryForm';
import editRegistryForm from './registry/form/editRegistryForm';
import addRegistryVariantForm from './registry/form/addRegistryVariantForm';

let reducer = combineReducers({
    arrRegion,
    refTables,
    faFileTree,
    registry,
    registryData,
    registryRecordTypes,
    toastrs,
    partyRegion,
    contextMenu,
    modalDialog,
    webSocket,
    splitter,
    adminRegion,
    stateRegion,
    router,
    form: formReducer.plugin({
        addFaForm: addFaForm,
        addPacketForm: addPacketForm,
        addPartyForm: addPartyForm,
        partyNameForm: partyNameForm,
        partyIdentifierForm: partyIdentifierForm,
        partyCreatorForm: partyCreatorForm,
        relationForm: relationForm,
        addRegistryForm: addRegistryForm,
        editRegistryForm: editRegistryForm,
        addRegistryVariantForm: addRegistryVariantForm
    })
});

// Store a middleware
const loggerMiddleware = createLogger({
    collapsed: _logCollapsed,
    duration: _logActionDuration,
    predicate: (getState, action) => action.type !== types.STORE_STATE_DATA
})

// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
var _utilsIsPlainObject = require('redux/lib/utils/isPlainObject');
function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }
var _utilsIsPlainObject2 = _interopRequireDefault(_utilsIsPlainObject);
var crtStr = function (reducer, initialState) {
console.log('lLLLLLL', createStore);
console.log('lLLLLLL', createStore.prototype);
console.log('lLLLLLL', createStore.listeners);
    var result = createStore(reducer, initialState);

console.log('lLLLLLL', result.listeners);

    result.dispatch = function (action) {
        if (!_utilsIsPlainObject2['default'](action)) {
          throw new Error('Actions must be plain objects. ' + 'Use custom middleware for async actions.');
        }

        if (typeof action.type === 'undefined') {
          throw new Error('Actions may not have an undefined "type" property. ' + 'Have you misspelled a constant?');
        }

        if (result.isDispatching) {
          throw new Error('Reducers may not dispatch actions.');
        }

        try {
          result.isDispatching = true;
          result.currentState = currentReducer(result.currentState, action);
        }catch (e) {
            console.error(111111, e)
        } finally {
          result.isDispatching = false;
        }

        result.listeners.slice().forEach(function (listener) {
          return listener();
        });
        return action;
      }.bind(result)
    return result;
}
// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

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
import {selectFaTab} from 'actions/arr/fa'
var fa = Object.assign({id: 1, versionId: 1});
store.dispatch(selectFaTab(fa));
*/

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
    var action = {
        type: types.STORE_SAVE
    }

    var result = {
        partyRegion: partyRegion(store.partyRegion, action),
        registryRegion: registry(store.registry, action),
        arrRegion: arrRegion(store.arrRegion, action),
        splitter: splitter(store.splitter, action),
    }

    return result
}

module.exports = {
    store,
    save
}
