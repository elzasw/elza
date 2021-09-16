import React, { useEffect } from 'react';
import {
    ConfigProps,
    Form,
    FormSection,
    formValueSelector,
    InjectedFormProps,
    reduxForm,
    SubmitHandler
} from 'redux-form';
import { connect } from "react-redux";
import PartEditForm from "../../form/part-edit-form/PartEditForm";
import { ApPartFormVO } from "../../../../api/ApPartFormVO";
import { Modal } from 'react-bootstrap';
import { Button } from "../../../ui";
import i18n from "../../../i18n";
import { showConfirmDialog } from "components/shared/dialog";
import { ThunkDispatch } from 'redux-thunk';
import { Action } from 'redux';
import { AppState } from '../../../../typings/store';
import { RulPartTypeVO } from '../../../../api/RulPartTypeVO';
import { WebApi } from '../../../../actions/WebApi';
import { modalDialogShow } from '../../../../actions/global/modalDialog';
import * as PartTypeInfo from '../../../../api/old/PartTypeInfo';
import { PartType } from '../../../../api/generated/model';
import { ApItemBitVO } from '../../../../api/ApItemBitVO';
import { registryDetailFetchIfNeeded } from '../../../../actions/registry/registry';
import { globalFundTreeInvalidate } from '../../../../actions/arr/globalFundTree';
import { ApPartVO } from '../../../../api/ApPartVO';
import { RulDescItemTypeExtVO } from '../../../../api/RulDescItemTypeExtVO';
import { DetailStoreState } from '../../../../types';
import { ApViewSettings } from '../../../../api/ApViewSettings';
import { objectById } from '../../../../shared/utils';
import { sortItems } from '../../../../utils/ItemInfo';
import ModalDialogWrapper from '../../../shared/dialog/ModalDialogWrapper';

const FORM_NAME = "partEditForm";

const formConfig: ConfigProps<ApPartFormVO> = {
    form: FORM_NAME,
};

type Props = {
    partTypeId: number;
    initialValues?: ApPartFormVO;
    handleSubmit: SubmitHandler<FormData, any, any>;
    apTypeId: number;
    scopeId: number;
    formData?: ApPartFormVO;
    partForm?: ApPartFormVO;
    submitting: boolean;
    parentPartId?: number;
    apId: number;
    partId?: number;
    onClose: () => void;
} & ReturnType<typeof mapStateToProps> & ReturnType<typeof mapDispatchToProps> & InjectedFormProps;

const PartEditModalBase = ({
    handleSubmit,
    onClose,
    refTables,
    partTypeId,
    apTypeId,
    scopeId,
    formData,
    partForm,
    submitting,
    change,
    parentPartId,
    apId,
    partId,
    anyTouched,
    showConfirmDialog,
}: Props) => {
    if (!refTables) {
        return <div />;
    }

    const handleClose = async () => {
        if (!anyTouched) {
            onClose();
            return;
        }

        const result = await showConfirmDialog("Provedene zmeny nebudou ulozeny. Opravdu si prejete pokracovat?");
        if (result) { onClose(); }
    }

    // eslint-disable-next-line
    useEffect(() => {
        change('partForm', partForm ? partForm : formData);
    }, [apTypeId]);

    return <ModalDialogWrapper
        className='dialog-visible dialog-lg'
        title={"title"}
        onHide={handleClose}
    >
        <Form onSubmit={handleSubmit}>
            <Modal.Body>
                <FormSection name="partForm">
                    <PartEditForm
                        formInfo={{
                            formName: FORM_NAME,
                            sectionName: "partForm"
                        }}
                        partTypeId={partTypeId}
                        apTypeId={apTypeId}
                        scopeId={scopeId}
                        formData={partForm}
                        submitting={submitting}
                        parentPartId={parentPartId}
                        apId={apId}
                        partId={partId}
                        />
                </FormSection>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" variant="outline-secondary" onClick={handleSubmit} disabled={submitting}>
                    {i18n('global.action.store')}
                </Button>

                <Button variant="link" onClick={handleClose} disabled={submitting}>
                    {i18n('global.action.cancel')}
                </Button>
            </Modal.Footer>
        </Form>
    </ModalDialogWrapper>
};

export const showPartCreateModal = (
    partType: RulPartTypeVO,
    apId: number,
    apTypeId: number,
    scopeId: number,
    parentPartId?: number,
    onUpdateFinish: () => void = () => { },
) => (dispatch: any) => dispatch(
    modalDialogShow(
        this,
        // TODO: není rozmyšleno, kde brát skloňované popisky!
        PartTypeInfo.getPartEditDialogLabel(partType.code as PartType, true),
        ({ onClose }) => {
            const handleClose = () => onClose();

            const handleSubmit = async (data: any) => {
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

                // console.log('SUBMIT ADD', apId, submitData);

                await WebApi.createPart(apId, submitData)
                onClose();
                await dispatch(registryDetailFetchIfNeeded(apId, true))
                onUpdateFinish();
            }

            const formData = {
                partTypeCode: partType.code,
                items: [],
            } as ApPartFormVO

            return <PartEditModal
                apId={apId}
                apTypeId={apTypeId}
                scopeId={scopeId}
                partTypeId={partType.id}
                parentPartId={parentPartId}
                formData={formData}
                initialValues={{}}
                onSubmit={handleSubmit}
                onClose={handleClose}
                />},
        'dialog-lg',
        dispatch(globalFundTreeInvalidate()),
    ),
);

interface ApPartData {
    partForm: ApPartFormVO;
}

export const showPartEditModal = (
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
    onUpdateFinish: () => void = () => {},
) => (dispatch:any) => dispatch(
    modalDialogShow(
        this,
        PartTypeInfo.getPartEditDialogLabel(partType, false),
        ({ onClose }) => {
            const partTypeId = objectById(refTables.partTypes.items, partType, 'code').id;

            const handleSubmit = async ({ partForm }: ApPartData) => {
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

                const result = await WebApi.updatePart(apId, part.id, submitData)
                onClose();
                await dispatch(registryDetailFetchIfNeeded(apId, true))
                onUpdateFinish();
                return result
            }

            const initialValues = {
                partForm: {
                    items: part.items ? sortItems(
                        partType,
                        part.items,
                        refTables,
                        descItemTypesMap,
                        apViewSettings.data!.rules[ruleSetId],
                    ) : [],
                },
            }

            const formData = {
                partId: part.id,
                parentPartId: part.partParentId,
                partTypeCode: refTables.partTypes.itemsMap[part.typeId].code,
                items: part.items ? sortItems(
                    partType,
                    part.items,
                    refTables,
                    descItemTypesMap,
                    apViewSettings.data!.rules[ruleSetId],
                ) : [],
            } as ApPartFormVO

            return <PartEditModal
                partTypeId={partTypeId}
                onSubmit={handleSubmit}
                apTypeId={apTypeId}
                scopeId={scopeId}
                initialValues={initialValues}
                formData={formData}
                parentPartId={part.partParentId}
                apId={apId}
                partId={part.id}
                onClose={() => onClose()}
                />
        },
        'dialog-lg',
    ),
);

const mapDispatchToProps = (dispatch: ThunkDispatch<AppState, void, Action<string>>) => ({
    showConfirmDialog: (message: string) => dispatch(showConfirmDialog(message)),
})
const selector = formValueSelector(FORM_NAME);
const mapStateToProps = (state: any) => {
    return {
        refTables: state.refTables,
        partForm: selector(state, 'partForm'),
    }
};

const PartEditModal = connect(mapStateToProps, mapDispatchToProps)(reduxForm<any, any>(formConfig)(PartEditModalBase));

export default PartEditModal
