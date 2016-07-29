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

const AddUserForm = class AddUserForm extends AbstractReactComponent {

    static defaultProps = {
        create: false
    };

    static PropTypes = {
        onCreateParty: React.PropTypes.func,
        create: React.PropTypes.bool
    };

    state = {
        createParty: false,
    };

    static validate(values, props) {
        const errors = {};

        let fields = ['username', 'password', 'passwordAgain'];

        if (props.create) {
            fields.push('party');
        }

        for (let field of fields) {
            if (!values[field]) {
                errors[field] = i18n('global.validation.required');
            }
        }
        if (values.password && values.passwordAgain && values.password !== values.passwordAgain) {
            errors.password = i18n('admin.user.validation.passNotEqual');
        }

        return errors;
    }

    constructor(props) {
        super(props);

        this.bindMethods(
            'handlePartyCreate',
            'handlePartyReceive',
        )
    }

    handlePartyCreate(partyTypeId) {
        const {onCreateParty} = this.props;

        onCreateParty && onCreateParty(partyTypeId, this.handlePartyReceive);
    };

    handlePartyReceive(newParty) {
        this.props.fields.party.onChange(newParty);
    };

    render() {
        const {fields: {username, password, passwordAgain, party}, create, handleSubmit, onClose} = this.props;

        const submitForm = submitReduxForm.bind(this, AddUserForm.validate);

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        {create && <PartyField label={i18n('admin.user.add.party')} {...party}  onCreate={this.handlePartyCreate} detail={false} />}
                        <FormInput label={i18n('admin.user.add.username')} autoComplete="off" type="text" {...username} />
                        <FormInput label={i18n(create ? 'admin.user.password' : 'admin.user.newPassword' )} autoComplete="off" type="password" {...password} />
                        <FormInput label={i18n('admin.user.passwordAgain')} autoComplete="off" type="password" {...passwordAgain} />
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

module.exports = reduxForm({
    form: 'addUserForm',
    fields: ['username', 'password', 'passwordAgain', 'party'],
})(AddUserForm);



