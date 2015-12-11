import {combineReducers, createStore, applyMiddleware} from 'redux'
import thunkMiddleware from 'redux-thunk'
import createLogger from 'redux-logger'

import {fas, faFileTree} from './fa/store.jsx'

let reducer = combineReducers({ fas, faFileTree });

const loggerMiddleware = createLogger()
const createStoreWithMiddleware = applyMiddleware(
    thunkMiddleware,
    loggerMiddleware
)(createStore)

var store = function configureStore(initialState) {
    return createStoreWithMiddleware(reducer, initialState)
}();

module.exports = {
    store
}
