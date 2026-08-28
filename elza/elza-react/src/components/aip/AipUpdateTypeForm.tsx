import React from 'react';
import { FormInputField, i18n } from 'components/shared';
import { Form, Modal } from 'react-bootstrap';
import { Button } from '../ui';
import { Form as FinalForm, Field } from 'react-final-form';
import {AipUpdateType} from "elza-api";
import { useIntl } from 'react-intl';
import { updateTypeMessages } from './messages';

interface FormFields {
    type: AipUpdateType;
}

interface Props {
    initialValues?: FormFields;
    onSubmit: (values: FormFields) => void;
    onClose?: () => void;
}

export function AipUpdateTypeForm({
    initialValues,
    onSubmit,
    onClose
}: Props) {
    const intl = useIntl();

    function validate(values: FormFields) {
        const errors: Partial<Record<keyof FormFields, string>> = {};

        if (!values.type) {
            errors.type = i18n('global.validation.required');
        }

        return errors;
    }

    async function handleSubmit(values: FormFields) {
        await onSubmit(values);
        onClose();
    }

    return (
        <FinalForm<FormFields>
            initialValues={initialValues}
            validate={validate}
            onSubmit={handleSubmit}
        >{({ submitting, handleSubmit }) => {
            return <Form>
                <Modal.Body>
                    <Field
                        name="type"
                        type="select"
                        component={FormInputField}
                        label={i18n('aip.form.update.type')}
                    >
                        <option key={null} />
                        {Object.values(AipUpdateType).map((i, index) => (
                            <option key={index} value={i}>
                                {intl.formatMessage(updateTypeMessages[i])}
                            </option>
                        ))}
                    </Field>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit} variant="outline-secondary" disabled={submitting}>
                        {i18n('global.action.choose')}
                    </Button>
                    <Button onClick={onClose} variant="link">
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        }}
        </FinalForm>
    );
}

export default AipUpdateTypeForm;
