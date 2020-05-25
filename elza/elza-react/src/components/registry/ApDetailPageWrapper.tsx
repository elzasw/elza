import React, {ReactElement, useState} from 'react';
import {ThunkDispatch} from 'redux-thunk';
import {Action} from 'redux';
import {connect} from 'react-redux';
//import ContentSpinner from "../components/Loader";
//import PageInfoContent from "../components/PageInfoContent";
//import * as DetailActions from "../shared/reducers/detail/DetailActions";
import {PartType} from '../../api/generated/model';
import * as PartTypeInfo from "../../api/old/PartTypeInfo";
import DetailMultiSection from "./Detail/DetailMultiSection";
//import * as ModalActions from "../shared/reducers/modal/ModalActions";
//import PartEditModal from "../components/modal/PartEditModal";
import Loading from '../shared/loading/Loading';
import {globalFundTreeInvalidate} from "../../actions/arr/globalFundTree";
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import {WebApi} from "../../actions/WebApi";
import {DetailActions} from "../../shared/detail";
import {DETAIL_VALIDATION_RESULT} from "../../constants";
import storeFromArea from "../../shared/utils/storeFromArea";
//import * as EntitiesClientApiCall from "./../api/call/EntitiesClientApiCall";
//import {filterPartFormForSubmit} from "../partutils";
import * as registry from '../../actions/registry/registry';
import {ApAccessPointVO} from "../../api/ApAccessPointVO";
import {ApValidationErrorsVO} from "../../api/ApValidationErrorsVO";
import {ApPartVO} from "../../api/ApPartVO";
import {DetailStoreState} from "../../types";
import DetailHeader from "./Detail/DetailHeader";
import PartEditForm from "./form/PartEditForm";
import {ApPartFormVO} from "../../api/ApPartFormVO";
import {ApAccessPointCreateVO} from "../../api/ApAccessPointCreateVO";

type OwnProps = {
    id: number; // ae id
    area: string;
    sider: ReactElement;
//    header: ReactElement;
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

        part.id && props.showPartEditModal(props.area, part, partType, props.id, apTypeId, props.refTables, part.partParentId);
        props.invalidateValidationErrors(props.id);
    };

    const handleAdd = (partType) => {
        props.showPartCreateModal(props.area, partType, props.id, apTypeId);
        props.invalidateValidationErrors(props.id);
    };

    const handleAddRelated = (part: ApPartVO) => {
        part.id && props.showPartCreateModal(props.area, PartType.REL, props.id, apTypeId, part.id);
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

    const isFetching = !props.detail.fetched || props.detail.isFetching ||
        !props.refTables.partTypes.fetched || props.refTables.partTypes.isFetching;

    if (isFetching) {
        return <div className={'detail-page-wrapper'}>
            <Loading/>
        </div>;
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
            />

            {allParts && <div key="part-sections">
                {props.refTables.partTypes.items.map((partType, index) =>
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
                        onAdd={() => handleAdd(partType.id)}
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
    showPartEditModal: (area: string, part: ApPartVO, partType, aeId: number, apTypeId: number, refTables, parentPartId?: number) => {
        dispatch(
            modalDialogShow(
                this,
                PartTypeInfo.getPartEditDialogLabel(partType, false),
                <PartEditForm
                    partTypeId={part.typeId}
                    apTypeId={apTypeId}
                    parentPartId={parentPartId}
                    aeId={aeId}
                    partId={part.id}
                    initialValues={{
                        partId: part.id,
                        parentPartId: part.partParentId,
                        partTypeCode: refTables.partTypes.itemsMap[part.typeId].code,
                        items: part.items, //todo: sort by type
                    } as ApPartFormVO}
                    onSubmit={(formData: ApPartFormVO) => {
                        if (part.id) {
                            formData.parentPartId = parentPartId;
                            return WebApi.updatePart(aeId, part.id, formData).then(() => {
                                dispatch(modalDialogHide());
                                dispatch(DetailActions.invalidate(area, aeId))
                            });
                        }
                    }}
                />,
                'dialog-lg',
                dispatch(globalFundTreeInvalidate()),
            )
        );

        /*dispatch(
          ModalActions.showForm(PartEditModal, {
            createDialog: false,
            partType,
            aeTypeId,
            parentPartId,
            aeId,
            partId: part.id,
            initialValues: {
              part: partType,
              items: sortItems(partType, part.items, codelist)
            },
            onSubmit: (formData: AePartFormVO) => {
              if (part.id) {
                formData.parentPartId = parentPartId;
                return EntitiesClientApiCall.formApi
                  .updatePart(aeId, part.id, filterPartFormForSubmit(formData));
              }
            },
            onSubmitSuccess: () => {
              dispatch(ModalActions.hide());
              dispatch(DetailActions.invalidate(area, aeId))
            }
          }, {
            title: PartTypeInfo.getPartEditDialogLabel(partType, false),
            width: "1200px"
          })
        )*/
    },
    showPartCreateModal: (area: string, partType: PartType, aeId: number, aeTypeId: number, parentPartId?: number) => {
        /*dispatch(
          ModalActions.showForm(PartEditModal, {
            createDialog: true,
            partType,
            aeTypeId,
            parentPartId,
            aeId,
            initialValues: {
              part: partType,
              items: []
            },
            onSubmit: (formData: AePartFormVO) => {
              formData.parentPartId = parentPartId;
              return EntitiesClientApiCall.formApi
                .createPart(aeId, filterPartFormForSubmit(formData));
            },
            onSubmitSuccess: () => {
              dispatch(ModalActions.hide());
              dispatch(DetailActions.invalidate(area, aeId))
            }
          }, {
            title: PartTypeInfo.getPartEditDialogLabel(partType, true),
            width: "1200px"
          })
        )*/
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

        dispatch(DetailActions.invalidate(area, apId))
    },
    invalidateValidationErrors: (aeId: number) => {
        dispatch(DetailActions.invalidate(DETAIL_VALIDATION_RESULT, aeId));
    },
});

const mapStateToProps = (state: any, props: OwnProps) => {
    return {
        detail: storeFromArea(state, registry.AREA_REGISTRY_DETAIL) as DetailStoreState<ApAccessPointVO>,
        refTables: state.refTables,
        editMode: true,
    }
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(ApDetailPageWrapper);
