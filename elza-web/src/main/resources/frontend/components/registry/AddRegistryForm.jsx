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
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils'
import {getRegistryRecordTypesIfNeeded, getRegistry} from 'actions/registry/registryRegionList'
import {WebApi} from 'actions'

const validate = (values, props) => {
    const errors = {};
    console.log('Moje props', props);
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
            parentRegisterTypeId: null,
            scopeId: null,
            fetched: this.props.parentRecordId === null
        };

    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(getRegistryRecordTypesIfNeeded());
        this.prepareState(nextProps);

    }

    componentDidMount() {
        if (this.props.initData) {
            this.props.load(this.props.initData);
        }
        this.dispatch(getRegistryRecordTypesIfNeeded());
        this.prepareState(this.props);
    }

    prepareState(props){
        if (props.parentRecordId !== null){
            this.setState({fetched: false});
            WebApi.getRegistry(props.parentRecordId).then(json => {
                this.setState({parentRegisterTypeId: json.registerTypeId, scopeId: json.scopeId, fetched: true});
            });
        }
    }

    render() {
        const {fields: { nameMain, characteristics, registerTypeId, scopeId}, handleSubmit, onClose} = this.props;

        var submitForm = submitReduxForm.bind(this, validate)

        var itemsForDropDownTree = [];
        if (this.props.registryRegionRecordTypes.item) {
            itemsForDropDownTree = this.props.registryRegionRecordTypes.item;
        }
        var disabled = false;
        if (this.state.parentRegisterTypeId){
            registerTypeId.value=this.state.parentRegisterTypeId;
            scopeId.value=this.state.scopeId;
        }

        if (this.props.parentRecordId || this.state.fetched === false){
            disabled = true;
        }

        if (disabled===false && (scopeId.value === undefined || scopeId.value===null)){

            var scopesData = [];
            this.props.refTables.scopesData.scopes.map(scope => {
                if (scope.versionId === null) {
                    scopesData = scope.data ? scope.data : [];
                }
            });
            if (scopesData.length) {

                scopeId.value = scopesData[0].id;
            }
        }

        return (
            <div key={this.props.key}>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <Scope disabled={disabled} versionId={this.props.versionId} label={i18n('registry.scope.class')}  {...scopeId} {...decorateFormField(scopeId)}/>
                        <DropDownTree
                            label={i18n('registry.add.typ.rejstriku')}
                            items = {itemsForDropDownTree}
                            addRegistryRecord={true}
                            {...registerTypeId}
                            {...decorateFormField(registerTypeId)}

                            disabled={disabled}
                            />
                        <Input type="text" label={i18n('registry.name')} {...nameMain} {...decorateFormField(nameMain)}/>
                        <Input type="textarea" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)} />
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




