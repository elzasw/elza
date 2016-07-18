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

    for (let field of ['name', 'code'] ) {
        if (!values[field]) {
            errors[field] = i18n('global.validation.required');
        }
    }

    return errors;
};

const AddGroupForm = class AddGroupForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        //this.bindMethods();

        this.state = {};
    }

    render() {
        const {fields: {name, code}, handleSubmit, onClose} = this.props;

        const submitForm = submitReduxForm.bind(this, validate);

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <FormInput label={i18n('admin.group.add.name')} type="text" {...name} />
                        <FormInput label={i18n('admin.group.add.code')} type="text" {...code} />
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.create')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};

AddGroupForm.propTypes = {};

module.exports = reduxForm({
        form: 'addGroupForm',
        fields: ['name', 'code'],
})(AddGroupForm);



