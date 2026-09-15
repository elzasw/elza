import React, {useState} from 'react';
import {Button, Col, Form, Modal, Row} from 'react-bootstrap';
import {connect} from 'react-redux';
import {ConfigProps, InjectedFormProps, reduxForm, Form as ReduxForm, SubmitHandler, Field} from 'redux-form';
import {CoordinateFileType} from '../../../../constants';
import { FormattedMessage, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { apDetailMessages } from '../messages';
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
    const intl = useIntl();
    return (
        <ReduxForm onSubmit={handleSubmit}>
            <Modal.Body>
                <Row>
                    <Col>
                        <FF field={FileInput} label={<FormattedMessage {...apDetailMessages.coordinateImportSelect} />} name={'file'} />
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
                                label={intl.formatMessage(apDetailMessages.coordinateFormat, { 0: x.toUpperCase() })}
                            />
                        ))}
                    </Col>
                </Row>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" variant="outline-secondary" disabled={submitting}>
                    {<FormattedMessage {...globalMessages.save} />}
                </Button>

                <Button variant="link" onClick={onClose} disabled={submitting}>
                    {<FormattedMessage {...globalMessages.cancel} />}
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
