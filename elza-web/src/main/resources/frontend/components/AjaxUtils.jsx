/**
 * Utility na ajaxové volání.
 *
 * @author Martin Šlapa
 * @since 8.12.2015
 */

var React = require('react');
import {i18n, Toastr, LongText} from 'components';

/**
 * Zavolání raw ajaxového volání podle vložených parametrů.
 *
 * @param {string} url
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @param {string} method - Metoda volání (GET, POST, PUT, DELETE, ...)
 * @param {Object} data - Odesílaná data
 * @returns {Promise} - Výsledek volání
 */
function ajaxCallRaw(url, params, method, data) {

    url = updateQueryStringParameters(url, params);

    return new Promise(function (resolve, reject) {
        console.log("#AjaxRaw." + method + " " + url, data);
        $.ajax({
            url: serverContextPath + url,
            type: method,
            processData: false,
            contentType: false,
            data: data,
            success: function (data,    // data ze serveru
                               status,  // status - 'success'
                               xhr) {   // xhr - responseText, responseJSON, status a statusText
                resolve(data);
            },
            error: function (xhr,
                             status,
                             err) {
                console.log("#AjaxRaw [" + xhr.status + "-" + status + "]",  xhr);

                var message;
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                } else if (xhr.responseText) {
                    message = xhr.responseText;
                } else {
                    message = null;
                }

                var result;

                if (xhr.status == 422) { // pro validaci
                    result = {
                        type: 'validation',
                        validation: true,
                        data: xhr.responseJSON
                    };
                } else { // ostatni
                    result = {
                        type: 'error',
                        error: true,
                        title: i18n('global.error.ajax'),
                        message: message,
                        status: xhr.status,
                        statusText: xhr.statusText
                    };
                }

                if (result.error) {

                    var messages = new Array();
                    messages.push(<small>{result.statusText} [{result.status}]</small>);
                    if (result.message) {
                        messages.push(<p><LongText text={result.message}/></p>);
                    }
                    Toastr.Actions.danger({title: result.title, message: messages});
                }

                reject(result);

            }
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

    return new Promise(function (resolve, reject) {
        console.log("#Ajax." + method + " " + url, data);
        $.ajax({
            url: serverContextPath + url,
            type: method,
            async: true,
            cache: false,
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            data: data ? JSON.stringify(data) : null,
            success: function (data,    // data ze serveru
                               status,  // status - 'success'
                               xhr) {   // xhr - responseText, responseJSON, status a statusText
                resolve(data);
            },
            error: function (xhr,
                             status,
                             err) {
                console.log("#Ajax [" + xhr.status + "-" + status + "]", xhr);

                var message;
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                } else if (xhr.responseText) {
                    message = xhr.responseText;
                } else {
                    message = null;
                }

                var result;

                if (xhr.status == 422) { // pro validaci
                    result = {
                        type: 'validation',
                        validation: true,
                        data: xhr.responseJSON
                    };
                } else { // ostatni
                    result = {
                        type: 'error',
                        error: true,
                        title: i18n('global.error.ajax'),
                        message: message,
                        status: xhr.status,
                        statusText: xhr.statusText
                    };
                }

                if (result.error) {

                    var messages = new Array();
                    messages.push(<small>{result.statusText} [{result.status}]</small>);
                    if (result.message) {
                        messages.push(<p><LongText text={result.message}/></p>);
                    }
                    Toastr.Actions.danger({title: result.title, message: messages});
                }

                reject(result);
            }
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
        for(var key in params) {
            var value = params[key];
            uri = updateQueryStringParameter(uri, key, value);
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
    var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
    var separator = uri.indexOf('?') !== -1 ? "&" : "?";
    if (uri.match(re)) {
        return uri.replace(re, '$1' + key + "=" + value + '$2');
    } else {
        return uri + separator + key + "=" + value;
    }
}

/**
 * Odeslání GET dotazu na server.
 *
 * @param {string} url - Adresa volání
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @returns {Promise} - Výsledek volání
 */
function ajaxGet(url, params) {
    return ajaxCall(url, params, "GET", null);
}

/**
 * Odeslání POST dotazu na server.
 *
 * @param {string} url - Adresa volání
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @param {Object} data - Odesílaná data
 * @returns {Promise} - Výsledek volání
 */
function ajaxPost(url, params, data) {
    return ajaxCall(url, params, "POST", data);
}

/**
 * Odeslání PUT dotazu na server.
 *
 * @param {string} url - Adresa volání
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @param {Object} data - Odesílaná data
 * @returns {Promise} - Výsledek volání
 */
function ajaxPut(url, params, data) {
    return ajaxCall(url, params, "PUT", data);
}

/**
 * Odeslání DELETE dotazu na server.
 *
 * @param {string} url - Adresa volání
 * @param {Object} params - Object query parametrů -> klíč: hodnota
 * @param {Object} data - Odesílaná data
 * @returns {Promise} - Výsledek volání
 */
function ajaxDelete(url, params, data) {
    return ajaxCall(url, params, "DELETE", data);
}

module.exports = {
    ajaxGet: ajaxGet,
    ajaxPost: ajaxPost,
    ajaxPut: ajaxPut,
    ajaxDelete: ajaxDelete,
    ajaxCall: ajaxCall,
    ajaxCallRaw: ajaxCallRaw
}