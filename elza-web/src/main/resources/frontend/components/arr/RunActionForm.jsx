/**
 * Formulář přidání výstupu.
 */

import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {Modal, Button} from 'react-bootstrap';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {fundActionFetchConfigIfNeeded} from 'actions/arr/fundAction.jsx'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if (!values.code) {
        errors.code = i18n('global.validation.required');
    }

    return errors;
};

const RunActionForm = class RunActionForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(fundActionFetchConfigIfNeeded(nextProps.versionId));
    }

    componentDidMount() {
        this.dispatch(fundActionFetchConfigIfNeeded(this.props.versionId));
    }

    render() {
        const {fields: {code}, handleSubmit, onClose, actionConfig} = this.props;
        const submitForm = submitReduxForm.bind(this, validate)

        return (
            <div className="run-action-form-container">
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <FormInput componentClass="select"
                               label={i18n('arr.fundAction.form.type')}
                               key='code-action'
                               ref='code-action'
                               className='form-control'
                                {...code}
                                {...decorateFormField(code)}
                        >
                            <option key="novalue" value={null}/>
                            {actionConfig.map((item) => (<option key={item.code} value={item.code}>{item.name}</option>))}
                        </FormInput>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.run')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};

RunActionForm.propTypes = {
    initData: React.PropTypes.object,
    onSubmitForm: React.PropTypes.func.isRequired,
    versionId: React.PropTypes.number.isRequired
};

module.exports = reduxForm({
        form: 'RunActionForm',
        fields: ['code'],
    },(state, props) => {
        const {arrRegion: {funds, activeIndex}} = state;

        let actionConfig = null;
        if (activeIndex !== null && funds[activeIndex].fundAction) {
            const {fundAction: {config: {fetched, data}}} = funds[activeIndex];
            if (fetched) {
                actionConfig = data;
            }
        }
        return {
            actionConfig
        }
    },
    {}
)(RunActionForm);
