/**
 * Akce pro formulář JP.
 */

import { WebApi } from 'actions/index';
import { findByRoutingKeyInGlobalState, getRoutingKeyType, indexById } from 'stores/app/utils';
import NodeRequestController from '../../../websocketController';
import { increaseNodeVersion } from '../node';
import { fundNodeInfoReceive } from '../nodeInfo';
import { fundSubNodeInfoReceive } from '../subNodeInfo';
import { AreaType, CreateDescItemResult, ItemFormActions } from './itemFormActions';
import { Api } from 'api';
import { AppState, DescItemTypeRef, Node } from 'typings/store';
import { DescItem, DescItemFromServer } from 'typings/DescItem';
import { ValueLocationIndex } from 'typings/store/SubNodeForm.types';
import { NodeItem } from 'elza-api';
import { transformToNodeItem } from './itemData';

// Konfigurace velikosti cache dat pro formulář
const CACHE_SIZE = 20;
const CACHE_SIZE2 = CACHE_SIZE / 2;

//var debouncedGetFundNodeForm = debounce(WebApi.getFundNodeForm,200);
export class NodeFormActions extends ItemFormActions {
    static AREA = AreaType.NODE_AREA;

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
    fundSubNodeFormValuesCopyFromPrev(versionId: number, nodeId: number, nodeVersionId: number, descItemTypeId: number, routingKey: string, valueLocation: ValueLocationIndex) {
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
    _getItemFormData(getState: () => AppState, dispatch, versionId: number, nodeId: number, routingKey: string, showChildren: boolean, showParents: boolean) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE': // podpora kešování
                const state = getState();
                const node = this._getParentObjStore(state, versionId, routingKey) as Node;
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
                    /*
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
                    */

                    // ##
                    // # Data požadovaného formuláře
                    // ##

                    return Api.node.nodeGetNodeData({
                        fundVersionId: versionId,
                        nodeId,
                        formData: true,
                        parents: !!(showParents && node.changeParent),
                        children: !!showChildren,
                        siblingsFrom: node.viewStartIndex,
                        siblingsMaxCount: node.pageSize,
                        siblingsFilter: node.filterText,
                        // nodeStatus omitted — NodePanel does not consume data.node;
                        // NodeEdit fetches it separately via its own useNodeFormData hook.
                    }).then(({ data: json }) => {
                        dispatch(
                            fundNodeInfoReceive(versionId, nodeId, routingKey, {
                                childNodes: json.siblings ? json.siblings : null,
                                nodeCount: json.nodeCount,
                                nodeIndex: json.nodeIndex,
                                parentNodes: json.parents ? json.parents : null,
                            }),
                        );

                        dispatch(fundSubNodeInfoReceive(versionId, nodeId, routingKey, { nodes: json.children }));

                        return json.formData;
                    });
                } else {
                    // je v cache, vrátíme ji
                    //console.log('### USE_CACHE')
                    return new Promise(function(resolve, reject) {
                        resolve(data);
                    });
                }
            // DATA_GRID routing previously fetched form-data for in-grid cell editing.
            // Replaced by FundDataGridCellForm (uses useNodeFormData hook → new endpoint directly);
            // this branch is no longer reachable from any dispatcher.
            default:
                break;
        }
    }

    // @Override
    _getItemFormStore(state: AppState, versionId: number, routingKey: string) {
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
    _getParentObjStore(state: AppState, versionId: number, routingKey: string) {
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
    _callUpdateDescItem(dispatch, formState, versionId: number, parentVersionId: number, parentId: number, descItem: DescItem) {
        // Umělé navýšení verze o 1 - aby mohla pozitivně projít případná další update operace
        dispatch(increaseNodeVersion(versionId, parentId, parentVersionId));

        return new Promise((resolve, reject) => {
            NodeRequestController.updateRequest(versionId, parentVersionId, parentId, descItem, json => {
                resolve(json);
            });
        });
    }

    // @Override
    _callDeleteDescItem(versionId: number, parentId: number, parentVersionId: number, descItem: DescItem) {
        return WebApi.deleteDescItem(versionId, parentId, parentVersionId, descItem);
    }

    // @Override
    async _callCreateDescItem(versionId: number, parentId: number, parentVersionId: number, _descItemTypeId: number, descItem: DescItem, refType: DescItemTypeRef) {
        const nodeItem = transformToNodeItem(descItem, parentId, parentVersionId, refType);
        const { data } = await Api.descItems.descItemCreateDescItem(versionId, nodeItem);
        return data as CreateDescItemResult;
    }

    // @Override
    _callArrCoordinatesImport(versionId: number, parentId: number, parentVersionId: number, descItemTypeId: number, file) {
        return WebApi.arrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDescItemCsvImport(versionId: number, parentId: number, parentVersionId: number, descItemTypeId: number, file) {
        return WebApi.descItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDeleteDescItemType(versionId: number, parentId: number, parentVersionId: number, descItemTypeId: number) {
        return WebApi.deleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId);
    }

    // @Override
    _callSetNotIdentifiedDescItem(
        versionId: number,
        nodeId: number,
        parentNodeVersion: number,
        descItemTypeId: number,
        descItemSpecId: number,
        descItemObjectId: number,
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
        versionId: number,
        nodeId: number,
        parentNodeVersion: number,
        descItemTypeId: number,
        descItemSpecId: number,
        descItemObjectId: number,
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

    _callSetInhibitDescItem(nodeId: number, itemId: number, inhibit: boolean) {
        if (inhibit) {
            return WebApi.inhibitDescItem(nodeId, itemId);
        }
        return WebApi.allowDescItem(nodeId, itemId);
    }

    // @Override
    _getParentObjIdInfo(parentObjStore: any, routingKey: string) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE':
                return {
                    parentId: parentObjStore.selectedSubNodeId,
                    parentVersion: parentObjStore.subNodeForm.versionId,
                };
            case 'DATA_GRID':
                return { parentId: parentObjStore.nodeId, parentVersion: parentObjStore.subNodeForm.versionId };
            default:
                break;
        }
    }
}

export const nodeFormActions = new NodeFormActions();
