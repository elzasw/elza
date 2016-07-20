require('./FundSettingsForm.less')

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {Modal, Button, Checkbox} from 'react-bootstrap';
import {indexById, objectById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {visiblePolicyFetchIfNeeded} from 'actions/arr/visiblePolicy.jsx'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    return errors;
};

var FundSettingsForm = class FundSettingsForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {

    }

    componentDidMount() {

    }

    render() {
        const {fields: {rightPanel: {tabs}, centerPanel: {panels}}, handleSubmit, onClose} = this.props;
        var submitForm = submitReduxForm.bind(this, validate)

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
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
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('visiblePolicy.action.save')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'fundSettingsForm',
    fields: ['rightPanel.tabs[].checked', 'rightPanel.tabs[].key', 'rightPanel.tabs[].name',
             'centerPanel.panels[].checked', 'centerPanel.panels[].key', 'centerPanel.panels[].name']
}, state => ({
    //initialValues: {records: state.arrRegion.visiblePolicy.data},
    //visiblePolicy: state.arrRegion.visiblePolicy,
    //visiblePolicyTypes: state.refTables.visiblePolicyTypes
}))(FundSettingsForm)