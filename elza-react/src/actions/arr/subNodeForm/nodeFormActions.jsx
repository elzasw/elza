/**
 * Akce pro formulář JP.
 */

import { WebApi } from 'actions/index';
import { findByRoutingKeyInGlobalState, getRoutingKeyType, indexById } from 'stores/app/utils';
import NodeRequestController from 'websocketController';
import { increaseNodeVersion } from '../node';
import { fundNodeInfoReceive } from '../nodeInfo';
import { fundSubNodeInfoReceive } from '../subNodeInfo';
import { ItemFormActions } from './itemFormActions';

// Konfigurace velikosti cache dat pro formulář
const CACHE_SIZE = 20;
const CACHE_SIZE2 = CACHE_SIZE / 2;

//var debouncedGetFundNodeForm = debounce(WebApi.getFundNodeForm,200);
export class NodeFormActions extends ItemFormActions {
    static AREA = 'NODE';

    constructor() {
        super(NodeFormActions.AREA);
    }

    /**
     * Akce kopírování hodnot konkrétního atributu z předcházející JP.
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} nodeVersionId verze node
     * @param {int} descItemTypeId id atribtu
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění
     */
    fundSubNodeFormValuesCopyFromPrev(versionId, nodeId, nodeVersionId, descItemTypeId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            dispatch(this._fundSubNodeFormDescItemTypeDeleteInStore(versionId, routingKey, valueLocation, true));
            WebApi.copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId).then(json => {
                dispatch(
                    this.fundSubNodeFormDescItemTypeCopyFromPrevResponse(versionId, routingKey, valueLocation, json),
                );
            });
        };
    }

    /**
     * Načtení server dat pro formulář pro aktuálně předané parametry s využitím cache - pokud jsou data v cache, použije je, jinak si vyžádá nová data a zajistí i nakešování okolí.
     * Odpovídá volání WebApi.getFundNodeForm, jen dále zajišťuje cache.
     */
    //@Override
    _getItemFormData(getState, dispatch, versionId, nodeId, routingKey, showChildren, showParents) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE': // podpora kešování
                const state = getState();
                const node = this._getParentObjStore(state, versionId, routingKey);
                if (node === null) {
                    console.error('Node not found, versionId=' + versionId);
                    return; // nemělo by nastat
                }

                const subNodeFormCache = node.subNodeFormCache;

                const data = subNodeFormCache.dataCache[nodeId];
                if (!data) {
                    // není v cache, načteme ji včetně okolí
                    // ##
                    // # Data pro cache, jen pokud již cache nenačítá
                    // ##
                    if (false) {
                        if (node.isNodeInfoFetching || !node.nodeInfoFetched || node.nodeInfoDirty) {
                            // nemáme platné okolí (okolní NODE) pro daný NODE, raději je načteme ze serveru; nemáme vlastně okolní NODE pro získání seznamu ID pro načtení formulářů pro cache
                            //console.log('### READ_CACHE', 'around')

                            dispatch(this._fundSubNodeFormCacheRequest(versionId, routingKey));
                            WebApi.getFundNodeFormsWithAround(versionId, nodeId, CACHE_SIZE2).then(json => {
                                dispatch(this._fundSubNodeFormCacheResponse(versionId, routingKey, json.forms));
                            });
                        } else {
                            // pro získání id okolí můžeme použít store
                            // Načtení okolí položky
                            const index = indexById(node.childNodes, nodeId);
                            const left = node.childNodes.slice(Math.max(index - CACHE_SIZE2, 0), index);
                            const right = node.childNodes.slice(index, index + CACHE_SIZE2);

                            const idsForFetch = [];
                            left.forEach(n => {
                                if (!subNodeFormCache.dataCache[n.id]) {
                                    idsForFetch.push(n.id);
                                }
                            });
                            right.forEach(n => {
                                if (!subNodeFormCache.dataCache[n.id]) {
                                    idsForFetch.push(n.id);
                                }
                            });

                            //console.log('### READ_CACHE', idsForFetch, node.childNodes, left, right)

                            if (idsForFetch.length > 0) {
                                // máme něco pro načtení
                                dispatch(this._fundSubNodeFormCacheRequest(versionId, routingKey));
                                WebApi.getFundNodeForms(versionId, idsForFetch).then(json => {
                                    dispatch(this._fundSubNodeFormCacheResponse(versionId, routingKey, json.forms));
                                });
                            }
                        }
                    }

                    // ##
                    // # Data požadovaného formuláře
                    // ##

                    const nodeParam = {nodeId};
                    const resultParam = {
                        formData: true,
                        parents: showParents && node.changeParent,
                        children: showChildren,
                        siblingsFrom: node.viewStartIndex,
                        siblingsMaxCount: node.pageSize,
                        siblingsFilter: node.filterText,
                    };
                    return WebApi.getNodeData(versionId, nodeParam, resultParam).then(json => {
                        dispatch(
                            fundNodeInfoReceive(versionId, nodeId, routingKey, {
                                childNodes: json.siblings ? json.siblings : null,
                                nodeCount: json.nodeCount,
                                nodeIndex: json.nodeIndex,
                                parentNodes: json.parents ? json.parents : null,
                            }),
                        );

                        dispatch(fundSubNodeInfoReceive(versionId, nodeId, routingKey, {nodes: json.children}));

                        return json.formData;
                    });
                } else {
                    // je v cache, vrátíme ji
                    //console.log('### USE_CACHE')
                    return new Promise(function (resolve, reject) {
                        resolve(data);
                    });
                }
            case 'DATA_GRID': // není podpora kešování
                const nodeParam = {nodeId};
                const resultParam = {
                    formData: true,
                };
                return WebApi.getNodeData(versionId, nodeParam, resultParam).then(json => json.formData);
            default:
                break;
        }
    }

    // @Override
    _getItemFormStore(state, versionId, routingKey) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE':
                const node = this._getParentObjStore(state, versionId, routingKey);
                if (node !== null) {
                    return node.subNodeForm;
                } else {
                    return null;
                }
            case 'DATA_GRID':
                const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
                if (fundIndex !== null) {
                    return state.arrRegion.funds[fundIndex].fundDataGrid.subNodeForm;
                } else {
                    return null;
                }
            default:
                break;
        }

        return null;
    }

    // @Override
    _getParentObjStore(state, versionId, routingKey) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE':
                const r = findByRoutingKeyInGlobalState(state, versionId, routingKey);
                if (r !== null) {
                    return r.node;
                }
                break;
            case 'DATA_GRID':
                const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
                if (fundIndex !== null) {
                    return state.arrRegion.funds[fundIndex].fundDataGrid;
                }
                break;
            default:
                break;
        }

        return null;
    }

    // @Override
    _callUpdateDescItem(dispatch, formState, versionId, parentVersionId, parentId, descItem) {
        // Umělé navýšení verze o 1 - aby mohla pozitivně projít případná další update operace
        console.log('Before update, parentVersionId: ', parentVersionId);
        dispatch(increaseNodeVersion(versionId, parentId, parentVersionId));

        console.log('update desc Item');
        return new Promise((resolve, reject) => {
            NodeRequestController.updateRequest(versionId, parentVersionId, parentId, descItem, json => {
                resolve(json);
            });
        });
    }

    // @Override
    _callDeleteDescItem(versionId, parentId, parentVersionId, descItem) {
        return WebApi.deleteDescItem(versionId, parentId, parentVersionId, descItem);
    }

    // @Override
    _callCreateDescItem(versionId, parentId, parentVersionId, descItemTypeId, descItem) {
        console.log('create desc Item');
        return WebApi.createDescItem(versionId, parentId, parentVersionId, descItemTypeId, descItem);
    }

    // @Override
    _callArrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        return WebApi.arrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDescItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        return WebApi.descItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDeleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId) {
        return WebApi.deleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId);
    }

    // @Override
    _callSetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        descItemTypeId,
        descItemSpecId,
        descItemObjectId,
    ) {
        return WebApi.setNotIdentifiedDescItem(
            versionId,
            nodeId,
            parentNodeVersion,
            descItemTypeId,
            descItemSpecId,
            descItemObjectId,
        );
    }

    // @Override
    _callUnsetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        descItemTypeId,
        descItemSpecId,
        descItemObjectId,
    ) {
        return WebApi.unsetNotIdentifiedDescItem(
            versionId,
            nodeId,
            parentNodeVersion,
            descItemTypeId,
            descItemSpecId,
            descItemObjectId,
        );
    }

    // @Override
    _getParentObjIdInfo(parentObjStore, routingKey) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE':
                return {
                    parentId: parentObjStore.selectedSubNodeId,
                    parentVersion: parentObjStore.subNodeForm.versionId,
                };
            case 'DATA_GRID':
                return {parentId: parentObjStore.nodeId, parentVersion: parentObjStore.subNodeForm.versionId};
            default:
                break;
        }
    }
}

export const nodeFormActions = new NodeFormActions();
