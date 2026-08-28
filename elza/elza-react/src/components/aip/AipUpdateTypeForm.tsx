import React from 'react';
import { i18n } from 'components/shared';
import { Form, Modal } from 'react-bootstrap';
import { Button } from '../ui';
import { Form as FinalForm, Field } from 'react-final-form';
import { useIntl } from 'react-intl';
import { AipUpdateType } from "elza-api";

import { updateTypeDescriptions, updateTypeMessages } from './messages';
import './AipUpdateTypeForm.scss';

interface FormFields {
    type: AipUpdateType;
}

interface Props {
    initialValues?: FormFields;
    onSubmit: (values: FormFields) => void;
    onClose?: () => void;
}

/**
 * Pořadí od nejčastější a nejbezpečnější volby po tu, která ruší napojení na popis.
 */
const UPDATE_TYPES: AipUpdateType[] = [
    AipUpdateType.RemapReferences,
    AipUpdateType.DownloadUpdate,
    AipUpdateType.DbUpdate,
    AipUpdateType.ForceUpdate,
];

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
            return <Form className="aip-update-type">
                <Modal.Body>
                    <Field name="type">
                        {({input, meta}) => (
                            <fieldset>
                                <legend className="form-label">{i18n('aip.form.update.type')}</legend>
                                {UPDATE_TYPES.map(type => (
                                    <Form.Check
                                        key={type}
                                        type="radio"
                                        id={`aipUpdateType-${type}`}
                                        name={input.name}
                                        value={type}
                                        checked={input.value === type}
                                        onChange={() => input.onChange(type)}
                                        disabled={submitting}
                                        label={
                                            <>
                                                <span className="update-type-name">
                                                    {intl.formatMessage(updateTypeMessages[type])}
                                                </span>
                                                <span className="update-type-hint">
                                                    {intl.formatMessage(updateTypeDescriptions[type])}
                                                </span>
                                            </>
                                        }
                                    />
                                ))}
                                {meta.touched && meta.error &&
                                    <div className="invalid-feedback d-block">{meta.error}</div>}
                            </fieldset>
                        )}
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
