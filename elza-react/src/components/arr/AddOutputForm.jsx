import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {reduxForm, Field, formValueSelector} from 'redux-form';
import {AbstractReactComponent, FormInput, FormInputField} from '../../components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { requestMessages } from './requestMessages';
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
            errors.name = this.props.intl.formatMessage(globalMessages.validationRequired);
        }
        if (props.create && !values.outputTypeId) {
            errors.outputTypeId = this.props.intl.formatMessage(globalMessages.validationRequired);
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
        const {create, handleSubmit, onClose, outputTypes, allTemplates, outputTypeId, submitting, outputFilters} = this.props;

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
                        <Field component={FormInputField} disabled={submitting} type="text" label={<FormattedMessage {...requestMessages.outputName} />} name={'name'} />
                        <Field
                            component={FormInputField}
                            type="text"
                            label={<FormattedMessage {...requestMessages.outputInternalCode} />}
                            name={'internalCode'}
                            disabled={submitting}
                        />
                        {create && (
                            <Field
                                component={FormInputField}
                                type="select"
                                label={<FormattedMessage {...requestMessages.outputOutputType} />}
                                name={'outputTypeId'}
                                disabled={submitting}
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
                            type="select"
                            label={<FormattedMessage {...requestMessages.outputTemplate} />}
                            name={'templateId'}
                            disabled={!outputTypeId || !templates || submitting}
                        >
                            <option key="-templateId" />
                            {templates &&
                                templates.map(i => (
                                    <option key={i.id} value={i.id}>
                                        {i.name}
                                    </option>
                                ))}
                        </Field>
                        <Field
                            component={FormInputField}
                            type="select"
                            label={<FormattedMessage {...requestMessages.outputOutputFilter} />}
                            name={'outputFilterId'}
                            disabled={submitting}
                        >
                            <option key="-outputFilterId" />
                            {outputFilters.data &&
                            outputFilters.data.map(i => (
                                <option key={i.id} value={i.id}>
                                    {i.name}
                                </option>
                            ))}
                        </Field>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" disabled={submitting} variant="outline-secondary">
                            {create ? this.props.intl.formatMessage(globalMessages.create) : this.props.intl.formatMessage(globalMessages.save)}
                        </Button>
                        <Button variant="link" onClick={onClose}>
                            {<FormattedMessage {...globalMessages.cancel} />}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

const form = reduxForm({
    form: AddOutputForm.FORM,
})(injectIntl(AddOutputForm));

const selector = formValueSelector(AddOutputForm.FORM);

export default connect((state, props) => {
    return {
        outputTypeId: selector(state, 'outputTypeId'),
        initialValues: props.initData,
        outputTypes: state.refTables.outputTypes.items,
        outputFilters: state.refTables.outputFilters,
        allTemplates: state.refTables.templates.items,
    };
})(form);
