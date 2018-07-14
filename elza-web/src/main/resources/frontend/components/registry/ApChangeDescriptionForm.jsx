import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField} from 'components/form/FormUtils.jsx'

class ApChangeDescriptionForm extends AbstractReactComponent {
    render() {
        const {fields: {description}, handleSubmit, onClose, submitting} = this.props;

        return <Form onSubmit={handleSubmit}>
            <Modal.Body>
                <FormInput
                    componentClass="textarea"
                    label={i18n('accesspoint.description')}
                    {...description}
                    {...decorateFormField(description)}
                />
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('global.action.store')}</Button>
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
    form: 'apChangeDescriptionForm',
    fields: ['description']
})(ApChangeDescriptionForm);



