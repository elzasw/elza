import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, DropDownTree, FormInput} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRegionList.jsx'

/**
 * Formulář editace rejstříkového hesla
 * <EditRegistryForm create onSubmit={this.handleCallEditRegistry} />
 */

class EditRegistryForm extends AbstractReactComponent {
    static validate = (values, props) => {
        const errors = {};

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

    state = {};

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

        const submitForm = handleSubmit(submitReduxForm.bind(this, EditRegistryForm.validate));

        const itemsForDropDownTree = registryRegionRecordTypes.item != null ? registryRegionRecordTypes.item : [];

        const registerTypesIdValue = initData.registerTypeId && !registerTypeId.value ? initData.registerTypeId : registerTypeId.value;
        return <Form onSubmit={submitForm}>
            <Modal.Body>
                <DropDownTree
                    label={i18n('registry.update.type')}
                    items = {itemsForDropDownTree}
                    addRegistryRecord={true}
                    {...registerTypeId}
                    {...decorateFormField(registerTypeId)}
                    value={registerTypesIdValue}
                    disabled={parentRecordId != null}
                />
                <FormInput type="text" label={i18n('registry.name')} {...record} {...decorateFormField(record)}/>
                <FormInput componentClass="textarea" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)}/>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" onClick={submitForm}>{i18n('global.action.store')}</Button>
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
        form: 'editRegistryForm',
        fields: ['record', 'characteristics', 'registerTypeId']
    },state => ({
        initialValues: state.form.editRegistryForm.initialValues,
        refTables: state.refTables,
        registryRegionRecordTypes: state.registryRegionRecordTypes
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'editRegistryForm', data})}
)(EditRegistryForm);



