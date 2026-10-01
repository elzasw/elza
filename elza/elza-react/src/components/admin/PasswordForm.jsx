import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {reduxForm, Field} from 'redux-form';
import {AbstractReactComponent, FormInput} from 'components/shared';
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang';
import { getIntl } from 'components/shared/lang/intlInstance';
import { exceptionMessages } from 'components/exception/messages';
import { checkPasswordPolicy } from './passwordPolicy';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    passNotEqual: { id: 'admin.user.validation.passNotEqual', defaultMessage: 'Zadaná hesla nejsou stejná' },
    oldPassword: { id: 'admin.user.oldPassword', defaultMessage: 'Staré heslo' },
    newPassword: { id: 'admin.user.newPassword', defaultMessage: 'Nové heslo' },
    passwordAgain: { id: 'admin.user.passwordAgain', defaultMessage: 'Opakovat heslo' },
    changeRequired: {
        id: 'admin.user.password.changeRequired',
        defaultMessage: 'Vyžadovat změnu hesla při dalším přihlášení',
    },
    neverExpire: { id: 'admin.user.password.neverExpire', defaultMessage: 'Heslo bez expirace' },
    forcedInfo: {
        id: 'admin.user.password.forcedInfo',
        defaultMessage: 'Platnost hesla vypršela. Pro pokračování si nastavte nové heslo.',
    },
});
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import FormInputField from '../shared/form/FormInputField';

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
            errors.password = getIntl().formatMessage(globalMessages.validationRequired);
        } else {
            const violation = checkPasswordPolicy(values.password, props.passwordPolicy);
            if (violation) {
                errors.password = getIntl().formatMessage(
                    exceptionMessages['exception.usr.PASSWORD_POLICY_VIOLATION'],
                    violation,
                );
            }
        }

        if (!props.admin) {
            if (!values.oldPassword) {
                errors.oldPassword = getIntl().formatMessage(globalMessages.validationRequired);
            }
            if (!values.passwordAgain) {
                errors.passwordAgain = getIntl().formatMessage(globalMessages.validationRequired);
            }
            if (!errors.password && values.password && values.passwordAgain && values.password !== values.passwordAgain) {
                errors.password = getIntl().formatMessage(messages.passNotEqual);
            }
        }

        return errors;
    };

    static propTypes = {
        admin: PropTypes.bool,
        /** Password change enforced after login - no way to cancel. */
        forced: PropTypes.bool,
    };

    state = {};

    submitReduxForm = (values, dispatch) =>
        submitForm(PasswordForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {handleSubmit, onClose, admin, forced, submitting} = this.props;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    {forced && (
                        <p>
                            <FormattedMessage {...messages.forcedInfo} />
                        </p>
                    )}
                    {!admin && (
                        <Field
                            component={FormInputField}
                            label={this.props.intl.formatMessage(messages.oldPassword)}
                            autoComplete="off"
                            type="password"
                            name={'oldPassword'}
                        />
                    )}
                    <Field
                        component={FormInputField}
                        label={this.props.intl.formatMessage(messages.newPassword)}
                        autoComplete="off"
                        type="password"
                        name={'password'}
                    />
                    {!admin && (
                        <Field
                            component={FormInputField}
                            label={this.props.intl.formatMessage(messages.passwordAgain)}
                            autoComplete="off"
                            type="password"
                            name={'passwordAgain'}
                        />
                    )}
                    {admin && (
                        <>
                            <Field
                                component={FormInputField}
                                label={this.props.intl.formatMessage(messages.changeRequired)}
                                type="checkbox"
                                name={'changeRequired'}
                                disabled={submitting}
                            />
                            <Field
                                component={FormInputField}
                                label={this.props.intl.formatMessage(messages.neverExpire)}
                                type="checkbox"
                                name={'neverExpire'}
                                disabled={submitting}
                            />
                        </>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        <FormattedMessage {...globalMessages.save} />
                    </Button>
                    {!forced && (
                        <Button variant="link" onClick={onClose}>
                            <FormattedMessage {...globalMessages.cancel} />
                        </Button>
                    )}
                </Modal.Footer>
            </Form>
        );
    }
}

export default connect(state => ({passwordPolicy: state.userDetail.passwordPolicy}))(
    reduxForm({
        form: 'passwordForm'
    })(injectIntl(PasswordForm)),
);
