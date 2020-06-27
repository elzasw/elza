import React, {useEffect} from 'react';
import {
    ConfigProps,
    Form,
    FormSection,
    formValueSelector,
    InjectedFormProps,
    reduxForm,
    SubmitHandler
} from 'redux-form';
import {connect} from "react-redux";
import PartEditForm from "./../form/PartEditForm";
import {ApPartFormVO} from "../../../api/ApPartFormVO";
import {Modal} from 'react-bootstrap';
import {Button} from "../../ui";
import i18n from "../../i18n";

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
} & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const PartEditModal = ({handleSubmit, onClose, refTables, partTypeId, apTypeId, scopeId, formData, partForm, submitting, change, parentPartId, apId, partId}: Props) => {
    if (!refTables) {
        return <div/>;
    }

    // eslint-disable-next-line
    useEffect(() => {
        change('partForm', partForm ? partForm : formData);
    }, [apTypeId]);

    return <Form onSubmit={handleSubmit}>
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

            <Button variant="link" onClick={onClose} disabled={submitting}>
                {i18n('global.action.cancel')}
            </Button>
        </Modal.Footer>
    </Form>;
};

const selector = formValueSelector(FORM_NAME);
const mapStateToProps = (state: any) => {
    return {
        refTables: state.refTables,
        partForm: selector(state, 'partForm'),
    }
};

export default connect(mapStateToProps)(reduxForm<any, any>(formConfig)(PartEditModal));
