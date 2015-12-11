import {WebApi} from 'actions'

export const FA_REQUEST_FA_FILE_TREE = 'FA_REQUEST_FA_FILE_TREE'
export const FA_RECEIVE_FA_FILE_TREE = 'FA_RECEIVE_FA_FILE_TREE'

export const FA_SELECT_FA = 'FA_SELECT_FA'
export const FA_CLOSE_FA = 'FA_CLOSE_FA'

export const FA_SELECT_NODE = 'FA_SELECT_NODE'
export const FA_CLOSE_NODE = 'FA_CLOSE_NODE'

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
        type: FA_RECEIVE_FA_FILE_TREE,
        items: json,
        receivedAt: Date.now()
    }
}

function requestFaFileTree() {
    return {
        type: FA_REQUEST_FA_FILE_TREE
    }
}

function selectFa(fa, moveToBegin=false) {
    return {
        type: FA_SELECT_FA,
        fa,
        moveToBegin
    }
}

function closeFa(fa) {
    return {
        type: FA_CLOSE_FA,
        fa
    }
}

function selectNode(node, moveToBegin=false) {
    return {
        type: FA_SELECT_NODE,
        node,
        moveToBegin
    }
}

function closeNode(node) {
    return {
        type: FA_CLOSE_NODE,
        node
    }
}

export const faActions = {
    selectFa: selectFa,
    closeFa: closeFa,
    selectNode: selectNode,
    closeNode: closeNode,
    fetchFaFileTree: fetchFaFileTree,
    fetchFaFileTreeIfNeeded: fetchFaFileTreeIfNeeded,
}
