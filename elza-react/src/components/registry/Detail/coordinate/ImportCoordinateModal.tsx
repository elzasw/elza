import React, {useState} from 'react';
import {Button, Col, Form, Modal, Row} from 'react-bootstrap';
import {connect} from 'react-redux';
import {ConfigProps, InjectedFormProps, reduxForm, Form as ReduxForm, SubmitHandler, Field} from 'redux-form';
import {CoordinateFileType} from '../../../../constants';
import i18n from '../../../i18n';
import FF from '../../../shared/form/FF';
import FileInput from '../../../shared/form/FileInput';
import FormInputField from '../../../shared/form/FormInputField';

const FORM_NAME = 'importCoordinatesForm';

export type FormData = {
    format: CoordinateFileType;
    file: File;
};

type Props = {
    message: string;
    onClose: () => void;
    handleSubmit: SubmitHandler<FormData, any, any>;
    onSubmit: (data: any) => void;
} & InjectedFormProps;

const ImportCoordinateModal = ({handleSubmit, onClose, submitting}: Props) => {
    return (
        <ReduxForm onSubmit={handleSubmit}>
            <Modal.Body>
                <Row>
                    <Col>
                        <FF field={FileInput} label={i18n('ap.coordinate.import.select')} name={'file'} />
                    </Col>
                </Row>
                <Row className="pt-2">
                    <Col>
                        {Object.keys(CoordinateFileType).map(x => (
                            <Field
                                component={FormInputField}
                                type="radio"
                                name="format"
                                value={x}
                                label={i18n('ap.coordinate.format', x.toUpperCase())}
                            />
                        ))}
                    </Col>
                </Row>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" variant="outline-secondary" disabled={submitting}>
                    {i18n('global.action.store')}
                </Button>

                <Button variant="link" onClick={onClose} disabled={submitting}>
                    {i18n('global.action.cancel')}
                </Button>
            </Modal.Footer>
        </ReduxForm>
    );
};

export default reduxForm<FormData, {}>({
    form: FORM_NAME,
    initialValues: {
        format: CoordinateFileType.KML,
        file: undefined,
    },
})(ImportCoordinateModal);
