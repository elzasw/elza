import PropTypes from 'prop-types';
import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, FormInput, i18n, Icon, NoFocusButton} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField, submitReduxFormWithProp} from 'components/form/FormUtils.jsx';
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRecordTypes.jsx';
import {WebApi} from 'actions/index.jsx';
import {getTreeItemById} from './registryUtils';
import Scope from '../shared/scope/Scope';
import LanguageCodeField from '../LanguageCodeField';
import ApItemNameForm from '../accesspoint/ApItemNameForm';
import {accessPointFormActions} from '../accesspoint/AccessPointFormActions';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog';
import AddDescItemTypeForm from '../arr/nodeForm/AddDescItemTypeForm';

/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm onSubmit={this.handleCallAddRegistry} />
 */
class AddRegistryForm extends AbstractReactComponent {
    static validate = (values, props) => {
        const errors = {};
        const structured = props.fields.structured && props.fields.structured.value;
        if (!structured) {
            if (!values.name) {
                errors.name = i18n('global.validation.required');
            }
            if (!values.description) {
                errors.description = i18n('global.validation.required');
            }
        }
        if (!values.scopeId) {
            errors.scopeId = i18n('global.validation.required');
        }

        if (!values.typeId) {
            errors.typeId = i18n('global.validation.required');
        }

        return errors;
    };

    static propTypes = {
        versionId: PropTypes.number,
        showSubmitTypes: PropTypes.bool.isRequired,
    };

    static defaultProps = {
        versionId: -1,
    };

    state = {
        disabled: false,
        ruleSystemId: null,
        accessPointId: null,
        step: 1,
        working: false,
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(getRegistryRecordTypesIfNeeded());
        this.prepareState(nextProps);
    }

    componentDidMount() {
        this.props.dispatch(getRegistryRecordTypesIfNeeded());
        this.prepareState(this.props);
    }

    prepareState = (props) => {
        const {fields: {typeId}, registryList: {filter: {registryTypeId}}, registryRegionRecordTypes, refTables: {recordTypes: {typeIdMap}}} = props;

        // Pokud není nastaven typ rejstříku, pokusíme se ho nastavit
        if (!typeId || typeId.value === '') {
            //  může se editovat výběr rejstříku editovat
            this.setState({disabled: false});
            if (registryTypeId && this.isValueUseable(registryRegionRecordTypes.item, registryTypeId)) {
                // pokud o vybrání nějaké položky, která je uvedena v registryRegion.registryTypesId
                const type = typeIdMap[registryTypeId]; // TODO React 16 check
                this.props.load({typeId: registryTypeId, structured: type && type.ruleSystemId != null});
            }
        }
    };

    isValueUseable(items, value) {
        if (!items) {
            return null;
        }
        const index = indexById(items, value, 'id');
        if (index !== null) {
            return items[index]['addRecord'];
        } else {
            let neededValue = null;
            items.map(
                (val) => {
                    if (neededValue === null && val['children']) {
                        neededValue = this.isValueUseable(val['children'], value);
                    }
                },
            );
            return neededValue;
        }
    }

    nextStep = () => {
        const {values, touchAll, fields: {id, structuredObj}} = this.props;
        const errors = AddRegistryForm.validate(values, this.props);
        if (Object.keys(errors).length > 0) {
            touchAll();
            return;
        }
        if (this.state.working) {
            return;
        }
        this.setState({working: true});
        WebApi.createStructuredAccessPoint(values.name, values.complement, values.languageCode, values.description, values.typeId, values.scopeId).then((data) => {
            id.onChange(data.id);
            structuredObj.onChange(data);
            this.setState({step: 2, working: false, data});
        });
    };

    add = () => {
        const {nameItemForm} = this.props;


        const formData = nameItemForm.formData;
        const itemTypes = [];
        const strictMode = true;

        let infoTypesMap = new Map(nameItemForm.infoTypesMap);

        formData.itemTypes.forEach(descItemType => {
            infoTypesMap.delete(descItemType.id);
        });

        nameItemForm.refTypesMap.forEach(refType => {
            if (infoTypesMap.has(refType.id)) {    // ještě ji na formuláři nemáme
                const infoType = infoTypesMap.get(refType.id);
                // v nestriktním modu přidáváme všechny jinak jen možné
                if (!strictMode || infoType.type !== 'IMPOSSIBLE') {
                    // nový item type na základě původního z refTables
                    itemTypes.push(refType);
                }
            }
        });

        const descItemTypes = [
            {
                groupItem: true,
                id: 'DEFAULT',
                name: i18n('subNodeForm.descItemGroup.default'),
                children: itemTypes,
            },
        ];

        const submit = (data) => {
            this.props.dispatch(modalDialogHide());
            this.props.dispatch(accessPointFormActions.fundSubNodeFormDescItemTypeAdd(data.descItemTypeId.id));
        };

        // Modální dialog
        this.props.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), <AddDescItemTypeForm
            descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>));
    };

    onBack = () => {
        const {fields: {id, structuredObj}} = this.props;
        if (this.state.step === 2) {
            WebApi.deleteAccessPoint(this.state.data.id).then(() => {
                this.setState({step: 1});
                id.onChange(null);
                structuredObj.onChange(null);
            });
        }
    };

    onClose = () => {
        const {onClose} = this.props;
        if (this.state.step === 2) {
            WebApi.deleteAccessPoint(this.state.data.id);
        }
        onClose && onClose();
    };

    render() {
        const {fields: {name, description, complement, languageCode, typeId, scopeId, structured}, handleSubmit, versionId, refTables: {scopesData}, submitting, registryRegionRecordTypes} = this.props;

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

        const value = getTreeItemById(typeId ? typeId.value : '', items);

        const isStructured = structured && structured.value;

        return (
            <div key={this.props.key}>
                <Form onSubmit={handleSubmit(okSubmitForm)}>
                    {this.state.step === 1 && <Modal.Body>
                        <Scope disabled={this.state.disabled} versionId={versionId}
                               label={i18n('registry.scopeClass')} {...scopeId}
                               value={scopeIdValue} {...decorateFormField(scopeId)}/>
                        <Autocomplete
                            label={i18n('registry.add.type')}
                            items={items}
                            tree
                            alwaysExpanded
                            allowSelectItem={(item) => item.addRecord}
                            {...typeId}
                            {...decorateFormField(typeId)}
                            onChange={item => {
                                typeId.onChange(item ? item.id : null);
                                structured.onChange(item && item.ruleSystemId != null);
                            }}
                            onBlur={item => {
                                typeId.onBlur(item ? item.id : null);
                                structured.onBlur(item && item.ruleSystemId != null);
                            }}
                            value={value}
                            disabled={this.state.disabled}
                        />
                        {!isStructured &&
                        <FormInput type="text" label={i18n('registry.name')} {...name} {...decorateFormField(name)}/>}
                        {!isStructured && <FormInput type="text"
                                                     label={i18n('accesspoint.complement')} {...complement} {...decorateFormField(complement)}/>}
                        <LanguageCodeField
                            label={i18n('accesspoint.languageCode')} {...languageCode} {...decorateFormField(languageCode)} />
                        {!isStructured && <FormInput as="textarea"
                                                     label={i18n('accesspoint.description')} {...description} {...decorateFormField(description)} />}
                    </Modal.Body>}
                    {this.state.step === 2 && <Modal.Body>
                        <NoFocusButton onClick={this.add}><Icon
                            glyph="fa-plus-circle"/>{i18n('subNodeForm.section.item')}</NoFocusButton>
                        <ApItemNameForm
                            parent={{id: this.state.data.names[0].objectId, accessPointId: this.state.data.id}}/>
                    </Modal.Body>}
                    <Modal.Footer>
                        {(!structured.value || this.state.step === 2) && this.props.showSubmitTypes &&
                        <Button onClick={handleSubmit(okAndDetailSubmitForm)}
                                disabled={submitting}>{i18n('global.action.storeAndViewDetail')}</Button>}
                        {(!structured.value || this.state.step === 2) &&
                        <Button type="submit" onClick={handleSubmit(okSubmitForm)}
                                disabled={submitting}>{i18n('global.action.store')}</Button>}
                        {structured.value && this.state.step === 1 && <Button type="button" onClick={this.nextStep}
                                                                              disabled={this.state.working}>{i18n('global.action.next')}</Button>}
                        {this.state.step === 2 &&
                        <Button variant="link" onClick={this.onBack}>{i18n('global.action.back')}</Button>}
                        <Button variant="link" onClick={this.onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

export default reduxForm({
        form: 'addRegistryForm',
        fields: ['id', 'name', 'complement', 'languageCode', 'description', 'typeId', 'scopeId', 'structured', 'structuredObj'],
    }, state => ({
        nameItemForm: state.ap.nameItemForm,
        initialValues: state.form.addRegistryForm.initialValues,
        refTables: state.refTables,
        registryList: state.app.registryList,
        registryRegionRecordTypes: state.registryRegionRecordTypes,
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addRegistryForm', data})},
)(AddRegistryForm);
