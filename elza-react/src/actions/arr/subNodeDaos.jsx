import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes';
import {findByRoutingKeyInGlobalState} from 'stores/app/utils.jsx';
import { Api } from "api";

export function isSubNodeDaosAction(action) {
    switch (action.type) {
        case types.FUND_SUB_NODE_DAOS_REQUEST:
        case types.FUND_SUB_NODE_DAOS_RECEIVE:
        case types.CHANGE_DAOS:
            return true;
        default:
            return false;
    }
}

/**
 * Načtení dat pokud je potřeba.
 *
 * @param {number} versionId - id verze
 * @param {number|null} nodeId - data key
 * @param {number|string} routingKey - routing key
 */
export function fundSubNodeDaosFetchIfNeeded(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        // Spočtení data key - se správným id
        const store = getSubNodeDaos(getState(), versionId, routingKey);

        if (!store) {
            return;
        }

        const dataKey = nodeId;
        if (store.currentDataKey !== dataKey) {
            // pokus se data key neschoduje, provedeme fetch
            dispatch(fundSubNodeDaosRequest(versionId, nodeId, routingKey));

            if (nodeId !== null) {
                // pokud chceme reálně načíst objekt, provedeme fetch
                return WebApi.getFundNodeDaos(versionId, nodeId).then(json => {
                    const newStore = getSubNodeDaos(getState(), versionId, routingKey);
                    if (newStore !== null) {
                        const newDataKey = newStore.currentDataKey;
                        if (newDataKey === dataKey) {
                            // jen pokud příchozí objekt odpovídá dtům, které chceme ve store
                            dispatch(fundSubNodeDaosReceive(versionId, nodeId, routingKey, json));
                        }
                    }
                });
            } else {
                // Response s prázdným objektem
                dispatch(fundSubNodeDaosReceive(versionId, nodeId, routingKey, null));
            }
        }
    };
}

export function fundSubNodeDaosReceive(versionId, nodeId, routingKey, json) {
    return {
        type: types.FUND_SUB_NODE_DAOS_RECEIVE,
        versionId,
        nodeId,
        routingKey,
        data: json,
        receivedAt: Date.now(),
    };
}

export function fundSubNodeDaosRequest(versionId, nodeId, routingKey) {
    return {
        type: types.FUND_SUB_NODE_DAOS_REQUEST,
        versionId,
        dataKey: nodeId,
        routingKey,
    };
}

export function fundSubNodeDaoChangeScenario(daoId, scenario, versionId, nodeId) {
    return (dispatch) => {
        return Api.daos.changeLinkScenario(daoId, scenario).then((response)=>{
            dispatch(fundSubNodeDaosInvalidate(versionId, [nodeId]));
            return response;
        });
    }
}

function getSubNodeDaos(state, versionId, routingKey) {
    const node = getNode(state, versionId, routingKey);
    if (node !== null) {
        return node.subNodeDaos;
    } else {
        return null;
    }
}

function getNode(state, versionId, routingKey) {
    const r = findByRoutingKeyInGlobalState(state, versionId, routingKey);
    if (r != null) {
        return r.node;
    }

    return null;
}

export function fundSubNodeDaosInvalidate(versionId, nodeIds) {
    return {
        type: types.CHANGE_DAOS,
        versionId,
        nodeIds: nodeIds,
    };
}
