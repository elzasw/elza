import * as types from 'actions/constants/ActionTypes.js';
import {combineReducers, createStore, applyMiddleware, compose} from 'redux'
import thunkMiddleware from 'redux-thunk'
import createLogger from 'redux-logger'
import {reducer as formReducer} from 'redux-form';
import {lenToBytesStr, roughSizeOfObject} from 'components/Utils.jsx';
import {splitterResize} from 'actions/global/splitter.jsx';
import {normalizeInt} from 'components/validate.jsx';

import reduxFormUtils from "./form/reduxFormUtils.jsx"

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
import inlineForm from './form/inlineForm.jsx';
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
        outputEditForm: inlineForm,
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

/**
 * Třída pro definici inline formulářů.
 */
var inlineFormSupport = new class {
    constructor() {
        this.forms = {}
        this.init = {}
        this.fields = {}
        this.initFields = {}
        this.initialFormData = {}
        this.wasChanged = {}
    }


    addForm(formName) {
        this.forms[formName] = true;
    }

    getFormData(formName, state) {
        const formState = state.form[formName];
        const reduxFormData = reduxFormUtils.getValues(this.initFields[formName], formState, false);
        return reduxFormData;
    }

    // DEEP ??
    setFields(formName, fields) {
        this.initFields[formName] = fields;
        this.fields[formName] = fields;
    }

    /**
     * Prvotní inicializace. Zde bude nutné udělat inicializaci validace vnořených dat!
     * @param formName
     * @param validate
     * @param onSave
     */
    // DEEP
    setInit(formName, validate, onSave) {
        if (!validate) {
            console.error("Chyba inicializace formuláře", formName, " chybí validate.");
        }
        if (!onSave) {
            console.error("Chyba inicializace formuláře", formName, " chybí onSave.");
        }
        this.init[formName] = {validate, onSave};
    }

    /**
     * Načtení store pro formulář.
     * @param formName
     * @param state
     * @returns {*}
     */
    getFormState(formName, state) {
        const formState = state.form[formName];
        if (!formState) {
            console.error("Nenalezen store pro formulář", formName);
            return {};
        }
        return formState;
    }

    // DEEP
    getValidatedFormState(state, dispatch, action) {
        const init = this.init[action.form];
        const formState = this.getFormState(action.form, state);
        var data = this.getFormData(action.form, state);
        var errors = init.validate(data);

        var result = {
            ...formState,
        }

        var stateChanged = false;
        this.fields[action.form].forEach(field => {
            if (errors[field]) {
                result[field] = {
                    ...result[field],
                    submitError: errors[field],
                    touched: true,
                }
                stateChanged = true;    // zatím natvrdo, ale chtělo by to porovnávat, zda jsme změnili chybu
            }
        })

        return {
            formState: result,
            stateChanged,
        };
    }

    // DEEP
    getMergedFormState(state, dispatch, action) {
        const formState = this.getFormState(action.form, state);

        var result = {
            ...formState,
        }

        const data = action.data;
        this.fields[action.form].forEach((field, fieldIndex) => {
            var fd = {
                ...formState[field]
            };
            result[field] = fd;

            var value = data[field];
            if (fd.touched) {
                if (fd.initial != value) {    // upravil hodnotu, ale mezitím někdo změnil tuto hodnotu, přepíšeme mu jí tou, co přišla
                    fd.initial = value;
                    fd.value = value;
                    fd.touched = false;
                    fd.visited = false;
                } else {    // editoval ji, ale někdo cizí menil jinou hodnotu, můžeme ji tedy nechat
                    // ...necháme hodnotu
                }
            } else {    // hodnotu neměnil, můžeme ji přepsat
                // Ostatní příznaky není třeba měnit
                fd.initial = value;
                fd.value = value;
            }
        });

        return result;
    }

    /**
     * Je již formulář naincializován - má již přes redux form inicializovány fieldy?
     * @param state
     * @param dispatch
     * @param action
     * @returns {boolean}
     */
    exists(state, dispatch, action) {
        if (!this.fields[action.form]) {
            return false;
        }

        const formState = this.getFormState(action.form, state);

        var someFieldExist = false;
        for (let a=0; a<this.initFields[action.form].length; a++) {
            const field = this.initFields[action.form][a];

            const dotIndex = field.indexOf('.');
            const openIndex = field.indexOf('[');
            let f;
            if (dotIndex >= 0 && (openIndex < 0 || dotIndex < openIndex)) { // is dot notation
                f = field.substring(0, dotIndex);
            } else if (openIndex >= 0 && (dotIndex < 0 || openIndex < dotIndex)) {  // is array notation
                f = field.substring(0, openIndex);
            } else {
                f = field;
            }

            if (formState[f]) {
                someFieldExist = true;
                break;
            }
        }

        return someFieldExist;
    }

    /**
     * Je tento formulář inline? - je registrovaný jako inline?
     * @param formName
     * @returns {*}
     */
    isSupported(formName) {
        return this.forms[formName];
    }

    // DEEP
    wasDataChanged(formName, state) {
        const changedInfo = this.wasChanged[formName];
        if (changedInfo) {
            if (changedInfo.bigChange) {    // bigChange bude nastaveno např. při odebrání nebo přidání řádků v kolekcích - již se to špatně testuje a tak při této změně dáme big změnu a již nebudeme testovat
                return true;
            }

            var changed = false;
            const keys = Object.keys(changedInfo);
            for (let a=0; a<keys.length; a++) {
                if (changedInfo[keys[a]]) {
                    changed = true;
                    break;
                }
            }
            return changed;
        } else {
            return false;
        }
    }

    // DEEP
    isDataValid(formName, data) {
        const init = this.init[formName];
        var errors = init.validate(data);
        var isValid = true;
        this.fields[formName].forEach(field => {
            if (errors[field]) {
                isValid = false;
            }
        })
        return isValid;
    }

    /**
     * Nastavení initial dat a vynulování stavu změněných položek, volá se po načtení nebo merge formuláře - máme stav, ze kterého budeme vycházet.
     * @param state
     * @param action
     */
    storeInitialData(state, action) {
        const formState = state.form[action.form];
        const initialFormData = reduxFormUtils.getValues(this.initFields[action.form], formState, true);
        this.initialFormData[action.form] = initialFormData;

        // !vynulování changes state!
        this.wasChanged[action.form] = {};
    }

    /**
     * Aktualizace změněných fieldů vůči původní initial hodnotě.
     * @param state
     * @param action
     */
    updateChanged(state, action) {
        // Initial hodnota
        const initialValue = reduxFormUtils.read(action.field, this.initialFormData[action.form]);

        // Nová hodnota
        const formData = this.getFormData(action.form, state);
        const currentValue = reduxFormUtils.read(action.field, formData);

        var changedInfo = this.wasChanged[action.form];
        if (!changedInfo) {
            changedInfo = {};
            this.wasChanged[action.form] = changedInfo;
        }
        if (!changedInfo.bigChange) {   // pokud je již big změna, nemá cenu udržovat podrobnosti o fieldech
            if (currentValue != initialValue) {
                changedInfo[action.field] = true;
            } else {
                changedInfo[action.field] = false;
            }
        }
    }

    /**
     * Pokud jsou data validní a byla změněna, colá se onSave callback.
     * @param state
     * @param dispatch
     * @param action
     */
    onBlur(state, dispatch, action) {
        if (!this.isSupported(action.form)) {
            return;
        }

        const init = this.init[action.form];
        if (!init) {
            console.error("Formulář není inicializován dispatchem initForm", action.form);
            return;
        }

        // const formState = this.getFormState(action.form, state);
        var data = this.getFormData(action.form, state);
        var changed = this.wasDataChanged(action.form, state);
        var isValid = this.isDataValid(action.form, data);

        if (changed && isValid) {
            init.onSave(data);
        }
    }
}();

var inlineFormMiddleware = function (_ref) {
    var getState = _ref.getState;
    var dispatch = _ref.dispatch;

    return (next) => {
        return (action) => {
            if (action.type === "redux-form/INITIALIZE") {
                if (inlineFormSupport.isSupported(action.form)) {
                    // Pokud formulář již existuje, pouze provedeme merge dat
                    if (inlineFormSupport.exists(getState(), dispatch, action)) {  // merge
                        const mergedState = inlineFormSupport.getMergedFormState(getState(), dispatch, action);

                        dispatch({
                            type: "redux-form/REPLACE_STATE",
                            formState: mergedState,
                        })

                        // Uchování prvotních dat pro porovnání změn po MERGE
                        inlineFormSupport.storeInitialData(getState(), action);
                    } else {    // init
                        next(action);
                        inlineFormSupport.setFields(action.form, action.fields);

                        // Uchování prvotních dat pro porovnání změn
                        inlineFormSupport.storeInitialData(getState(), action);
                    }
                } else {    // standardní poslání dál, není to náš formulář
                    next(action);
                }
            } else if (action.type === "redux-form/INPLACE_INIT") {
                inlineFormSupport.setInit(action.form, action.validate, action.onSave);
            } else if (action.type === "redux-form/CHANGE") {
                if (inlineFormSupport.isSupported(action.form)) {
                    var newAction = {
                        ...action,
                        touch: true,
                    }
                    next(newAction);

                    // Aktualizace wasChanged
                    inlineFormSupport.updateChanged(getState(), action);

                    // ---
                    var vfs = inlineFormSupport.getValidatedFormState(getState(), dispatch, action);
                    if (vfs.stateChanged) {
                        dispatch({
                            type: "redux-form/REPLACE_STATE",
                            formState: vfs.formState,
                        })
                    }
                } else {    // standardní poslání dál, není to náš formulář
                    next(action);
                }
            } else {
                next(action);

                switch (action.type) {
                    case "redux-form/BLUR":
                        inlineFormSupport.onBlur(getState(), dispatch, action);
                        break;
                }
            }
        }
    }
}

var createStoreWithMiddleware;
if (_logStoreState) {
    createStoreWithMiddleware = applyMiddleware(
        thunkMiddleware,
        loggerMiddleware,
        inlineFormMiddleware
    )(createStore)
} else {
    createStoreWithMiddleware = applyMiddleware(
        thunkMiddleware,
        inlineFormMiddleware
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

    var result = {
        partyRegion: partyRegion(store.partyRegion, action),
        registryRegion: registryRegion(store.registryRegion, action),
        arrRegion: arrRegion(store.arrRegion, action),
        fundRegion: fundRegion(store.fundRegion, action),
        adminRegion: adminRegion(store.adminRegion, action),
        splitter: splitter(store.splitter, action)
    }
    // console.log("SAVE", result)
    return result;
}

/**
 * Registrace inline formulářů.
 */
inlineFormSupport.addForm("outputEditForm");

// ----------------------------------------------------
module.exports = {
    store,
    save
}
