import React from 'react';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {reduxForm} from 'redux-form';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx';

const validate = (values, props) => {
    const errors = {};

    function isNormalInteger(str) {
        const n = ~~Number(str);
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
};

class GoToPositionForm extends AbstractReactComponent {
    state = {};

    submitOptions = {finishOnSubmit: true};

    submitReduxForm = (values, dispatch) => submitForm(validate, values, this.props, this.props.onSubmitForm, dispatch, this.submitOptions);

    render() {
        const {fields: {position}, handleSubmit, onClose, maxPosition} = this.props;

        return (
            <div>
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <FormInput type="text"
                                   label={i18n('arr.fund.subNodes.findPositionNumber', maxPosition)} {...position} {...decorateFormField(position)} />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit">{i18n('global.action.store')}</Button>
                        <Button variant="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

export default reduxForm({
    form: 'goToPosition',
    fields: ['position'],
})(GoToPositionForm);

