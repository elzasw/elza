import {WebApi} from 'actions'
import {indexById} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/actionTypes';

export function faSelectNodeTab(node, moveTabToBegin=false) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var index = indexById(activeFa.nodes.nodes, node.id);
        var nodeTab = activeFa.nodes.nodes[index];
        if (nodeTab.selectedSubNodeId != null) {    // musíme poslat akci vybrání subnode (aby se řádek vybral např. ve stromu)
            dispatch(faSelectSubNode(nodeTab.selectedSubNodeId, nodeTab, false, false));
        }

        return dispatch({
            type: types.FA_FA_SELECT_NODE_TAB,
            node,
            moveTabToBegin
        });
    }
}

export function faCloseNodeTab(node) {
    return {
        type: types.FA_FA_CLOSE_NODE_TAB,
        node
    }
}

export function faSelectSubNode(subNodeId, subNodeParentNode, openUnexistingNodeTab=false, moveTabToBegin=false) {
    return {
        type: types.FA_FA_SELECT_SUBNODE,
        subNodeId,
        subNodeParentNode,
        openUnexistingNodeTab,
        moveTabToBegin,
    }
}

