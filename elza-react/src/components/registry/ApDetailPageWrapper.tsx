import React, { ReactElement, useEffect, useState, useRef } from 'react';
import { connect } from 'react-redux';
import { Action } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { objectByProperty } from "stores/app/utils";
import * as registry from '../../actions/registry/registry';
import { registryDetailFetchIfNeeded } from '../../actions/registry/registry';
import { WebApi } from '../../actions/WebApi';
import { ApAccessPointVO } from '../../api/ApAccessPointVO';
import { ApPartVO } from '../../api/ApPartVO';
import { ApValidationErrorsVO } from '../../api/ApValidationErrorsVO';
import { ApViewSettingRule, ApViewSettings } from '../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../api/PartValidationErrorsVO';
import { RulDescItemTypeExtVO } from '../../api/RulDescItemTypeExtVO';
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
import { showPartCreateModal, showPartEditModal } from './modal/part-edit-modal';
import i18n from 'components/i18n';
import { showConfirmDialog } from "components/shared/dialog";
import './ApDetailPageWrapper.scss';

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
    deletePart,
    // deleteParts,
    showConfirmDialog,
    showPartEditModal,
    showPartCreateModal,
    descItemTypesMap,
    refTables,
}) => {
    const apTypeId = detail.fetched && detail.data ? detail.data.typeId : 0;

    const [collapsed, setCollapsed] = useState<boolean>(false);

    const containerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (id) {
            refreshDetail(id, false);
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

    const handleSetPreferred = async (part: ApPartVO) => {
        if (part.id) {
            saveScrollPosition();
            await setPreferred(id, part.id);
            restoreScrollPosition();
            refreshValidation(id);
        }
    };

    const handleDelete = async (part: ApPartVO) => {
        const message = part.value ? i18n("ap.detail.delete.confirm.value", part.value) : i18n("ap.detail.delete.confirm");
        const confirmResult = await showConfirmDialog(message);

        if(confirmResult){
            if (part.id) {
                saveScrollPosition();
                await deletePart(id, part.id);
                restoreScrollPosition();
            }

            refreshValidation(id);
        }
    };

    const saveScrollPosition = () => {
        scrollTop = containerRef.current?.scrollTop || undefined;
    }

    const restoreScrollPosition = () => {
        if(containerRef.current && scrollTop){
            containerRef.current.scrollTop = scrollTop;
            scrollTop = undefined;
        }
    }

    const handleEdit = (part: ApPartVO) => {
        const partType = refTables.partTypes.itemsMap ? refTables.partTypes.itemsMap[part.typeId].code : null;

        saveScrollPosition();
        part.id && detail.data &&
            showPartEditModal(
                part,
                partType,
                id,
                apTypeId,
                detail.data.ruleSetId,
                detail.data.scopeId,
                refTables,
                descItemTypesMap as any,
                apViewSettings,
                part.partParentId,
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

    const allParts = detail.data ? detail.data.parts : [];

    const getRelatedPartSections = (parentParts: ApPartVO[]) => {
        if (parentParts.length === 0) { return []; }

        const parentIds = parentParts
            .filter(value => value.id)
            .map(value => value.id);
            
        return allParts
            .filter(value => value.partParentId && parentIds.includes(value.partParentId));
    };

    const bindings = createBindings(detail.data);


    const groupPartsByType = (data: ApPartVO[]):Record<string, ApPartVO[]> => 
        data.reduce<Record<string, ApPartVO[]>>((accumulator, value) => {
            (accumulator[value.typeId.toString()] = accumulator[value.typeId] || []).push(value);
            return accumulator;
        }, {});

    const groupedParts = groupPartsByType(
        sortPrefer(
            allParts.filter(part => !part.partParentId),
            detail.data?.preferredPart,
        )
    );

    const validationResult = apValidation.data;

    const getSectionValidationErrors = (parts:ApPartVO[] = []) => {
        const errors:PartValidationErrorsVO[] = [];
        parts.forEach((part)=>{
            const error = objectByProperty(validationResult?.partErrors, part.id, "id");
            if(error){errors.push(error)}
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
                            const parts = groupedParts[partType.id] || [];
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
                            if(partType.code === "PT_BODY" && parts.length === 1){
                                return (
                                    <DetailBodySection
                                        key={partType.code}
                                        label={partType.name}
                                        editMode={editMode}
                                        parts={parts}
                                        onEdit={handleEdit}
                                        bindings={bindings}
                                        onAdd={() => handleAdd(partType)}
                                        partValidationErrors={getSectionValidationErrors(parts)}
                                        itemTypeSettings={apViewSettingRule?.itemTypes || []}
                                        globalEntity={globalEntity}
                                        partType={partType}
                                        onDelete={handleDelete}
                                    />
                                );
                            }
                            return (
                                <DetailMultiSection
                                    key={partType.code}
                                    label={partType.name}
                                    singlePart={!partType.repeatable && parts.length === 1}
                                    editMode={editMode}
                                    parts={parts}
                                    relatedParts={getRelatedPartSections(parts)}
                                    preferred={detail.data ? detail.data.preferredPart : undefined}
                                    globalCollapsed={globalCollapsed}
                                    onSetPreferred={handleSetPreferred}
                                    onEdit={handleEdit}
                                    onDelete={handleDelete}
                                    bindings={bindings}
                                    onAdd={() => handleAdd(partType)}
                                    onAddRelated={onAddRelated}
                                    partValidationErrors={getSectionValidationErrors(parts)}
                                    itemTypeSettings={apViewSettingRule?.itemTypes || []}
                                    globalEntity={globalEntity}
                                    partType={partType}
                                />
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
};

const mapDispatchToProps = (dispatch: ThunkDispatch<AppState, void, Action<string>>) => ({
    showConfirmDialog: (message: string) => dispatch(showConfirmDialog(message)),
    showPartEditModal: (
        part: ApPartVO,
        partType: unknown,
        apId: number,
        apTypeId: number,
        ruleSetId: number,
        scopeId: number,
        refTables: unknown,
        descItemTypesMap: Record<number, RulDescItemTypeExtVO>,
        apViewSettings: DetailStoreState<ApViewSettings>,
        parentPartId?: number,
        onUpdateFinish: () => void = () => {},
    ) => dispatch(showPartEditModal(part, partType as any, apId, apTypeId, ruleSetId, scopeId, refTables as any, descItemTypesMap, apViewSettings, parentPartId, onUpdateFinish)),
    showPartCreateModal: (
        partType: RulPartTypeVO,
        apId: number,
        apTypeId: number,
        scopeId: number,
        parentPartId?: number,
        onUpdateFinish: () => void = () => {},
    ) => dispatch(showPartCreateModal(partType, apId, apTypeId, scopeId, parentPartId, onUpdateFinish)),
    setPreferred: async (apId: number, partId: number) => {
        await WebApi.setPreferPartName(apId, partId);
        return dispatch(registryDetailFetchIfNeeded(apId, true));
    },
    deletePart: async (apId: number, partId: number) => {
        await WebApi.deletePart(apId, partId);
        return dispatch(registryDetailFetchIfNeeded(apId, true));
    },
    deleteParts: async (apId: number, parts: ApPartVO[]) => {
        for (let part of parts) {
            if (part.id) {
                await WebApi.deletePart(apId, part.id);
            }
        }

        dispatch(registryDetailFetchIfNeeded(apId, true));
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
    refreshDetail: (apId: number, force: boolean = true) => {
        dispatch(registryDetailFetchIfNeeded(apId, force));
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

export default connect(mapStateToProps, mapDispatchToProps)(ApDetailPageWrapper);
