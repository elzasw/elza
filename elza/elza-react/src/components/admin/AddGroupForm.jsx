import PropTypes from 'prop-types';
import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInputField, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';

/**
 * Formulář přidání nebo uzavření AS.
 */
class AddGroupForm extends AbstractReactComponent {

    static validate = (values, props) => {
        const errors = {};

        for (let field of ['name', 'code']) {
            if (!values[field]) {
                errors[field] = i18n('global.validation.required');
            }
        }

        return errors;
    };

    static propTypes = {
        create: PropTypes.bool,
    };
    submitReduxForm = (values, dispatch) =>
        submitForm(AddGroupForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {create, handleSubmit, onClose, submitting} = this.props;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
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
                        disabled={!create}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n(create ? 'global.action.create' : 'global.action.update')}
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
    form: 'addGroupForm',
})(AddGroupForm);
