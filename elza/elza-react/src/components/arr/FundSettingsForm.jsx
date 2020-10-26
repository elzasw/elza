import React from 'react';
import {reduxForm, Field, FieldArray} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
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
                            <h4>{i18n('arr.fund.settings.panel.center.title')}</h4>
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
                            <h4>{i18n('arr.fund.settings.panel.right.title')}</h4>
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
                            <h4>{i18n('arr.fund.settings.rules')}</h4>
                            <FormLabel>{i18n('arr.fund.settings.rules.strictMode')}</FormLabel>
                            <Field
                                component={FormInputField}
                                name={'strictMode.value'}
                                as="select"
                                placeholder="select"
                            >
                                <option value="">{i18n('arr.fund.settings.rules.strictMode.default')}</option>
                                <option value="true">{i18n('arr.fund.settings.rules.strictMode.true')}</option>
                                <option value="false">{i18n('arr.fund.settings.rules.strictMode.false')}</option>
                            </Field>
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary">
                        {i18n('visiblePolicy.action.save')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm({
    form: 'fundSettingsForm',
})(FundSettingsForm);
