import {WebApi} from 'actions'
import {indexById} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/actionTypes';

export function faSelectNodeTab(index) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var nodeTab = activeFa.nodes.nodes[index];
        dispatch({
            type: types.FA_FA_SELECT_NODE_TAB,
            index,
        });
        if (nodeTab.selectedSubNodeId != null) {    // musíme poslat akci vybrání subnode (aby se řádek vybral např. ve stromu)
            dispatch(faSelectSubNode(nodeTab.selectedSubNodeId, nodeTab, false));
        }
    }
}

export function faCloseNodeTabInt(index) {
    return {
        type: types.FA_FA_CLOSE_NODE_TAB,
        index
    }
}
export function faCloseNodeTab(index) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var wasSelected = false;
        if (activeFa.nodes.activeIndex == index) {  // zavíráme aktuálně vybranou
            wasSelected = true;
        }
        dispatch(faCloseNodeTabInt(index));
        var newState = getState();
        var newActiveFa = newState.arrRegion.fas[newState.arrRegion.activeIndex];
        if (wasSelected) { 
            if (newActiveFa.nodes.nodes.length > 0) {    // je vybraná nějakaá, jiná, protože ještě nejaké záložky jsou
                dispatch(faSelectNodeTab(newActiveFa.nodes.activeIndex));
            } else {    // není žádná záložka
                dispatch(faSelectSubNode(null, null, false));
            }
        }
    }
}
export function faCloseNodeTab2(index) {
    return {
        type: types.FA_FA_CLOSE_NODE_TAB,
        index
    }
}

export function faSelectSubNode(subNodeId, subNodeParentNode, openNewTab=false) {
    return {
        type: types.FA_FA_SELECT_SUBNODE,
        subNodeId,
        subNodeParentNode,
        openNewTab,
    }
}

