/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm onSubmit={this.handleCallAddRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';

import {AbstractReactComponent, i18n, DropDownTree, Scope} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm, submitReduxFormWithProp} from 'components/form/FormUtils'
import {getRegistryRecordTypesIfNeeded, getRegistry} from 'actions/registry/registryRegionList'
import {WebApi} from 'actions'

const validate = (values, props) => {
    const errors = {};
    if (!values.record) {
        errors.record = i18n('global.validation.required');
    }

    if (!values.characteristics) {
        errors.characteristics = i18n('global.validation.required');
    }

    if (!values.scopeId) {
        errors.scopeId = i18n('global.validation.required');
    }

    if (!values.registerTypeId) {
        errors.registerTypeId = i18n('global.validation.required');
    }

    return errors;
};

var AddRegistryForm = class AddRegistryForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {
            disabled: false
        };

    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(getRegistryRecordTypesIfNeeded());
    }

    componentDidMount() {
        /*if (this.props.initData) {
            this.props.load(this.props.initData);
        }/**/

        this.dispatch(getRegistryRecordTypesIfNeeded());
        this.prepareState();
    }

    prepareState(props = this.props){
        const {parentRecordId, registryRegion, registryRegionRecordTypes} = props;
        if (parentRecordId !== null) {
            this.setState({disabled: true});
            WebApi.getRegistry(parentRecordId).then(json => {
                this.props.load(json);
            });
        } else {
            this.setState({disabled: false});
            if (registryRegion.registryTypesId && this.isValueUseable(registryRegionRecordTypes.item, registryRegion.registryTypesId)){
                this.props.load({registerTypeId: registryRegion.registryTypesId});
            }
        }

    }

    isValueUseable(items, value) {
        if (!items) {
            return null;
        }
        const index = indexById(items, value, "id");
        if (index !== null) {
            return items[index]['addRecord'];
        } else {
            let neededValue = null;
            items.map(
                (val) => {
                    if (neededValue === null && val['children']) {
                        neededValue = this.isValueUseable(val['children'], value);
                    }
                }
            );

            return neededValue;
        }
    }

    render() {
        const {fields: {record, characteristics, registerTypeId, scopeId}, handleSubmit, onClose, versionId, refTables: {scopesData}, registryRegionRecordTypes, registryRegion} = this.props;

        const okSubmitForm = submitReduxFormWithProp.bind(this, validate, 'store');
        const okAndDetailSubmitForm = submitReduxFormWithProp.bind(this, validate, 'storeAndViewDetail');

        const itemsForDropDownTree = registryRegionRecordTypes.item ? registryRegionRecordTypes.item : [];


        var registerTypesIdValue = registerTypeId.value;
        if (registryRegion.registryTypesId && !registerTypeId.value && this.isValueUseable(registryRegionRecordTypes.item, registryRegion.registryTypesId)){
            registerTypesIdValue = registryRegion.registryTypesId;
        }

        var scopeIdValue = scopeId.value;
        if (!scopeId.value) {
            let index = scopesData.scopes ? indexById(scopesData.scopes, versionId, 'versionId') : false;
            if (index && scopesData.scopes[index].scopes) {
                scopeIdValue = scopesData.scopes[index].scopes[0].id;
            }
        }

        return (
            <div key={this.props.key}>
                <Modal.Body>
                    <form onSubmit={handleSubmit(okSubmitForm)}>
                        <Scope disabled={this.state.disabled} versionId={versionId} label={i18n('registry.scopeClass')} {...scopeId} value={scopeIdValue} {...decorateFormField(scopeId)}/>
                        <DropDownTree
                            label={i18n('registry.add.type')}
                            items = {itemsForDropDownTree}
                            addRegistryRecord={true}
                            {...registerTypeId}
                            {...decorateFormField(registerTypeId)}
                            value={registerTypesIdValue}
                            disabled={this.state.disabled}
                            />
                        <Input type="text" label={i18n('registry.name')} {...record} {...decorateFormField(record)}/>
                        <Input type="textarea" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)} />
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    {this.props.showSubmitTypes && <Button onClick={handleSubmit(okAndDetailSubmitForm)}>{i18n('global.action.storeAndViewDetail')}</Button>}
                    <Button onClick={handleSubmit(okSubmitForm)}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'addRegistryForm',
    fields: ['record', 'characteristics', 'registerTypeId', 'scopeId'],
},state => ({
        initialValues: state.form.addRegistryForm.initialValues,
        refTables: state.refTables,
        registryRegion: state.registryRegion,
        registryRegionRecordTypes: state.registryRegionRecordTypes

}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addRegistryForm', data})}
)(AddRegistryForm);




