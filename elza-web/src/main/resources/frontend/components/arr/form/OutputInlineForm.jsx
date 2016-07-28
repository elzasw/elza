/**
 * Formulář inline editace výstupu.
 */

import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx'
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx'
import {initForm} from "actions/form/inlineForm.jsx"
import {indexById} from 'stores/app/utils.jsx'

const OutputInlineForm = class OutputInlineForm extends AbstractReactComponent {

    /**
     * Validace formuláře.
     */
    static validate(values, props) {
        const errors = {};

        if (!values.name) {
            errors.name = i18n('global.validation.required');
        }

        return errors;
    }

    static PropTypes = {
        create: React.PropTypes.bool,
        initData: React.PropTypes.object,
        onSave: React.PropTypes.func.isRequired,
        templates: React.PropTypes.array.isRequired
    };

    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        const {fields: {outputTypeId}, outputTypes} = nextProps;
        this.dispatch(outputTypesFetchIfNeeded());
        if (outputTypeId.value) {
            const index = indexById(outputTypes, outputTypeId.value);
            if (index !== null) {
                this.dispatch(templatesFetchIfNeeded(outputTypes[index].code));
            }
        }
    }

    componentDidMount() {
        this.dispatch(outputTypesFetchIfNeeded());
        this.dispatch(templatesFetchIfNeeded());
        this.props.initForm(this.props.onSave)
    }

    render() {
        const {fields: {name, internalCode, temporary, templateId, outputTypeId}, disabled, outputTypes, allTemplates} = this.props;

        let outputType = false;
        if (outputTypes) {
            const index = indexById(outputTypes, this.props.initData.outputTypeId);
            outputType = index !== null ? outputTypes[index].name : false;
        }


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
            <div className="edit-output-form-container">
                <form>
                    <FormInput type="text" label={i18n('arr.output.name')} disabled={disabled} {...name} {...decorateFormField(name, true)} />
                    <FormInput type="text" label={i18n('arr.output.internalCode')} disabled={disabled} {...internalCode} {...decorateFormField(internalCode, true)} />
                    <FormInput type="text" label={i18n('arr.output.outputType')} disabled value={outputType}/>
                    <FormInput componentClass="select" label={i18n('arr.output.template')} disabled={disabled || !outputTypeId.value || !templates} {...templateId} {...decorateFormField(templateId, true)} >
                        <option key='-templateId'/>
                        {templates && templates.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                    </FormInput>
                </form>
            </div>
        )
    }
}

const fields = ['name', 'outputTypeId', 'internalCode', 'templateId'];
module.exports = reduxForm({
        form: 'outputEditForm',
        fields,
        validate: OutputInlineForm.validate,
    },(state, props) => {
        return {
            initialValues: props.initData,
            outputTypes: state.refTables.outputTypes.items,
            allTemplates: state.refTables.templates.items,
        }
    },
    {initForm: (onSave) => (initForm("outputEditForm", OutputInlineForm.validate, onSave))}
)(OutputInlineForm);
