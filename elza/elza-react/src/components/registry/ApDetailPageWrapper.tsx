import React, {ReactElement, useEffect, useState} from 'react';
import {ThunkDispatch} from 'redux-thunk';
import {Action} from 'redux';
import {connect} from 'react-redux';
import {PartType} from '../../api/generated/model';
import * as PartTypeInfo from "../../api/old/PartTypeInfo";
import DetailMultiSection from "./Detail/DetailMultiSection";
import Loading from '../shared/loading/Loading';
import {globalFundTreeInvalidate} from "../../actions/arr/globalFundTree";
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import {WebApi} from "../../actions/WebApi";
import {DetailActions} from "../../shared/detail";
import {AP_VALIDATION} from "../../constants";
import storeFromArea from "../../shared/utils/storeFromArea";
import * as registry from '../../actions/registry/registry';
import {ApAccessPointVO} from "../../api/ApAccessPointVO";
import {ApValidationErrorsVO} from "../../api/ApValidationErrorsVO";
import {ApPartVO} from "../../api/ApPartVO";
import {DetailStoreState} from "../../types";
import DetailHeader from "./Detail/DetailHeader";
import {ApPartFormVO} from "../../api/ApPartFormVO";
import PartEditModal from "./modal/PartEditModal";
import {sortItems} from "../../utils/ItemInfo";
import {RulPartTypeVO} from "../../api/RulPartTypeVO";
import {registryDetailFetchIfNeeded} from "../../actions/registry/registry";

type OwnProps = {
    id: number; // ap id
    sider: ReactElement;
    editMode: boolean;
    globalCollapsed: boolean;
    apValidation: DetailStoreState<ApValidationErrorsVO>;
    globalEntity: boolean;
}

type Props = OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps>;

function createBindings(accessPoint: ApAccessPointVO | undefined) {
    let bindings: any = {
        itemsMap: {},
        partsMap: {},
    }

    bindings.addItem = (id, sync) => {
        const state = bindings.itemsMap[id] || true;
        bindings.itemsMap[id] = state && sync;
    };

    bindings.addPart = (id, sync) => {
        const state = bindings.partsMap[id] || true;
        bindings.partsMap[id] = state && sync;
    }

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

/**
 * Detail globální archivní entity.
 */
const ApDetailPageWrapper: React.FC<Props> = (props: Props) => {
    const apTypeId = props.detail.fetched ? props.detail.data!.typeId : 0;
    const refTables = props.refTables;

    const [collapsed, setCollapsed] = useState<boolean>(false);

    useEffect(() => {
        props.refreshValidation(props.id);
    }, [props.id])

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

        part.id && props.showPartEditModal(part, partType, props.id, apTypeId, detail.scopeId, props.refTables, part.partParentId);
        props.refreshValidation(props.id);
    };

    const handleAdd = (partType: PartType) => {
        const detail = props.detail.data!;

        props.showPartCreateModal(partType, props.id, apTypeId, detail.scopeId);
        props.refreshValidation(props.id);
    };

    const handleAddRelated = (part: ApPartVO) => {
        const detail = props.detail.data!;

        part.id && props.showPartCreateModal(PartType.REL, props.id, apTypeId, detail.scopeId, part.id);
        props.refreshValidation(props.id);
    };

    const handleDeletePart = (parts: Array<ApPartVO>) => {
        props.deleteParts(props.id, parts);
        props.refreshValidation(props.id);
    };

    const groupBy = (data, key) => data.reduce((rv, x) => {
        (rv[x[key]] = rv[x[key]] || []).push(x);
        return rv;
    }, {});

    const getRelatedPartSections = (parentParts: ApPartVO[], parentPartType): ApPartVO[] => {
        if (parentParts.length === 0) {
            return [];
        }

        const parentIds = parentParts.filter(value => value.id).map(value => value.id);
        const allParts = props.detail.data ? props.detail.data.parts as ApPartVO[] : [];
        return allParts.filter(value => value.partParentId).filter(value => value.partParentId && parentIds.includes(value.partParentId));
    };

    const isFetchingPartyTypes = !props.refTables.partTypes.fetched && props.refTables.partTypes.isFetching;

    const isFetching = !props.detail.fetched && props.detail.isFetching;

    if (isFetchingPartyTypes || isFetching) {
        return <div className={'detail-page-wrapper'}>
            <Loading/>
        </div>;
    }

    if (!isFetching && (!props.detail.id || !props.detail.data)) {
        return <div className={'detail-page-wrapper'}/>;
    }

    const accessPoint = props.detail.data;

    const bindings = createBindings(accessPoint);

    const allParts = accessPoint ? accessPoint.parts as ApPartVO[] : [];
    const typedParts = groupBy(allParts, 'typeId');

    const validationResult = props.apValidation.data;

    return <div className={'detail-page-wrapper'}>
        <div key="1" className="layout-scroll">
            <DetailHeader
                item={props.detail.data!}
                id={props.detail.data!.id}
                collapsed={collapsed}
                onToggleCollapsed={() => {
                    setCollapsed(!collapsed)
                }}
                validationErrors={validationResult && validationResult.errors}
                onInvalidateDetail={() => props.refreshDetail(props.detail.data!.id)}
            />

            {allParts && <div key="part-sections">
                {props.refTables.partTypes.items.map((partType: RulPartTypeVO, index) =>
                    <DetailMultiSection
                        key={partType.code}
                        label={partType.name}
                        editMode={props.editMode}
                        parts={typedParts[partType.id] ? typedParts[partType.id] : []}
                        relatedParts={getRelatedPartSections(typedParts[partType.id] ? typedParts[partType.id] : [], partType.id)}
                        preferred={props.detail.data ? props.detail.data.preferredPart : undefined}
                        globalCollapsed={props.globalCollapsed}
                        onSetPreferred={handleSetPreferred}
                        onEdit={handleEdit}
                        onDelete={handleDelete}
                        bindings={bindings}
                        onAdd={() => handleAdd(partType.code as any as PartType)}
                        onDeleteParts={handleDeletePart}
                        partValidationErrors={validationResult && validationResult.partErrors}
                        globalEntity={props.globalEntity}
                    />
                )}
            </div>}
        </div>
    </div>
};

const mapDispatchToProps = (
    dispatch: ThunkDispatch<{}, {}, Action<string>>
) => ({
    showPartEditModal: (part: ApPartVO, partType, apId: number, apTypeId: number, scopeId: number, refTables, parentPartId?: number) => {
        dispatch(
            modalDialogShow(
                this,
                PartTypeInfo.getPartEditDialogLabel(partType, false),
                <PartEditModal
                    partType={partType}
                    onSubmit={(data) => {
                        if (!part.id) {
                            return;
                        }
                        const formData: ApPartFormVO = data.partForm;
                        const submitItems = formData.items.map(x => {
                            // @ts-ignore
                            delete x['type'];
                            return x;
                        });

                        const submitData = {
                            items: submitItems,
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
                            items: sortItems(partType, part.items, refTables),
                        }
                    }}
                    formData={{
                        partId: part.id,
                        parentPartId: part.partParentId,
                        partTypeCode: refTables.partTypes.itemsMap[part.typeId].code,
                        items: sortItems(partType, part.items, refTables),
                    } as ApPartFormVO}
                    parentPartId={part.partParentId}
                    apId={apId}
                    partId={part.id}
                    onClose={() => {
                        dispatch(modalDialogHide());
                    }}
                />,
                'dialog-lg'
            )
        );
    },
    showPartCreateModal: (partType: PartType, apId: number, apTypeId: number, scopeId: number, parentPartId?: number) => {
        dispatch(
            modalDialogShow(
                this,
                PartTypeInfo.getPartEditDialogLabel(partType, true),
                <PartEditModal
                    partType={partType}
                    onSubmit={data => {
                        const formData: ApPartFormVO = data.partForm;
                        const submitItems = formData.items.map(x => {
                            // @ts-ignore
                            delete x['type'];
                            return x;
                        });

                        const submitData = {
                            items: submitItems,
                            parentPartId: parentPartId,
                            partTypeCode: partType,
                        } as ApPartFormVO;

                        console.log('SUBMIT ADD', apId, submitData);

                        return WebApi.createPart(apId, submitData).then(() => {
                            dispatch(modalDialogHide());
                            dispatch(registryDetailFetchIfNeeded(apId, true));
                        });
                    }}
                    apTypeId={apTypeId}
                    scopeId={scopeId}
                    formData={{
                        partTypeCode: partType,
                        items: [],
                    } as ApPartFormVO}
                    parentPartId={parentPartId}
                    initialValues={{}}
                    apId={apId}
                    onClose={() => {
                        dispatch(modalDialogHide());
                    }}
                />,
                'dialog-lg',
                dispatch(globalFundTreeInvalidate()),
            )
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
        dispatch(DetailActions.fetchIfNeeded(AP_VALIDATION, apId, (id) => {
            return WebApi.validateAccessPoint(id)
        }, true));
    },
    refreshDetail: (apId: number) => {
        dispatch(registryDetailFetchIfNeeded(apId, true));
    }
});

const mapStateToProps = (state: any, props: OwnProps) => {
    return {
        detail: storeFromArea(state, registry.AREA_REGISTRY_DETAIL) as DetailStoreState<ApAccessPointVO>,
        apValidation: storeFromArea(state, AP_VALIDATION) as DetailStoreState<ApValidationErrorsVO>,
        refTables: state.refTables,
        editMode: true,
    }
};

export default connect(mapStateToProps, mapDispatchToProps)(ApDetailPageWrapper);
