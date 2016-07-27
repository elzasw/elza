/**
 * Formulář přidání nebo uzavření AS.
 */


import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Icon, FormInput, Autocomplete, VersionValidationState, PartyField} from 'components/index.jsx';
import {Modal, Button} from 'react-bootstrap';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    let fields = ['username', 'password'];

    if (props.create) {
        fields.push('party');
        fields.push('passwordAgain');
    }

    for (let field of  fields) {
        if (!values[field]) {
            errors[field] = i18n('global.validation.required');
        }
    }
    if (values.password && values.passwordAgain && values.password !== values.passwordAgain) {
        errors.password = i18n('admin.user.validation.passNotEqual');
    }

    return errors;
};

const AddUserForm = class AddUserForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        //this.bindMethods();

        this.state = {};
    }

    render() {
        const {fields: {username, password, passwordAgain, party}, create, handleSubmit, onClose} = this.props;

        const submitForm = submitReduxForm.bind(this, validate);

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        {create && <PartyField label={i18n('admin.user.add.party')} {...party} />}
                        <FormInput label={i18n('admin.user.add.username')} autoComplete="off" type="text" {...username} />
                        <FormInput label={i18n('admin.user.password')} autoComplete="off" type="password" {...password} />
                        {create && <FormInput label={i18n('admin.user.passwordAgain')} autocomplete="off" type="password" {...passwordAgain} />}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n(create ? 'global.action.create' : 'global.action.update')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};

AddUserForm.propTypes = {};

module.exports = reduxForm({
        form: 'addUserForm',
        fields: ['username', 'password', 'passwordAgain', 'party'],
    }
    ,(state, props) => {
        return {
            initialValues: props.initData,
        }
    },
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addUserForm', data})}
)(AddUserForm);



