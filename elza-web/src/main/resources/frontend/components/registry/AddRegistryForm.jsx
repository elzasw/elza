/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm create onSubmit={this.handleCallAddRegistry} />
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, DropDownTree} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'
import {requestScopesIfNeeded} from 'actions/scopes/scopesData'

const validate = (values, props) => {
    const errors = {};
    if (props.create && !values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
    }

    if (props.create && !values.characteristics) {
        errors.characteristics = i18n('global.validation.required');
    }
    return errors;
};

var AddRegistryForm = class AddRegistryForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(requestScopesIfNeeded());
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.dispatch(refRecordTypesFetchIfNeeded());
        this.dispatch(requestScopesIfNeeded());
        if (this.props.initData) {
            this.props.load(this.props.initData);
        }
    }

    render() {
        const {fields: { nameMain, characteristics, registerTypeId}, handleSubmit, onClose} = this.props;

console.log(this.props.refTables);
        return (
            <div key={this.props.key}>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <DropDownTree
                            label={i18n('registry.detail.typ.rejstriku')}
                            items = {this.props.refTables.recordTypes.items}
                            //value = {this.props.registry.registryTypesId}
                            {...registerTypeId}
                            onSelect={registerTypeId.onChange}
                            />

                        <Input type="text" label={i18n('registry.name')} {...nameMain} {...decorateFormField(nameMain)} />
                        <Input type="textarea" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)} />
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
    fields: ['nameMain', 'characteristics', 'registerTypeId'],
    validate
},state => ({
        initialValues: state.form.addRegistryForm.initialValues,
        refTables: state.refTables,
        registry: state.registry

}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addRegistryForm', data})}
)(AddRegistryForm)



