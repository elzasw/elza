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

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if (!values.name) {
        errors.name = i18n('global.validation.required');
    }

    return errors;
};

var OutputInlineForm = class OutputInlineForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(outputTypesFetchIfNeeded());
        this.dispatch(templatesFetchIfNeeded());
    }

    componentDidMount() {
        this.dispatch(outputTypesFetchIfNeeded());
        this.dispatch(templatesFetchIfNeeded());
        this.props.initForm(this.props.onSave)
    }

    render() {
        const {fields: {name, internalCode, temporary, templateId, outputTypeId}, disabled, outputTypes, templates} = this.props;

        var outputType = false;
        if (outputTypes) {
            const index = indexById(outputTypes, this.props.initData.outputTypeId);
            outputType = index !== null ? outputTypes[index].name : false;
        }

        return (
            <div className="edit-output-form-container">
                <form>
                    <FormInput type="text" label={i18n('arr.output.name')} disabled={disabled} {...name} {...decorateFormField(name, true)} />
                    <FormInput type="text" label={i18n('arr.output.internalCode')} disabled={disabled} {...internalCode} {...decorateFormField(internalCode, true)} />
                    <FormInput type="text" label={i18n('arr.output.outputType')} disabled value={outputType}/>
                    <FormInput componentClass="select" label={i18n('arr.output.template')} disabled={disabled} {...templateId} {...decorateFormField(templateId, true)}>
                        <option key='-templateId'/>
                        {templates.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                    </FormInput>
                </form>
            </div>
        )
    }
}

OutputInlineForm.propTypes = {
    create: React.PropTypes.bool,
    initData: React.PropTypes.object,
    onSave: React.PropTypes.func.isRequired,
    templates: React.PropTypes.array.isRequired
};

const fields = ['name', 'internalCode', "templateId"];
module.exports = reduxForm({
        form: 'outputEditForm',
        fields,
        validate,
    },(state, props) => {
        return {
            initialValues: props.initData,
            outputTypes: state.refTables.outputTypes.items,
            templates: state.refTables.templates.items,
        }
    },
    {initForm: (onSave) => (initForm("outputEditForm", validate, onSave))}
)(OutputInlineForm);
