import {combineReducers, createStore, applyMiddleware} from 'redux'
import thunkMiddleware from 'redux-thunk'
import createLogger from 'redux-logger'

/**
 * Sestavení reducerů.
 */
import arrRegion from './arr/arrRegion';
import faFileTree from './arr/faFileTree';
import record from './record/record';
import recordData from './record/recordData';
import partyRegion from './party/partyRegion';
import contextMenu from './global/contextMenu';
import adminRegion from './admin/adminRegion';

let reducer = combineReducers({
    arrRegion,
    faFileTree,
    record,
    partyRegion,
    contextMenu,
    adminRegion
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

import {selectFaTab} from 'actions/arr/fa'
var fa = Object.assign({id: 1, versionId: 1});
store.dispatch(selectFaTab(fa));

module.exports = {
    store
}
