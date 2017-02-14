
import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Icon, FormInput, VersionValidationState, PartyField} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'

/**
 * Formulář přidání nebo uzavření AS.
 */

class AddUserForm extends AbstractReactComponent {

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

    handlePartyCreate = (partyTypeId) => {
        const {onCreateParty} = this.props;

        onCreateParty && onCreateParty(partyTypeId, this.handlePartyReceive);
    };

    handlePartyReceive = (newParty) => {
        this.props.fields.party.onChange(newParty);
    };

    render() {
        const {fields: {username, password, passwordAgain, party}, create, handleSubmit, onClose, submitting} = this.props;

        const submitForm = submitReduxForm.bind(this, AddUserForm.validate);

        return <Form onSubmit={handleSubmit(submitForm)}>
                <Modal.Body>
                        {create && <PartyField label={i18n('admin.user.add.party')} {...party}  onCreate={this.handlePartyCreate} detail={false} />}
                        <FormInput label={i18n('admin.user.add.username')} autoComplete="off" type="text" {...username} />
                        <FormInput label={i18n(create ? 'admin.user.password' : 'admin.user.newPassword' )} autoComplete="off" type="password" {...password} />
                        <FormInput label={i18n('admin.user.passwordAgain')} autoComplete="off" type="password" {...passwordAgain} />

                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" onClick={handleSubmit(submitForm)} disabled={submitting}>{i18n(create ? 'global.action.create' : 'global.action.update')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
    form: 'addUserForm',
    fields: ['username', 'password', 'passwordAgain', 'party'],
})(AddUserForm);



