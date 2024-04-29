import React from 'react';
import { FormInputField, i18n } from 'components/shared';
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

    function validate(values: FormFields) {
        const errors: Partial<Record<keyof FormFields, string>> = {};

        if (!values.name && create) {
            errors.name = i18n('global.validation.required');
        }
        if (!values.code && create) {
            errors.code = i18n('global.validation.required');
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
                        label={i18n('admin.group.title.name')}
                    />
                    <Field
                        name="code"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.group.title.code')}
                        disabled={!create}
                    />
                    <Field
                        name="description"
                        type="textarea"
                        component={FormInputField}
                        label={i18n('admin.group.title.description')}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit} variant="outline-secondary" disabled={submitting}>
                        {i18n(create ? 'global.action.create' : 'global.action.update')}
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

export default AddGroupForm;
