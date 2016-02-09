/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm create onSubmit={this.handleCallAddRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';

import {AbstractReactComponent, i18n, DropDownTree, Scope} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'
import {getRegistryRecordTypesIfNeeded, getRegistry} from 'actions/registry/registryList'


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

var AddRegistryForm = class AddRegistryForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {parentRegisterTypeId: null};
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(getRegistryRecordTypesIfNeeded());
    }

    componentDidMount() {
        if (this.props.initData) {
            this.props.load(this.props.initData);
        }
        this.dispatch(getRegistryRecordTypesIfNeeded());

        if (this.props.registryData.item.registerTypeId && this.props.registryData.item.recordId === this.props.parentRecordId){
            this.setState({parentRegisterTypeId: this.props.registryData.item.registerTypeId});
        }
        else if (!this.state.parentRegisterTypeId && this.props.parentRecordId){
            this.dispatch(getRegistry(this.props.parentRecordId)).then(json => {
                this.setState({parentRegisterTypeId: json.item.registerTypeId});
            });
        }

    }
    render() {
        const {fields: { nameMain, characteristics, registerTypeId, scopeId}, handleSubmit, onClose} = this.props;
        var itemsForDropDownTree = [];
        if (this.props.registryRecordTypes.item) {
            itemsForDropDownTree = this.props.registryRecordTypes.item;
        }
        var disabled = false;
        if (this.state.parentRegisterTypeId){
            registerTypeId.value=this.state.parentRegisterTypeId;
        }

        if (this.props.parentRecordId){
            disabled = true;
        }

        return (
            <div key={this.props.key}>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <Scope versionId={null} label={i18n('registry.scope.class')}  {...scopeId} {...decorateFormField(scopeId)} onBlur={false}/>
                        <DropDownTree
                            label={i18n('registry.add.typ.rejstriku')}
                            items = {itemsForDropDownTree}
                            addRegistryRecord={true}
                            {...registerTypeId}
                            {...decorateFormField(registerTypeId)}
                            disabled={disabled}
                            onBlur={false}
                            />
                        <Input type="text" label={i18n('registry.name')} {...nameMain} {...decorateFormField(nameMain)} onBlur={false}/>
                        <Input type="textarea" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)} onBlur={false} />
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
    fields: ['nameMain', 'characteristics', 'registerTypeId', 'scopeId'],
    validate
},state => ({
        initialValues: state.form.addRegistryForm.initialValues,
        refTables: state.refTables,
        registry: state.registry,
        registryData: state.registryData,
        registryRecordTypes: state.registryRecordTypes

}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addRegistryForm', data})}
)(AddRegistryForm)




