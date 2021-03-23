import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {reduxForm, Field, formValueSelector} from 'redux-form';
import {AbstractReactComponent, FormInput, FormInputField, i18n} from '../../components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {decorateFormField, submitForm} from '../form/FormUtils';
import {outputTypesFetchIfNeeded} from '../../actions/refTables/outputTypes';
import {templatesFetchIfNeeded} from '../../actions/refTables/templates';
import {indexById} from '../../stores/app/utils';

/**
 * Formulář přidání výstupu.
 */

class AddOutputForm extends AbstractReactComponent {
    static FORM = 'addOutputForm';

    static defaultProps = {
        create: false,
    };

    static propTypes = {
        create: PropTypes.bool,
        initData: PropTypes.object,
        onSubmitForm: PropTypes.func.isRequired,
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
    }

    submitReduxForm = (values, dispatch) =>
        submitForm(AddOutputForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {create, handleSubmit, onClose, outputTypes, allTemplates, outputTypeId} = this.props;

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
            <div className="add-output-form-container">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <Field component={FormInputField} type="text" label={i18n('arr.output.name')} name={'name'} />
                        <Field
                            component={FormInputField}
                            type="text"
                            label={i18n('arr.output.internalCode')}
                            name={'internalCode'}
                        />
                        {create && (
                            <Field
                                component={FormInputField}
                                as="select"
                                label={i18n('arr.output.outputType')}
                                name={'outputTypeId'}
                            >
                                <option key="-outputTypeId" />
                                {outputTypes.map(i => (
                                    <option key={i.id} value={i.id}>
                                        {i.name}
                                    </option>
                                ))}
                            </Field>
                        )}
                        <Field
                            component={FormInputField}
                            as="select"
                            label={i18n('arr.output.template')}
                            name={'templateId'}
                            disabled={!outputTypeId || !templates}
                        >
                            <option key="-templateId" />
                            {templates &&
                                templates.map(i => (
                                    <option key={i.id} value={i.id}>
                                        {i.name}
                                    </option>
                                ))}
                        </Field>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary">
                            {create ? i18n('global.action.create') : i18n('global.action.update')}
                        </Button>
                        <Button variant="link" onClick={onClose}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

const form = reduxForm({
    form: AddOutputForm.FORM,
})(AddOutputForm);

const selector = formValueSelector(AddOutputForm.FORM);

export default connect((state, props) => {
    return {
        outputTypeId: selector(state, 'outputTypeId'),
        initialValues: props.initData,
        outputTypes: state.refTables.outputTypes.items,
        allTemplates: state.refTables.templates.items,
    };
})(form);
