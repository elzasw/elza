import React from 'react';
import {Field, formValueSelector, reduxForm} from 'redux-form';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { getIntl } from 'components/shared/lang/intlInstance';
import { templateMessages } from './templateMessages';
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
            errors.type = getIntl().formatMessage(globalMessages.validationRequired);
        }

        if (!values.name) {
            errors.name = getIntl().formatMessage(globalMessages.validationRequired);
        }

        if (values.type === NEW_TEMPLATE) {
            for (const template of props.templates) {
                if (values.name.toUpperCase() === template.toUpperCase()) {
                    errors.name = getIntl().formatMessage(templateMessages.globalValidationExists);
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
                                    label={<FormattedMessage {...templateMessages.fundAddTemplateNew} />}
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
                                    label={<FormattedMessage {...templateMessages.fundAddTemplateExists} />}
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
                                label={<FormattedMessage {...templateMessages.fundAddTemplateName} />}
                            />
                        )}
                        {type === EXISTS_TEMPLATE && (
                            <Field
                                disabled={submitting}
                                name="name"
                                type="select"
                                component={FormInputField}
                                label={<FormattedMessage {...templateMessages.fundAddTemplateName} />}
                            >
                                <option value={''} key="no-select">
                                    {<FormattedMessage {...templateMessages.globalActionSelect} />}
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
                            label={<FormattedMessage {...templateMessages.fundAddTemplateWithValues} />}
                            inline
                            disabled={submitting}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary">{<FormattedMessage {...globalMessages.add} />}</Button>
                        <Button variant="link" onClick={onClose}>
                            {<FormattedMessage {...globalMessages.cancel} />}
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
