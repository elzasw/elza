/**
 * Formulář editace rejstříkového hesla
 * <EditRegistryForm create onSubmit={this.handleCallEditRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'

const validate = (values, props) => {
    const errors = {};
    if (props.create && !values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
    }
    return errors;
};

var EditRegistryForm = class EditRegistryForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.props.load(props.initData);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {fields: { nameMain, characteristics}, handleSubmit, onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <Input type="text" label={i18n('registry.name')} {...nameMain} {...decorateFormField(nameMain)} />
                        <Input type="text" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)} />

                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
        form: 'editRegistryForm',
        fields: ['nameMain', 'characteristics'],
        validate
    },state => ({
        initialValues: state.form.editRegistryForm.initialValues,
        refTables: state.refTables
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'editRegistryForm', data})}
)(EditRegistryForm)



