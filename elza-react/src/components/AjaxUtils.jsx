/**
 * Utility na ajaxové volání.
 *
 * @author Martin Šlapa
 * @since 8.12.2015
 * ---
 * Revize
 *
 * @author Petr Compel
 * @since 19.10.2016
 */
import React from 'react';
import {lenToBytesStr, roughSizeOfObject} from 'components/Utils.jsx';
//import {store} from '../stores/AppStoreState.jsx';
import {createException} from 'components/ExceptionUtils.jsx';
import i18n from './i18n';

import $ from 'jquery';
import {logout} from 'actions/global/login';

const serverContextPath = window.serverContextPath;

const clientLog = window.clientLog !== undefined && window.clientLog;

// Nastavení úrovně logování
const _logCalls = clientLog;
const _logErrors = clientLog;
const _logResults = clientLog;
const _logDuration = false; // moznost logovani delky volani

let _callIndex = 0;
let _store = null;
function setStore(appstore) {
    _store = appstore;
}

function requestCounter(method, url, data) {
    let callStr;
    if (_logCalls || _logResults) {
        let callIndex = _callIndex++;
        let indexStr = callIndex;
        if (callIndex < 10) indexStr = '0' + indexStr;
        if (callIndex < 100) indexStr = '0' + indexStr;
        if (callIndex < 1000) indexStr = '0' + indexStr;
        if (callIndex < 10000) indexStr = '0' + indexStr;
        callStr = '(' + indexStr + ') ' + method + ' ' + url;
    }
    return callStr;
}

/**
 * Vyřešení výjimky.
 *
 * @param status     stav
 * @param statusText textový zápis stavu
 * @param data       data výjimky
 */
function resolveException(status, statusText, data) {
    let result;
    if (status == 422) {
        // pro validaci
        result = {
            type: 'validation',
            validation: true,
            data: data,
        };
    } else if (status == 400) {
        result = {
            createToaster: true,
            type: 'BaseCode',
            code: 'BAD_REQUEST',
            level: 'danger',
            message: i18n('global.exception.bad.request.tech'),
            status: status,
            statusText: statusText,
        };
    } else if (status == 401) {
        result = {
            type: 'unauthorized',
            unauthorized: true,
            data: data,
        };
    } else if (data) {
        // other errors containing data
        result = {
            createToaster: true,
            type: data.type,
            code: data.code,
            level: data.level,
            properties: data.properties,
            message: data.message,
            stackTrace: data.stackTrace,
            status: status,
            statusText: statusText,
        };
    } else {
        // other unknown errors
        result = {
            type: 'unknown',
        };
    }

    if (result.createToaster) {
        console.log('___', _store);
        _store.dispatch(createException(result));
    }
    if (result.unauthorized) {
        _store.dispatch(logout());
    }

    return result;
}

/**
 * Zavolání raw ajaxového volání podle vložených parametrů.
 *
 * @param {string} url
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @param {string} method - Metoda volání (GET, POST, PUT, DELETE, ...)
 * @param {Object} data - Odesílaná data
 * @param {string|bool} contentType - content Type v hlavičce
 * @param {bool} ignoreError - zda se mají chyby ignorovat
 * @returns {Promise} - Výsledek volání
 */
function ajaxCallRaw(url, params, method, data, contentType = false, ignoreError = false) {
    url = updateQueryStringParameters(url, params);

    return new Promise((resolve, reject) => {
        const callStr = requestCounter(method, url, data);

        let tStart;
        if (_logDuration) {
            tStart = new Date().getTime();
        }

        if (_logCalls) {
            console.info('->', callStr);
        }

        $.ajax({
            url: serverContextPath + url,
            type: method,
            processData: false,
            contentType: contentType,
            data: data,
            success: function (
                data, // data ze serveru
                status, // status - 'success'
                xhr,
            ) {
                // xhr - responseText, responseJSON, status a statusText
                if (_logResults) {
                    const lenStr = '(' + lenToBytesStr(roughSizeOfObject(data)) + ')';
                    if (_logDuration) {
                        const t = new Date().getTime() - tStart;
                        console.info('<-', callStr, lenStr, data, t + ' ms');
                    } else {
                        console.info('<-', callStr, lenStr, data);
                    }
                }
                resolve(data);
            },
            error: function (xhr, status, err) {
                if (_logErrors) {
                    if (ignoreError) {
                        console.warn('<-', callStr, '[' + xhr.status + '-' + status + ']', xhr);
                    } else {
                        console.error('<-', callStr, '[' + xhr.status + '-' + status + ']', xhr);
                    }
                }

                let result = resolveException(xhr.status, xhr.statusText, xhr.responseJSON);
                reject(result);
            },
        });
    });
}

/**
 * Zavolání ajaxového volání podle vložených parametrů.
 *
 * @param {string} url
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @param {string} method - Metoda volání (GET, POST, PUT, DELETE, ...)
 * @param {Object} data - Odesílaná data
 * @returns {Promise} - Výsledek volání
 */
function ajaxCall(url, params, method, data) {
    url = updateQueryStringParameters(url, params);

    return new Promise((resolve, reject) => {
        const callStr = requestCounter(method, url, data);

        let tStart;
        if (_logDuration) {
            tStart = new Date().getTime();
        }

        if (_logCalls) {
            console.info('->', callStr, data);
        }

        $.ajax({
            url: serverContextPath + url,
            type: method,
            async: true,
            cache: false,
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
            },
            data: data ? JSON.stringify(data) : null,
            success: function (
                data, // data ze serveru
                status, // status - 'success'
                xhr,
            ) {
                // xhr - responseText, responseJSON, status a statusText
                if (_logResults) {
                    const len = JSON.stringify(data).length;
                    const lenStr = '(' + lenToBytesStr(len) + ')';

                    if (_logDuration) {
                        const t = new Date().getTime() - tStart;
                        console.info('<-', callStr, lenStr, data, t + ' ms');
                    } else {
                        console.info('<-', callStr, lenStr, data);
                    }
                }
                resolve(data);
            },
            error: function (xhr, status, err) {
                if (_logErrors) {
                    console.error('<-', callStr, '[' + xhr.status + '-' + status + ']', xhr);
                }

                let result = resolveException(xhr.status, xhr.statusText, xhr.responseJSON);
                reject(result);
            },
        });
    });
}

/**
 * Provede aktualizaci všech query parametrů v adrese.
 *
 * @param {string} uri - Upravovaná adresa
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @returns {string} upravená adresa
 */
function updateQueryStringParameters(uri, params) {
    if (params) {
        for (let key in params) {
            if (params.hasOwnProperty(key)) {
                const value = params[key];
                uri = updateQueryStringParameter(uri, key, value);
            }
        }
    }
    return uri;
}

/**
 * Provede aktualizaci query parametru v adrese.
 * @param {string} uri - Upravovaná adresa
 * @param {string} key - Klíč parametru
 * @param {string} value - Hodnota parametru
 * @returns {string} upravená adresa
 */
function updateQueryStringParameter(uri, key, value) {
    if (value == null || value == undefined) {
        return uri;
    }

    const upValue = encodeURIComponent(value);

    const re = new RegExp('([?&])' + key + '=.*?(&|$)', 'i');
    const separator = uri.indexOf('?') !== -1 ? '&' : '?';
    if (uri.match(re)) {
        return uri.replace(re, '$1' + key + '=' + upValue + '$2');
    } else {
        return uri + separator + key + '=' + upValue;
    }
}

/**
 * Odeslání GET dotazu na server.
 *
 * @param {string} url - Adresa volání
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @returns {Promise} - Výsledek volání
 */
const ajaxGet = (url, params = null) => ajaxCall(url, params, 'GET', null);

/**
 * Odeslání POST dotazu na server.
 *
 * @param {string} url - Adresa volání
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @param {Object} data - Odesílaná data
 * @returns {Promise} - Výsledek volání
 */
const ajaxPost = (url, params = null, data = null) => ajaxCall(url, params, 'POST', data);

/**
 * Odeslání PUT dotazu na server.
 *
 * @param {string} url - Adresa volání
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @param {Object} data - Odesílaná data
 * @returns {Promise} - Výsledek volání
 */
const ajaxPut = (url, params = null, data = null) => ajaxCall(url, params, 'PUT', data);

/**
 * Odeslání DELETE dotazu na server.
 *
 * @param {string} url - Adresa volání
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @param {Object} data - Odesílaná data
 * @returns {Promise} - Výsledek volání
 */
const ajaxDelete = (url, params = null, data = null) => ajaxCall(url, params, 'DELETE', data);

export default {
    ajaxGet,
    ajaxPost,
    ajaxPut,
    ajaxDelete,
    ajaxCall,
    ajaxCallRaw,
    setStore,
};
