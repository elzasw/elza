/**
 * Formulář přidání výstupu.
 */

import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {Modal, Button, Input} from 'react-bootstrap';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if (!values.name) {
        errors.name = i18n('global.validation.required');
    }
    if (!values.code) {
        errors.code = i18n('global.validation.required');
    }

    return errors;
};

var AddOutputForm = class AddOutputForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        // this.props.load(this.props.initData);
    }

    render() {
        const {fields: {name, code, temporary}, handleSubmit, onClose} = this.props;

        var submitForm = submitReduxForm.bind(this, validate)

        return (
            <div className="add-output-form-container">
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <Input type="text" label={i18n('arr.output.name')} {...name} {...decorateFormField(name)} />
                        <Input type="text" label={i18n('arr.output.code')} {...code} {...decorateFormField(code)} />
                        <Input type="checkbox" label={i18n('arr.output.temporary')} {...temporary} {...decorateFormField(temporary)} />
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.create')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
        form: 'addOutputForm',
        fields: ['name', 'code', 'temporary'],
    },state => ({
        initialValues: {temporary: false},
    }),
    {/*load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPacketForm', data})*/}
)(AddOutputForm)
