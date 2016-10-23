require('./FundSettingsForm.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {Modal, Button, Checkbox, Form} from 'react-bootstrap';
import {indexById, objectById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {visiblePolicyFetchIfNeeded} from 'actions/arr/visiblePolicy.jsx'

class FundSettingsForm extends AbstractReactComponent {

    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};

        return errors;
    };

    state = {};

    componentWillReceiveProps(nextProps) {

    }

    componentDidMount() {

    }

    render() {
        const {fields: {rightPanel: {tabs}, centerPanel: {panels}}, handleSubmit, onClose} = this.props;
        var submitForm = submitReduxForm.bind(this, FundSettingsForm.validate);

        return <Form onSubmit={handleSubmit(submitForm)}>
                <Modal.Body>
                    <div className="fund-settings-form">
                        <div className="center-panel">
                            <h4>{i18n('arr.fund.settings.panel.center.title')}</h4>
                            {panels.map((val, index) =>
                                <div key={index}>
                                    <Checkbox {...val.checked} value={true}>{val.name.initialValue}</Checkbox>
                                </div>
                            )}
                        </div>
                        <div className="right-panel">
                            <h4>{i18n('arr.fund.settings.panel.right.title')}</h4>
                            {tabs.map((val, index) =>
                                <div key={index}>
                                    <Checkbox {...val.checked} value={true}>{val.name.initialValue}</Checkbox>
                                </div>
                            )}
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" onClick={handleSubmit(submitForm)}>{i18n('visiblePolicy.action.save')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </Form>
    }
}

export default reduxForm({
    form: 'fundSettingsForm',
    fields: ['rightPanel.tabs[].checked', 'rightPanel.tabs[].key', 'rightPanel.tabs[].name',
             'centerPanel.panels[].checked', 'centerPanel.panels[].key', 'centerPanel.panels[].name']
}, state => ({
    //initialValues: {records: state.arrRegion.visiblePolicy.data},
    //visiblePolicy: state.arrRegion.visiblePolicy,
    //visiblePolicyTypes: state.refTables.visiblePolicyTypes
}))(FundSettingsForm)