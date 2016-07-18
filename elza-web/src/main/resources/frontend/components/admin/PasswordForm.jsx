/**
 * Formulář přidání nebo uzavření AS.
 */


import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Icon, FormInput, Autocomplete, VersionValidationState} from 'components/index.jsx';
import {Modal, Button, FormControl, FormGroup, ControlLabel, HelpBlock} from 'react-bootstrap';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import PartyField from 'components/party/PartyField.jsx'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
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

const PasswordForm = class PasswordForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        //this.bindMethods();

        this.state = {};
    }

    render() {
        const {fields: {oldPassword, password, passwordAgain}, handleSubmit, onClose, admin} = this.props;

        const submitForm = submitReduxForm.bind(this, validate);

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        {!admin && <FormInput label={i18n('admin.user.oldPassword')} autoComplete="off" type="password" {...oldPassword} />}
                        <FormInput label={i18n('admin.user.newPassword')} autoComplete="off" type="password" {...password} />
                        {!admin && <FormInput label={i18n('admin.user.passwordAgain')} autoComplete="off" type="password" {...passwordAgain} />}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.update')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};

PasswordForm.propTypes = {
    admin: React.PropTypes.bool
};

module.exports = reduxForm({
        form: 'passwordForm',
        fields: ['oldPassword', 'password', 'passwordAgain'],
})(PasswordForm);



