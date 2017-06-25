/**
 * Vstupní soubor pro UI - inicializace a zobrazení VIEW.
 */

'use strict'

// Import css Bootstrapu
import './elza-styles.less';

import React from 'react';
import { Utils } from 'components/index.jsx';
import {WebApi, WebApiCls} from 'actions/index.jsx';
import {loginFail} from 'actions/global/login.jsx';
import {userDetailChange} from 'actions/user/userDetail.jsx'


// Přidání custom style bsStyle action
import {Button} from 'react-bootstrap';
import {bootstrapUtils} from 'react-bootstrap/lib/utils';
bootstrapUtils.addStyle(Button, 'action');

// CustomEvent polyfill pro IE 9+
if ( typeof window.CustomEvent !== "function" ){
    function CustomEvent ( event, params ) {
    params = params || { bubbles: false, cancelable: false, detail: undefined };
    var evt = document.createEvent( 'CustomEvent' );
    evt.initCustomEvent( event, params.bubbles, params.cancelable, params.detail );
    return evt;
    }

    CustomEvent.prototype = window.Event.prototype;
    window.CustomEvent = CustomEvent;
}

// Globální init
Utils.init();
// es6-symbol polyfill nefunguje s kodem vygenerovanym pres babel (for-of iterace), musime pouzit core-js
require('core-js/fn/symbol');
require('core-js/fn/array');
const es6promise = require('es6-promise');
//var es5Shim = require('es5-shim');
es6promise.polyfill();

// Nastavení neomezeného počtu listenerů pro event emitter - v ELZA je emitter použit pro klávesové zkratky, kde je více listenerů
const EventEmitter = require('events').EventEmitter;
EventEmitter.defaultMaxListeners = 0

// Web socket
const websocket = require('./websocket');

function xx() {
    setTimeout(fc, 1000)
}
function fc() {
    console.log(document.activeElement)
    xx()
}
/** IE FIxy **/
const IE = Utils.detectIE();
if (IE !== false) {
    (function() {
        const html = document.getElementsByTagName("html")[0];
        if (IE < 12) {
            html.className = html.className + " ie ie"+ IE;
        } else {
            html.className = html.className + " ieEdge";
        }
    })();
}
/*
 IE Doesn't have a .startsWith either?
 */
if (!String.prototype.startsWith) {
    String.prototype.startsWith = function (str){
        return this.lastIndexOf(str, 0) === 0;
    };
}

//setTimeout(fc, 1500)

// Načtení dat z local storage = vrácení aplikace do předchozího stavu
import {AppStore} from 'stores/index.jsx';
import {storeRestoreFromStorage} from 'actions/store/store.jsx';
import {storeSave} from 'actions/store/storeEx.jsx';
import {i18n, Exception} from "components/index";
import {addToastr} from "components/shared/toastr/ToastrActions.jsx"
AppStore.store.dispatch(storeRestoreFromStorage());

window.onerror = function(message, url, line, column, error) {
    let devMessage = error;
    try {
        if (devMessage.stack) {
            devMessage = devMessage.stack
        }
    } catch (e) {}

    AppStore.store.dispatch(addToastr(i18n('exception.client'), [<Exception title={i18n('exception.client')} data={{
        message,
        devMessage: devMessage,
        properties: {
            url,
            line,
            column
        }
    }} />], "danger", "lg", null))
};

// Globální vypnutí focus na split buttony
import SplitToggle from './node_modules/react-bootstrap/lib/SplitToggle';
SplitToggle.defaultProps = {
    ...SplitToggle.defaultProps,
    tabIndex: -1
};

// Pokud dostane focus body, chceme jej změnit na implcitiní focus pro ribbon
import {setFocus} from 'actions/global/focus.jsx';
{
    const testBodyfocus = () => {
        if (document.activeElement === document.body) { // focus je na body, nastavíme ho podle aktuálně přepnuté oblasti
            AppStore.store.dispatch(setFocus(null, 1));
        }

        setTimeout(testBodyfocus, 150)
    };
    //testBodyfocus()
}
/*
import {setFocus} from 'actions/global/focus.jsx';
document.body.addEventListener("focus", () => {
    //AppStore.store.dispatch(setFocus(null, null, 'ribbon'));
})
*/

// Ukládání stavu aplikace
function scheduleStoreSave() {
    setTimeout(() => {
        AppStore.store.dispatch(storeSave());
        scheduleStoreSave();
    }, 1000)
}
scheduleStoreSave();

// seznam callbacků, které kvůli nepříhlášení se musí ještě vykonat
let calbacks = [];

const login = (callback) => {
    calbacks.push(callback);
    AppStore.store.dispatch(loginFail(() => {
        calbacks.forEach(callback => callback());
        calbacks = [];
    }));
}

// zjištění všech metod z api
const methods = Object.getOwnPropertyNames(WebApiCls.prototype);

// přetížení všech metod ve WebApi, původní metody mají prefix podtržítka
for(const i in  methods) {
    const method =  methods[i];
    WebApi["_" + method] = WebApi[method];
    WebApi[method] = (...x) => {
        return new Promise((resolve, reject) => {
            const ret = WebApi["_" + x[0]].call(...x);

            ret.then((json) => {
                resolve(json);
            }).catch((err) => {
                if (err.unauthorized) {
                    login(() => {
                        WebApi[x[0]].call(...x).then(resolve).catch(reject);
                    });
                } else {
                    reject(err);
                }
            });
        });

    }
    WebApi[method] = WebApi[method].bind(this, method);

}

// Načtení oprávnění a jeho uložení do store po přihlášení
WebApi.getUserDetail()
    .then(userDetail => {
        AppStore.store.dispatch(userDetailChange(userDetail))
    })

// Aplikace
import {AppContainer} from 'react-hot-loader'
import Redbox from 'redbox-react'
import ReactDOM from 'react-dom'

import Root from './router';


class CustomRedbox extends React.Component {
    static PropTypes = {
        error: React.PropTypes.instanceOf(Error).isRequired
    };

    render() {
        const {error} = this.props;
        console.error(error);
        return <Redbox error={error} />;
    }
}

const render = Component => {
    const MOUNT_POINT = document.getElementById('content');

    ReactDOM.render(
        <AppContainer errorReported={CustomRedbox}>
            <Component store={AppStore.store} />
        </AppContainer>,
        MOUNT_POINT
    )
};


render(Root);

if (module.hot) {
    module.hot.accept('./router', () => render(Root));
}
