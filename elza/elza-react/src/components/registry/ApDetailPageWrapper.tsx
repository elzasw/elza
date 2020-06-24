import React, {ReactElement, useState} from 'react';
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
import {DETAIL_VALIDATION_RESULT} from "../../constants";
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
    id: number; // ae id
    area: string;
    sider: ReactElement;
    editMode: boolean;
    globalCollapsed: boolean;
    validationResult?: ApValidationErrorsVO;
    globalEntity: boolean;
}

type Props = OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps>;

/**
 * Detail globální archivní entity.
 */
const ApDetailPageWrapper: React.FC<Props> = (props: Props) => {
    const apTypeId = props.detail.fetched ? props.detail.data!.typeId : 0;
    const refTables = props.refTables;

    const [collapsed, setCollapsed] = useState<boolean>(false);

    const handleSetPreferred = (part: ApPartVO) => {
        if (part.id) {
            props.setPreferred(props.area, props.id, part.id);
            props.invalidateValidationErrors(props.id);
        }
    };

    const handleDelete = (part: ApPartVO) => {
        if (part.id) {
            props.deletePart(props.area, props.id, part.id);
        }
        props.invalidateValidationErrors(props.id);
    };

    const handleEdit = (part: ApPartVO) => {
        const partType = refTables.partTypes.itemsMap[part.typeId].code;
        const detail = props.detail.data!;

        part.id && props.showPartEditModal(props.area, part, partType, props.id, apTypeId, detail.scopeId, props.refTables, part.partParentId);
        props.invalidateValidationErrors(props.id);
    };

    const handleAdd = (partType: PartType) => {
        const detail = props.detail.data!;

        props.showPartCreateModal(props.area, partType, props.id, apTypeId, detail.scopeId);
        props.invalidateValidationErrors(props.id);
    };

    const handleAddRelated = (part: ApPartVO) => {
        const detail = props.detail.data!;

        part.id && props.showPartCreateModal(props.area, PartType.REL, props.id, apTypeId, detail.scopeId, part.id);
        props.invalidateValidationErrors(props.id);
    };

    const handleDeletePart = (parts: Array<ApPartVO>) => {
        props.deleteParts(props.area, props.id, parts);
        props.invalidateValidationErrors(props.id);
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

    const allParts = props.detail.data ? props.detail.data.parts as ApPartVO[] : [];
    const typedParts = groupBy(allParts, 'typeId');

    return <div className={'detail-page-wrapper'}>
        <div key="1" className="layout-scroll">
            <DetailHeader
                item={props.detail.data!}
                id={props.detail.data!.id}
                collapsed={collapsed}
                onToggleCollapsed={() => {
                    setCollapsed(!collapsed)
                }}
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
                        onAdd={() => handleAdd(partType.code as any as PartType)}
                        onDeleteParts={handleDeletePart}
                        validationResult={props.validationResult}
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
    showPartEditModal: (area: string, part: ApPartVO, partType, apId: number, apTypeId: number, scopeId: number, refTables, parentPartId?: number) => {
        dispatch(
            modalDialogShow(
                this,
                PartTypeInfo.getPartEditDialogLabel(partType, false),
                <PartEditModal
                    partType={partType}
                    handleSubmit={(formData: ApPartFormVO) => {
                        if (!part.id) {
                            return;
                        }

                        formData.parentPartId = parentPartId;

                        return WebApi.updatePart(apId, part.id, formData).then(() => {
                            dispatch(modalDialogHide());
                            dispatch(DetailActions.invalidate(area, apId));
                        });
                    }}
                    apTypeId={apTypeId}
                    scopeId={scopeId}
                    initialValues={{
                        items: sortItems(partType, part.items, refTables),
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
                'dialog-lg',
                () => {
                    dispatch(globalFundTreeInvalidate());
                },
            )
        );
    },
    showPartCreateModal: (area: string, partType: PartType, apId: number, apTypeId: number, scopeId: number, parentPartId?: number) => {
        //todo: Zde potrebuju partType, ale nove jedeme pres ID a ne pres kod, ktery chodi v partType: PartType

        dispatch(
            modalDialogShow(
                this,
                PartTypeInfo.getPartEditDialogLabel(partType, true),
                <PartEditModal
                    partType={partType}
                    handleSubmit={(formData: ApPartFormVO) => {
                        formData.parentPartId = parentPartId;

                        return WebApi.createPart(apId, formData).then(() => {
                            dispatch(modalDialogHide());
                            dispatch(DetailActions.invalidate(area, apId));
                        });
                    }}
                    apTypeId={apTypeId}
                    scopeId={scopeId}
                    formData={{
                        partTypeCode: partType,  //todo: zde pouzivame ciselnik kodu partu, ne ten z refTables, snad to bude OK
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
    setPreferred: async (area: string, apId: number, partId: number) => {
        await WebApi.setPreferPartName(apId, partId);
        dispatch(DetailActions.invalidate(area, apId))
    },
    deletePart: async (area: string, apId: number, partId: number) => {
        await WebApi.deletePart(apId, partId);
        dispatch(DetailActions.invalidate(area, apId))
    },
    deleteParts: async (area: string, apId: number, parts: ApPartVO[]) => {
        for (let part of parts) {
            if (part.id) {
                await WebApi.deletePart(apId, part.id);
            }
        }

        dispatch(DetailActions.invalidate(area, apId));
    },
    invalidateValidationErrors: (apId: number) => {
        dispatch(DetailActions.invalidate(DETAIL_VALIDATION_RESULT, apId));
    },
    refreshDetail: (apId: number) => {
        dispatch(registryDetailFetchIfNeeded(apId, true));
    }
});

const mapStateToProps = (state: any, props: OwnProps) => {
    return {
        detail: storeFromArea(state, registry.AREA_REGISTRY_DETAIL) as DetailStoreState<ApAccessPointVO>,
        refTables: state.refTables,
        editMode: true,
    }
};

export default connect(mapStateToProps, mapDispatchToProps)(ApDetailPageWrapper);
