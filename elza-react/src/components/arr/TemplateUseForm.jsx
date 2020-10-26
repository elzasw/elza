import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import FormInputField from "../shared/form/FormInputField";

export const NEW_TEMPLATE = 'new';
export const EXISTS_TEMPLATE = 'exists';

/**
 * Formulář šablony - použití.
 */
class TemplateUseForm extends AbstractReactComponent {
    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};

        if (!values.name) {
            errors.name = i18n('global.validation.required');
        }

        return errors;
    };

    static propTypes = {};

    UNSAFE_componentWillReceiveProps(nextProps) {}

    componentDidMount() {}

    submitReduxForm = (values, dispatch) =>
        submitForm(TemplateUseForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {
            handleSubmit,
            onClose,
            submitting,
            templates,
        } = this.props;
        return (
            <div className="todo">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <Field
                            disabled={submitting}
                            name="name"
                            type="select"
                            component={FormInputField}
                            label={i18n('arr.fund.useTemplate.name')}
                        >
                            <option value={''} key="no-select">
                                {i18n('global.action.select')}
                            </option>
                            {templates.map(template => (
                                <option value={template} key={template}>
                                    {template}
                                </option>
                            ))}
                        </Field>
                        <Field
                            name="replaceValues"
                            type="checkbox"
                            component={FormInputField}
                            label={i18n('arr.fund.useTemplate.replaceValues')}
                            inline
                            disabled={submitting}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary">{i18n('global.action.use')}</Button>
                        <Button variant="link" onClick={onClose}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

TemplateUseForm.defaultProps = {
    templates: [],
};

export default reduxForm({form: 'templateUseForm'}, null, {
    load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'templateUseForm', data}),
})(TemplateUseForm);
