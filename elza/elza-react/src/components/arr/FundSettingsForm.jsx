import React from 'react';
import {reduxForm, Field, FieldArray} from 'redux-form';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { fundFormMessages } from './fundFormMessages';
import {Form, FormCheck, FormControl, FormLabel, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';

import './FundSettingsForm.scss';
import FormInputField from '../shared/form/FormInputField';

class FundSettingsForm extends AbstractReactComponent {
    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};

        return errors;
    };

    state = {};

    submitReduxForm = (values, dispatch) =>
        submitForm(FundSettingsForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {handleSubmit, onClose} = this.props;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    <div className="fund-settings-form">
                        <div className="center-panel">
                            <h4>{<FormattedMessage {...fundFormMessages.fundSettingsPanelCenterTitle} />}</h4>
                            <FieldArray
                                name={'centerPanel.panels'}
                                component={({fields, meta}) => {
                                    return fields.map((item, index, fields) => {
                                        return (
                                            <div key={index}>
                                                <Field
                                                    type="checkbox"
                                                    name={`${item}.checked`}
                                                    component={FormInputField}
                                                    label={fields.get(index).name}
                                                    value={true}
                                                />
                                            </div>
                                        );
                                    });
                                }}
                            />
                        </div>
                        <div className="right-panel">
                            <h4>{<FormattedMessage {...fundFormMessages.fundSettingsPanelRightTitle} />}</h4>
                            <FieldArray
                                name={'rightPanel.tabs'}
                                component={({fields, meta}) => {
                                    return fields.map((item, index, fields) => {
                                        return (
                                            <div key={index}>
                                                <Field
                                                    type="checkbox"
                                                    name={`${item}.checked`}
                                                    component={FormInputField}
                                                    label={fields.get(index).name}
                                                    value={true}
                                                />
                                            </div>
                                        );
                                    });
                                }}
                            />
                        </div>
                        <div className="rules">
                            <h4>{<FormattedMessage {...fundFormMessages.fundSettingsRules} />}</h4>
                            <FormLabel>{<FormattedMessage {...fundFormMessages.fundSettingsRulesStrictMode} />}</FormLabel>
                            <Field
                                component={FormInputField}
                                name={'strictMode.value'}
                                type="select"
                                placeholder="select"
                            >
                                <option value="">{<FormattedMessage {...fundFormMessages.fundSettingsRulesStrictModeDefault} />}</option>
                                <option value="true">{<FormattedMessage {...fundFormMessages.fundSettingsRulesStrictModeTrue} />}</option>
                                <option value="false">{<FormattedMessage {...fundFormMessages.fundSettingsRulesStrictModeFalse} />}</option>
                            </Field>
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary">
                        {<FormattedMessage {...fundFormMessages.visiblePolicyActionSave} />}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {<FormattedMessage {...globalMessages.cancel} />}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm({
    form: 'fundSettingsForm',
})(injectIntl(FundSettingsForm));
