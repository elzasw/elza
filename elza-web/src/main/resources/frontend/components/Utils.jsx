/**
 * Utility metody.
 */

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
    init: function() {
        init();
    }
}