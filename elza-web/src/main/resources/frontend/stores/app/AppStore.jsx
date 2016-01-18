import {combineReducers, createStore, applyMiddleware} from 'redux'
import thunkMiddleware from 'redux-thunk'
import createLogger from 'redux-logger'
import {reducer as formReducer} from 'redux-form';

/**
 * Sestavení reducerů.
 */
import arrRegion from './arr/arrRegion';
import refTables from './refTables/refTables';
import faFileTree from './arr/faFileTree';
import registry from './registry/registry';
import registryData from './registry/registryData';
import toastrs from './toastr/toastrs';
import partyRegion from './party/partyRegion';
import contextMenu from './global/contextMenu';
import modalDialog from './global/modalDialog';
import adminRegion from './admin/adminRegion';
import addFaForm from './arr/form/addFaForm';
import addPartyPersonForm from './party/form/addPartyPersonForm';
import addPartyDynastyForm from './party/form/addPartyDynastyForm';
import addPartyEventForm from './party/form/addPartyEventForm';
import addPartyGroupForm from './party/form/addPartyGroupForm';
import addPartyOtherForm from './party/form/addPartyOtherForm';
import addRegistryForm from './registry/form/addRegistryForm';

let reducer = combineReducers({
    arrRegion,
    refTables,
    faFileTree,
    registry,
    registryData,
    toastrs,
    partyRegion,
    contextMenu,
    modalDialog,
    adminRegion,
    form: formReducer.plugin({
        addFaForm: addFaForm,
        addPartyPersonForm: addPartyPersonForm,
        addPartyDynastyForm: addPartyDynastyForm,
        addPartyOtherForm: addPartyOtherForm,
        addPartyEventForm: addPartyEventForm,
        addPartyGroupForm: addPartyGroupForm,
        addRegistryForm: addRegistryForm
    })
});

// Store a middleware
const loggerMiddleware = createLogger()
const createStoreWithMiddleware = applyMiddleware(
    thunkMiddleware,
    loggerMiddleware
)(createStore)

var initialState = {
}
var store = function configureStore(initialState) {
    return createStoreWithMiddleware(reducer, initialState)
}(initialState);

/*
import {selectFaTab} from 'actions/arr/fa'
var fa = Object.assign({id: 1, versionId: 1});
store.dispatch(selectFaTab(fa));
*/

let curr
function handleChange() {
    let prev = curr
    curr = store.getState().arrRegion;

    if (curr !== prev) {
        console.log("@@@@@@@@@@@@@@@@@@@@@@CHANGE", prev, curr);
    }
}

store.subscribe(handleChange);

module.exports = {
    store
}
