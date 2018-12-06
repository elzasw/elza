import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n, Icon, FormInput, HorizontalLoader} from 'components/shared';
import {Modal, Button, HelpBlock, FormGroup, Form, Row, Col} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRecordTypes.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
import {submitForm} from 'components/form/FormUtils.jsx'
import {getTreeItemById} from "./../../components/registry/registryUtils";
import {PARTY_TYPE_CODES} from '../../constants.tsx'

import DatationField from './DatationField';

import './RelationForm.less'
import Scope from "../shared/scope/Scope";

/**
 * Formulář nové osoby
 */
class AddPartyForm extends AbstractReactComponent {

    static fields = [
        'partyType', // skrýté pole updated při loadu
        'accessPoint.typeId',
        'accessPoint.scopeId',
        'prefferedName.nameFormType.id',
        'prefferedName.degreeBefore',
        'prefferedName.degreeAfter',
        'prefferedName.mainPart',
        'prefferedName.otherPart',
        'prefferedName.validFrom.calendarTypeId',
        'prefferedName.validFrom.value',
        'prefferedName.validFrom.textDate',
        'prefferedName.validFrom.note',
        'prefferedName.validTo.calendarTypeId',
        'prefferedName.validTo.value',
        'prefferedName.validTo.textDate',
        'prefferedName.validTo.note',
        'prefferedName.partyNameComplements[].complementTypeId',
        'prefferedName.partyNameComplements[].complement',
    ];

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (!data[name]) {
                errors[name] = i18n('global.validation.required')
            }
            return errors
        }, {});

    validate = (values) => {
        const {partyType} = this.props;
        let errors = AddPartyForm.validateInline(values);
        if (!values.prefferedName.mainPart) {
            if (!errors.prefferedName) {
                errors.prefferedName = {}
            }
            errors.prefferedName.mainPart = i18n('global.validation.required');
        }
        if (!values.accessPoint.typeId) {
            if (!errors.accessPoint) {
                errors.accessPoint = {}
            }
            errors.accessPoint.typeId = i18n('global.validation.required');
        }

        if (values.prefferedName.complements && values.prefferedName.complements.length > 0) {
            if (!errors.prefferedName) {
                errors.prefferedName = {}
            }
            errors.prefferedName.complements = values.prefferedName.complements.map(AddPartyForm.requireFields('complementTypeId', 'complement'));
            if (errors.prefferedName.complements.filter(i => Object.keys(i).length !== 0).length === 0) {
                delete errors.prefferedName.complements;
            }
        }
        errors.prefferedName !== undefined && Object.keys(errors.prefferedName).length === 0 && delete errors.prefferedName;

        return errors;
    };



    static validateInline = (values) => {
        const errors = {prefferedName:{}};

        errors.prefferedName.validFrom = DatationField.reduxValidate(values.prefferedName.validFrom);
        errors.prefferedName.validTo = DatationField.reduxValidate(values.validTo);
        if (errors.prefferedName.validFrom == null && errors.prefferedName.validTo == null) {
            delete errors.prefferedName;
        }

        if (values.accessPoint && !values.accessPoint.scopeId) {
            if (!errors.accessPoint) {
                errors.accessPoint = {};
            }
            errors.accessPoint.scopeId = i18n('global.validation.required');
        }

        return errors;

    };

    static PropTypes = {
        versionId: React.PropTypes.number,
        partyType: React.PropTypes.object.isRequired,
    };

    static defaultProps = {
        versionId: -1
    };

    state = {
        complementsTypes: [],
        initialized: false,
    };

    componentWillReceiveProps(nextProps) {
        this.dataRefresh(nextProps);
    }

    componentDidMount() {
        this.dataRefresh();
    }

    /**
     * Vrátí id typu rejstříku, který je první možné vybrat.
     * @param recordTypes již načtené typy rejstříků
     * @return id nebo null
     */
    getPreselectRecordTypeId = (recordTypes) => {
        const items = recordTypes.item;
        let res = null;
        let stop = false;
        const loop = (item) => {
            if (item.addRecord) {
                if (res != null) {
                    res = null;
                    stop = true;
                } else {
                    res = item;
                }
            } else if (item.children && item.children.length > 0) {
                for (let child of item.children) {
                    loop(child);
                    if (stop) {
                        return;
                    }
                }
            } else {
                res = null
            }
        };

        for (let type of items) {
            loop(type);
            if (res) {
                break;
            }
        }


        return res ? res.id : null;
    };

    dataRefresh = (props = this.props) => {
        const {recordTypes, refTables:{partyNameFormTypes, scopesData:{scopes}, calendarTypes}, partyType} = props;
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());// nacteni seznamů typů forem jmen (uřední, ...)
        this.dispatch(getRegistryRecordTypesIfNeeded(partyType.id));
        this.dispatch(requestScopesIfNeeded(null));

        const scope = scopes.filter(i => i.versionId === null);

        // Inicializace pokud všechny podmínky OK pak spustí loadData
        !this.state.initialized &&
            recordTypes.fetched &&
            !recordTypes.isFetching &&
            recordTypes.registryTypeId === partyType.id &&
            partyNameFormTypes.fetched &&
            !partyNameFormTypes.isFetching &&
            scopes.length > 0 &&
            scope.length > 0 &&
            !scope[0].isFetching &&
            calendarTypes.fetched &&
            !calendarTypes.isFetching &&
            this.loadData(props);
    };

    /**
     * Pokud nejsou nastaveny hodnoty - nastavíme hodnotu do pole scopeId
     */
    loadData(props) {
        const {recordTypes, refTables: {partyNameFormTypes, scopesData:{scopes}, calendarTypes}, partyType} = props;

        const typeId = this.getPreselectRecordTypeId(recordTypes);
        const filteredScopes = scopes.filter(i => i.versionId === null)[0].scopes;
        const scopeId = filteredScopes && filteredScopes[0] && filteredScopes[0].id ? filteredScopes[0].id : null;
        const complementsTypes = partyType.complementTypes;
        const firstCalId = calendarTypes.items[0].id;


        this.setState({initialized: true, complementsTypes});
        this.props.load({
            partyType,
            accessPoint:{
                typeId,
                scopeId,
            },
            prefferedName:{
                validFrom: {calendarTypeId: firstCalId},
                validTo: {calendarTypeId: firstCalId}
            }
        });
    }

    /**
     * HANDLE CLOSE
     * *********************************************
     * Zavření dialogového okénka formuláře
     */
    handleClose = () => {
        this.dispatch(modalDialogHide());
    };

    submitType = 'store';

    onSubmit = (values) => {
        var partyTypeId = this.props.partyTypeId;
        var submitType = this.submitType;
        return this.props.onSubmitForm(submitType, {...values, partyTypeId});
    };

    submitReduxForm = (values, dispatch) => submitForm(this.validate,values,this.props,this.onSubmit,dispatch);

    /**
     * RENDER
     * *********************************************
     * Vykreslení formuláře
     */
    render() {
        const {complementsTypes} = this.state;

        const {
            fields: {
                accessPoint: {
                    typeId,
                    scopeId
                },
                prefferedName: {
                    nameFormType,
                    degreeBefore,
                    degreeAfter,
                    mainPart,
                    otherPart,
                    partyNameComplements,
                    note,
                    validFrom,
                    validTo
                },
            },
            refTables:{partyNameFormTypes},
            partyType,
            versionId,
            handleSubmit,
            showSubmitTypes,
            recordTypes,
            submitting
        } = this.props;

        const {initialized} = this.state;

        if (!initialized) {
            return <HorizontalLoader />;
        }

        const treeItems = recordTypes.fetched ? recordTypes.item : [];
        const value = typeId.value === null ? null : getTreeItemById(typeId.value, treeItems);
        const complementsList = complementsTypes && complementsTypes.map(i => <option value={i.complementTypeId} key={'index' + i.complementTypeId}>{i.name}</option>);

        return <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            <Modal.Body className="dialog-3-col add-party-form">
                <div className="flex">
                    <div className="flex-2 col">
                        <Row>
                            <Col xs={12}>
                                <FormGroup validationState={typeId.touched && typeId.error ? 'error' : null}>
                                    <Autocomplete
                                        label={i18n('party.recordType')}
                                        items={treeItems}
                                        tree
                                        alwaysExpanded
                                        allowSelectItem={(item) => item.addRecord}
                                        {...typeId}
                                        value={value}
                                        onChange={item => typeId.onChange(item ? item.id : null)}
                                        onBlur={item => typeId.onBlur(item ? item.id : null)}
                                    />
                                </FormGroup>
                                <Scope versionId={versionId} label={i18n('party.recordScope')} {...scopeId} />
                            </Col>
                            <Col xs={12} md={6}>
                                <FormInput type="text" label={i18n('party.name.mainPart')} {...mainPart} />
                            </Col>
                            <Col xs={12} md={6}>
                                <FormInput type="text" label={i18n('party.name.otherPart')} {...otherPart} />
                            </Col>
                            {partyType.code == PARTY_TYPE_CODES.PERSON && <Col xs={12}>
                                <Row>
                                    <Col xs={12} md={6}>
                                        <FormInput type="text" label={i18n('party.name.degreeBefore')} {...degreeBefore} />
                                    </Col>
                                    <Col xs={12} md={6}>
                                        <FormInput type="text" label={i18n('party.name.degreeAfter')} {...degreeAfter} />
                                    </Col>
                                </Row>
                            </Col>}

                            <Col xs={12}>
                                <label>{i18n('party.name.complements')}</label> <Button bsStyle="action" onClick={() => {partyNameComplements.addField({complementTypeId:null, complement: null})}}><Icon glyph="fa-plus"/></Button>
                                {partyNameComplements.map((complement, index) => <div className="complement" key={'complement' + index}>
                                    <FormInput componentClass="select" {...complement.complementTypeId}>
                                        <option key='0'/>
                                        {complementsList}
                                    </FormInput>
                                    <FormInput type="text" {...complement.complement}/>
                                    <Button className="btn-icon" onClick={() => {partyNameComplements.removeField(index)}}><Icon glyph="fa-times"/></Button>
                                </div>)}
                            </Col>
                            <Col xs={12}>
                                <FormInput componentClass="select" label={i18n('party.name.nameFormType')} {...nameFormType.id}>
                                    <option key="null" />
                                    {partyNameFormTypes.items.map((i) => <option value={i.id} key={i.id}>{i.name}</option>)}
                                </FormInput>
                            </Col>
                        </Row>
                    </div>
                </div>
            </Modal.Body>
            <Modal.Footer>
                {showSubmitTypes && <Button type="submit" onClick={()=>{this.submitType = 'storeAndViewDetail'}} disabled={submitting}>{i18n('global.action.storeAndViewDetail')}</Button>}
                <Button type="submit" disabled={submitting}>{i18n('global.action.store')}</Button>
                <Button bsStyle="link" onClick={this.handleClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>;
    }
}

export default reduxForm({
        form: 'addPartyForm',
        fields: AddPartyForm.fields,
        validate: AddPartyForm.validateInline
    }, state => ({
        initialValues: state.form.addPartyForm.initialValues,
        refTables: state.refTables,
        recordTypes: state.registryRegionRecordTypes
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyForm', data}),}
)(AddPartyForm);
