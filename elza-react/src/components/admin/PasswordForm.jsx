import PropTypes from 'prop-types';
import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';

/**
 * Formulář přidání nebo uzavření AS.
 */
class PasswordForm extends AbstractReactComponent {
    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};

        if (!values.password) {
            errors.password = i18n('global.validation.required');
        }

        if (!props.admin) {
            if (!values.oldPassword) {
                errors.oldPassword = i18n('global.validation.required');
            }
            if (!values.passwordAgain) {
                errors.passwordAgain = i18n('global.validation.required');
            }
            if (values.password && values.passwordAgain && values.password !== values.passwordAgain) {
                errors.password = i18n('admin.user.validation.passNotEqual');
            }
        }

        return errors;
    };

    static propTypes = {
        admin: PropTypes.bool,
    };

    state = {};

    submitReduxForm = (values, dispatch) =>
        submitForm(PasswordForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {
            fields: {oldPassword, password, passwordAgain},
            handleSubmit,
            onClose,
            admin,
            submitting,
        } = this.props;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    {!admin && (
                        <FormInput
                            label={i18n('admin.user.oldPassword')}
                            autoComplete="off"
                            type="password"
                            {...oldPassword}
                        />
                    )}
                    <FormInput
                        label={i18n('admin.user.newPassword')}
                        autoComplete="off"
                        type="password"
                        {...password}
                    />
                    {!admin && (
                        <FormInput
                            label={i18n('admin.user.passwordAgain')}
                            autoComplete="off"
                            type="password"
                            {...passwordAgain}
                        />
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n('global.action.update')}
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
    form: 'passwordForm',
    fields: ['oldPassword', 'password', 'passwordAgain'],
})(PasswordForm);
