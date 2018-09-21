import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxFormWithProp} from 'components/form/FormUtils.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRecordTypes.jsx'
import {WebApi} from 'actions/index.jsx';
import {getTreeItemById} from "./registryUtils";
import Scope from "../shared/scope/Scope";
import StoreSuggestField from "../../shared/field/StoreSuggestField";
import LanguageCodeField from "../LanguageCodeField";

/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm onSubmit={this.handleCallAddRegistry} />
 */
class AddRegistryForm extends AbstractReactComponent {
    static validate = (values, props) => {
        const errors = {};
        if (!values.name) {
            errors.name = i18n('global.validation.required');
        }
        if (!values.description) {
            errors.description = i18n('global.validation.required');
        }
        if (!values.scopeId) {
            errors.scopeId = i18n('global.validation.required');
        }

        if (!values.typeId) {
            errors.typeId = i18n('global.validation.required');
        }

        return errors;
    };

    static PropTypes = {
        versionId: React.PropTypes.number,
        showSubmitTypes: React.PropTypes.bool.isRequired
    };

    static defaultProps = {
        versionId: -1
    };

    state = {
        disabled: false
    };

    componentWillReceiveProps(nextProps) {
        this.dispatch(getRegistryRecordTypesIfNeeded());
        this.prepareState(nextProps);
    }

    componentDidMount() {
        this.dispatch(getRegistryRecordTypesIfNeeded());
        this.prepareState(this.props);
    }

    prepareState = (props) => {
        const {fields: {typeId}, registryList:{filter:{registryTypeId}}, registryRegionRecordTypes} = props;

        // Pokud není nastaven typ rejstříku, pokusíme se ho nastavit
        if (!typeId || typeId.value === "") {
            //  může se editovat výběr rejstříku editovat
            this.setState({disabled: false});
            if(registryTypeId && this.isValueUseable(registryRegionRecordTypes.item, registryTypeId)) {
                 // pokud o vybrání nějaké položky, která je uvedena v registryRegion.registryTypesId
                this.props.load({typeId: registryTypeId});
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
        const {fields: {name, description, complement, languageCode, typeId, scopeId}, handleSubmit, onClose, versionId, refTables: {scopesData}, submitting, registryRegionRecordTypes} = this.props;

        const okSubmitForm = submitReduxFormWithProp.bind(this, AddRegistryForm.validate, 'store');
        const okAndDetailSubmitForm = submitReduxFormWithProp.bind(this, AddRegistryForm.validate, 'storeAndViewDetail');
        const items = registryRegionRecordTypes.item ? registryRegionRecordTypes.item : [];

        let scopeIdValue = scopeId.value;
        if (!scopeId.value) {
            let index = scopesData.scopes ? indexById(scopesData.scopes, versionId, 'versionId') : false;
            if (index && scopesData.scopes[index].scopes) {
                scopeIdValue = scopesData.scopes[index].scopes[0].id;
            }
        }

        const value = getTreeItemById(typeId ? typeId.value : "", items);

        return (
            <div key={this.props.key}>
                <Form onSubmit={handleSubmit(okSubmitForm)}>
                    <Modal.Body>
                        <Scope disabled={this.state.disabled} versionId={versionId} label={i18n('registry.scopeClass')} {...scopeId} value={scopeIdValue} {...decorateFormField(scopeId)}/>
                        <Autocomplete
                            label={i18n('registry.add.type')}
                            items={items}
                            tree
                            alwaysExpanded
                            allowSelectItem={(item) => item.addRecord}
                            {...typeId}
                            {...decorateFormField(typeId)}
                            onChange={item => typeId.onChange(item ? item.id : null)}
                            onBlur={item => typeId.onBlur(item ? item.id : null)}
                            value={value}
                            disabled={this.state.disabled}
                            />
                        <FormInput type="text" label={i18n('registry.name')} {...name} {...decorateFormField(name)}/>
                        <FormInput type="text" label={i18n('accesspoint.complement')} {...complement} {...decorateFormField(complement)}/>
                        <LanguageCodeField label={i18n('accesspoint.languageCode')} {...languageCode} {...decorateFormField(languageCode)} />
                        <FormInput componentClass="textarea" label={i18n('accesspoint.description')} {...description} {...decorateFormField(description)} />
                    </Modal.Body>
                    <Modal.Footer>
                        {this.props.showSubmitTypes && <Button onClick={handleSubmit(okAndDetailSubmitForm)} disabled={submitting}>{i18n('global.action.storeAndViewDetail')}</Button>}
                        <Button type="submit" onClick={handleSubmit(okSubmitForm)} disabled={submitting}>{i18n('global.action.store')}</Button>
                        <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }
}
export default reduxForm({
    form: 'addRegistryForm',
    fields: ['name', 'complement', 'languageCode', 'description', 'typeId', 'scopeId'],
},state => ({
    initialValues: state.form.addRegistryForm.initialValues,
    refTables: state.refTables,
    registryList: state.app.registryList,
    registryRegionRecordTypes: state.registryRegionRecordTypes
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addRegistryForm', data})}
)(AddRegistryForm);
