/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm create onSubmit={this.handleCallAddRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, DropDownTree} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'

const validate = (values, props) => {
    const errors = {};
    if (props.create && !values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
    }

    if (props.create && !values.characteristics) {
        errors.characteristics = i18n('global.validation.required');
    }
    return errors;
};

var AddRegistryForm = class AddRegistryForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        console.log(props, this.props);


        this.props.load(props.initData);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }


    render() {
        const {fields: { nameMain, characteristics}, handleSubmit, onClose} = this.props;

        return (
            <div key={this.props.key}>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <DropDownTree
                            items = {this.props.refTables.recordTypes.items}
                            selectedItemID = {this.props.registry.registryTypesId}

                            />

                        <Input type="text" label={i18n('registry.name')} {...nameMain} {...decorateFormField(nameMain)} />
                        <Input type="textarea" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)} />
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit}>{i18n('global.action.create')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'addRegistryForm',
    fields: ['nameMain', 'characteristics'],
    validate
},state => ({
        initialValues: state.form.addRegistryForm.initialValues,
        refTables: state.refTables,
        //registry: state.registry

}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addRegistryForm', data})}
)(AddRegistryForm)



