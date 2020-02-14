/**
 * Formulář přidání výstupu.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import {fundActionFetchConfigIfNeeded} from 'actions/arr/fundAction.jsx'
import {addToastrInfo,addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'

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

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(fundActionFetchConfigIfNeeded(nextProps.versionId));
    }

    componentDidMount() {
        this.props.dispatch(fundActionFetchConfigIfNeeded(this.props.versionId));
    }
    submitOptions = {
        closeOnSubmit:true
    }

    submitReduxForm = (values, dispatch) => submitForm(validate,values,this.props,this.props.onSubmitForm,dispatch,this.submitOptions);


    render() {
        const {fields: {code}, handleSubmit, onClose, actionConfig, submitting} = this.props;
        return (
            <div className="run-action-form-container">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
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
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" disabled={submitting}>{i18n('global.action.run')}</Button>
                        <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }
};

RunActionForm.propTypes = {
    initData: PropTypes.object,
    onSubmitForm: PropTypes.func.isRequired,
    versionId: PropTypes.number.isRequired
};

export default reduxForm({
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
