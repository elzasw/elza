/**
 * Utility metody.
 */

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

module.exports = {
    StringSet: StringSet,
    StringMap: StringMap,
    init: function() {
        init();
    }
}