import {combineReducers, createStore, applyMiddleware} from 'redux'
import thunkMiddleware from 'redux-thunk'
import createLogger from 'redux-logger'

/**
 * Sestavení reducerů.
 */
import fas from './fa/fas';
import faFileTree from './fa/faFileTree';

let reducer = combineReducers({
    fas,
    faFileTree
});

// Store a middleware
const loggerMiddleware = createLogger()
const createStoreWithMiddleware = applyMiddleware(
    thunkMiddleware,
    loggerMiddleware
)(createStore)

var initialState = {
}
var store = function configureStore(initialState) {
    return createStoreWithMiddleware(reducer, initialState)
}(initialState);

module.exports = {
    store
}
