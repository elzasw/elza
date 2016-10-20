import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Icon, FormInput, Autocomplete, VersionValidationState} from 'components/index.jsx';
import {Modal, Button, FormControl, FormGroup, ControlLabel, HelpBlock, Form} from 'react-bootstrap';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'

/**
 * Formulář přidání nebo uzavření AS.
 */

class AddGroupForm extends AbstractReactComponent {

    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};

        for (let field of ['name', 'code'] ) {
            if (!values[field]) {
                errors[field] = i18n('global.validation.required');
            }
        }

        return errors;
    };

    static PropTypes = {};

    state = {};

    render() {
        const {fields: {name, code, description}, create, handleSubmit, onClose} = this.props;

        const submitForm = submitReduxForm.bind(this, AddGroupForm.validate);

        return <Form onSubmit={handleSubmit(submitForm)}>
            <Modal.Body>

                    <FormInput label={i18n('admin.group.title.name')} type="text" {...name} />
                    {create && <FormInput label={i18n('admin.group.title.code')} type="text" {...code} />}
                    {!create && <FormInput disabled label={i18n('admin.group.title.code')} type="text" {...code} />}
                    <FormInput componentClass="textarea" label={i18n('admin.group.title.description')} type="text" {...description} />
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" onClick={handleSubmit(submitForm)}>{i18n(create ? 'global.action.create' : 'global.action.update')}</Button>
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

export default  reduxForm({
        form: 'addGroupForm',
        fields: ['name', 'code', 'description'],
    },(state, props) => {
        return {
            initialValues: props.initData,
        }
    }
)(AddGroupForm);



