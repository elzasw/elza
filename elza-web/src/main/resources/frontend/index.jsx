/**
 * Vstupní soubor pro UI - inicializace a zobrazení VIEW.
 */

'use strict'

// Import css Bootstrapu
require ('./elza-styles.less');

import React from 'react';
import ReactDOM from 'react-dom';
import { createHistory, useBasename } from 'history'
import { Route, Link, History, Lifecycle } from 'react-router'
import { Utils } from 'components'
import {WebApi, WebApiCls} from 'actions'
import {loginFail} from 'actions/global/login';
import {userDetailChange} from 'actions/user/userDetail'

// Globální init
Utils.init();
var es6promise = require('es6-promise');
//var es5Shim = require('es5-shim');

// Nastavení neomezeného počtu listenerů pro event emitter - v ELZA je emitter použit pro klávesové zkratky, kde je více listenerů
var EventEmitter = require('events').EventEmitter;
EventEmitter.defaultMaxListeners = 0

// Web socket
var websocket = require('./websocket');

function xx() {
    setTimeout(fc, 1000)
}
function fc() {
    console.log(document.activeElement)
    xx()
}
//setTimeout(fc, 1500)

// Načtení dat z local storage = vrácení aplikace do předchozího stavu
import {AppStore} from 'stores';
import {storeSave, storeRestoreFromStorage} from 'actions/store/store';
AppStore.store.dispatch(storeRestoreFromStorage());

// Globální vypnutí focus na split buttony
import SplitToggle from './node_modules/react-bootstrap/lib/SplitToggle';
SplitToggle.defaultProps = {
    ...SplitToggle.defaultProps,
    tabIndex: -1
}

// Pokud dostane focus body, chceme jej změnit na implcitiní focus pro ribbon
import {setFocus} from 'actions/global/focus';
{
    function testBodyfocus() {
        if (document.activeElement === document.body) { // focus je na body, nastavíme ho podle aktuálně přepnuté oblasti
            AppStore.store.dispatch(setFocus(null, 1));
        }

        setTimeout(testBodyfocus, 150)
    }
    //testBodyfocus()
}
/*
import {setFocus} from 'actions/global/focus';
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
var calbacks = [];

const login = (callback) => {
    calbacks.push(callback);
    AppStore.store.dispatch(loginFail(() => {
        calbacks.forEach(callback => callback());
        calbacks = [];
    }));
}

// zjištění všech metod z api
var methods = Object.getOwnPropertyNames(WebApiCls.prototype);

// přetížení všech metod ve WebApi, původní metody mají prefix podtržítka
for(var i in  methods) {
    var method =  methods[i];
    WebApi["_" + method] = WebApi[method];
    WebApi[method] = (...x) => {
        return new Promise((resolve, reject) => {
            var ret = WebApi["_" + x[0]].call(...x);

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

    }.bind(this, method);
}

// Načtení oprávnění a jeho uložení do store po přihlášení
WebApi.getUserDetail()
    .then(userDetail => {
        AppStore.store.dispatch(userDetailChange(userDetail))
    })

// Aplikace
var Router = require('./router');
Router.start();
