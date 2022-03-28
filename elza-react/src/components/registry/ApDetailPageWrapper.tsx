import React, { ReactElement, useEffect, useState, useRef } from 'react';
import { connect } from 'react-redux';
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
    updateRevisionPart,
    // deleteParts,
    showConfirmDialog,
    showPartEditModal,
    showPartCreateModal,
    descItemTypesMap,
    refTables,
    select,
}) => {
    const apTypeId = detail.fetched && detail.data ? detail.data.typeId : 0;

    const [collapsed, setCollapsed] = useState<boolean>(false);

    const containerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (id) {
            refreshDetail(id, false, false);
        }
    }, []);

    useEffect(() => {
        fetchViewSettings();
        refreshValidation(id);
    }, [id]);

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

    if (!detail.id || !detail.data) {
        return <div className={'detail-page-wrapper'} />;
    }

    const handleSetPreferred = async ({part, updatedPart}: RevisionPart) => {
        const nextPreferredPart = part ? part : updatedPart;
        if (nextPreferredPart?.id) {
            saveScrollPosition();
            part ? await setPreferred(id, nextPreferredPart.id) : await setRevisionPreferred(id, nextPreferredPart.id);
            restoreScrollPosition();
            refreshValidation(id);
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

            refreshValidation(id);
        }
    };

    const handleRevert = async ({part, updatedPart}: RevisionPart) => {
        if(!part || !updatedPart){throw "No part to update."}
        const confirmResult = await showConfirmDialog(i18n("ap.detail.revert.confirm"));

        if(confirmResult){
            saveScrollPosition();
            await deleteRevisionPart(id, updatedPart.id);
            restoreScrollPosition();
            refreshValidation(id);
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
        refreshValidation(id);
    };

    const handleAdd = (partType: RulPartTypeVO, parentPartId?: number) => {
        if(detail.data){
            saveScrollPosition();
            showPartCreateModal(
                partType, 
                id, 
                apTypeId, 
                detail.data.scopeId, 
                parentPartId,
                () => restoreScrollPosition()
            );
        }
        refreshValidation(id);
    };


    /*
    const handleDeletePart = (parts: Array<ApPartVO>) => {
        deleteParts(id, parts);
        refreshValidation(id);
    };
    */

   /**
    *   Prikladova data pro revize
    *
    *   Mela by fungovat s archivni entitou Tomas Garrigue Masaryk z camu.
    *   Pro zprovozneni je potreba upravit id, aby odpovidala databazi a
    *   zmenit promennou revisionTest na true.
    */
    const revisionTest = false;
    const exampleUpdatedParts = [
        {
            // Preferovane jmeno
            id: 1169,
            value: "modified name",
            state: ApStateVO.OK,
            typeId: 1,
            items: [{
                "@class": ".ApItemStringVO",
                id: 2403,
                typeId: 24,
                value: "modified value",
                position: 1,
                specId: null,
            }] as any
        },
        {
            // 2. jmeno (Masaryk, T. G.)
            id: 1170,
            value: null as any,
            specId: null as any,
            state: ApStateVO.OK,
            typeId: 1,
            items: [] as any
        },
        {
            // Pridane jmeno - neni potreba menit id
            id: 9999,
            value: "new name",
            state: ApStateVO.OK,
            typeId: 1,
            items: [{
                typeId: 29,
                specId: 18,
                id: 9875,
            }]
        },
        {
            // Udalost - manzelstvi, upraveny item - misto uzavreni
            id: 1175,
            value: "modified relation",
            parentPartId: 1124,
            typeId: 5,
            state: null as any,
            items: [{
                "@class": ".ApItemAccessPointRefVO",
                externalName: "modified ref item",
                externalUrl: "https://www.google.com",
                id: 2415,
                position: 1,
                specId: 384,
                typeId: 32,
            }] as any
        },
        {
            // Telo - Strucna charakteristika
            id: 1163,
            value: "modified body",
            typeId: 7,
            state: null as any,
            items: [{
                "@class": ".ApItemStringVO",
                id: 2385,
                position: 1,
                typeId: 26,
                value: "modified body value",
            }] as any
        }
    ]

    const allParts = sortPrefer( detail.data ? detail.data.parts : [], detail.data?.preferredPart);
    const allRevisionParts = detail.data.revStateApproval ? getRevisionParts(allParts, detail.data.revParts) : getRevisionParts(allParts, []);
    const filteredRevisionParts = allRevisionParts.filter(({part, updatedPart}) => !part?.partParentId && !updatedPart?.partParentId );

    /*
    const getRelatedPartSections = (parentParts: ApPartVO[]) => {
        if (parentParts.length === 0) { return []; }

        const parentIds = parentParts
            .filter(value => value.id)
            .map(value => value.id);
            
        return allParts
            .filter(value => value.partParentId && parentIds.includes(value.partParentId));
    };
    */

    const getRelatedPartSections = (parentParts: RevisionPart[]) => {
        if (parentParts.length === 0) { return []; }
        const parentIds: number[] = [];
        const updatedParentIds: number[] = [];

        parentParts.forEach(({part, updatedPart})=>{
            if(part){parentIds.push(part.id)}
            if(updatedPart){updatedParentIds.push(updatedPart.id)}
        })

        return allRevisionParts
            .filter(value => 
                value.part?.partParentId && parentIds.includes(value.part?.partParentId)
                || value.part?.partParentId && updatedParentIds.includes(value.part?.partParentId)
                || value.updatedPart?.partParentId && parentIds.includes(value.updatedPart?.partParentId)
                || value.updatedPart?.partParentId && updatedParentIds.includes(value.updatedPart?.partParentId)
        );
    };

    const bindings = createBindings(detail.data);
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

    /*
    const groupPartsByType = (data: ApPartVO[]):Record<string, ApPartVO[]> => 
        data.reduce<Record<string, ApPartVO[]>>((accumulator, value) => {
            (accumulator[value.typeId.toString()] = accumulator[value.typeId] || []).push(value);
            return accumulator;
        }, {});
    */

    /*
    const groupedParts = groupPartsByType(
        sortPrefer(
            allParts.filter(part => !part.partParentId),
            detail.data?.preferredPart,
        )
    );
    */

    const groupedRevisionParts = groupPartsByType(filteredRevisionParts);
    const validationResult = apValidation.data;

    /*
    const getSectionValidationErrors = (parts:ApPartVO[] = []) => {
        const errors:PartValidationErrorsVO[] = [];
        parts.forEach((part)=>{
            const error = objectByProperty(validationResult?.partErrors, part.id, "id");
            if(error){errors.push(error)}
        })
        return errors;
    };
    */
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

    const sortedParts = detail.data && refTables.partTypes.items
        ? sortPart(refTables.partTypes.items, apViewSettings.data?.rules[detail.data.ruleSetId])
        : [];

    return (
        <div className={'detail-page-wrapper'} ref={containerRef}>
            <div key="1" className="layout-scroll">
                <DetailHeader
                    item={detail.data!}
                    id={detail.data!.id}
                    collapsed={collapsed}
                    onToggleCollapsed={() => {
                        setCollapsed(!collapsed);
                    }}
                    validationErrors={validationResult && validationResult.errors}
                    onInvalidateDetail={() => refreshDetail(detail.data!.id)}
                />

                {allParts && (
                    <div key="part-sections">
                        {sortedParts.map((partType: RulPartTypeVO) => {
                            // const parts = groupedParts[partType.id] || [];
                            const revisionParts = groupedRevisionParts[partType.id] || [];

                            const onAddRelated = partType.childPartId
                            ? (parentPartId:number) => {
                                const childPartType = partType.childPartId ? objectByProperty(
                                    refTables.partTypes.items,
                                    partType.childPartId,
                                    "id"
                                ) : null;
                                if (childPartType !== null) {
                                    handleAdd(childPartType, parentPartId);
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
                                        editMode={editMode}
                                        part={revisionParts[0]}
                                        onEdit={handleEdit}
                                        bindings={bindings}
                                        onAdd={() => handleAdd(partType)}
                                        partValidationErrors={getSectionValidationErrors(revisionParts)}
                                        itemTypeSettings={apViewSettingRule?.itemTypes || []}
                                        globalEntity={globalEntity}
                                        partType={partType}
                                        onDelete={handleDelete}
                                        onRevert={handleRevert}
                                        revision={detail.data ? !!detail.data.revStateApproval : false}
                                        select={select}
                                    />
                                );
                            }
                            return (
                                <DetailMultiSection
                                    key={partType.code}
                                    label={partType.name}
                                    singlePart={!partType.repeatable && revisionParts.length === 1}
                                    editMode={editMode}
                                    parts={revisionParts}
                                    relatedParts={getRelatedPartSections(revisionParts)}
                                    preferred={detail.data ? detail.data.preferredPart : undefined}
                                    newPreferred={detail.data ? detail.data.newPreferredPart : undefined}
                                    revPreferred={detail.data ? detail.data.revPreferredPart : undefined}
                                    revision={detail.data ? !!detail.data.revStateApproval : false}
                                    globalCollapsed={globalCollapsed}
                                    onSetPreferred={handleSetPreferred}
                                    onEdit={handleEdit}
                                    onDelete={handleDelete}
                                    onRevert={handleRevert}
                                    bindings={bindings}
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
    ) => dispatch(showPartCreateModal(partType, apId, apTypeId, scopeId, history, select, parentPartId, onUpdateFinish)),
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
    refreshValidation: (apId: number) => {
        dispatch(
            DetailActions.fetchIfNeeded(
                AP_VALIDATION,
                apId,
                id => {
                    return WebApi.validateAccessPoint(id);
                },
                true,
            ),
        );
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
