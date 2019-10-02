import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button, Checkbox, Form} from 'react-bootstrap';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx'
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx'
import {indexById} from 'stores/app/utils.jsx'

/**
 * Formulář přidání výstupu.
 */

class AddOutputForm extends AbstractReactComponent {

    static defaultProps = {
        create: false
    };

    static PropTypes = {
        create: React.PropTypes.bool,
        initData: React.PropTypes.object,
        onSubmitForm: React.PropTypes.func.isRequired,
        templates: React.PropTypes.array.isRequired
    };

    /**
     * Validace formuláře.
     */
    static validate(values, props) {
        const errors = {};

        if (!values.name) {
            errors.name = i18n('global.validation.required');
        }
        if (props.create && !values.outputTypeId) {
            errors.outputTypeId = i18n('global.validation.required');
        }

        return errors;
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(outputTypesFetchIfNeeded());
        if (nextProps.fields.outputTypeId.value) {
            const index = indexById(nextProps.outputTypes, nextProps.fields.outputTypeId.value);
            if (index !== null) {
                this.dispatch(templatesFetchIfNeeded(nextProps.outputTypes[index].code))
            }
        }
    }

    componentDidMount() {
        this.dispatch(outputTypesFetchIfNeeded());
    }

    submitReduxForm = (values, dispatch) => submitForm(AddOutputForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {fields: {name, internalCode, templateId, outputTypeId}, create, handleSubmit, onClose, outputTypes, allTemplates} = this.props;

        let templates = false;
        if (outputTypeId.value) {
            const index = indexById(outputTypes, outputTypeId.value);
            if (index !== null) {
                const temp = allTemplates[outputTypes[index].code];
                if (temp && temp.fetched) {
                    templates = temp.items;
                }
            }
        }

        return (
            <div className="add-output-form-container">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <FormInput type="text" label={i18n('arr.output.name')} {...name} {...decorateFormField(name)} />
                        <FormInput type="text" label={i18n('arr.output.internalCode')} {...internalCode} {...decorateFormField(internalCode)} />
                        {create && <FormInput componentClass="select" label={i18n('arr.output.outputType')} {...outputTypeId} {...decorateFormField(outputTypeId)}>
                            <option key='-outputTypeId'/>
                            {outputTypes.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                        </FormInput>}
                        <FormInput componentClass="select" label={i18n('arr.output.template')} {...templateId} disabled={!outputTypeId.value || !templates}>
                            <option key='-templateId'/>
                            {templates && templates.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                        </FormInput>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit">{create ? i18n('global.action.create') : i18n('global.action.update')}</Button>
                        <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }
}

export default reduxForm({
        form: 'addOutputForm',
        fields: ['name', 'internalCode', 'outputTypeId', "templateId"],
    },
    (state, props) => {
        return {
            initialValues: props.initData,
            outputTypes: state.refTables.outputTypes.items,
            allTemplates: state.refTables.templates.items,
        }
    }
)(AddOutputForm);
