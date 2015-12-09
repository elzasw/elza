export const ADD_FA = 'ADD_FA'
export const SELECT_FA = 'SELECT_FA'
export const CLOSE_FA = 'CLOSE_FA'
export const FIRST_FA_SELECT = 'FIRST_FA_SELECT'

export const ADD_NODE = 'ADD_NODE'
export const SELECT_NODE = 'SELECT_NODE'
export const CLOSE_NODE = 'CLOSE_NODE'
export const FIRST_NODE = 'CLOSE_NODE'

function addFa(fa) {
    return {
        type: ADD_FA,
        fa
    }
}

function selectFa(faId) {
    return {
        type: SELECT_FA,
        faId
    }
}

function firstFaSelect(faId) {
    return {
        type: FIRST_FA_SELECT,
        faId
    }
}

function closeFa(faId) {
    return {
        type: CLOSE_FA,
        faId
    }
}

function addNode(node) {
    return {
        type: ADD_NODE,
        node
    }
}

function selectNode(nodeId) {
    return {
        type: SELECT_NODE,
        nodeId
    }
}

function closeNode(nodeId) {
    return {
        type: CLOSE_NODE,
        nodeId
    }
}

function firstNode(nodeId) {
    return {
        type: FIRST_NODE,
        nodeId
    }
}

var fa = {
    addFa: addFa,
    selectFa: selectFa,
    closeFa: closeFa,
    firstFaSelect: firstFaSelect,
    addNode: addNode,
    selectNode: selectNode,
    closeNode: closeNode,
    firstNode: firstNode,
}

export const AppActions = {
    faActions: fa
}