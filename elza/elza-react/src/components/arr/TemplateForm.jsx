import React from 'react';
import {Field, formValueSelector, reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Col, Form, Modal, Row} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import {connect} from "react-redux";
import FormInputField from "../shared/form/FormInputField";

export const NEW_TEMPLATE = 'new';
export const EXISTS_TEMPLATE = 'exists';

/**
 * Formulář šablony.
 */
class TemplateForm extends AbstractReactComponent {
    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};

        if (!values.type) {
            errors.type = i18n('global.validation.required');
        }

        if (!values.name) {
            errors.name = i18n('global.validation.required');
        }

        if (values.type === NEW_TEMPLATE) {
            for (const template of props.templates) {
                if (values.name.toUpperCase() === template.toUpperCase()) {
                    errors.name = i18n('global.validation.exists');
                }
            }
        }

        return errors;
    };

    static propTypes = {};

    UNSAFE_componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
    }

    submitReduxForm = (values, dispatch) =>
        submitForm(TemplateForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {
            handleSubmit,
            onClose,
            submitting,
            templates,
            type,
        } = this.props;
        return (
            <div className="todo">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <Row>
                            <Col xs={2}>
                                <Field
                                    disabled={submitting}
                                    component={FormInputField}
                                    type="radio"
                                    name="type"
                                    value={NEW_TEMPLATE}
                                    label={i18n('arr.fund.addTemplate.new')}
                                    inline
                                />
                            </Col>
                            <Col md={8}>
                                <Field
                                    disabled={submitting}
                                    component={FormInputField}
                                    type="radio"
                                    name="type"
                                    value={EXISTS_TEMPLATE}
                                    label={i18n('arr.fund.addTemplate.exists')}
                                    inline
                                />
                            </Col>
                        </Row>
                        {type === NEW_TEMPLATE && (
                            <Field
                                disabled={submitting}
                                name="name"
                                type="text"
                                component={FormInputField}
                                label={i18n('arr.fund.addTemplate.name')}
                            />
                        )}
                        {type === EXISTS_TEMPLATE && (
                            <Field
                                disabled={submitting}
                                name="name"
                                type="select"
                                component={FormInputField}
                                label={i18n('arr.fund.addTemplate.name')}
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
                        )}
                        <Field
                            name="withValues"
                            type="checkbox"
                            component={FormInputField}
                            label={i18n('arr.fund.addTemplate.withValues')}
                            inline
                            disabled={submitting}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary">{i18n('global.action.add')}</Button>
                        <Button variant="link" onClick={onClose}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

TemplateForm.defaultProps = {
    templates: [],
};

//
// export default reduxForm({form: 'templateForm'}, null, {
//     load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'templateForm', data}),
// })(TemplateForm);

const form = reduxForm({
    form: 'templateForm',
})(TemplateForm);

const selector = formValueSelector('templateForm');

export default connect((state, props) => {
    return {
        type: selector(state, 'type'),
    };
})(form);
