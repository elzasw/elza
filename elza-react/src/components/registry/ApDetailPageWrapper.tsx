import React, { ReactElement, useEffect, useState } from 'react';
import { connect } from 'react-redux';
import { Action } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { objectByProperty } from "stores/app/utils";
import { globalFundTreeInvalidate } from '../../actions/arr/globalFundTree';
import { modalDialogHide, modalDialogShow } from '../../actions/global/modalDialog';
import * as registry from '../../actions/registry/registry';
import { registryDetailFetchIfNeeded } from '../../actions/registry/registry';
import { WebApi } from '../../actions/WebApi';
import { ApAccessPointVO } from '../../api/ApAccessPointVO';
import { ApItemBitVO } from '../../api/ApItemBitVO';
import { ApPartFormVO } from '../../api/ApPartFormVO';
import { ApPartVO } from '../../api/ApPartVO';
import { ApValidationErrorsVO } from '../../api/ApValidationErrorsVO';
import { ApViewSettingRule, ApViewSettings } from '../../api/ApViewSettings';
import { PartType } from '../../api/generated/model';
import * as PartTypeInfo from '../../api/old/PartTypeInfo';
import { PartValidationErrorsVO } from '../../api/PartValidationErrorsVO';
import { RulDescItemTypeExtVO } from '../../api/RulDescItemTypeExtVO';
import { RulPartTypeVO } from '../../api/RulPartTypeVO';
import { AP_VALIDATION, AP_VIEW_SETTINGS } from '../../constants';
import { DetailActions } from '../../shared/detail';
import { indexById, objectById } from '../../shared/utils';
import storeFromArea from '../../shared/utils/storeFromArea';
import { Bindings, DetailStoreState } from '../../types';
import { BaseRefTableStore } from '../../typings/BaseRefTableStore';
import { AppState } from '../../typings/store';
import { sortItems } from '../../utils/ItemInfo';
import Loading from '../shared/loading/Loading';
import { DetailBodySection, DetailMultiSection } from './Detail/section';
import { DetailHeader } from './Detail/header';
import PartEditModal from './modal/PartEditModal';

function createBindings(accessPoint: ApAccessPointVO | undefined) {
    let bindings: Bindings = {
        itemsMap: {},
        partsMap: {},
    };

    const newItem = (id:number, sync:boolean, map: {[key:number]:boolean}) => 
         (map[id] || true) && sync;

    if (accessPoint) {
        const externalIds = accessPoint.externalIds || [];
        externalIds.forEach(externalId => {
            const bindingItemList = externalId.bindingItemList || [];
            bindingItemList.forEach(item => {
              if (item.itemId) {
                bindings.itemsMap[item.itemId] = newItem(item.itemId, item.sync, bindings.itemsMap);
              } else if (item.partId) {
                bindings.partsMap[item.partId] = newItem(item.partId, item.sync, bindings.partsMap);
              }
            });
        });
    }
    return bindings;
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
    showPartEditModal,
    showPartCreateModal,
    descItemTypesMap,
    refTables,
}) => {
    const apTypeId = detail.fetched && detail.data ? detail.data.typeId : 0;

    const [collapsed, setCollapsed] = useState<boolean>(false);

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

    const handleSetPreferred = (part: ApPartVO) => {
        if (part.id) {
            setPreferred(id, part.id);
            refreshValidation(id);
        }
    };

    const handleDelete = (part: ApPartVO) => {
        if (part.id) {
            deletePart(id, part.id);
        }
        refreshValidation(id);
    };

    const handleEdit = (part: ApPartVO) => {
        const partType = refTables.partTypes.itemsMap ? refTables.partTypes.itemsMap[part.typeId].code : null;

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
            );
        refreshValidation(id);
    };

    const handleAdd = (partType: RulPartTypeVO, parentPartId?: number) => {
        if(detail.data){
            showPartCreateModal(partType, id, apTypeId, detail.data.scopeId, parentPartId);
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
        <div className={'detail-page-wrapper'}>
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
                            console.log(parts);
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
                            if(partType.code === "PT_BODY"){
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
                                    />
                                );
                            }
                            return (
                                <DetailMultiSection
                                    key={partType.code}
                                    label={partType.name}
                                    singlePart={!partType.repeatable}
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

interface ApPartData {
    partForm: ApPartFormVO;
}

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({
    showPartEditModal: (
        part: ApPartVO,
        partType,
        apId: number,
        apTypeId: number,
        ruleSetId: number,
        scopeId: number,
        refTables,
        descItemTypesMap: Record<number, RulDescItemTypeExtVO>,
        apViewSettings: DetailStoreState<ApViewSettings>,
        parentPartId?: number,
    ) => {
        dispatch(
            modalDialogShow(
                this,
                PartTypeInfo.getPartEditDialogLabel(partType, false),
                <PartEditModal
                    partTypeId={objectById(refTables.partTypes.items, partType, 'code').id}
                    onSubmit={({ partForm }: ApPartData) => {
                        if (!part.id) { return; }
                        const submitData = {
                            items: partForm.items.filter(i => {
                                if (i['@class'] === '.ApItemEnumVO') {
                                    return i.specId !== undefined;
                                } else {
                                    return (i as ApItemBitVO).value !== undefined;
                                }
                            }),
                            parentPartId: parentPartId,
                            partId: part.id,
                            partTypeCode: partType,
                        } as ApPartFormVO;

                        console.log('SUBMIT EDIT', apId, part.id, submitData);

                        return WebApi.updatePart(apId, part.id, submitData).then(() => {
                            dispatch(modalDialogHide());
                            dispatch(registryDetailFetchIfNeeded(apId, true));
                        });
                    }}
                    apTypeId={apTypeId}
                    scopeId={scopeId}
                    initialValues={{
                        partForm: {
                            items: part.items?sortItems(
                                partType,
                                part.items,
                                refTables,
                                descItemTypesMap,
                                apViewSettings.data!.rules[ruleSetId],
                            ):[],
                        },
                    }}
                    formData={
                        {
                            partId: part.id,
                            parentPartId: part.partParentId,
                            partTypeCode: refTables.partTypes.itemsMap[part.typeId].code,
                            items: part.items?sortItems(
                                partType,
                                part.items,
                                refTables,
                                descItemTypesMap,
                                apViewSettings.data!.rules[ruleSetId],
                            ):[],
                        } as ApPartFormVO
                    }
                    parentPartId={part.partParentId}
                    apId={apId}
                    partId={part.id}
                    onClose={() => {
                        dispatch(modalDialogHide());
                    }}
                />,
                'dialog-lg',
            ),
        );
    },
    showPartCreateModal: (
        partType: RulPartTypeVO,
        apId: number,
        apTypeId: number,
        scopeId: number,
        parentPartId?: number,
    ) => {
        dispatch(
            modalDialogShow(
                this,
                // TODO: není rozmyšleno, kde brát skloňované popisky!
                PartTypeInfo.getPartEditDialogLabel(partType.code as PartType, true),
                <PartEditModal
                    partTypeId={partType.id}
                    onSubmit={data => {
                        const formData: ApPartFormVO = data.partForm;

                        const submitData = {
                            items: formData.items.filter(i => {
                                if (i['@class'] === '.ApItemEnumVO') {
                                    return i.specId !== undefined;
                                } else {
                                    return (i as ApItemBitVO).value !== undefined;
                                }
                            }),
                            parentPartId: parentPartId,
                            partTypeCode: partType.code,
                        } as ApPartFormVO;

                        console.log('SUBMIT ADD', apId, submitData);

                        return WebApi.createPart(apId, submitData).then(() => {
                            dispatch(modalDialogHide());
                            dispatch(registryDetailFetchIfNeeded(apId, true));
                        });
                    }}
                    apTypeId={apTypeId}
                    scopeId={scopeId}
                    formData={
                        {
                            partTypeCode: partType.code,
                            items: [],
                        } as ApPartFormVO
                    }
                    parentPartId={parentPartId}
                    initialValues={{}}
                    apId={apId}
                    onClose={() => {
                        dispatch(modalDialogHide());
                    }}
                />,
                'dialog-lg',
                dispatch(globalFundTreeInvalidate()),
            ),
        );
    },
    setPreferred: async (apId: number, partId: number) => {
        await WebApi.setPreferPartName(apId, partId);
        dispatch(registryDetailFetchIfNeeded(apId, true));
    },
    deletePart: async (apId: number, partId: number) => {
        await WebApi.deletePart(apId, partId);
        dispatch(registryDetailFetchIfNeeded(apId, true));
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
