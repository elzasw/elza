import React from 'react';
import {ConfigProps, Form, FormSection, getFormValues, reduxForm, SubmitHandler} from 'redux-form';
import {connect} from "react-redux";
import PartEditForm from "./../form/PartEditForm";
import {ApPartFormVO} from "../../../api/ApPartFormVO";
import {PartType} from "../../../api/generated/model";
import {Modal} from 'react-bootstrap';
import {Button} from "../../ui";
import i18n from "../../i18n";

const FORM_NAME = "partEditForm";

const formConfig: ConfigProps<ApPartFormVO> = {
    form: FORM_NAME,
};

type Props = {
    partType: PartType;
    initialValues?: ApPartFormVO;
    createDialog: boolean;
    handleSubmit: SubmitHandler<FormData, any, any>;
    aeTypeId: number;
    formData?: ApPartFormVO;
    submitting: boolean;
    parentPartId?: number;
    aeId: number;
    partId: number;
    onClose: () => void;
} & ReturnType<typeof mapStateToProps>;

const PartEditModal = ({handleSubmit, onClose, refTables, createDialog, partType, aeTypeId, formData, submitting, parentPartId, aeId, partId}: Props) => {
    if (!refTables) {
        return <div/>;
    }

    return <Form onSubmit={handleSubmit}>
        <Modal.Body>
            <FormSection name="">
                <PartEditForm
                    formInfo={{
                        formName: FORM_NAME,
                        sectionName: ""
                    }}
                    partType={partType}
                    apTypeId={aeTypeId}
                    formData={formData}
                    submitting={submitting}
                    parentPartId={parentPartId}
                    apId={aeId}
                    partId={partId}
                />
            </FormSection>
        </Modal.Body>
        <Modal.Footer>
            <Button type="submit" variant="outline-secondary" onClick={handleSubmit} disabled={submitting}>
                {i18n('global.action.save')}
            </Button>

            <Button variant="link" onClick={onClose} disabled={submitting}>
                {i18n('global.action.cancel')}
            </Button>
        </Modal.Footer>
    </Form>;
};

const mapStateToProps = (state: any) => {
    return {
        formData: getFormValues(FORM_NAME)(state),
        refTables: state.refTables,
    }
};

export default connect(mapStateToProps)(reduxForm<any, any>(formConfig)(PartEditModal));
