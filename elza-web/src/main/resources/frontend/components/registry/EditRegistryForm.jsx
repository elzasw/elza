/**
 * Formulář editace rejstříkového hesla
 * <EditRegistryForm create onSubmit={this.handleCallEditRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, DropDownTree} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRegionList'

const validate = (values, props) => {
    const errors = {};
    if (!values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
    }

    if (!values.characteristics) {
        errors.characteristics = i18n('global.validation.required');
    }


    if (!values.registerTypeId) {
        errors.registerTypeId = i18n('global.validation.required');
    }
    return errors;
};

var EditRegistryForm = class EditRegistryForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(getRegistryRecordTypesIfNeeded());
    }

    componentDidMount() {
        if (this.props.initData) {
            this.props.load(this.props.initData);
        }
        this.dispatch(getRegistryRecordTypesIfNeeded());
    }


    render() {
        const {fields: { nameMain, characteristics, registerTypeId}, handleSubmit, onClose} = this.props;

        var submitForm = submitReduxForm.bind(this, validate)

        var disabled = false;

        if (this.props.parentRecordId){
            disabled = true;
        }

        var itemsForDropDownTree = [];
        if (this.props.registryRegionRecordTypes.item) {
            itemsForDropDownTree = this.props.registryRegionRecordTypes.item;
        }

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <DropDownTree
                            label={i18n('registry.add.typ.rejstriku')}
                            items = {itemsForDropDownTree}
                            addRegistryRecord={true}
                            {...registerTypeId}
                            {...decorateFormField(registerTypeId)}
                            disabled={disabled}
                            />
                        <Input type="text" label={i18n('registry.name')} {...nameMain} {...decorateFormField(nameMain)}/>
                        <Input type="textarea" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)}/>

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

module.exports = reduxForm({
        form: 'editRegistryForm',
        fields: ['nameMain', 'characteristics', 'registerTypeId'],
    },state => ({
        initialValues: state.form.editRegistryForm.initialValues,
        refTables: state.refTables,
        registryRegionRecordTypes: state.registryRegionRecordTypes
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'editRegistryForm', data})}
)(EditRegistryForm)



