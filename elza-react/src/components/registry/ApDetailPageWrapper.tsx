import React, {ReactElement, useEffect, useState} from 'react';
import {ThunkDispatch} from 'redux-thunk';
import {Action} from 'redux';
import {connect} from 'react-redux';
import {PartType} from '../../api/generated/model';
import * as PartTypeInfo from '../../api/old/PartTypeInfo';
import DetailMultiSection from './Detail/DetailMultiSection';
import Loading from '../shared/loading/Loading';
import {globalFundTreeInvalidate} from '../../actions/arr/globalFundTree';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog';
import {WebApi} from '../../actions/WebApi';
import {DetailActions} from '../../shared/detail';
import {AP_VALIDATION, AP_VIEW_SETTINGS} from '../../constants';
import storeFromArea from '../../shared/utils/storeFromArea';
import * as registry from '../../actions/registry/registry';
import {ApAccessPointVO} from '../../api/ApAccessPointVO';
import {ApValidationErrorsVO} from '../../api/ApValidationErrorsVO';
import {ApPartVO} from '../../api/ApPartVO';
import {DetailStoreState} from '../../types';
import DetailHeader from './Detail/DetailHeader';
import {ApPartFormVO} from '../../api/ApPartFormVO';
import PartEditModal from './modal/PartEditModal';
import {sortItems} from '../../utils/ItemInfo';
import {RulPartTypeVO} from '../../api/RulPartTypeVO';
import {registryDetailFetchIfNeeded} from '../../actions/registry/registry';
import {ApViewSettingRule, ApViewSettings} from '../../api/ApViewSettings';
import {indexById, objectById} from '../../shared/utils';
import {RulDescItemTypeExtVO} from '../../api/RulDescItemTypeExtVO';
import {ApItemBitVO} from '../../api/ApItemBitVO';

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

function createBindings(accessPoint: ApAccessPointVO | undefined) {
    let bindings: any = {
        itemsMap: {},
        partsMap: {},
    };

    bindings.addItem = (id, sync) => {
        const state = bindings.itemsMap[id] || true;
        bindings.itemsMap[id] = state && sync;
    };

    bindings.addPart = (id, sync) => {
        const state = bindings.partsMap[id] || true;
        bindings.partsMap[id] = state && sync;
    };

    if (accessPoint) {
        const externalIds = accessPoint.externalIds;
        if (externalIds) {
            externalIds.forEach(externalId => {
                externalId.bindingItemList.forEach(item => {
                    if (item.itemId) {
                        bindings.addItem(item.itemId, item.sync);
                    } else if (item.partId) {
                        bindings.addPart(item.partId, item.sync);
                    }
                });
            });
        }
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

/**
 * Detail globální archivní entity.
 */
const ApDetailPageWrapper: React.FC<Props> = (props: Props) => {
    const apTypeId = props.detail.fetched && props.detail.data ? props.detail.data.typeId : 0;
    const refTables = props.refTables;

    const [collapsed, setCollapsed] = useState<boolean>(false);

    useEffect(() => {
        props.fetchViewSettings();
        props.refreshValidation(props.id);
    }, [props.id]);

    const handleSetPreferred = (part: ApPartVO) => {
        if (part.id) {
            props.setPreferred(props.id, part.id);
            props.refreshValidation(props.id);
        }
    };

    const handleDelete = (part: ApPartVO) => {
        if (part.id) {
            props.deletePart(props.id, part.id);
        }
        props.refreshValidation(props.id);
    };

    const handleEdit = (part: ApPartVO) => {
        const partType = refTables.partTypes.itemsMap[part.typeId].code;
        const detail = props.detail.data!;

        part.id &&
            props.showPartEditModal(
                part,
                partType,
                props.id,
                apTypeId,
                detail.ruleSetId,
                detail.scopeId,
                props.refTables,
                props.descItemTypesMap,
                props.apViewSettings,
                part.partParentId,
            );
        props.refreshValidation(props.id);
    };

    const handleAdd = (partType: RulPartTypeVO, parentPartId?: number) => {
        const detail = props.detail.data!;

        props.showPartCreateModal(partType, props.id, apTypeId, detail.scopeId, parentPartId);
        props.refreshValidation(props.id);
    };

    const handleDeletePart = (parts: Array<ApPartVO>) => {
        props.deleteParts(props.id, parts);
        props.refreshValidation(props.id);
    };

    const groupBy = (data, key) =>
        data.reduce((rv, x) => {
            (rv[x[key]] = rv[x[key]] || []).push(x);
            return rv;
        }, {});

    const getRelatedPartSections = (parentParts: ApPartVO[], parentPartType): ApPartVO[] => {
        if (parentParts.length === 0) {
            return [];
        }

        const parentIds = parentParts.filter(value => value.id).map(value => value.id);
        const allParts = props.detail.data ? (props.detail.data.parts as ApPartVO[]) : [];
        return allParts
            .filter(value => value.partParentId)
            .filter(value => value.partParentId && parentIds.includes(value.partParentId));
    };

    // TODO: find better way to check if all reftables are fetched
    const isFetchingPartyTypes = !props.refTables.partTypes.fetched || props.refTables.partTypes.isFetching;

    const isFetchingApTypes = !props.refTables.apTypes.fetched || props.refTables.apTypes.isFetching ||
        !props.refTables.recordTypes.fetched || props.refTables.recordTypes.isFetching;

    const isFetchingItemTypes = !props.refTables.descItemTypes.fetched || props.refTables.descItemTypes.isFetching;

    const isFetching = !props.detail.fetched || props.detail.isFetching;

    const isFetchingViewSettings = props.apViewSettings.isFetching;

    if (isFetchingPartyTypes || isFetching || isFetchingViewSettings || isFetchingApTypes || isFetchingItemTypes) {
        return (
            <div className={'detail-page-wrapper'}>
                <Loading />
            </div>
        );
    }

    if (!props.detail.id || !props.detail.data) {
        return <div className={'detail-page-wrapper'} />;
    }

    const accessPoint = props.detail.data;

    const bindings = createBindings(accessPoint);

    const allParts = accessPoint ? (accessPoint.parts as ApPartVO[]) : [];
    const typedParts = groupBy(
        sortPrefer(
            allParts.filter(part => !part.partParentId),
            accessPoint?.preferredPart,
        ),
        'typeId',
    );

    const validationResult = props.apValidation.data;

    const sortedParts = accessPoint
        ? sortPart(props.refTables.partTypes.items, props.apViewSettings.data?.rules[accessPoint.ruleSetId])
        : [];

    return (
        <div className={'detail-page-wrapper'}>
            <div key="1" className="layout-scroll">
                <DetailHeader
                    item={props.detail.data!}
                    id={props.detail.data!.id}
                    collapsed={collapsed}
                    onToggleCollapsed={() => {
                        setCollapsed(!collapsed);
                    }}
                    validationErrors={validationResult && validationResult.errors}
                    onInvalidateDetail={() => props.refreshDetail(props.detail.data!.id)}
                />

                {allParts && (
                    <div key="part-sections">
                        {sortedParts.map((partType: RulPartTypeVO, index) => {
                            const onAddRelated = partType.childPartId
                                ? parentPartId => {
                                      const childPartType = objectById(
                                          props.refTables.partTypes.items,
                                          partType.childPartId,
                                      );
                                      if (childPartType !== null) {
                                          handleAdd(childPartType, parentPartId);
                                      } else {
                                          console.error('childPartType ' + partType.childPartId + ' not found');
                                      }
                                  }
                                : undefined;
                            const apViewSettingRule = props.apViewSettings.data!.rules[props.detail.data!.ruleSetId];
                            return (
                                <DetailMultiSection
                                    key={partType.code}
                                    label={partType.name}
                                    editMode={props.editMode}
                                    parts={typedParts[partType.id] ? typedParts[partType.id] : []}
                                    relatedParts={getRelatedPartSections(
                                        typedParts[partType.id] ? typedParts[partType.id] : [],
                                        partType.id,
                                    )}
                                    preferred={props.detail.data ? props.detail.data.preferredPart : undefined}
                                    globalCollapsed={props.globalCollapsed}
                                    onSetPreferred={handleSetPreferred}
                                    onEdit={handleEdit}
                                    onDelete={handleDelete}
                                    bindings={bindings}
                                    onAdd={() => handleAdd(partType)}
                                    onDeleteParts={handleDeletePart}
                                    onAddRelated={onAddRelated}
                                    partValidationErrors={validationResult && validationResult.partErrors}
                                    itemTypeSettings={apViewSettingRule?.itemTypes || []}
                                    globalEntity={props.globalEntity}
                                />
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
};

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
                    onSubmit={data => {
                        if (!part.id) {
                            return;
                        }
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
    refreshDetail: (apId: number) => {
        dispatch(registryDetailFetchIfNeeded(apId, true));
    },
    fetchViewSettings: () => {
        dispatch(
            DetailActions.fetchIfNeeded(AP_VIEW_SETTINGS, '', () => {
                return WebApi.getApTypeViewSettings();
            }),
        );
    },
});

const mapStateToProps = (state: any, props: OwnProps) => {
    return {
        detail: storeFromArea(state, registry.AREA_REGISTRY_DETAIL) as DetailStoreState<ApAccessPointVO>,
        apValidation: storeFromArea(state, AP_VALIDATION) as DetailStoreState<ApValidationErrorsVO>,
        apViewSettings: storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
        descItemTypesMap: state.refTables.descItemTypes.itemsMap,
        refTables: state.refTables,
        editMode: true,
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(ApDetailPageWrapper);
