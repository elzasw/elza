import React from 'react';
import { FormInputField } from 'components/shared';
import { defineMessages, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    name: { id: 'admin.group.title.name', defaultMessage: 'Název' },
    code: { id: 'admin.group.title.code', defaultMessage: 'Kód' },
    description: { id: 'admin.group.title.description', defaultMessage: 'Popis' },
});
import { Form, Modal } from 'react-bootstrap';
import { Button } from '../ui';
import { Form as FinalForm, Field } from 'react-final-form';

interface FormFields {
    name: string;
    code: string;
    description?: string;
}

interface Props {
    create?: boolean;
    initialValues?: FormFields;
    onSubmit: (values: FormFields) => Promise<any>;
    onClose: () => void;
}

export function AddGroupForm({
    create,
    initialValues,
    onSubmit,
    onClose
}: Props) {
    const intl = useIntl();


    function validate(values: FormFields) {
        const errors: Partial<Record<keyof FormFields, string>> = {};

        if (!values.name && create) {
            errors.name = intl.formatMessage(globalMessages.validationRequired);
        }
        if (!values.code && create) {
            errors.code = intl.formatMessage(globalMessages.validationRequired);
        }

        return errors;
    };

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
                        name="name"
                        type="text"
                        component={FormInputField}
                        label={intl.formatMessage(messages.name)}
                    />
                    <Field
                        name="code"
                        type="text"
                        component={FormInputField}
                        label={intl.formatMessage(messages.code)}
                        disabled={!create}
                    />
                    <Field
                        name="description"
                        type="textarea"
                        component={FormInputField}
                        label={intl.formatMessage(messages.description)}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit} variant="outline-secondary" disabled={submitting}>
                        {intl.formatMessage(create ? globalMessages.create : globalMessages.save)}
                    </Button>
                    <Button onClick={onClose} variant="link">
                        {intl.formatMessage(globalMessages.cancel)}
                    </Button>
                </Modal.Footer>
            </Form>
        }}
        </FinalForm>
    );
}

export default AddGroupForm;
