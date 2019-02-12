
import {normalizeDoubleWithDot, pad2} from 'components/validate.jsx'
import {ShortcutManager} from 'react-shortcuts';

/**
 * Utility metody.
 */

import "./Utils.less";

export function consolidateState(prevState, newState) {
    var equals = stateEquals(prevState, newState);
    if (!equals) {
        //console.log(newState);
    }
    return equals ? prevState : newState;
}

export function chooseInputEl(el1, el2) {
    var result = null;

    if (el1 && el2) {
        var el1Rect = el1.getBoundingClientRect();
        var el2Rect = el2.getBoundingClientRect();

            if (el1Rect.top < el2Rect.top) {
                result = el1;
            } else if (el1Rect.top == el2Rect.top && el1Rect.left < el2Rect.left) {
                result = el1;
            } else {
                result = el2;
            }
    } else if (!el1 && el2) {
        result = el2;
    } else if (el1 && !el2) {
        result = el1;
    }
    return result;
}

/**
 * Nastavení focusu na první viditelný nezakázaný element pro editaci.
 * @param el {Object} container
 * @param setInputFocus {bool} true, pokud má být obsah vybraný (např. u input type text)
 * @return {bool} true, pokud se podařilo najít a nastavit focus
 */
export function setInputFocus(el, selectContent = false) {
    var elem = $('input:visible:enabled', el).get(0);
    var select = $('select:visible:enabled', el).get(0);
    elem = chooseInputEl(elem, select);

    var textarea = $('textarea:visible:enabled', el).get(0);
    elem = chooseInputEl(elem, textarea);

    var button = $('button:visible:enabled', el).get(0);
    elem = chooseInputEl(elem, button);

    // Vlastní prvky podle definovaného tab indexu
    var custom = $('div,shortcut', el)
        .filter(function() {
            return $(this).attr("tabIndex") >= 0;
        })
        .get(0);
    elem = chooseInputEl(elem, custom);

    if (elem) {
        elem.focus();
        if (selectContent) {
            elem.select();
        }
        return true
    }
    return false
}

export function propsEquals(x, y, attrs) {
    if (typeof attrs !== 'undefined' && attrs !== null) {
        for (var a=0; a<attrs.length; a++) {
            var p = attrs[a];

            if ( ! x.hasOwnProperty( p ) ) continue;
              // other properties were tested using x.constructor === y.constructor

            if ( ! y.hasOwnProperty( p ) ) {
                return false;
            }
              // allows to compare x[ p ] and y[ p ] when set to undefined

            if ( x[ p ] === y[ p ] ) continue;
              // if they have the same strict value or identity then they are equal

            if ( typeof( x[ p ] ) !== "object" ) {
                return false;
            }
              // Numbers, Strings, Functions, Booleans must be strictly equal

            if (x[ p ] !==  y[ p ] ) {
                return false;
            }
        }
        return true;
    } else {
        return stateEquals(x, y)
    }
}

export function stateEquals(x, y) {
  for ( var p in x ) {
    if ( ! x.hasOwnProperty( p ) ) continue;
      // other properties were tested using x.constructor === y.constructor

    if ( ! y.hasOwnProperty( p ) ) {
        return false;
    }
      // allows to compare x[ p ] and y[ p ] when set to undefined

    if ( x[ p ] === y[ p ] ) continue;
      // if they have the same strict value or identity then they are equal

    if ( typeof( x[ p ] ) !== "object" && typeof( x[ p ] ) !== "boolean") {
        return false;
    }

    // Numbers, Strings, Functions, Booleans must be strictly equal
    if (x[ p ] !==  y[ p ] ) {
//console.log(p)
        return false;
    }
  }
    return true;
}

export function objectEquals( x, y ) {
  if ( x === y ) return true;
    // if both x and y are null or undefined and exactly the same

  if ( ! ( x instanceof Object ) || ! ( y instanceof Object ) ) return false;
    // if they are not strictly equal, they both need to be Objects

  if ( x.constructor !== y.constructor ) return false;
    // they must have the exact same prototype chain, the closest we can do is
    // test there constructor.

  for ( var p in x ) {
    if ( ! x.hasOwnProperty( p ) ) continue;
      // other properties were tested using x.constructor === y.constructor

    if ( ! y.hasOwnProperty( p ) ) return false;
      // allows to compare x[ p ] and y[ p ] when set to undefined

    if ( x[ p ] === y[ p ] ) continue;
      // if they have the same strict value or identity then they are equal

    if ( typeof( x[ p ] ) !== "object" ) return false;
      // Numbers, Strings, Functions, Booleans must be strictly equal

    if ( ! Object.equals( x[ p ],  y[ p ] ) ) return false;
      // Objects and Arrays must be tested recursively
  }

  for ( p in y ) {
    if ( y.hasOwnProperty( p ) && ! x.hasOwnProperty( p ) ) return false;
      // allows x[ p ] to be set to undefined
  }
  return true;
}

export function lenToBytesStr(len) {
    var lenStr;
    if (len < 1000) {
        lenStr = ('' + len).substring(0, 3) + " B";
    } else if (len < 1000000) {
        lenStr = ('' + (len/1000)).substring(0, 3) + " kB";
    } else {
        lenStr = ('' + (len/1000000)).substring(0, 3) + " MB";
    }
    return lenStr;
}

export function humanFileSize(bytes, si = false) {
    var thresh = si ? 1000 : 1024;
    if (Math.abs(bytes) < thresh) {
        return bytes + ' B';
    }
    var units = si
        ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
        : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
    var u = -1;
    do {
        bytes /= thresh;
        ++u;
    } while (Math.abs(bytes) >= thresh && u < units.length - 1);
    return bytes.toFixed(1) + ' ' + units[u];
}

export function roughSizeOfObject( object ) {

    var objectList = [];
    var stack = [ object ];
    var bytes = 0;

    while ( stack.length ) {
        var value = stack.pop();

        if ( typeof value === 'boolean' ) {
            bytes += 4;
        }
        else if ( typeof value === 'string' ) {
            bytes += value.length * 2;
        }
        else if ( typeof value === 'number' ) {
            bytes += 8;
        }
        else if
        (
            typeof value === 'object'
            && objectList.indexOf( value ) === -1
        )
        {
            objectList.push( value );

            for( var i in value ) {
                stack.push( value[ i ] );
            }
        }
    }
    return bytes;
}

export function StringSet() {
    var setObj = {}, val = {};

    this.add = function(str) {
        setObj[str] = val;
    };

    this.clear = function() {
        setObj = {};
    };

    this.size = function() {
        var size = 0;
        for (var i in setObj) {
            size++;
        }
        return size;
    };

    this.contains = function(str) {
        return setObj[str] === val;
    };

    this.remove = function(str) {
        delete setObj[str];
    };

    this.values = function() {
        var values = [];
        for (var i in setObj) {
            if (setObj[i] === val) {
                values.push(i);
            }
        }
        return values;
    };
}

export function StringMap() {
    var setObj = {};

    this.put = function(key, val) {
        setObj[key] = val;
    };

    this.get = function(key) {
        return setObj[key];
    };

    this.contains = function(key) {
        return typeof(setObj[key]) !== 'undefined';
    };

    this.remove = function(key) {
        delete setObj[key];
    };

    this.values = function() {
        var values = [];
        for (var i in setObj) {
            values.push(setObj[i]);
        }
        return values;
    };
}

// Inicializace typu prohlížeče
export function browser() {
    return {
        // Opera 8.0+
        opera: (!!window.opr && !!window.opr.addons) || !!window.opera || navigator.userAgent.indexOf(' OPR/') >= 0,
        // Firefox 1.0+
        firefox: typeof InstallTrigger !== 'undefined',
        // At least Safari 3+: "[object HTMLElementConstructor]"
        safari: Object.prototype.toString.call(window.HTMLElement).indexOf('Constructor') > 0,
        // Internet Explorer 6-11
        ie: /*@cc_on!@*/false || !!document.documentMode,
        // Edge 20+
        edge: !window.ie && !!window.StyleMedia,
        // Chrome 1+
        chrome: !!window.chrome && !!window.chrome.webstore,
        // Blink engine detection
        blink: (window.chrome || window.opera) && !!window.CSS,
    };
}

export function getKeyModifier() {
    const brows = browser();
    if (brows.ie || brows.edge) {
        return 'ctrl+shift+';
    } else {
        return 'ctrl+alt+';
    }
}

export function init() {
    Array.prototype.each = function(callback){
        if (!callback) return false;
        for (var i=0; i<this.length; i++){
            if (callback(this[i], i) == false) break;
        }
    };
    Array.prototype.one = function(callback){
        var result = null;
        if (!callback) return result;
        for (var i=0; i<this.length; i++){
            var ret = callback(this[i], i);
            if (typeof ret != 'undefined' && ret !== null) {
                result = ret;
                break;
            }
        }
        return result;
    };
    if (typeof Object.values != 'function') {
        Object.values = x =>
            Object.keys(x).reduce((y, z) =>
            y.push(x[z]) && y, []);
    }
    if (typeof Object.assign != 'function') {
      (function () {
        Object.assign = function (target) {
          'use strict';
          if (target === undefined || target === null) {
            throw new TypeError('Cannot convert undefined or null to object');
          }

          var output = Object(target);
          for (var index = 1; index < arguments.length; index++) {
            var source = arguments[index];
            if (source !== undefined && source !== null) {
              for (var nextKey in source) {
                if (source.hasOwnProperty(nextKey)) {
                  output[nextKey] = source[nextKey];
                }
              }
            }
          }
          return output;
        };
      })();
    }
}

export function barrierCall(index, promise, onData, onError) {
    promise
        .then((result)=>{
            //console.log("Promise #" + index + " OK", result);
            onData(index, result);
        })
        .catch((error)=>{
            //console.log("Promise #" + index + " ERROR", error);
            onError(index, error);
        });
}

export function barrier(...promises) {
    var errors = {};
    var results = {};
    return new Promise(function (resolve, reject) {

        var tryFinish = () => {
            //console.log("TRY FINISH", results, errors, Object.keys(results).length, Object.keys(errors).length, promises.length);
            if (Object.keys(results).length + Object.keys(errors).length == promises.length) {
                var result = {};
                Object.keys(results).forEach(key => result[key] = { error: false, data: results[key] });
                Object.keys(errors).forEach(key => result[key] = { error: true, data: errors[key] });

                if (Object.keys(errors).length > 0) {
                    reject(result);
                } else {
                    resolve(result);
                }
            }
        }
        var handleData = (index, data) => {
            results[index] = data;
            tryFinish();
        }
        var handleError = (index, data) => {
            errors[index] = data;
            tryFinish();
        }

        for (var a=0; a<promises.length; a++) {
            barrierCall(a, promises[a], handleData, handleError);
        }
    })
}

/**
 * Převod datumu do řetězce - v budoucnu při více locale nahradit metodou pracující s locale.
 * @param date {Date} datum
 * @return {String} datum
 */
export function dateToString(date) {
    var dd = date.getDate().toString();
    var mm = (date.getMonth() + 1).toString();
    var yyyy = date.getFullYear().toString();
    return (dd[1] ? dd : "0" + dd[0]) + "." + (mm[1] ? mm : "0" + mm[0]) + "." + yyyy;
}

/**
 * Převod času do řetězce - v budoucnu při více locale nahradit metodou pracující s locale.
 * @param date {Date} datum
 * @return {String} datum
 */
export function timeToString(date) {
    var hh = date.getHours().toString();
    var ii = date.getMinutes().toString();
    var ss = date.getSeconds().toString();
    /** Formátování - místo 01 = 1 **/
    var f = (col) => (col[1] ? col : "0" + col[0]);
    return f(hh) + ":" + f(ii) + ":" + f(ss);
}

/**
 * Převod Obejct:Date do řetězce včetně času
 * @param date {Date} datum a čas
 * @return {String} datum
 * TODO Možná změnit dateTimeToString => dateToDateTimeString a dateToString => dateToDateString
 */
export function dateTimeToString(date) {
    var dd = date.getDate().toString();
    var mm = (date.getMonth() + 1).toString();
    var yyyy = date.getFullYear().toString();
    var hh = date.getHours().toString();
    var ii = date.getMinutes().toString();
    /** Formátování - místo 01 = 1 **/
    var f = (col) => (col[1] ? col : "0" + col[0]);
    return f(dd) + "." + f(mm) + "." + yyyy + " " + f(hh) + ":" + f(ii);
}

/**
 * Porovnání dvou hodnot. Undefined, null a prázdný řetězec jsou ekvivalentní.
 * @return {boolean} true, pokud jsou předané parametry stejné
 */
export function valuesEquals(v1, v2) {
    if (v1 === v2) {
        return true;
    }

    var v1empty = typeof v1 === 'undefined' || v1 === null || v1.length === 0
    var v2empty = typeof v2 === 'undefined' || v2 === null || v2.length === 0

    if (v1empty && v2empty) {
        return true
    }

    return false
}

/**
 * Převede WKT text coordinates na objekt { type: TYP, data: "bod,bod" }
 *
 * Aktuálně server posílá takto pouze body
 * Polygony a linie posílá jako TYPE( X ) kde X je rovno počtu bodů
 *
 * @param value
 * @returns object
 */
export function objectFromWKT(value) {
    if (typeof value === 'undefined' || value === null || value == '' || typeof value === "object") {
        return {type: "POINT", data: null};
    }
    const state = {type: null, data: null};
    const start = value.indexOf("(");
    state.type = value.substr(0, start).trim();
    if (state.type === "POINT") {
        state.data = value.substr(start + 1, value.length - start - 2).split(", ").join("\n").split(" ").join(",");
    } else {
        state.data = value.substr(start + 2, value.length - start - 4);
    }
    return state;
}

/**
 * Převádí typ a body na WKT
 *
 * Vyžaduje funkci normalizeDoubleWithDot
 *
 * type = POINT, val = 1.423,13.456 => POINT( 1.423 13.456 )
 * type = LINESTRING, val = 1.423,13.456\n12.423,13.456 => LINESTRING( 1.423 13.456, 12.423 13.456)
 * type = POLYGON, val = 1.423,13.456\n12.423,13.456\n1.423,1.456 => POLYGON(( 1.423 13.456, 12.423 13.456, 1.423 1.456, 1.423 13.456 ))
 *
 * @param type POINT,LINESTRING,POLYGON
 * @param val body(s desetinou ".") oddělené čárkou a 1 bod na 1 řádku
 * @returns string WK Text
 */
export function wktFromTypeAndData(type, val) {
    let points = val.split(",").map(function (dat) {
        return normalizeDoubleWithDot(dat);
    }).join(" ").split("\n").join(", ");
    if (type === "POLYGON") {
        points = "(" + points + ", " + points.substr(0, points.indexOf(",")) + ")";
    }
    return type + "(" + points + ")";
}

/**
 * Převede WKT typ na písmenkovou reprezentaci
 *
 * @param type
 * @returns string
 */
export function wktType(type) {
    switch (type) {
        case "POINT":
            return "B";
        case "POLYGON":
            return "P";
        case "LINESTRING":
            return "L";
        default:
            return "N";
    }
}

/**
 * detect IE
 * returns version of IE or false, if browser is not Internet Explorer
 */
export function detectIE() {
    var ua = window.navigator.userAgent;

    // Test values; Uncomment to check result …

    // IE 10
    // ua = 'Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0)';

    // IE 11
    // ua = 'Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko';

    // Edge 12 (Spartan)
    // ua = 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0';

    // Edge 13
    // ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586';

    var msie = ua.indexOf('MSIE ');
    if (msie > 0) {
        // IE 10 or older => return version number
        return parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
    }

    var trident = ua.indexOf('Trident/');
    if (trident > 0) {
        // IE 11 => return version number
        var rv = ua.indexOf('rv:');
        return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
    }

    var edge = ua.indexOf('Edge/');
    if (edge > 0) {
        // Edge (IE 12+) => return version number
        return parseInt(ua.substring(edge + 5, ua.indexOf('.', edge)), 10);
    }

    // other browser
    return false;
}

var _scrollbarWidth = null
export function calculateScrollbarWidth() {
    if (_scrollbarWidth == null) {
        // Create the measurement node
        var scrollDiv = document.createElement("div");
        scrollDiv.className = "scrollbar-measure";
        document.body.appendChild(scrollDiv);

        // Get the scrollbar width
        _scrollbarWidth = scrollDiv.offsetWidth - scrollDiv.clientWidth;

        // Delete the DIV
        document.body.removeChild(scrollDiv);
    }
}
calculateScrollbarWidth();

export function getScrollbarWidth() {
    return _scrollbarWidth;
}

/**
 * Zarovnání čísla na dvě pozice přidáním 0, pokud je potřeba.
 * @param number číslo
 * @return {string} <číslo> nebo 0<číslo>
 */
function _dtpad(number) {
    var r = String(number);
    if ( r.length === 1 ) {
        r = '0' + r;
    }
    return r;
}

/**
 * Převede datum a čas na lokální datum a čas v UTC.
 * @param date
 * @return {*}
 */
export function dateTimeToLocalUTC(date) {
    if (!date) {
        return date;
    }

    return date.getFullYear()
        + '-' + _dtpad( date.getMonth() + 1)
        + '-' + _dtpad( date.getDate())
        + 'T' + _dtpad( date.getHours())
        + ':' + _dtpad( date.getMinutes())
        + ':' + _dtpad( date.getSeconds())
        + '.' + _dtpad( date.getMilliseconds());
}

/**
 * Převede datum a čas na datum a čas v UTC se zónou.
 * @param date
 * @return {*}
 */
export function dateTimeToZonedUTC(date) {
    if (!date) {
        return date;
    }

    const zone = date.getTimezoneOffset();

    return date.getFullYear()
        + '-' + _dtpad( date.getMonth() + 1)
        + '-' + _dtpad( date.getDate())
        + 'T' + _dtpad( date.getHours())
        + ':' + _dtpad( date.getMinutes())
        + ':' + _dtpad( date.getSeconds())
        + '.' + _dtpad( date.getMilliseconds())
        + toZoneString(zone);
}

function toZoneString(zone) {
    const positive = zone >= 0;
    const zoneTmp = positive ? zone : -zone;
    let h = Math.floor(zoneTmp / 60);
    let m = Math.floor(zoneTmp % 60);
    return (positive ? "-" : "+") + pad2(h) + ":" + pad2(m);
}

export const removeUndefined = (obj) => {
    for (let key in obj ) {
        if (obj.hasOwnProperty(key)) {
            if (obj[key] === undefined || obj[key] === null) {
                delete obj[key];
            }
        }
    }
    return obj;
};
export const isNotBlankObject = (obj) => {
    const newObj = removeUndefined(obj);
    return Object.keys(newObj).length > 0
};
/**
 * Vloží zkratky z výchozí keymapy do druhé předané keymapy, pokud se v ní nenachází
 * @param {object} defaultKeymap - výchozí keymapa, ze které budou zkratky čteny
 * @param {object} keymap - keymapa, do které budou zkratky vloženy
 * @return {object}
 */
function overrideKeymap(defaultKeymap,keymap) {
    keymap = {...keymap};
    for(let component in defaultKeymap){
        if(keymap && keymap[component]){
            let newComponentKeymap = {};
            for(let action in defaultKeymap[component]){
                if(!keymap[component][action]){
                    newComponentKeymap[action] = defaultKeymap[component][action];
                } else {
                    newComponentKeymap[action] = keymap[component][action];
                }
            }
            keymap[component] = newComponentKeymap;
            //console.log("existing component",component,keymap);
        } else {
            keymap[component] = {...defaultKeymap[component]};
            //console.log("undefined component",component,keymap);
        }
        checkValueDuplicity(keymap[component]);
    }
    return keymap;
}
/**
 * Spojí dvě keymapy, případně přepíše hodnoty výchozí keymapy hodnotami z rozšiřující
 * @param {object} defaultKeymap - výchozí keymapa
 * @param {object} extendingKeymap - rozšiřující keymapa
 * @return {object} mergedKeymap - nová keymapa
 */
export function mergeKeymaps(defaultKeymap,extendingKeymap){
    let mergedKeymap = {};
    for(let c in defaultKeymap){ //vytvoření nového objektu, aby se nepřepisoval původní
        mergedKeymap[c] = {};
        for(let a in defaultKeymap[c]){
            mergedKeymap[c][a] = defaultKeymap[c][a];
        }
    }
    for(let component in extendingKeymap){
        if(mergedKeymap && mergedKeymap[component]){
            for(let action in extendingKeymap[component]){
                mergedKeymap[component][action] = extendingKeymap[component][action];
            }
        } else {
            mergedKeymap[component] = {...extendingKeymap[component]};
        }
    }
    return mergedKeymap;
}
/**
 * Zkontroluje, jestli objekt neobsahuje duplicitní hodnoty. Pokud je hodnotou pole, jsou prohledány všechny jeho hodnoty
 * => ["a","b"] je považováno za duplicitní s "a". hodnota "b" je pak dále porovnávána s dalšími hodnotami.
 * Objekty jsou porovnávány jako stringy
 * @param {object} object - objekt, ve kterém se budou hledat duplicity
 */
function checkValueDuplicity(object){
    let newObj = {};
    for(let i in object){
        let objectValue = object[i];
        if(!Array.isArray(objectValue)){
            objectValue = [objectValue];
        }
        for(let j=0;j<objectValue.length;j++){
            if(typeof newObj[objectValue[j]] === "undefined"){
                newObj[objectValue[j]] = i;
            } else {
                console.warn("Duplicity found for '"+objectValue[j]+"' assigned to '"+i+"'. Already used in '"+newObj[objectValue[j]]+"'");
            }
        }
    }
}
/**
 * Přidá na předanou komponentu shortcut manager s předanou keymapou.
 * Pokud shortcut manager existuje v kontextu komponenty je mu pouze doplněna keymapa o hodnoty z předané keymapy.
 * Pokud je předána přepisující keymapa je ignorován kontext komponenty a je vytvořen nový shortcut manager s keymapou vytvořenou spojením obou předaných
 * @param {object} component - komponenta, ke které se přidá shortcut manager
 * @param {object} defaultKeymap - výchozí keymapa
 * @param {object} overridingKeymap -
 */
export function addShortcutManager(component,defaultKeymap,overridingKeymap) {
    let shortcutManager;
    if(component.context && component.context.shortcuts && !overridingKeymap){
        let keymap = component.context.shortcuts._keymap;
        component.context.shortcuts._keymap = overrideKeymap(defaultKeymap,keymap);
        shortcutManager = component.context.shortcuts;
    } else {
        if(overridingKeymap){
            defaultKeymap = overrideKeymap(defaultKeymap,overridingKeymap);
        }
        shortcutManager = new ShortcutManager(defaultKeymap)
    }
    component.shortcutManager = shortcutManager;
}

/*export default {
    dateTimeToLocalUTC,
    wktType,
    wktFromTypeAndData,
    objectFromWKT,
    getScrollbarWidth: getScrollbarWidth,
    valuesEquals: valuesEquals,
    dateToString: dateToString,
    timeToString: timeToString,
    dateTimeToString: dateTimeToString,
    StringSet: StringSet,
    StringMap: StringMap,
    barrier: barrier,
    lenToBytesStr: lenToBytesStr,
    roughSizeOfObject: roughSizeOfObject,
    consolidateState: consolidateState,
    stateEquals: stateEquals,
    propsEquals: propsEquals,
    setInputFocus: setInputFocus,
    browser: browser,
    getKeyModifier: getKeyModifier,
    detectIE: detectIE,
    removeUndefined,
    isNotBlankObject,
    humanFileSize,
    overrideKeymap: overrideKeymap,
    mergeKeymaps: mergeKeymaps,
    addShortcutManager: addShortcutManager,
    init: function() {
        init();
    }
}*/
