import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {formValueSelector, Field, reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, FormInput, i18n} from 'components/shared';
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx';
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx';
import {indexById} from 'stores/app/utils.jsx';
import RegistryField from '../registry/RegistryField';
import {FormInputField} from '../shared';
import {WebApi} from "actions/index";
import Tags from "components/form/Tags";

/**
 * Formulář inline editace výstupu.
 */
class OutputInlineForm extends AbstractReactComponent {
    static FORM = 'outputEditForm';

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
        onSave: PropTypes.func.isRequired,
        disabled: PropTypes.bool.isRequired,
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(outputTypesFetchIfNeeded());
        if (nextProps.outputTypeId) {
            const index = indexById(nextProps.outputTypes, parseInt(nextProps.outputTypeId));
            if (index !== null) {
                this.props.dispatch(templatesFetchIfNeeded(nextProps.outputTypes[index].code));
            }
        }
    }

    componentDidMount() {
        this.props.dispatch(outputTypesFetchIfNeeded());
        this.props.dispatch(templatesFetchIfNeeded());
    }

    getOutputType = (id) => {
        const { outputTypes } = this.props;

        if (outputTypes) {
            const index = indexById(outputTypes, parseInt(id));
            return index !== null ? outputTypes[index] : null;
        }
        return null;
    }

    getOutputTemplates = (outputType) => {
        const { allTemplates } = this.props;
        const templates = [];
        if (outputType) {
            const template = allTemplates[outputType.code];
            if (template && template.fetched) {
                templates.push(...template.items);
            }
        }
        return templates;
    }

    getOutputAvailableTemplates = (templates) => {
        const { outputDetail } = this.props;
        if(!outputDetail.templateIds) {
            return templates;
        } else {
            return templates.filter((item) => outputDetail.templateIds.findIndex((id)=>item.id === id) < 0);
        }
    }

    render() {
        const {outputTypeId, disabled, outputDetail} = this.props;

        const outputType = this.getOutputType(outputTypeId);
        const outputTypeName = outputType ? outputType.name : "Unknown";
        const templates = this.getOutputTemplates(outputType);
        const availableTemplates = this.getOutputAvailableTemplates(templates);

        return (
            <div className="edit-output-form-container">
                <form>
                    <Field
                        component={FormInputField}
                        type="text"
                        label={i18n('arr.output.name')}
                        disabled={disabled}
                        name={'name'}
                    />
                    <Field
                        component={FormInputField}
                        type="text"
                        label={i18n('arr.output.internalCode')}
                        disabled={disabled}
                        name={'internalCode'}
                    />
                    <div>
                        <FormInput type="text" label={i18n('arr.output.outputType')} disabled value={outputTypeName} />
                    </div>
                    <div>
                        <label className="control-label">{i18n('arr.output.template')}</label>
                        <div>
                        {!disabled &&
                            <Autocomplete
                                ref="template-select"
                                className="form-group"
                                value={null}
                                items={availableTemplates}
                                disabled={disabled}
                                onChange={this.handleChangeTemplate}
                            />
                        }
                            <Tags disabled={disabled} items={outputDetail.templateIds||[]} onRemove={(item)=>this.handleRemoveTemplate(item)} renderItem={({item})=>{
                                const templateId = item;
                                const template = templates.find((temp)=>temp.id===templateId);
                                return template ? template.name : "Unknown template";
                            }}/>
                        </div>
                    </div>
                    <div>
                        <label className="control-label">{i18n('arr.output.title.anonymizedAp')}</label>
                        <Field
                            component={FormInputField}
                            as={RegistryField}
                            type="simple"
                            name={'anonymizedAp'}
                            addEmpty={true}
                            emptyTitle={i18n('arr.output.title.anonymizedAp.remove')}
                            disabled={disabled}
                        />
                    </div>
                </form>
            </div>
        );
    }

    handleRemoveTemplate = (templateId) => {
        const {outputDetail} = this.props;
        WebApi.deleteOutputTemplate(outputDetail.id, templateId);
    };


    handleAddTemplate = (templateId) => {
        const {outputDetail} = this.props;
        WebApi.addOutputTemplate(outputDetail.id, templateId);
        // Zbytek zařídí websocket
    };

    handleChangeTemplate = (template) => {
        this.handleAddTemplate(template.id);
    }
}

const form = reduxForm({
    form: OutputInlineForm.FORM,
    validate: OutputInlineForm.validate,
    asyncBlurFields: OutputInlineForm.fields,
    asyncValidate: (values, dispatch, props, blurredField) => {
        return props.onSave(values);
    },
})(OutputInlineForm);

const selector = formValueSelector(OutputInlineForm.FORM);

export default connect((state, props) => {
    return {
        outputTypeId: selector(state, 'outputTypeId'),
        outputTypes: state.refTables.outputTypes.items,
        allTemplates: state.refTables.templates.items,
    };
})(form);
