import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {Modal, Button, Checkbox, Form} from 'react-bootstrap';
import {indexById, objectById} from 'stores/app/utils.jsx'
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import {visiblePolicyFetchIfNeeded} from 'actions/arr/visiblePolicy.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'

/**
 * Validace formuláře.
 * @todo šlapa odstranit
 */
const validate = (values, props) => {
    const errors = {};
    return errors;
};

class VisiblePolicyForm extends AbstractReactComponent {
    state = {};

    componentDidMount() {
        this.loadVisiblePolicy();
    }

    componentWillReceiveProps(nextProps) {
        this.loadVisiblePolicy();
    }

    loadVisiblePolicy = () => {
        const {nodeId, fundVersionId} = this.props;
        this.dispatch(visiblePolicyFetchIfNeeded(nodeId, fundVersionId));
    };

    handleResetVisiblePolicy = () => {
        if(confirm(i18n('visiblePolicy.action.reset.confirm'))) {
            this.props.onSubmitForm({records: []});
            this.dispatch(modalDialogHide());
        }
    };

    submitReduxForm = (values, dispatch) => submitForm(validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {fields: {records}, handleSubmit, onClose, nodeId, fundVersionId, visiblePolicy, visiblePolicyTypes, arrRegion} = this.props;

        let activeFund = null;
        if (arrRegion.activeIndex != null) {
            activeFund = arrRegion.funds[arrRegion.activeIndex];
        }

        let visiblePolicyTypeItems;

        if (activeFund == null) {
            visiblePolicyTypeItems = visiblePolicyTypes;
        } else {
            let activeVersion = activeFund.activeVersion;
            visiblePolicyTypeItems = {};

            for(let id in visiblePolicyTypes.items) {
                let item = visiblePolicyTypes.items[id];
                if (activeVersion.ruleSetId === item.ruleSetId) {
                    visiblePolicyTypeItems[id] = item;
                }
            }
        }

        return (
            <div>
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        {records.map((val, index) =>
                            <div key={index}>
                                <Checkbox {...val.checked} value={true}>{visiblePolicyTypeItems[val.id.initialValue].name}</Checkbox>
                            </div>
                        )}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit">{i18n('visiblePolicy.action.save')}</Button>
                        <Button onClick={this.handleResetVisiblePolicy}>{i18n('visiblePolicy.action.reset')}</Button>
                        <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }
}

export default reduxForm({
    form: 'visiblePolicyForm',
    fields: ['records[].id', 'records[].checked']
}, state => ({
    initialValues: {records: state.arrRegion.visiblePolicy.data},
    visiblePolicy: state.arrRegion.visiblePolicy,
    visiblePolicyTypes: state.refTables.visiblePolicyTypes,
    arrRegion: state.arrRegion
}))(VisiblePolicyForm)