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
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRegionList'

const validate = (values, props) => {
    const errors = {};
    console.log(props);
    console.log(values);
    if (!values.record) {
        errors.record = i18n('global.validation.required');
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
        const {fields: { record, characteristics, registerTypeId}, handleSubmit, onClose, initData, registryRegionRecordTypes, parentRecordId} = this.props;

        const submitForm = submitReduxForm.bind(this, validate);

        const itemsForDropDownTree = registryRegionRecordTypes.item != null ? registryRegionRecordTypes.item : [];

        const registerTypesIdValue = initData.registerTypeId && !registerTypeId.value ? initData.registerTypeId : registerTypeId.value;
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <DropDownTree
                            label={i18n('registry.update.type')}
                            items = {itemsForDropDownTree}
                            addRegistryRecord={true}
                            {...registerTypeId}
                            {...decorateFormField(registerTypeId)}
                            value={registerTypesIdValue}
                            disabled={parentRecordId != null}
                        />
                        <Input type="text" label={i18n('registry.name')} {...record} {...decorateFormField(record)}/>
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
};

module.exports = reduxForm({
        form: 'editRegistryForm',
        fields: ['record', 'characteristics', 'registerTypeId']
    },state => ({
        initialValues: state.form.editRegistryForm.initialValues,
        refTables: state.refTables,
        registryRegionRecordTypes: state.registryRegionRecordTypes
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'editRegistryForm', data})}
)(EditRegistryForm);



