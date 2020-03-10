import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Form, FormCheck, FormControl, FormLabel, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';

import './FundSettingsForm.scss';

class FundSettingsForm extends AbstractReactComponent {

    /**
     * Validace formuláře.
     * @todo šlapa odstranit
     */
    static validate = (values, props) => {
        const errors = {};

        return errors;
    };

    state = {};

    submitReduxForm = (values, dispatch) => submitForm(FundSettingsForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {fields: {rightPanel: {tabs}, centerPanel: {panels}, strictMode}, handleSubmit, onClose} = this.props;

        return <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            <Modal.Body>
                <div className="fund-settings-form">
                    <div className="center-panel">
                        <h4>{i18n('arr.fund.settings.panel.center.title')}</h4>
                        {panels.map((val, index) =>
                            <div key={index}>
                                <FormCheck {...val.checked} value={true}>{val.name.initialValue}</FormCheck>
                            </div>,
                        )}
                    </div>
                    <div className="right-panel">
                        <h4>{i18n('arr.fund.settings.panel.right.title')}</h4>
                        {tabs.map((val, index) =>
                            <div key={index}>
                                <FormCheck {...val.checked} value={true}>{val.name.initialValue}</FormCheck>
                            </div>,
                        )}
                    </div>
                    <div className="rules">
                        <h4>{i18n('arr.fund.settings.rules')}</h4>
                        <FormLabel>{i18n('arr.fund.settings.rules.strictMode')}</FormLabel>
                        <FormControl {...strictMode.value} as="select" placeholder="select">
                            <option value="">{i18n('arr.fund.settings.rules.strictMode.default')}</option>
                            <option value="true">{i18n('arr.fund.settings.rules.strictMode.true')}</option>
                            <option value="false">{i18n('arr.fund.settings.rules.strictMode.false')}</option>
                        </FormControl>
                    </div>
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit">{i18n('visiblePolicy.action.save')}</Button>
                <Button variant="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>;
    }
}

export default reduxForm({
    form: 'fundSettingsForm',
    fields: ['rightPanel.tabs[].checked', 'rightPanel.tabs[].key', 'rightPanel.tabs[].name',
        'centerPanel.panels[].checked', 'centerPanel.panels[].key', 'centerPanel.panels[].name',
        'strictMode.value', 'strictMode.key', 'strictMode.name'],
})(FundSettingsForm);
