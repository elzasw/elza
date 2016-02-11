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
                //this.setState({parentRegisterTypeId: json.registerTypeId, scopeId: json.scopeId, fetched: true});
                this.props.load({registerTypeId: json.registerTypeId, scopeId: json.scopeId});
            });
        } else {
            this.setState({disabled: false});
            var scopesData = [];
            props.refTables.scopesData.scopes.map(scope => {
                if (scope.versionId === null) {
                    scopesData = scope.scopes.data;
                }
            });
            if (scopesData.length) {
                this.props.load({scopeId: scopesData[0].id});
            }
        }

    }

    render() {
        const {fields: { nameMain, characteristics, registerTypeId, scopeId}, handleSubmit, onClose} = this.props;

        var submitForm = submitReduxForm.bind(this, validate)

        var itemsForDropDownTree = [];
        if (this.props.registryRegionRecordTypes.item) {
            itemsForDropDownTree = this.props.registryRegionRecordTypes.item;
        }

        return (
            <div key={this.props.key}>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <Scope disabled={this.state.disabled} versionId={this.props.versionId} label={i18n('registry.scope.class')}  {...scopeId} {...decorateFormField(scopeId)}/>
                        <DropDownTree
                            label={i18n('registry.add.typ.rejstriku')}
                            items = {itemsForDropDownTree}
                            addRegistryRecord={true}
                            {...registerTypeId}
                            {...decorateFormField(registerTypeId)}

                            disabled={this.state.disabled}
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




