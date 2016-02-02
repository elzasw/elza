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
 */
function setInputFocus(el, selectContent) {

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
    }
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

module.exports = {
    StringSet: StringSet,
    StringMap: StringMap,
    barrier: barrier,
    lenToBytesStr: lenToBytesStr,
    roughSizeOfObject: roughSizeOfObject,
    consolidateState: consolidateState,
    stateEquals: stateEquals,
    propsEquals: propsEquals,
    setInputFocus: setInputFocus,
    init: function() {
        init();
    }
}