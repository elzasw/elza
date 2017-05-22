import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'

const validate = (values, props) => {
    const errors = {};

    function isNormalInteger(str) {
        var n = ~~Number(str);
        return String(n) === str && n >= 0;
    }

    if (!values.position) {
        errors.position = i18n('arr.fund.subNodes.findPositionNumber.error.required', props.maxPosition);
    } else if (!isNormalInteger(values.position)) {
        errors.position = i18n('arr.fund.subNodes.findPositionNumber.error.type', props.maxPosition);
    } else if (values.position > props.maxPosition || values.position <= 0) {
        errors.position = i18n('arr.fund.subNodes.findPositionNumber.error.interval', props.maxPosition);
    }

    return errors;
}

var GoToPositionForm = class GoToPositionForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
    }

    submitOptions = {finishOnSubmit:true}

    submitReduxForm = (values, dispatch) => submitForm(validate,values,this.props,this.props.onSubmitForm,dispatch,this.submitOptions);

    render() {
        const {fields: {position}, handleSubmit, onClose, maxPosition} = this.props;

        return (
            <div>
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <FormInput type="text" label={i18n('arr.fund.subNodes.findPositionNumber', maxPosition)} {...position} {...decorateFormField(position)} />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit">{i18n('global.action.store')}</Button>
                        <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }
}

module.exports = connect()(GoToPositionForm)

export default reduxForm({
    form: 'goToPosition',
    fields: ['position']
})(GoToPositionForm);

