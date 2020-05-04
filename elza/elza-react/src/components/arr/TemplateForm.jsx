import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Col, Form, FormCheck, Modal, Row} from 'react-bootstrap';
import {Button} from '../ui';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx';

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

    UNSAFE_componentWillReceiveProps(nextProps) {}

    componentDidMount() {}

    submitReduxForm = (values, dispatch) =>
        submitForm(TemplateForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {
            fields: {name, type, withValues},
            handleSubmit,
            onClose,
            submitting,
            templates,
        } = this.props;
        return (
            <div className="todo">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <Row>
                            <Col xs={2}>
                                <FormCheck
                                    type={'radio'}
                                    disabled={submitting}
                                    {...type}
                                    inline
                                    value={NEW_TEMPLATE}
                                    checked={type.value === NEW_TEMPLATE}
                                    label={i18n('arr.fund.addTemplate.new')}
                                />
                            </Col>
                            <Col md={3}>
                                <FormCheck
                                    type={'radio'}
                                    disabled={submitting}
                                    {...type}
                                    inline
                                    value={EXISTS_TEMPLATE}
                                    checked={type.value === EXISTS_TEMPLATE}
                                    label={i18n('arr.fund.addTemplate.exists')}
                                />
                            </Col>
                        </Row>
                        {type.value === NEW_TEMPLATE && (
                            <FormInput
                                disabled={submitting}
                                type="text"
                                label={i18n('arr.fund.addTemplate.name')}
                                {...name}
                                {...decorateFormField(name)}
                            />
                        )}
                        {type.value === EXISTS_TEMPLATE && (
                            <FormInput
                                disabled={submitting}
                                as="select"
                                label={i18n('arr.fund.addTemplate.name')}
                                {...name}
                                {...decorateFormField(name)}
                            >
                                <option value={''} key="no-select">
                                    {i18n('global.action.select')}
                                </option>
                                {templates.map(template => (
                                    <option value={template} key={template}>
                                        {template}
                                    </option>
                                ))}
                            </FormInput>
                        )}
                        <FormCheck disabled={submitting} {...withValues} inline label={i18n('arr.fund.addTemplate.withValues')} />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit">{i18n('global.action.add')}</Button>
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

export default reduxForm({form: 'templateForm', fields: ['name', 'type', 'withValues']}, null, {
    load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'templateForm', data}),
})(TemplateForm);
