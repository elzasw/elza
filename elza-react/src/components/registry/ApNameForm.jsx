import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {decorateFormField} from 'components/form/FormUtils.jsx';


import LanguageCodeField from '../LanguageCodeField';

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
                  onClose,
              } = this.props;

        return <Form onSubmit={handleSubmit}>
            <Modal.Body>
                <FormInput type="input" label={i18n('accesspoint.name.name')} {...name} />
                <FormInput type="input" label={i18n('accesspoint.name.complement')} {...complement} />
                <LanguageCodeField
                    label={i18n('accesspoint.languageCode')} {...languageCode} {...decorateFormField(languageCode)}  />
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('global.action.store')}</Button>
                <Button variant="link" onClick={onClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>;
    }
}

export default reduxForm({
    form: 'ApNameForm',
    fields: ApNameForm.fields,
})(ApNameForm);
