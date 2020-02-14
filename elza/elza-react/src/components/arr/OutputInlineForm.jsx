import PropTypes from 'prop-types';
import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx'
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx'
import {initForm} from "actions/form/inlineForm.jsx"
import {indexById} from 'stores/app/utils.jsx'

/**
 * Formulář inline editace výstupu.
 */
class OutputInlineForm extends AbstractReactComponent {

    static fields = ['name', 'outputTypeId', 'internalCode', 'templateId'];

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

    static propTypes = {
        create: PropTypes.bool,
        initData: PropTypes.object,
        onSave: PropTypes.func.isRequired,
        templates: PropTypes.array.isRequired
    };

    state = {};

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {fields: {outputTypeId}, outputTypes} = nextProps;
        this.props.dispatch(outputTypesFetchIfNeeded());
        if (outputTypeId.value) {
            const index = indexById(outputTypes, outputTypeId.value);
            if (index !== null) {
                this.props.dispatch(templatesFetchIfNeeded(outputTypes[index].code));
            }
        }
    }

    componentDidMount() {
        this.props.dispatch(outputTypesFetchIfNeeded());
        this.props.dispatch(templatesFetchIfNeeded());
        this.props.initForm(this.props.onSave)
    }

    render() {
        const {fields: {name, internalCode, templateId, outputTypeId}, disabled, outputTypes, allTemplates} = this.props;

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

        return <div className="edit-output-form-container">
            <form>
                <FormInput type="text" label={i18n('arr.output.name')}
                           disabled={disabled} {...name} {...decorateFormField(name, true)} />
                <FormInput type="text" label={i18n('arr.output.internalCode')}
                           disabled={disabled} {...internalCode} {...decorateFormField(internalCode, true)} />
                <div className="row-layout">
                    <FormInput type="text" label={i18n('arr.output.outputType')} disabled value={outputType}/>
                    <FormInput as="select" label={i18n('arr.output.template')}
                               disabled={disabled || !outputTypeId.value || !templates} {...templateId} {...decorateFormField(templateId, true)} >
                        <option key='-templateId'/>
                        {templates && templates.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                    </FormInput>
                </div>
            </form>
        </div>;
    }
}

export default reduxForm({
        form: 'outputEditForm',
        fields: OutputInlineForm.fields,
        validate: OutputInlineForm.validate,
    }, (state, props) => {
        return {
            initialValues: props.initData,
            outputTypes: state.refTables.outputTypes.items,
            allTemplates: state.refTables.templates.items,
        }
    },
    {initForm: (onSave) => (initForm("outputEditForm", OutputInlineForm.validate, onSave))}
)(OutputInlineForm);
