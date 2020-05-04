import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx';
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRecordTypes.jsx';
import {getTreeItemById} from './registryUtils';

/**
 * Formulář editace rejstříkového hesla
 * <EditRegistryForm create onSubmit={this.handleCallEditRegistry} />
 */

class EditRegistryForm extends AbstractReactComponent {
    static validate = (values, props) => {
        const errors = {};

        if (!values.typeId) {
            errors.typeId = i18n('global.validation.required');
        }
        return errors;
    };

    state = {};

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(getRegistryRecordTypesIfNeeded());
    }

    componentDidMount() {
        if (this.props.initData) {
            this.props.load(this.props.initData);
        }
        this.props.dispatch(getRegistryRecordTypesIfNeeded());
    }

    submitReduxForm = (values, dispatch) =>
        submitForm(EditRegistryForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {
            fields: {typeId},
            handleSubmit,
            onClose,
            registryRegionRecordTypes,
            submitting,
        } = this.props;

        const items = registryRegionRecordTypes.item != null ? registryRegionRecordTypes.item : [];

        const value = getTreeItemById(typeId ? typeId.value : '', items);

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    <Autocomplete
                        label={i18n('registry.update.type')}
                        items={items}
                        tree
                        alwaysExpanded
                        allowSelectItem={item => item.addRecord}
                        {...typeId}
                        {...decorateFormField(typeId)}
                        onChange={item => typeId.onChange(item ? item.id : null)}
                        onBlur={item => typeId.onBlur(item ? item.id : null)}
                        value={value}
                        disabled={false}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n('global.action.store')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm(
    {
        form: 'editRegistryForm',
        fields: ['typeId'],
    },
    state => ({
        initialValues: state.form.editRegistryForm.initialValues,
        refTables: state.refTables,
        registryRegionRecordTypes: state.registryRegionRecordTypes,
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'editRegistryForm', data})},
)(EditRegistryForm);
