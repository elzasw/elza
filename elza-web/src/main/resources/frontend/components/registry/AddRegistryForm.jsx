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
import {decorateFormField, submitReduxForm, submitReduxFormWithProp} from 'components/form/FormUtils'
import {getRegistryRecordTypesIfNeeded, getRegistry} from 'actions/registry/registryRegionList'
import {WebApi} from 'actions'

const validate = (values, props) => {
    const errors = {};
    if (!values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
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

    componentWillReceiveProps(nextProps){
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
        if (props.parentRecordId !== null) {
            this.setState({disabled: true});
            WebApi.getRegistry(props.parentRecordId).then(json => {
                this.props.load({registerTypeId: json.registerTypeId, scopeId: json.scopeId});
            });
        } else {
            this.setState({disabled: false});
            var registerTypesId = null;
            if (this.isValueUseable(props.registryRegionRecordTypes.item, props.registryRegion.registryTypesId) && props.registryRegion.registryTypesId){
                registerTypesId = props.registryRegion.registryTypesId;
            }

            if (registerTypesId) {
                this.props.load({registerTypeId: registerTypesId});
            }
        }

    }

    isValueUseable(items, value) {
        if (!items){return null;}
        var index = indexById(items, value, "id");
        if (index != null) {
            return items[index]['addRecord'];
        }else{
            var neededValue = null;
            items.map(
                (val) => {
                    if (neededValue === null && val['children']){
                        neededValue = this.isValueUseable(val['children'], value);
                    }
                }
            );

            return neededValue;
        }
    }

    render() {
        const {fields: { nameMain, characteristics, registerTypeId, scopeId}, handleSubmit, onClose} = this.props;

        var okSubmitForm = submitReduxFormWithProp.bind(this, validate, 'store')
        var okAndDetailSubmitForm = submitReduxFormWithProp.bind(this, validate, 'storeAndViewDetail')

        var itemsForDropDownTree = [];
        if (this.props.registryRegionRecordTypes.item) {
            itemsForDropDownTree = this.props.registryRegionRecordTypes.item;
        }


        var registerTypesIdValue = null;
        if (this.isValueUseable(this.props.registryRegionRecordTypes.item, this.props.registryRegion.registryTypesId) && this.props.registryRegion.registryTypesId && !registerTypeId.value){
            registerTypesIdValue = this.props.registryRegion.registryTypesId;
        }
        else{
            registerTypesIdValue = registerTypeId.value;
        }

        var scopeIdValue = null;
        if (!scopeId.value){
            if (this.props.refTables.scopesData.scopes
                && this.props.refTables.scopesData.scopes[indexById(this.props.refTables.scopesData.scopes, this.props.versionId, 'versionId')]
                && this.props.refTables.scopesData.scopes[indexById(this.props.refTables.scopesData.scopes, this.props.versionId, 'versionId')].scopes
            ) {
                scopeIdValue = this.props.refTables.scopesData.scopes[indexById(this.props.refTables.scopesData.scopes, this.props.versionId, 'versionId')].scopes[0].id;
            }
        }else{
            scopeIdValue = scopeId.value
        }

        return (
            <div key={this.props.key}>
                <Modal.Body>
                    <form onSubmit={handleSubmit(okSubmitForm)}>
                        <Scope disabled={this.state.disabled} versionId={this.props.versionId} label={i18n('registry.scope.class')} {...scopeId} value={scopeIdValue} {...decorateFormField(scopeId)}/>
                        <DropDownTree
                            label={i18n('registry.add.typ.rejstriku')}
                            items = {itemsForDropDownTree}
                            addRegistryRecord={true}
                            {...registerTypeId}
                            {...decorateFormField(registerTypeId)}
                            value={registerTypesIdValue}
                            disabled={this.state.disabled}
                            />
                        <Input type="text" label={i18n('registry.name')} {...nameMain} {...decorateFormField(nameMain)}/>
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
    fields: ['nameMain', 'characteristics', 'registerTypeId', 'scopeId'],
},state => ({
        initialValues: state.form.addRegistryForm.initialValues,
        refTables: state.refTables,
        registryRegion: state.registryRegion,
        registryRegionData: state.registryRegionData,
        registryRegionRecordTypes: state.registryRegionRecordTypes

}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addRegistryForm', data})}
)(AddRegistryForm)




