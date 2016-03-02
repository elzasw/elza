/**
 * Utility metody.
 */

function consolidateState(prevState, newState) {
    var equals = stateEquals(prevState, newState);
    if (!equals) {
        //console.log(newState);
    }
    return equals ? prevState : newState;
}

function chooseInputEl(el1, el2) {
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
function setInputFocus(el, selectContent = false) {
    var elem = $('input:visible:enabled', el).get(0);
    var select = $('select:visible:enabled', el).get(0);
    elem = chooseInputEl(elem, select);

    var textarea = $('textarea:visible:enabled', el).get(0);
    elem = chooseInputEl(elem, textarea);

    var button = $('button:visible:enabled', el).get(0);
    elem = chooseInputEl(elem, button);

    if (elem) {
        elem.focus();
        if (selectContent) {
            elem.select();
        }
        return true
    }
    return false
}

function propsEquals(x, y, attrs) {
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
}

function stateEquals(x, y) {
  for ( var p in x ) {
    if ( ! x.hasOwnProperty( p ) ) continue;
      // other properties were tested using x.constructor === y.constructor

    if ( ! y.hasOwnProperty( p ) ) return false;
      // allows to compare x[ p ] and y[ p ] when set to undefined

    if ( x[ p ] === y[ p ] ) continue;
      // if they have the same strict value or identity then they are equal

    if ( typeof( x[ p ] ) !== "object" ) return false;
      // Numbers, Strings, Functions, Booleans must be strictly equal

    if (x[ p ] !==  y[ p ] ) return false;
  }
    return true;
}

function objectEquals( x, y ) {
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

function lenToBytesStr(len) {
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

function roughSizeOfObject( object ) {

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

function StringSet() {
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

function StringMap() {
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

var _browser
// Inicializace typu prohlížeče
{
    // Opera 8.0+
    var opera = (!!window.opr && !!opr.addons) || !!window.opera || navigator.userAgent.indexOf(' OPR/') >= 0;
        // Firefox 1.0+
    var firefox = typeof InstallTrigger !== 'undefined';
        // At least Safari 3+: "[object HTMLElementConstructor]"
    var safari = Object.prototype.toString.call(window.HTMLElement).indexOf('Constructor') > 0;
        // Internet Explorer 6-11
    var ie = /*@cc_on!@*/false || !!document.documentMode;
        // Edge 20+
    var edge = !ie && !!window.StyleMedia;
        // Chrome 1+
    var chrome = !!window.chrome && !!window.chrome.webstore;
        // Blink engine detection
    var blink = (chrome || opera) && !!window.CSS;    

    _browser = {
        opera,
        firefox,
        safari,
        ie,
        edge,
        chrome,
        blink,
    }
}
function browser() {
    return _browser;
}

function getKeyModifier() {
    var browser = _browser;
    if (browser.ie || browser.edge) {
        return 'ctrl+shift+';
    } else {
        return 'ctrl+alt+';
    }
}

function init() {
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

function barrierCall(index, promise, onData, onError) {
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

function barrier(...promises) {
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
function dateToString(date) {
    var dd = date.getDate().toString();
    var mm = (date.getMonth() + 1).toString();
    var yyyy = date.getFullYear().toString();
    return (dd[1] ? dd : "0" + dd[0]) + "." + (mm[1] ? mm : "0" + mm[0]) + "." + yyyy;
}

/**
 * Převod Obejct:Date do řetězce včetně času
 * @param date {Date} datum a čas
 * @return {String} datum
 * TODO Možná změnit dateTimeToString => dateToDateTimeString a dateToString => dateToDateString
 */
function dateTimeToString(date) {
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
function valuesEquals(v1, v2) {
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

module.exports = {
    valuesEquals: valuesEquals,
    dateToString: dateToString,
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
    init: function() {
        init();
    }
}