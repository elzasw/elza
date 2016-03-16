
import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils'

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

    render() {
        const {fields: {position}, handleSubmit, onClose, maxPosition} = this.props;

        var submitForm = submitReduxForm.bind(this, validate)

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <Input type="text" label={i18n('arr.fund.subNodes.findPositionNumber', maxPosition)} {...position} {...decorateFormField(position)} />
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = connect()(GoToPositionForm)

export default reduxForm({
    form: 'goToPosition',
    fields: ['position']
})(GoToPositionForm);

