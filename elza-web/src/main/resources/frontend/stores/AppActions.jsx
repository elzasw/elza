import {Utils} from 'components'
import {WebApi} from 'actions'

export const REQUEST_FA_FILE_TREE = 'REQUEST_FA_FILE_TREE'
export const RECEIVE_FA_FILE_TREE = 'RECEIVE_FA_FILE_TREE'

export const SELECT_FA = 'SELECT_FA'
export const CLOSE_FA = 'CLOSE_FA'

export const SELECT_NODE = 'SELECT_NODE'
export const CLOSE_NODE = 'CLOSE_NODE'

export const FETCH_PARTIES = 'FETCH_PARTIES'

export const GET_OBJECT_INFO = 'GET_OBJECT_INFO'

function fetchFaFileTreeIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.faFileTree.fetched && !state.faFileTree.isFetching) {
            return dispatch(fetchFaFileTree());
        }
    }
}

function fetchFaFileTree() {
    return dispatch => {
        dispatch(requestFaFileTree())
        return WebApi.getFaFileTree()
            .then(json => dispatch(receiveFaFileTree(json)));
    }
}

function receiveFaFileTree(json) {
    return {
        type: RECEIVE_FA_FILE_TREE,
        items: json,
        receivedAt: Date.now()
    }
}

function requestFaFileTree() {
    return {
        type: REQUEST_FA_FILE_TREE
    }
}

var ObjectInfo = class ObjectInfo {
    constructor() {
        this.nodeIds = new Utils.StringSet();
        this.faIds = new Utils.StringSet();

        this.addNode = this.addNode.bind(this);
        this.addFa = this.addFa.bind(this);
    }

    addNode(node) {
        console.log('addNode', node);
        this.nodeIds.add(node.id);
    }

    addFa(fa) {
        console.log('addFa', fa);
        this.faIds.add(fa.id);
    }
}

function fetchParties() {
    return {
        type: FETCH_PARTIES
    }
}

function getObjectInfo(objectInfo) {
    return {
        type: GET_OBJECT_INFO,
        objectInfo
    }
}

function selectFa(fa, moveToBegin=false) {
    return {
        type: SELECT_FA,
        fa,
        moveToBegin
    }
}

function closeFa(fa) {
    return {
        type: CLOSE_FA,
        fa
    }
}

function selectNode(node, moveToBegin=false) {
    return {
        type: SELECT_NODE,
        node,
        moveToBegin
    }
}

function closeNode(node) {
    return {
        type: CLOSE_NODE,
        node
    }
}

var fa = {
    selectFa: selectFa,
    closeFa: closeFa,
    selectNode: selectNode,
    closeNode: closeNode,
    getObjectInfo: getObjectInfo,
    fetchFaFileTree: fetchFaFileTree,
    fetchFaFileTreeIfNeeded: fetchFaFileTreeIfNeeded,
}

export const AppActions = {
    faActions: fa,
    ObjectInfo: ObjectInfo
}