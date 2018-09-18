import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Button, Checkbox, Form, Modal} from 'react-bootstrap';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'


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

    static PropTypes = {

    };

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {

    }

    submitReduxForm = (values, dispatch) => submitForm(TemplateUseForm.validate, values, this.props,this.props.onSubmitForm, dispatch);

    render() {
        const {fields: {name, replaceValues}, handleSubmit, onClose, submitting, templates} = this.props;
        return (
            <div className="todo">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <FormInput disabled={submitting} componentClass="select" label={i18n('arr.fund.useTemplate.name')} {...name} {...decorateFormField(name)} >
                            <option value={""} key="no-select">{i18n('global.action.select')}</option>
                            {templates.map(template => <option value={template} key={template}>{template}</option>)}
                        </FormInput>
                        <Checkbox
                            disabled={submitting}
                            {...replaceValues}
                            inline
                        >
                            {i18n('arr.fund.useTemplate.replaceValues')}
                        </Checkbox>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit">{i18n('global.action.use')}</Button>
                        <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }
}

TemplateUseForm.defaultProps = {
    templates: []
};

export default reduxForm(
    {form: 'templateUseForm', fields: ['name', 'replaceValues']},
    null,
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'templateUseForm', data})}
)(TemplateUseForm);
