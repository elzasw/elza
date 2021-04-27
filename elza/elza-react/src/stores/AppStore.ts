import * as types from 'actions/constants/ActionTypes';
import {applyMiddleware, compose, createStore, Middleware} from 'redux';
import thunkMiddleware from 'redux-thunk';
import {createLogger} from 'redux-logger';
import {lenToBytesStr, roughSizeOfObject} from 'components/Utils.jsx';
import {splitterResize} from 'actions/global/splitter.jsx';
import {composeWithDevTools} from 'redux-devtools-extension';
import rootReducer from './reducers.jsx';
import reduxFormUtils from './app/form/reduxFormUtils.jsx';
import reduxImmutableStateInvariant from 'redux-immutable-state-invariant';

/**
 * reducery pro save
 */
import app from './app/app.jsx';
import arrRegion from './app/arr/arrRegion.jsx';
import fundRegion from './app/fund/fundRegion';
import splitter from './app/global/splitter.jsx';
import adminRegion from './app/admin/adminRegion.jsx';
import {AppWindow} from '../typings/globals';
import {FormErrors} from 'redux-form';

// Nastavení úrovně logování
const _logStoreSize = false;
const _logActionDuration = false;
const _logCollapsed = true;

const appWindow = (window as any) as AppWindow;

const clientLog = (window as any).clientLog !== undefined && (window as any).clientLog;

// Store a middleware
const loggerMiddleware = createLogger({
          collapsed: _logCollapsed,
          duration: _logActionDuration,
          predicate: (getState, action) =>
            appWindow.__DEV__
            //&& clientLog
            && action.type !== types.STORE_STATE_DATA
            && action.type !== types.GLOBAL_SPLITTER_RESIZE,
      });

/**
 * Třída pro definici inline formulářů.
 */
const inlineFormSupport = new (class InlineFormSupport {
    forms: {[key: string]: boolean} = {};
    init: {[key: string]: {validate: Function; onSave: Function}} = {};
    fields: {[key: string]: any} = {};
    initFields: {[key: string]: any} = {};
    initialFormData: {[key: string]: any} = {};
    wasChanged: {[key: string]: {bigChange?: boolean} & {[key: string]: boolean}} = {};

    addForm(formName: string) {
        this.forms[formName] = true;
    }

    getFormData(formName: string, state) {
        const formState = state.form[formName];
        const reduxFormData = reduxFormUtils.getValues(this.initFields[formName], formState, false);
        return reduxFormData;
    }

    setFields(formName: string, fields) {
        this.initFields[formName] = fields;
        this.fields[formName] = fields;
    }

    /**
     * Prvotní inicializace.
     * @param formName
     * @param validate
     * @param onSave
     */
    setInit(formName: string, validate: Function, onSave: Function) {
        if (!validate) {
            console.error('Chyba inicializace formuláře', formName, ' chybí validate.');
        }
        if (!onSave) {
            console.error('Chyba inicializace formuláře', formName, ' chybí onSave.');
        }
        this.init[formName] = {validate, onSave};
    }

    /**
     * Načtení store pro formulář.
     * @param formName
     * @param state
     * @param ignoreNotFound true, pokud se má ignorovat neexistence formuláře ve store
     * @returns {*}
     */
    getFormState(formName: string, state, ignoreNotFound = false) {
        const formState = state.form[formName];
        if (!formState) {
            !ignoreNotFound && console.error('Nenalezen store pro formulář', formName);
            return {};
        }
        return formState;
    }

    setAttributes(formName: string, formState, commonAttrs, fieldAttrs) {
        const formStateWithAttrs = reduxFormUtils.setAttributes(
            this.initFields[formName],
            formState,
            commonAttrs,
            fieldAttrs,
        );
        return formStateWithAttrs;
    }

    // DEEP
    // getValidatedFormState(state, dispatch, action) {
    //     const init = this.init[action.form];
    //     const formState = this.getFormState(action.form, state);
    //     var data = this.getFormData(action.form, state);
    //     var errors = init.validate(data);
    //
    //     var result = {
    //         ...formState,
    //     }
    //
    //     var stateChanged = false;
    //     this.fields[action.form].forEach(field => {
    //         if (errors[field]) {
    //             result[field] = {
    //                 ...result[field],
    //                 submitError: errors[field],
    //                 touched: true,
    //             }
    //             stateChanged = true;    // zatím natvrdo, ale chtělo by to porovnávat, zda jsme změnili chybu
    //         }
    //     })
    //
    //     return {
    //         formState: result,
    //         stateChanged,
    //     };
    // }

    mergeFormState(formName: string, localFormState, serverFormState, action) {
        console.log('....MERGE', 'local', localFormState, 'server', JSON.parse(JSON.stringify(serverFormState)));

        reduxFormUtils.mergeState(this.initFields[formName], localFormState, serverFormState);
        console.log('....MERGED', serverFormState);

        return serverFormState;
    }

    // DEEP
    getMergedFormState(state, dispatch, action) {
        console.log('>>>>>MERGE<<<<<');

        const formState = this.getFormState(action.form, state);
        console.log('Before merge', JSON.stringify(formState, null, 4));
        console.log('Merge with', JSON.stringify(action.form, null, 4));

        let result = {
            ...formState,
        };

        const data = action.data;
        this.fields[action.form].forEach((field, fieldIndex) => {
            let fd = {
                ...formState[field],
            };
            result[field] = fd;

            let value = data[field];
            if (fd.touched) {
                if (fd.initial != value) {
                    // upravil hodnotu, ale mezitím někdo změnil tuto hodnotu, přepíšeme mu jí tou, co přišla
                    fd.initial = value;
                    fd.value = value;
                    fd.touched = false;
                    fd.visited = false;
                } else {
                    // editoval ji, ale někdo cizí menil jinou hodnotu, můžeme ji tedy nechat
                    // ...necháme hodnotu
                }
            } else {
                // hodnotu neměnil, můžeme ji přepsat
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

        const formState = this.getFormState(action.form, state, true);

        let someFieldExist = false;
        for (let a = 0; a < this.initFields[action.form].length; a++) {
            const field = this.initFields[action.form][a];

            const dotIndex = field.indexOf('.');
            const openIndex = field.indexOf('[');
            let f;
            if (dotIndex >= 0 && (openIndex < 0 || dotIndex < openIndex)) {
                // is dot notation
                f = field.substring(0, dotIndex);
            } else if (openIndex >= 0 && (dotIndex < 0 || openIndex < dotIndex)) {
                // is array notation
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
    isSupported(formName: string) {
        return this.forms[formName];
    }

    setBigChange(formName: string) {
        const changedInfo = this.wasChanged[formName];
        if (changedInfo) {
            changedInfo.bigChange = true;
        }
    }

    wasDataChanged(formName: string, state) {
        const changedInfo = this.wasChanged[formName];
        if (changedInfo) {
            if (changedInfo.bigChange) {
                // bigChange bude nastaveno např. při odebrání nebo přidání řádků v kolekcích - již se to špatně testuje a tak při této změně dáme big změnu a již nebudeme testovat
                return true;
            }

            let changed = false;
            const keys = Object.keys(changedInfo);
            for (let a = 0; a < keys.length; a++) {
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

    isDataValid(formName: string, data) {
        const init = this.init[formName];
        let errors = init.validate(data);
        return !this._hasErrors(errors);
    }

    _hasErrors(errors: FormErrors) {
        const fields = Object.keys(errors);
        for (let a = 0; a < fields.length; a++) {
            const value = errors[fields[a]];
            if (value) {
                if (typeof value === 'object') {
                    if (this._hasErrors(value)) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
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

        let changedInfo = this.wasChanged[action.form];
        if (!changedInfo) {
            changedInfo = {};
            this.wasChanged[action.form] = changedInfo;
        }
        if (!changedInfo.bigChange) {
            // pokud je již big změna, nemá cenu udržovat podrobnosti o fieldech
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
    trySave(state, dispatch, action) {
        if (!this.isSupported(action.form)) {
            return;
        }

        const init = this.init[action.form];
        if (!init) {
            console.error('Formulář není inicializován dispatchem initForm', action.form);
            return;
        }

        // const formState = this.getFormState(action.form, state);
        let data = this.getFormData(action.form, state);
        let changed = this.wasDataChanged(action.form, state);
        let isValid = this.isDataValid(action.form, data);

        if (changed && isValid) {
            // Nastavení příznaku, že data byla odeslána na server, příznak slouží pro pozdější merge
            const formState = this.getFormState(action.form, state);
            const newFormState = this.setAttributes(action.form, formState, {sendToServer: true}, {touched: false});
            dispatch({
                type: 'redux-form/REPLACE_STATE',
                formState: newFormState,
            });

            // Odeslání dat na server
            init.onSave(data);
        }
    }
})();

const inlineFormMiddleware = function (_ref) {
    const getState = _ref.getState;
    const dispatch = _ref.dispatch;

    return next => {
        return action => {
            if (action.type === 'redux-form/INITIALIZE') {
                if (inlineFormSupport.isSupported(action.form)) {
                    // Pokud formulář již existuje, pouze provedeme merge dat
                    if (inlineFormSupport.exists(getState(), dispatch, action)) {
                        // merge
                        // Uchování aktuálního store pro pozdější merge
                        // Načtení aktuálních formulářových dat
                        const localFormState = inlineFormSupport.getFormState(action.form, getState());

                        delete getState().form[action.form];
                        // Promítnutí aktuálních nových příchozích dat do store
                        next(action);

                        // Načtení store, který odpovídá novým datům
                        const serverFormState = inlineFormSupport.getFormState(action.form, getState());

                        // Provedení merge local a server formuláře a nastavení výsledného store
                        inlineFormSupport.mergeFormState(action.form, localFormState, serverFormState, action);
                        // !!!!neni potreba, protoze jiz upravujeme serverFormState
                        // dispatch({
                        //     type: "redux-form/REPLACE_STATE",
                        //     formState: serverFormState,
                        // })

                        // Načtení nového merge stavu
                        // const mergedState = inlineFormSupport.getMergedFormState(getState(), dispatch, action);
                        //
                        // dispatch({
                        //     type: "redux-form/REPLACE_STATE",
                        //     formState: mergedState,
                        // })

                        // Uchování prvotních dat pro porovnání změn po MERGE
                        inlineFormSupport.storeInitialData(getState(), action);
                    } else {
                        // init
                        next(action);
                        inlineFormSupport.setFields(action.form, action.fields);

                        // Uchování prvotních dat pro porovnání změn
                        inlineFormSupport.storeInitialData(getState(), action);
                    }
                } else {
                    // standardní poslání dál, není to náš formulář
                    next(action);
                }
            } else if (action.type === 'redux-form/INPLACE_INIT') {
                inlineFormSupport.setInit(action.form, action.validate, action.onSave);
            } else if (
                action.type === 'redux-form/ADD_ARRAY_VALUE' ||
                action.type === 'redux-form/REMOVE_ARRAY_VALUE'
            ) {
                if (inlineFormSupport.isSupported(action.form)) {
                    inlineFormSupport.setBigChange(action.form);
                }
                next(action);

                if (action.type === 'redux-form/REMOVE_ARRAY_VALUE') {
                    inlineFormSupport.trySave(getState(), dispatch, action);
                }
            } else if (action.type === 'redux-form/CHANGE') {
                if (inlineFormSupport.isSupported(action.form)) {
                    let newAction = {
                        ...action,
                        touch: true,
                    };
                    next(newAction);

                    // Aktualizace wasChanged
                    inlineFormSupport.updateChanged(getState(), action);

                    // ---
                    // let vfs = inlineFormSupport.getValidatedFormState(getState(), dispatch, action);
                    // if (vfs.stateChanged) {
                    //     dispatch({
                    //         type: "redux-form/REPLACE_STATE",
                    //         formState: vfs.formState,
                    //     })
                    // }
                } else {
                    // standardní poslání dál, není to náš formulář
                    next(action);
                }
            } else {
                next(action);

                switch (action.type) {
                    case 'redux-form/BLUR':
                        inlineFormSupport.trySave(getState(), dispatch, action);
                        break;
                    default:
                        break;
                }
            }
        };
    };
};

let createStoreWithMiddleware;

if (appWindow.__DEV__) {
    const composeFunction =
        typeof appWindow.__DEVTOOLS__ !== 'undefined' && appWindow.__DEVTOOLS__
            ? ((composeWithDevTools as any) as typeof compose)
            : compose;

    const immutableMiddleware = reduxImmutableStateInvariant({
        ignore: [
            'adminRegion',
            'app',
            'contextMenu',
            'developer',
            'focus',
            'form',
            'fundRegion',
            'login',
            'modalDialog',
            'refTables',
            'router',
            'splitter',
            'stateRegion',
            'status',
            'structures',
            'tab',
            'toastr',
            'userDetail',
            'websocket',
        ],
    });

    const middleWares: Middleware<any, any, any>[] = [
        // immutableMiddleware,
        thunkMiddleware,
        loggerMiddleware,
        inlineFormMiddleware,
    ];

    createStoreWithMiddleware = composeFunction(applyMiddleware(...middleWares))(createStore);
} else {
    createStoreWithMiddleware = compose(applyMiddleware(thunkMiddleware, inlineFormMiddleware))(createStore);
}

const initialState = {};
export const store = (function configureStore(initialState) {
    const state = createStoreWithMiddleware(rootReducer, initialState);
    if (module.hot) {
        // Enable Webpack hot module replacement for reducers
        module.hot.accept('./reducers.jsx', () => {
            const nextRootReducer = require('./reducers.jsx').default;

            state.replaceReducer(nextRootReducer);
        });
    }
    return state;
})(initialState);

/*
  const finalCreateStore = compose(
    applyMiddleware(thunkMiddleware),
    typeof window === 'object' && typeof window.devToolsExtension !== 'undefined' ? window.devToolsExtension() : f => f
  )(createStore);

  const _store = finalCreateStore(reducer, initialState);
*/
/*
import {selectFundTab} from 'actions/arr/fund.jsx'
let fund = Object.assign({id: 1, versionId: 1});
store.dispatch(selectFundTab(fund));
*/

// Resize
window.addEventListener('resize', () => {
    const state = store.getState();
    store.dispatch(splitterResize(state.splitter.leftSize, state.splitter.rightSize));
});

if (_logStoreSize) {
    let curr;

    function handleChange() {
        let prev = curr;
        curr = store.getState().arrRegion;

        if (curr !== prev) {
            let lenStr = lenToBytesStr(roughSizeOfObject(curr));
            console.log('Velikost store', lenStr);
            //console.log("@@@@@@@@@@@@@@@@@@@@@@CHANGE", prev, curr);
        }
    }

    store.subscribe(handleChange);
}

export const save = function (store) {
    const action = {
        type: types.STORE_SAVE,
    };

    //let rrd = registryRegionData(store.registryRegionData, action)
    //console.log(result.registryRegion);
    // result.registryRegion._info = result.registryRegion.registryRegionData._info
    // result.registryRegion.selectedId = result.registryRegion.registryRegionData.selectedId

    const result = {
        app: app(store.app, action),
        arrRegion: arrRegion(store.arrRegion, action),
        fundRegion: fundRegion(store.fundRegion, action),
        adminRegion: adminRegion(store.adminRegion, action),
        splitter: splitter(store.splitter, action),
    };
    // console.log("SAVE", result)
    return result;
};

/**
 * Registrace inline formulářů.
 */
inlineFormSupport.addForm('outputEditForm');
inlineFormSupport.addForm('permissionsEditForm');
inlineFormSupport.addForm('requestEditForm');
