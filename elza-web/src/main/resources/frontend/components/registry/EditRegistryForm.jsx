/**
 * Formulář editace rejstříkového hesla
 * <EditRegistryForm create onSubmit={this.handleCallEditRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Scope, DropDownTree} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryList'

const validate = (values, props) => {
    const errors = {};
    if (props.create && !values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
    }

    if (props.create && !values.characteristics) {
        errors.characteristics = i18n('global.validation.required');
    }

    if (props.create && !values.scopeId) {
        errors.scopeId = i18n('global.validation.required');
    }

    if (props.create && !values.registerTypeId) {
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
        const {fields: { nameMain, characteristics, registerTypeId, scopeId}, handleSubmit, onClose} = this.props;
        var disabled = false;
        if (this.props.parentRegisterTypeId){
            registerTypeId.value=this.props.parentRegisterTypeId;
        }
        if (this.props.parentRecordId){
            disabled = true;
        }

        var itemsForDropDownTree = [];
        if (this.props.registryRecordTypes.item) {
            itemsForDropDownTree = this.props.registryRecordTypes.item;
        }

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <Scope versionId={null} label={i18n('registry.scope.class')}  {...scopeId} {...decorateFormField(scopeId)}/>
                        <DropDownTree
                            label={i18n('registry.add.typ.rejstriku')}
                            items = {itemsForDropDownTree}
                            addRegistryRecord={true}
                            {...registerTypeId}
                            {...decorateFormField(registerTypeId)}
                            disabled={disabled}
                            />
                        <Input type="text" label={i18n('registry.name')} {...nameMain} {...decorateFormField(nameMain)} />
                        <Input type="textarea" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)} />

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
        fields: ['nameMain', 'characteristics', 'registerTypeId', 'scopeId'],
        validate
    },state => ({
        initialValues: state.form.editRegistryForm.initialValues,
        refTables: state.refTables,
        registryRecordTypes: state.registryRecordTypes
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'editRegistryForm', data})}
)(EditRegistryForm)



