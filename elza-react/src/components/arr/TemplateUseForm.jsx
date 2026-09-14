import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { getIntl } from 'components/shared/lang/intlInstance';
import { templateMessages } from './templateMessages';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import FormInputField from '../shared/form/FormInputField';

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
            errors.name = getIntl().formatMessage(globalMessages.validationRequired);
        }

        return errors;
    };

    static propTypes = {};

    componentDidMount() {}

    submitReduxForm = (values, dispatch) =>
        submitForm(TemplateUseForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {handleSubmit, onClose, submitting, templates} = this.props;
        return (
            <div className="todo">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <Field
                            disabled={submitting}
                            name="name"
                            type="select"
                            component={FormInputField}
                            label={<FormattedMessage {...templateMessages.fundUseTemplateName} />}
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
                        <Field
                            name="replaceValues"
                            type="checkbox"
                            component={FormInputField}
                            label={<FormattedMessage {...templateMessages.fundUseTemplateReplaceValues} />}
                            inline
                            disabled={submitting}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary">
                            {<FormattedMessage {...templateMessages.globalActionUse} />}
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

TemplateUseForm.defaultProps = {
    templates: [],
};

export default reduxForm({form: 'templateUseForm'})(TemplateUseForm);
