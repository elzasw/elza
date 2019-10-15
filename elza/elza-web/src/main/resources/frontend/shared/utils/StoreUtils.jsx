/*
 ///////////////////////////////////////////////////////////////
 /// The tests
 ///////////////////////////////////////////////////////////////
 var printResult = function (x) {
 if (x) { document.write('<div style="color: green;">Passed</div>'); }
 else { document.write('<div style="color: red;">Failed</div>'); }
 };
 var assert = { isTrue: function (x) { printResult(x); }, isFalse: function (x) { printResult(!x); } }
 assert.isTrue(objectEquals(null,null));
 assert.isFalse(objectEquals(null,undefined));
 assert.isFalse(objectEquals(/abc/, /abc/));
 assert.isFalse(objectEquals(/abc/, /123/));
 var r = /abc/;
 assert.isTrue(objectEquals(r, r));

 assert.isTrue(objectEquals("hi","hi"));
 assert.isTrue(objectEquals(5,5));
 assert.isFalse(objectEquals(5,10));

 assert.isTrue(objectEquals([],[]));
 assert.isTrue(objectEquals([1,2],[1,2]));
 assert.isFalse(objectEquals([1,2],[2,1]));
 assert.isFalse(objectEquals([1,2],[1,2,3]));

 assert.isTrue(objectEquals({},{}));
 assert.isTrue(objectEquals({a:1,b:2},{a:1,b:2}));
 assert.isTrue(objectEquals({a:1,b:2},{b:2,a:1}));
 assert.isFalse(objectEquals({a:1,b:2},{a:1,b:3}));

 assert.isTrue(objectEquals({1:{name:"mhc",age:28}, 2:{name:"arb",age:26}},{1:{name:"mhc",age:28}, 2:{name:"arb",age:26}}));
 assert.isFalse(objectEquals({1:{name:"mhc",age:28}, 2:{name:"arb",age:26}},{1:{name:"mhc",age:28}, 2:{name:"arb",age:27}}));

 Object.prototype.equals = function (obj) { return objectEquals(this, obj); };
 var assertFalse = assert.isFalse,
 assertTrue = assert.isTrue;

 assertFalse({}.equals(null));
 assertFalse({}.equals(undefined));

 assertTrue("hi".equals("hi"));
 assertTrue(new Number(5).equals(5));
 assertFalse(new Number(5).equals(10));
 assertFalse(new Number(1).equals("1"));

 assertTrue([].equals([]));
 assertTrue([1,2].equals([1,2]));
 assertFalse([1,2].equals([2,1]));
 assertFalse([1,2].equals([1,2,3]));
 assertTrue(new Date("2011-03-31").equals(new Date("2011-03-31")));
 assertFalse(new Date("2011-03-31").equals(new Date("1970-01-01")));

 assertTrue({}.equals({}));
 assertTrue({a:1,b:2}.equals({a:1,b:2}));
 assertTrue({a:1,b:2}.equals({b:2,a:1}));
 assertFalse({a:1,b:2}.equals({a:1,b:3}));

 assertTrue({1:{name:"mhc",age:28}, 2:{name:"arb",age:26}}.equals({1:{name:"mhc",age:28}, 2:{name:"arb",age:26}}));
 assertFalse({1:{name:"mhc",age:28}, 2:{name:"arb",age:26}}.equals({1:{name:"mhc",age:28}, 2:{name:"arb",age:27}}));

 var a = {a: 'text', b:[0,1]};
 var b = {a: 'text', b:[0,1]};
 var c = {a: 'text', b: 0};
 var d = {a: 'text', b: false};
 var e = {a: 'text', b:[1,0]};
 var i = {
 a: 'text',
 c: {
 b: [1, 0]
 }
 };
 var j = {
 a: 'text',
 c: {
 b: [1, 0]
 }
 };
 var k = {a: 'text', b: null};
 var l = {a: 'text', b: undefined};

 assertTrue(a.equals(b));
 assertFalse(a.equals(c));
 assertFalse(c.equals(d));
 assertFalse(a.equals(e));
 assertTrue(i.equals(j));
 assertFalse(d.equals(k));
 assertFalse(k.equals(l));

 // from comments on stackoverflow post
 assert.isFalse(objectEquals([1, 2, undefined], [1, 2]));
 assert.isFalse(objectEquals([1, 2, 3], { 0: 1, 1: 2, 2: 3 }));
 assert.isFalse(objectEquals(new Date(1234), 1234));

 // no two different function is equal really, they capture their context variables
 // so even if they have same toString(), they won't have same functionality
 var func = function (x) { return true; };
 var func2 = function (x) { return true; };
 assert.isTrue(objectEquals(func, func));
 assert.isFalse(objectEquals(func, func2));
 assert.isTrue(objectEquals({ a: { b: func } }, { a: { b: func } }));
 assert.isFalse(objectEquals({ a: { b: func } }, { a: { b: func2 } }));
 */

function processStore2(store, action) {
    return store.reducer(store, action)
}

export default class StoreUtils {
    static consolidateState(prevState, newState) {
        return StoreUtils.stateEquals(prevState, newState) ? prevState : newState;
    }

    static processStore(name, state, action) {
        return {
            ...state,
            [name]: processStore2(state[name], action),
        }
    }

    static processConcreteStore(store, action) {
        return processStore2(store, action);
    }

    static propsEquals(x, y, attrs) {
        if (typeof attrs !== 'undefined' && attrs !== null) {
            for (let a = 0; a < attrs.length; a++) {
                const p = attrs[a];

                if (!x.hasOwnProperty(p)) continue;
                // other properties were tested using x.constructor === y.constructor

                if (!y.hasOwnProperty(p)) {
                    return false;
                }
                // allows to compare x[ p ] and y[ p ] when set to undefined

                if (x[p] === y[p]) continue;
                // if they have the same strict value or identity then they are equal

                if (typeof( x[p] ) !== "object") {
                    return false;
                }
                // Numbers, Strings, Functions, Booleans must be strictly equal

                if (x[p] !== y[p]) {
                    return false;
                }
            }
            return true;
        } else {
            return StoreUtils.stateEquals(x, y)
        }
    }

    static stateEquals(x, y) {
        for (const p in x) {
            if (!x.hasOwnProperty(p)) continue;
            // other properties were tested using x.constructor === y.constructor

            if (!y.hasOwnProperty(p)) return false;
            // allows to compare x[ p ] and y[ p ] when set to undefined

            if (x[p] === y[p]) continue;
            // if they have the same strict value or identity then they are equal

            if (typeof( x[p] ) !== "object") return false;
            // Numbers, Strings, Functions, Booleans must be strictly equal

            if (x[p] !== y[p]) {
//console.log(p)
                return false;
            }
        }
        return true;
    }

    static objectEquals(x, y) {
        if (x === null || x === undefined || y === null || y === undefined) {
            return x === y;
        }
        // after this just checking type of one would be enough
        if (x.constructor !== y.constructor) {
            return false;
        }
        // if they are functions, they should exactly refer to same one (because of closures)
        if (x instanceof Function) {
            return x === y;
        }
        // if they are regexps, they should exactly refer to same one (it is hard to better equality check on current ES)
        if (x instanceof RegExp) {
            return x === y;
        }
        if (x === y || x.valueOf() === y.valueOf()) {
            return true;
        }
        if (Array.isArray(x) && x.length !== y.length) {
            return false;
        }

        // if they are dates, they must had equal valueOf
        if (x instanceof Date) {
            return false;
        }

        // if they are strictly equal, they both need to be object at least
        if (!(x instanceof Object)) {
            return false;
        }
        if (!(y instanceof Object)) {
            return false;
        }

        // recursive object equality check
        let p = Object.keys(x);
        return Object.keys(y).every(function (i) {
                return p.indexOf(i) !== -1;
            }) &&
            p.every(function (i) {
                return StoreUtils.objectEquals(x[i], y[i]);
            });
    }
};
