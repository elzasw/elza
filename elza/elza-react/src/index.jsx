/**
 * Vstupní soubor pro UI - inicializace a zobrazení VIEW.
 */
// Import css Bootstrapu
import './elza-styles.scss';
import 'font-awesome/css/font-awesome.css';
import './elza-iconset/ez-icons.css';
import React from 'react';
import {Utils} from './components/shared';
import * as es6promise from 'es6-promise';
import Moment from 'moment';
import momentLocalizer from 'react-widgets-moment';
import {store} from 'stores/index.jsx';
import AjaxUtils from 'components/AjaxUtils.jsx';
import EventEmitter from 'events';
// Přidání custom style variant action
//import {Button,} from 'react-bootstrap';
//import {bootstrapUtils} from 'react-bootstrap';

// Pokud dostane focus body, chceme jej změnit na implcitiní focus pro ribbon
import {setFocus} from 'actions/global/focus.jsx';
import {FOCUS_KEYS} from './constants.tsx';

import websocket from './websocketActions.jsx';
import {storeRestoreFromStorage} from 'actions/store/store.jsx';
import {storeSave} from 'actions/store/storeEx.jsx';
import {i18n, Exception} from 'components/shared';

import {addToastr} from 'components/shared/toastr/ToastrActions.jsx';

import ReactDOM from 'react-dom';
import Root from './router';


//bootstrapUtils.addStyle(Button, 'action');

// CustomEvent polyfill pro IE 9+
if (typeof window.CustomEvent !== 'function') {
    function CustomEvent(event, params) {
        params = params || {bubbles: false, cancelable: false, detail: undefined};
        var evt = document.createEvent('CustomEvent');
        evt.initCustomEvent(event, params.bubbles, params.cancelable, params.detail);
        return evt;
    }

    CustomEvent.prototype = window.Event.prototype;
    window.CustomEvent = CustomEvent;
}

// Globální init
Utils.init();

es6promise.polyfill();

// Nastavení neomezeného počtu listenerů pro event emitter - v ELZA je emitter použit pro klávesové zkratky, kde je více listenerů
EventEmitter.defaultMaxListeners = 0;

function xx() {
    setTimeout(fc, 1000);
}

function fc() {
    console.log(document.activeElement);
    xx();
}

/** IE FIxy **/
const IE = Utils.detectIE();
if (IE !== false) {
    (function() {
        const html = document.getElementsByTagName('html')[0];
        if (IE < 12) {
            html.className = html.className + ' ie ie' + IE;
        } else {
            html.className = html.className + ' ieEdge';
        }
    })();
}
/*
 IE Doesn't have a .startsWith either?
 */
if (!String.prototype.startsWith) {
    String.prototype.startsWith = function(str) {
        return this.lastIndexOf(str, 0) === 0;
    };
}

//setTimeout(fc, 1500)

Moment.locale(window.navigator.language || 'cs-CZ');
momentLocalizer();

// Načtení dat z local storage = vrácení aplikace do předchozího stavu

AjaxUtils.setStore(store);
// Web socket - až po initu store
store.dispatch(storeRestoreFromStorage());

window.onerror = function(message, url, line, column, error) {
    let stackTrace = error;
    try {
        if (stackTrace.stack) {
            stackTrace = stackTrace.stack;
        }
    } catch (e) {
    }

    store.dispatch(addToastr(i18n('exception.client'), [<Exception key="exception-key-onerror"
                                                                   title={i18n('exception.client')} data={{
        message,
        stackTrace: stackTrace,
        properties: {
            url,
            line,
            column,
        },
    }}/>], 'danger', 'lg', null));
};

// Globální vypnutí focus na split buttony
/*
SplitToggle.defaultProps = {
    ...SplitToggle.defaultProps,
    tabIndex: -1
};
*/
{
    const testBodyfocus = () => {
        if (document.activeElement === document.body) { // focus je na body, nastavíme ho podle aktuálně přepnuté oblasti
            store.dispatch(setFocus(FOCUS_KEYS.NONE, 1));
        }

        setTimeout(testBodyfocus, 150);
    };
    //testBodyfocus()
}
/*
import {setFocus} from 'actions/global/focus.jsx';
document.body.addEventListener("focus", () => {
    //store.dispatch(setFocus(null, null, 'ribbon'));
})
*/

// Ukládání stavu aplikace
function scheduleStoreSave() {
    setTimeout(() => {
        store.dispatch(storeSave());
        scheduleStoreSave();
    }, 10000);
}

scheduleStoreSave();

// Aplikace

const render = Component => {
    const MOUNT_POINT = document.getElementById('content');

    ReactDOM.render(<Component store={store}/>, MOUNT_POINT);
};


render(Root);
