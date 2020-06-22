import React from 'react';
import {ConfigProps, Form as ReduxForm, InjectedFormProps, reduxForm, SubmitHandler,} from 'redux-form';
import {Col, Modal, Row} from 'react-bootstrap';
import {connect} from "react-redux";
import {Action} from "redux";
import {ThunkDispatch} from "redux-thunk";
import {Button} from "../../ui";
import i18n from "../../i18n";
import ExtSystemFilterSection from '../form/filter/ExtSystemFilterSection';
import './ApPushToExt.scss';

const FORM_NAME = "apPushToExt";

type FormProps = {
    extSystem?: string
}

const validate = (values) => {
    const errors: any = {};
    if (!values.extSystem) {
        errors.extSystem = i18n('global.validation.required');
    }
    return errors;
};

const formConfig: ConfigProps<FormProps> = {
    form: FORM_NAME,
    validate
};

type Props = {
    handleSubmit: SubmitHandler<FormData, any, any>;
    formData?: FormProps;
    submitting: boolean;
    onClose: () => void;
    extSystems: any[];
} & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const ApPushToExt = ({handleSubmit, onClose, submitting, extSystems}: Props) => {
    return <ReduxForm className="ap-push-to-ext-modal" onSubmit={handleSubmit}>
        <Modal.Body>
            <Row noGutters>
                <Col>
                    <ExtSystemFilterSection hideName submitting={submitting} extSystems={extSystems}/>
                </Col>
            </Row>
        </Modal.Body>
        <Modal.Footer>
            <Button type="submit" variant="outline-secondary">{i18n('global.action.write')}</Button>
            <Button variant="link" onClick={onClose} disabled={submitting}>
                {i18n('global.action.close')}
            </Button>
        </Modal.Footer>
    </ReduxForm>;
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({});

const mapStateToProps = (state: any) => {
    return {};
};

export default connect(mapStateToProps, mapDispatchToProps)(reduxForm<any, any>(formConfig)(ApPushToExt));
