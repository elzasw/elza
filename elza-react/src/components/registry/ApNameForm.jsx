import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Icon, FormInput} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField} from 'components/form/FormUtils.jsx'


import LanguageCodeField from "../LanguageCodeField";

/**
 * Formulář formy jména osoby
 */
class ApNameForm extends AbstractReactComponent {

    static fields = [
        'name',
        'complement',
        'languageCode',
    ];

    render() {
        const {
            fields: {
                name,
                complement,
                languageCode,
            },
            handleSubmit,
            submitting,
            onClose
        } = this.props;

        return <Form onSubmit={handleSubmit}>
            <Modal.Body>
                <FormInput type="input" label={i18n('accesspoint.name.name')} {...name} />
                <FormInput type="input" label={i18n('accesspoint.name.complement')} {...complement} />
                <LanguageCodeField label={i18n('accesspoint.languageCode')} {...languageCode} {...decorateFormField(languageCode)}  />
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('global.action.store')}</Button>
                <Button bsStyle="link" onClick={onClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>;
    }
}

export default reduxForm({
    form: 'ApNameForm',
    fields: ApNameForm.fields
})(ApNameForm);
