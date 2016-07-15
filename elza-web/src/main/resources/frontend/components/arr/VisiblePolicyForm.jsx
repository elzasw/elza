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

var VisiblePolicyForm = class VisiblePolicyForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleResetVisiblePolicy');
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        this.loadVisiblePolicy();
    }

    componentDidMount() {
        this.loadVisiblePolicy();
    }

    loadVisiblePolicy() {
        const {nodeId, fundVersionId} = this.props;
        this.dispatch(visiblePolicyFetchIfNeeded(nodeId, fundVersionId));
    }

    handleResetVisiblePolicy() {
        if(confirm(i18n('visiblePolicy.action.reset.confirm'))) {
            this.props.onSubmitForm({records: []});
        }
    }

    render() {
        const {fields: {records}, handleSubmit, onClose, nodeId, fundVersionId, visiblePolicy, visiblePolicyTypes} = this.props;
        var submitForm = submitReduxForm.bind(this, validate)

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        {records.map((val, index) =>
                            <div key={index}>
                                <Checkbox {...val.checked} value={true}>{visiblePolicyTypes.items[val.id.initialValue].name}</Checkbox>
                            </div>
                        )}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('visiblePolicy.action.save')}</Button>
                    <Button onClick={this.handleResetVisiblePolicy}>{i18n('visiblePolicy.action.reset')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'visiblePolicyForm',
    fields: ['records[].id', 'records[].checked']
}, state => ({
    initialValues: {records: state.arrRegion.visiblePolicy.data},
    visiblePolicy: state.arrRegion.visiblePolicy,
    visiblePolicyTypes: state.refTables.visiblePolicyTypes
}))(VisiblePolicyForm)