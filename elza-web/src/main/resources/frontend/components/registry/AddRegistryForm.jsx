import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n, Scope, FormInput} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxFormWithProp} from 'components/form/FormUtils.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registry.jsx'
import {WebApi} from 'actions/index.jsx';
import {getTreeItemById} from "./registryUtils";

/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm onSubmit={this.handleCallAddRegistry} />
 */
class AddRegistryForm extends AbstractReactComponent {
    static validate = (values, props) => {
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
        console.log(this.props.initialValues);
        this.prepareState(this.props);
    }

    prepareState = (props) => {
        const {fields: {registerTypeId}, parentRecordId, registryList:{filter:{registryTypeId}}, registryRegionRecordTypes} = props;

        // Pokud není nastaven typ rejstříku, pokusíme se ho nastavit
        if (!registerTypeId || registerTypeId.value === "") {
            // Pokud je předán parentRecordId, přednačte se do prvku výběr rejstříku a tento prvek se nastaví jako disabled
            if (parentRecordId !== null) {
                if (!this.state.disabled) {
                    WebApi.getRegistry(parentRecordId).then(json => {
                        this.props.load({registerTypeId: json.registerTypeId, scopeId: json.scopeId});
                    });
                    this.setState({disabled: true});
                }
            } else {    //  pokud není předán parentRecordId, může se výběr rejstříku editovat
                this.setState({disabled: false});
                if (registryTypeId && this.isValueUseable(registryRegionRecordTypes.item, registryTypeId)){ // pokud o vybrání nějaké položky, která je uvedena v registryRegion.registryTypesId
                    this.props.load({registerTypeId: registryTypeId});
                }
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
        const {fields: {record, characteristics, registerTypeId, scopeId}, handleSubmit, onClose, versionId, refTables: {scopesData}, submitting, registryRegionRecordTypes, registryRegion} = this.props;

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

        const value = getTreeItemById(registerTypeId ? registerTypeId.value : "", items);

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
                            allowSelectItem={(id, item) => item.addRecord}
                            {...registerTypeId}
                            {...decorateFormField(registerTypeId)}
                            onChange={item => registerTypeId.onChange(item ? item.id : null)}
                            onBlur={item => registerTypeId.onBlur(item ? item.id : null)}
                            value={value}
                            disabled={this.state.disabled}
                            />
                        <FormInput type="text" label={i18n('registry.name')} {...record} {...decorateFormField(record)}/>
                        <FormInput componentClass="textarea" label={i18n('registry.characteristics')} {...characteristics} {...decorateFormField(characteristics)} />
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
    fields: ['record', 'characteristics', 'registerTypeId', 'scopeId'],
},state => ({
    initialValues: state.form.addRegistryForm.initialValues,
    refTables: state.refTables,
    registryList: state.app.registryList,
    registryRegionRecordTypes: state.registryRegionRecordTypes
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addRegistryForm', data})}
)(AddRegistryForm);




