export const SELECT_FA = 'SELECT_FA'
export const CLOSE_FA = 'CLOSE_FA'

export const SELECT_NODE = 'SELECT_NODE'
export const CLOSE_NODE = 'CLOSE_NODE'

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
}

export const AppActions = {
    faActions: fa
}