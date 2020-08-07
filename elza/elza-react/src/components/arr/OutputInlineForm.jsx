import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {formValueSelector, Field, reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx';
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx';
import {indexById} from 'stores/app/utils.jsx';
import RegistryField from '../registry/RegistryField';
import {FormInputField} from '../shared';

/**
 * Formulář inline editace výstupu.
 */
class OutputInlineForm extends AbstractReactComponent {
    static fields = ['name', 'outputTypeId', 'internalCode', 'templateId', 'anonymizedAp'];

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
        const {
            fields: {outputTypeId},
            outputTypes,
        } = nextProps;
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

    render() {
        const {outputTypeId, disabled, outputTypes, allTemplates} = this.props;

        let outputType = false;
        if (outputTypes) {
            const index = indexById(outputTypes, parseInt(this.props.initialValues.outputTypeId));
            outputType = index !== null ? outputTypes[index].name : false;
        }

        let templates = false;
        if (outputTypeId) {
            const index = indexById(outputTypes, parseInt(outputTypeId));
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
                    <div className="row-layout">
                        <FormInput type="text" label={i18n('arr.output.outputType')} disabled value={outputType} />
                        <Field
                            component={FormInputField}
                            as="select"
                            label={i18n('arr.output.template')}
                            disabled={disabled || !outputTypeId || !templates}
                            name={'templateId'}
                        >
                            <option key="-templateId" />
                            {templates &&
                                templates.map(i => (
                                    <option key={i.id} value={i.id}>
                                        {i.name}
                                    </option>
                                ))}
                        </Field>
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
}

const form = reduxForm({
    form: OutputInlineForm.FORM,
    fields: OutputInlineForm.fields,
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
