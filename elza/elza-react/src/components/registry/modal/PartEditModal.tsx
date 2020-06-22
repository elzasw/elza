import React from 'react';
import {ConfigProps, Form, getFormValues, reduxForm, SubmitHandler} from 'redux-form';
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
    handleSubmit: SubmitHandler<FormData, any, any>;
    apTypeId: number;
    scopeId: number;
    formData?: ApPartFormVO;
    submitting: boolean;
    parentPartId?: number;
    apId: number;
    partId?: number;
    onClose: () => void;
} & ReturnType<typeof mapStateToProps>;

const PartEditModal = ({handleSubmit, onClose, refTables, partType, apTypeId, scopeId, formData, submitting, parentPartId, apId, partId}: Props) => {
    if (!refTables) {
        return <div/>;
    }

    return <Form onSubmit={handleSubmit}>
        <Modal.Body>
            <PartEditForm
                formInfo={{
                    formName: FORM_NAME,
                    sectionName: "partForm"
                }}
                partType={partType}
                apTypeId={apTypeId}
                scopeId={scopeId}
                formData={formData}
                submitting={submitting}
                parentPartId={parentPartId}
                apId={apId}
                partId={partId}
            />
        </Modal.Body>
        <Modal.Footer>
            <Button type="submit" variant="outline-secondary" onClick={handleSubmit} disabled={submitting}>
                {i18n('global.action.store')}
            </Button>

            <Button variant="link" onClick={onClose} disabled={submitting}>
                {i18n('global.action.cancel')}
            </Button>
        </Modal.Footer>
    </Form>;
};

const mapStateToProps = (state: any) => {
    return {
        refTables: state.refTables,
    }
};

export default connect(mapStateToProps)(reduxForm<any, any>(formConfig)(PartEditModal));
