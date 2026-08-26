/**
 * Akce pro formulář JP.
 */

import { WebApi } from 'actions/index';
import { findByRoutingKeyInGlobalState, getRoutingKeyType } from 'stores/app/utils';
import NodeRequestController from '../../../websocketController';
import { increaseNodeVersion } from '../node';
import { fundNodeInfoReceive } from '../nodeInfo';
import { fundSubNodeInfoReceive } from '../subNodeInfo';
import { AreaType, CreateDescItemResult, ItemFormActions } from './itemFormActions';
import * as ActionTypes from 'actions/constants/ActionTypes';
import { Api } from 'api';
import { AppState, DescItemTypeRef, Node } from 'typings/store';
import { DescItem, DescItemFromServer } from 'typings/DescItem';
import { ValueLocationIndex } from 'typings/store/SubNodeForm.types';
import { NodeItem } from 'elza-api';
import { transformToNodeItem } from './itemData';

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
     * Načtení server dat pro formulář pro aktuálně předané parametry.
     *
     * NODE area override: calls the new /node/node-data endpoint inline with `nodeStatus: true` so the
     * response carries both NodeFormData and NodeStatus. Both are dispatched into Redux so NodePanel can
     * forward them to NodeEdit/NodeView as seed props (eliminating the duplicate fetch the hook used to do).
     */
    // @Override
    _fundSubNodeFormFetch(versionId: number, nodeId: number, routingKey: string, needClean: boolean, showChildren?: boolean, showParents?: boolean) {
        return (dispatch, getState) => {
            dispatch(this.fundSubNodeFormRequest(versionId, nodeId, routingKey));

            const state = getState();
            const node = this._getParentObjStore(state, versionId, routingKey) as Node;
            if (node === null) {
                console.error('Node not found, versionId=' + versionId);
                return;
            }

            // Parents are only fetched when the parent chain may have changed (e.g. user navigates to a
            // different level). When switching focus between siblings at the same level the previously
            // fetched parents stay valid — `parentsRequested` is false and the dispatch passes `null` to
            // signal "no update", which the node reducer treats as preserve-previous.
            const parentsRequested = !!(showParents && node.changeParent);
            const childrenRequested = !!showChildren;

            Api.node.nodeGetNodeData({
                fundVersionId: versionId,
                nodeId,
                formData: true,
                parents: parentsRequested,
                children: childrenRequested,
                siblingsFrom: node.viewStartIndex,
                siblingsMaxCount: node.pageSize,
                siblingsFilter: node.filterText,
                nodeStatus: true,
            }).then(({ data: json }) => {
                dispatch(
                    fundNodeInfoReceive(versionId, nodeId, routingKey, {
                        childNodes: json.siblings ? json.siblings : null,
                        nodeCount: json.nodeCount,
                        nodeIndex: json.nodeIndex,
                        // Pass null when parents weren't requested so the reducer preserves the previous
                        // list. An empty array means "requested but empty" (e.g. focus on the fund root).
                        parentNodes: parentsRequested ? (json.parents ?? []) : null,
                    }),
                );

                if (childrenRequested) {
                    dispatch(fundSubNodeInfoReceive(versionId, nodeId, routingKey, { nodes: json.children ?? [] }));
                }

                const newState = getState();
                const subNodeForm = this._getItemFormStore(newState, versionId, routingKey);
                if (subNodeForm && subNodeForm.fetchingId == nodeId) {
                    // Dispatch the receive action directly so we can carry the nodeStatus payload alongside
                    // the legacy formData. The reducer's NODE branch reads action.nodeStatus.
                    dispatch({
                        type: ActionTypes.FUND_SUB_NODE_FORM_RECEIVE,
                        area: this.area,
                        versionId,
                        nodeId,
                        routingKey,
                        data: json.formData,
                        nodeStatus: json.node,
                        rulDataTypes: newState.refTables.rulDataTypes,
                        refDescItemTypes: newState.refTables.descItemTypes,
                        groups: newState.refTables.groups.data,
                        receivedAt: Date.now(),
                        needClean,
                    });
                }
            });
        };
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
            default:
                break;
        }
    }
}

export const nodeFormActions = new NodeFormActions();
