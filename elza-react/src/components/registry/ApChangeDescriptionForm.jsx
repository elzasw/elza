import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {decorateFormField} from 'components/form/FormUtils.jsx';

class ApChangeDescriptionForm extends AbstractReactComponent {
    render() {
        const {
            fields: {description},
            handleSubmit,
            onClose,
            submitting,
        } = this.props;

        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <FormInput
                        as="textarea"
                        label={i18n('accesspoint.description')}
                        {...description}
                        {...decorateFormField(description)}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" disabled={submitting}>
                        {i18n('global.action.store')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm({
    form: 'apChangeDescriptionForm',
    fields: ['description'],
})(ApChangeDescriptionForm);
