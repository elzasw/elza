import React, { ReactElement, useEffect, useState, useRef, PropsWithChildren } from 'react';
import { connect, useDispatch } from 'react-redux';
import { Action } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { objectByProperty } from "stores/app/utils";
import * as registry from '../../actions/registry/registry';
import {goToAe} from '../../actions/registry/registry';
import { WebApi } from '../../actions/WebApi';
import { ApAccessPointVO } from '../../api/ApAccessPointVO';
import { ApPartVO } from '../../api/ApPartVO';
import { ApValidationErrorsVO } from '../../api/ApValidationErrorsVO';
import { ApViewSettingRule, ApViewSettings } from '../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../api/PartValidationErrorsVO';
import { RulPartTypeVO } from '../../api/RulPartTypeVO';
import { AP_VALIDATION, AP_VIEW_SETTINGS } from '../../constants';
import { DetailActions } from '../../shared/detail';
import { indexById } from '../../shared/utils';
import storeFromArea from '../../shared/utils/storeFromArea';
import { Bindings, DetailStoreState } from '../../types';
import { BaseRefTableStore } from '../../typings/BaseRefTableStore';
import { AppState } from '../../typings/store';
import Loading from '../shared/loading/Loading';
import { DetailBodySection, DetailMultiSection } from './Detail/section';
import { DetailHeader } from './Detail/header';
import { showPartCreateModal, showPartEditModal } from './part-edit';
import i18n from 'components/i18n';
import { showConfirmDialog } from "components/shared/dialog";
import './ApDetailPageWrapper.scss';
import { RevisionPart, getRevisionParts } from './revision';
import { ApStateVO } from 'api/ApStateVO';
import {Api} from '../../api';
import {RouteComponentProps, withRouter} from "react-router";
import { RevStateApproval } from 'api/RevStateApproval';
import Icon from 'components/shared/icon/FontIcon';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { SyncProgress } from 'api/ApBindingVO';
import { WebsocketEventType } from 'components/shared/web-socket/enums';
import { addToastrDanger } from 'components/shared/toastr/ToastrActions';

function createBindings(accessPoint: ApAccessPointVO | undefined) {
    const bindingsMaps: Bindings = {
        itemsMap: {},
        partsMap: {},
    };

    const newItem = (id:number, sync:boolean, map: {[key:number]:boolean}) => 
         (map[id] || true) && sync;

    if (accessPoint) {
        const bindings = accessPoint.bindings || [];
        bindings.forEach(externalId => {
            const bindingItemList = externalId.bindingItemList || [];
            bindingItemList.forEach(item => {
              if (item.itemId) {
                bindingsMaps.itemsMap[item.itemId] = newItem(item.itemId, item.sync, bindingsMaps.itemsMap);
              } else if (item.partId) {
                bindingsMaps.partsMap[item.partId] = newItem(item.partId, item.sync, bindingsMaps.partsMap);
              }
            });
        });
    }
    return bindingsMaps;
}

function sortPart(items: RulPartTypeVO[], data: ApViewSettingRule | undefined) {
    const parts = [...items];
    if (data && data.partsOrder) {
        parts.sort((a, b) => {
            const aIndex = indexById(data.partsOrder, a.code, 'code') || 0;
            const bIndex = indexById(data.partsOrder, b.code, 'code') || 0;
            return aIndex - bIndex;
        });
    }
    return parts;
}

function sortPrefer(parts: ApPartVO[], preferredPart?: number) {
    if (preferredPart != null) {
        parts.sort((a, b) => {
            if (a.id == preferredPart) {
                return -1;
            } else if (b.id == preferredPart) {
                return 1;
            } else {
                return 0;
            }
        });
    }
    return parts;
}

type OwnProps = {
    id: number; // ap id
    sider: ReactElement;
    editMode: boolean;
    globalCollapsed: boolean;
    apValidation: DetailStoreState<ApValidationErrorsVO>;
    apViewSettings: DetailStoreState<ApViewSettings>;
    globalEntity: boolean;
    select: boolean;
};

type Props = OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps>;

let scrollTop: number | undefined = undefined;

export enum ExportState {
    PENDING = "PENDING",
    STARTED = "STARTED",
    COMPLETED = "COMPLETED",
}

const WaitingOverlay = ({
    children,
}:PropsWithChildren<{}>) => {
    return <div className="waiting-overlay">
        <div className="waiting-icon">
            <Icon glyph="fa-spin fa-circle-o-notch"/>
        </div>
        <div>
            {children}
        </div>
    </div>
}

/**
 * Detail globální archivní entity.
 */
const ApDetailPageWrapper: React.FC<Props> = ({
    id, // ap id
    editMode,    
    globalCollapsed,
    apValidation,
    apViewSettings,
    globalEntity,
    detail,
    refreshDetail,
    fetchViewSettings,
    refreshValidation,
    setPreferred,
    setRevisionPreferred,
    deletePart,
    deleteRevisionPart,
    showConfirmDialog,
    showPartEditModal,
    showPartCreateModal,
    refTables,
    select,
}) => {
    const apTypeId = detail.fetched && detail.data ? detail.data.typeId : 0;

    const [collapsed, setCollapsed] = useState<boolean>(false);
    const [revisionActive, setRevisionActive] = useState<boolean>(false);
    const [exportState, setExportState] = useState<ExportState>(ExportState.COMPLETED);

    const containerRef = useRef<HTMLDivElement>(null);

    const websocket = useWebsocket();
    const bindings = detail.data?.bindings || [];
    const dispatch = useDispatch();

    useEffect(() => {
        if (id) {
            refreshDetail(id, false, false);
        }
    }, [id, refreshDetail]);

    // show accesspoint export message on websocket message
    useEffect(()=>{
        const eventMap = {
            [WebsocketEventType.ACCESS_POINT_EXPORT_NEW]: ({ accessPointId }) => {
                if(accessPointId.toString() === id.toString()){
                    setExportState(ExportState.PENDING);
                }
            },
            [WebsocketEventType.ACCESS_POINT_EXPORT_STARTED]: ({ accessPointId }) => {
                if(accessPointId.toString() === id.toString()){
                    setExportState(ExportState.STARTED);
                }
            },
            [WebsocketEventType.ACCESS_POINT_EXPORT_COMPLETED]: ({ accessPointId }) => {
                if(accessPointId.toString() === id.toString()){
                    setExportState(ExportState.COMPLETED);
                    refreshDetail(id, true, false);
                }
            },
            [WebsocketEventType.ACCESS_POINT_EXPORT_FAILED]: ({ accessPointId }) => {
                if(accessPointId.toString() === id.toString()){
                    dispatch(addToastrDanger(i18n("ap.push-to-ext.failed.title"), i18n("ap.push-to-ext.failed.message")))
                    setExportState(ExportState.COMPLETED);
                    refreshDetail(id, true, false);
                }
            },
        }

        const listener = websocket?.addListener((message:any) => { // TODO create websocket message types
            const handler = eventMap[message.eventType];
            if(handler){ handler(message) }
        })

        return () => {
            websocket?.removeListener(listener);
        }
    },[id, websocket])

    // show accesspoint export message on bindings state
    useEffect(() => {
        const stateMap = {
            [SyncProgress.UPLOAD_PENDING]: ExportState.PENDING,
            [SyncProgress.UPLOAD_STARTED]: ExportState.STARTED,
        }
        bindings.forEach(({syncProgress}) => {
            const state = stateMap[syncProgress];
            if(state){
                setExportState(state);
            }
        })
    },[bindings])

    useEffect(() => {
        fetchViewSettings();
        if(detail.fetched && detail.data){
            refreshValidation(id, revisionActive);
        }
    }, [id, detail]);

    const isStoreLoading = (stores: Array<BaseRefTableStore<unknown> | DetailStoreState<unknown>>) => 
        stores.some((store) => !store.fetched || store.isFetching)

    if (isStoreLoading([
        refTables.partTypes,
        refTables.recordTypes as any,
        refTables.apTypes,
        refTables.descItemTypes,
        detail,
        apViewSettings
    ])) {
        return (
            <div className={'detail-page-wrapper'}>
                <Loading />
            </div>
        );
    }

    // Show message when entity with specified id does not exist
    if (id == null || (id && (!detail.id || !detail.data))) {
        return <div  className="detail-page-wrapper missing-entity">
            <div className="message-container">
                <div className="message">
                    <div className="message-icon">
                        <Icon glyph="fa-regular fa-times-circle-o"/>
                    </div>
                    <div className="message-text">
                        {i18n("ap.detail.entityMissing")}
                    </div>
                </div>
            </div>
        </div>;
    }

    const handleSetPreferred = async ({part, updatedPart}: RevisionPart) => {
        const nextPreferredPart = part ? part : updatedPart;
        if (nextPreferredPart?.id) {
            saveScrollPosition();
            part ? await setPreferred(id, nextPreferredPart.id) : await setRevisionPreferred(id, nextPreferredPart.id);
            restoreScrollPosition();
            refreshValidation(id, revisionActive);
        }
    };

    const handleDelete = async ({part, updatedPart}: RevisionPart) => {
        const deletedPart = part ? part : updatedPart;
        const message = deletedPart?.value ? i18n("ap.detail.delete.confirm.value", deletedPart.value) : i18n("ap.detail.delete.confirm");
        const confirmResult = await showConfirmDialog(message);

        if(confirmResult){
            if (deletedPart?.id) {
                saveScrollPosition();
                part ? await deletePart(id, deletedPart.id) : await deleteRevisionPart(id, deletedPart.id);
                restoreScrollPosition();
            }

            refreshValidation(id, revisionActive);
        }
    };

    const handleRevert = async ({part, updatedPart}: RevisionPart) => {
        if(!part || !updatedPart){throw "No part to update."}
        const confirmResult = await showConfirmDialog(i18n("ap.detail.revert.confirm"));

        if(confirmResult){
            saveScrollPosition();
            await deleteRevisionPart(id, updatedPart.id);
            restoreScrollPosition();
            refreshValidation(id, revisionActive);
        }
    }

    const saveScrollPosition = () => {
        scrollTop = containerRef.current?.scrollTop || undefined;
    }

    const restoreScrollPosition = () => {
        if(containerRef.current && scrollTop){
            containerRef.current.scrollTop = scrollTop;
            scrollTop = undefined;
        }
    }

    const handleEdit = (part: RevisionPart) => {
        const partTypeId = part.part?.typeId ? part.part.typeId : part.updatedPart?.typeId;
        const partType = refTables.partTypes.itemsMap && partTypeId ? refTables.partTypes.itemsMap[partTypeId].code : null;

        saveScrollPosition();
        detail.data &&
            showPartEditModal(
                part.part,
                part.updatedPart,
                partType,
                id,
                apTypeId,
                detail.data.ruleSetId,
                detail.data.scopeId,
                refTables,
                apViewSettings,
                !!detail.data.revStateApproval,
                () => restoreScrollPosition()
            );
        refreshValidation(id, revisionActive);
    };

    const handleAdd = (partType: RulPartTypeVO, parentPartId?: number, revParentPartId?: number) => {
        if(detail.data){
            saveScrollPosition();
            showPartCreateModal(
                partType, 
                id, 
                apTypeId, 
                detail.data.scopeId, 
                parentPartId,
                () => restoreScrollPosition(),
                revParentPartId
            );
        }
        refreshValidation(id, revisionActive);
    };

    const allParts = sortPrefer( detail.data ? detail.data.parts : [], detail.data?.preferredPart);
    const allRevisionParts = detail.data?.revStateApproval && revisionActive ? getRevisionParts(allParts, detail.data.revParts) : getRevisionParts(allParts, []);
    const filteredRevisionParts = allRevisionParts.filter(({part, updatedPart}) => 
        !part?.partParentId 
            && !updatedPart?.partParentId 
            && !part?.revPartParentId 
            && !updatedPart?.revPartParentId);

    const getRelatedPartSections = (parentParts: RevisionPart[]) => {
        if (parentParts.length === 0) { return []; }
        const parentIds: number[] = [];
        const updatedParentIds: number[] = [];

        parentParts.forEach(({part, updatedPart})=>{
            if(part){parentIds.push(part.id)}
            if(updatedPart){updatedParentIds.push(updatedPart.id)}
        })

        console.log(allRevisionParts, parentParts, parentIds, updatedParentIds)

        return allRevisionParts
            .filter(value => 
                value.part?.partParentId && parentIds.includes(value.part?.partParentId)
                || value.part?.partParentId && updatedParentIds.includes(value.part?.partParentId)
                || value.updatedPart?.revPartParentId && parentIds.includes(value.updatedPart?.revPartParentId)
                || value.updatedPart?.partParentId && parentIds.includes(value.updatedPart?.partParentId)
                || value.updatedPart?.partParentId && updatedParentIds.includes(value.updatedPart?.partParentId)
                || value.updatedPart?.revPartParentId && updatedParentIds.includes(value.updatedPart?.revPartParentId)
        );
    };

    const bindingsMaps = createBindings(detail.data);
    const groupPartsByType = (data: RevisionPart[]) => {
        return data.reduce<Record<string, RevisionPart[]>>((accumulator, value) => {
            const typeId = value.part?.typeId || value.updatedPart?.typeId;
            if(typeId != undefined){
                const currentValue = accumulator[typeId] || [];
                accumulator[typeId.toString()] = [...currentValue, value];
            }
            return accumulator;
        }, {});
    }

    const groupedRevisionParts = groupPartsByType(filteredRevisionParts);
    const validationResult = apValidation.isFetching ? undefined : apValidation.data;

    const getSectionValidationErrors = (parts:RevisionPart[] = []) => {
        const errors:PartValidationErrorsVO[] = [];
        parts.forEach(({part, updatedPart})=>{
            const error = part && objectByProperty(validationResult?.partErrors, part.id, "id");
            const updatedError = updatedPart && objectByProperty(validationResult?.partErrors, updatedPart.id, "id");
            if(error){errors.push(error)}
            if(updatedError){errors.push(updatedError)}
        })
        return errors;
    };

    const canEdit = () => {
        const revState = detail.data?.revStateApproval;
        if(!revState){return editMode;}
        if(revState === RevStateApproval.TO_APPROVE){ return false; }
        return  editMode && revisionActive;
    }

    const sortedParts = detail.data && refTables.partTypes.items
        ? sortPart(refTables.partTypes.items, apViewSettings.data?.rules[detail.data.ruleSetId])
        : [];

    return (
        <div className={'detail-page-wrapper'} ref={containerRef}>
            {exportState !== "COMPLETED" && <WaitingOverlay>
                {
                    exportState === ExportState.PENDING 
                        ? i18n("ap.push-to-ext.pending.message") 
                        : i18n("ap.push-to-ext.started.message")
                }
            </WaitingOverlay>}
            <div key="1" className="layout-scroll">
                <DetailHeader
                    item={detail.data!}
                    id={detail.data!.id}
                    collapsed={collapsed}
                    onToggleCollapsed={() => setCollapsed(!collapsed)}
                    onToggleRevision={() => {
                        setRevisionActive(!revisionActive);
                        refreshValidation(id, !revisionActive);
                    }}
                    validationErrors={validationResult?.errors}
                    validationPartErrors={validationResult?.partErrors}
                    onInvalidateDetail={() => refreshDetail(detail.data!.id)}
                    onInvalidateValidation={() => refreshValidation(id, !revisionActive)}
                    revisionActive={revisionActive}
                />

                {allParts && (
                    <div key="part-sections">
                        {sortedParts.map((partType: RulPartTypeVO) => {
                            // const parts = groupedParts[partType.id] || [];
                            const revisionParts = groupedRevisionParts[partType.id] || [];

                            const onAddRelated = partType.childPartId
                            ? (parentPartId?:number, revParentPartId?:number) => {
                                const childPartType = partType.childPartId ? objectByProperty(
                                    refTables.partTypes.items,
                                    partType.childPartId,
                                    "id"
                                ) : null;
                                if (childPartType !== null) {
                                    handleAdd(childPartType, parentPartId, revParentPartId);
                                } else {
                                    console.error('childPartType ' + partType.childPartId + ' not found');
                                }
                            }
                            : undefined;
                            const apViewSettingRule = apViewSettings.data!.rules[detail.data!.ruleSetId];
                            if(partType.code === "PT_BODY" && revisionParts.length === 1){
                                return (
                                    <DetailBodySection
                                        key={partType.code}
                                        label={partType.name}
                                        editMode={canEdit()}
                                        part={revisionParts[0]}
                                        onEdit={handleEdit}
                                        bindings={bindingsMaps}
                                        onAdd={() => handleAdd(partType)}
                                        partValidationErrors={getSectionValidationErrors(revisionParts)}
                                        itemTypeSettings={apViewSettingRule?.itemTypes || []}
                                        globalEntity={globalEntity}
                                        partType={partType}
                                        onDelete={handleDelete}
                                        onRevert={handleRevert}
                                        revision={detail.data ? !!detail.data.revStateApproval && revisionActive : false}
                                        select={select}
                                    />
                                );
                            }
                            return (
                                <DetailMultiSection
                                    key={partType.code}
                                    label={partType.name}
                                    singlePart={!partType.repeatable && revisionParts.length === 1}
                                    editMode={canEdit()}
                                    parts={revisionParts}
                                    relatedParts={getRelatedPartSections(revisionParts)}
                                    preferred={detail.data ? detail.data.preferredPart : undefined}
                                    newPreferred={detail.data && revisionActive ? detail.data.newPreferredPart : undefined}
                                    revPreferred={detail.data && revisionActive ? detail.data.revPreferredPart : undefined}
                                    revision={detail.data ? !!detail.data.revStateApproval && revisionActive : false}
                                    globalCollapsed={globalCollapsed}
                                    onSetPreferred={handleSetPreferred}
                                    onEdit={handleEdit}
                                    onDelete={handleDelete}
                                    onRevert={handleRevert}
                                    bindings={bindingsMaps}
                                    onAdd={() => handleAdd(partType)}
                                    onAddRelated={onAddRelated}
                                    partValidationErrors={getSectionValidationErrors(revisionParts)}
                                    itemTypeSettings={apViewSettingRule?.itemTypes || []}
                                    globalEntity={globalEntity}
                                    partType={partType}
                                    select={select}
                                />
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
};

const mapDispatchToProps = (dispatch: ThunkDispatch<AppState, any, Action<string>>, {history, select}: RouteComponentProps & {select: boolean}) => ({
    showConfirmDialog: (message: string) => dispatch(showConfirmDialog(message)),
    showPartEditModal: (
        part: ApPartVO | undefined,
        updatedPart: ApPartVO | undefined,
        partType: unknown,
        apId: number,
        apTypeId: number,
        ruleSetId: number,
        scopeId: number,
        refTables: unknown,
        apViewSettings: DetailStoreState<ApViewSettings>,
        revision: boolean,
        onUpdateFinish: () => void = () => {},
    ) => dispatch(showPartEditModal(part, updatedPart, partType as any, apId, apTypeId, ruleSetId, scopeId, history, refTables as any, apViewSettings, revision, onUpdateFinish, select)),
    showPartCreateModal: (
        partType: RulPartTypeVO,
        apId: number,
        apTypeId: number,
        scopeId: number,
        parentPartId?: number,
        onUpdateFinish: () => void = () => {},
        revParentPartId?: number,
    ) => dispatch(showPartCreateModal(partType, apId, apTypeId, scopeId, history, select, parentPartId, onUpdateFinish, revParentPartId)),
    setPreferred: async (apId: number, partId: number) => {
        await WebApi.setPreferPartName(apId, partId);
        return dispatch(goToAe(history, apId, true, !select));
    },
    setRevisionPreferred: async (apId: number, partId: number) => {
        await Api.accesspoints.setPreferNameRevision(apId, partId);
        return dispatch(goToAe(history, apId, true, !select));
    },
    deletePart: async (apId: number, partId: number) => {
        await WebApi.deletePart(apId, partId);
        return dispatch(goToAe(history, apId, true, !select));
    },
    deleteRevisionPart: async (apId: number, partId: number) => {
        await Api.accesspoints.deleteRevisionPart(apId, partId);
        return dispatch(goToAe(history, apId, true, !select));
    },
    deleteParts: async (apId: number, parts: ApPartVO[]) => {
        for (let part of parts) {
            if (part.id) {
                await WebApi.deletePart(apId, part.id);
            }
        }

        dispatch(goToAe(history, apId, true, !select));
    },
    updateRevisionPart: async (apId: number, part: ApPartVO, typeCode: string) => {
        await WebApi.updateRevisionPart(apId, part.id, {
            parentPartId: part.partParentId,
            partId: part.id,
            items: part.items?.filter((item) => item) || [],
            partTypeCode: typeCode,
        })
    },
    refreshValidation: (apId: number, includeRevision?: boolean) => {
        dispatch(DetailActions.fetchIfNeeded(
            AP_VALIDATION,
            apId,
            (id: number) => WebApi.validateAccessPoint(id, includeRevision),
            true
        ));
    },
    refreshDetail: (apId: number, force: boolean = true, redirect: boolean = true) => {
        dispatch(goToAe(history, apId, force, redirect));
    },
    fetchViewSettings: () => {
        dispatch(
            DetailActions.fetchIfNeeded(AP_VIEW_SETTINGS, '', () => {
                return WebApi.getApTypeViewSettings();
            }),
        );
    },
});

const mapStateToProps = (state: AppState) => {
    return {
        detail: storeFromArea(state, registry.AREA_REGISTRY_DETAIL) as DetailStoreState<ApAccessPointVO>,
        apValidation: storeFromArea(state, AP_VALIDATION) as DetailStoreState<ApValidationErrorsVO>,
        apViewSettings: storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
        descItemTypesMap: state.refTables.descItemTypes.itemsMap,
        refTables: state.refTables,
    };
};

export default withRouter(connect<any, any, RouteComponentProps>(mapStateToProps, mapDispatchToProps)(ApDetailPageWrapper));
