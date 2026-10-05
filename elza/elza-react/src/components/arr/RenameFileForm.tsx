import React from 'react';
import { FormInputField } from 'components/shared';
import { useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang';
import { fundFormMessages } from './fundFormMessages';
import { Form, Modal } from 'react-bootstrap';
import { Button } from '../ui';
import { Form as FinalForm, Field } from 'react-final-form';

interface FormFields {
    name: string;
}

interface Props {
    initialValues?: FormFields;
    onSubmit: (values: FormFields) => Promise<unknown>;
    onClose: () => void;
}

export function RenameFileForm({ initialValues, onSubmit, onClose }: Props) {
    const intl = useIntl();

    function validate(values: FormFields) {
        const errors: Partial<Record<keyof FormFields, string>> = {};

        if (!values.name) {
            errors.name = intl.formatMessage(globalMessages.validationRequired);
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
        >{({ submitting, handleSubmit }) => (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <Field
                        name="name"
                        type="text"
                        component={FormInputField}
                        label={intl.formatMessage(fundFormMessages.dmsFileName)}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {intl.formatMessage(globalMessages.save)}
                    </Button>
                    <Button onClick={onClose} variant="link">
                        {intl.formatMessage(globalMessages.cancel)}
                    </Button>
                </Modal.Footer>
            </Form>
        )}
        </FinalForm>
    );
}

export type RenameFileFormProps = Props;
