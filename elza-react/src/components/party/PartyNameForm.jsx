import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Scope, Icon, FormInput, HorizontalLoader} from 'components/shared';
import DatationField from './DatationField'
import {Modal, Button, Row, Col, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRecordTypes.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
import {submitForm} from 'components/form/FormUtils.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {PARTY_TYPE_CODES, RELATION_CLASS_CODES} from '../../constants.tsx'


import './RelationForm.scss'

const stringNormalize = val => val && val.trim().length > 0 ? val.trim() : null;

/**
 * Formulář formy jména osoby
 */
class PartyNameForm extends AbstractReactComponent {

    static fields = [
        'nameFormType.id',
        'degreeBefore',
        'degreeAfter',
        'mainPart',
        'otherPart',
        'note',
        'validFrom.calendarTypeId',
        'validFrom.value',
        'validFrom.textDate',
        'validFrom.note',
        'validTo.calendarTypeId',
        'validTo.value',
        'validTo.textDate',
        'validTo.note',
        'partyNameComplements[].complementTypeId',
        'partyNameComplements[].complement',
    ];

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (!data[name]) {
                errors[name] = i18n('global.validation.required')
            }
            return errors
        }, {});

    /**
     * VALIDATE
     * *********************************************
     * Kontrola vyplnění formuláře identifikátoru
     * @return object errors - seznam chyb
     */
    static validate = function (values) {
        let errors = {
            ...PartyNameForm.requireFields('mainPart')(values),
            ...PartyNameForm.validateInline(values)
        };
        errors.partyNameComplements = values.partyNameComplements.map(PartyNameForm.requireFields('complementTypeId', 'complement'));
        if (errors.partyNameComplements.filter(i => Object.keys(i).length !== 0).length === 0) {
            delete errors.partyNameComplements;
        }

        return errors;
    };



    static validateInline = (values) => {
        const errors = {};

        errors.validFrom = DatationField.reduxValidate(values.validFrom);
        errors.validTo = DatationField.reduxValidate(values.validTo);

        if (!errors.validFrom) {
            delete errors.validFrom
        }
        if (!errors.validTo) {
            delete errors.validTo
        }
        return errors;
    };

    static propTypes = {
        partyType: PropTypes.object.isRequired,
    };

    state = {
        complementsTypes: [],
        initialized: false,
    };

    componentDidMount() {
        this.dataRefresh();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.dataRefresh(nextProps);
    }

    dataRefresh = (props = this.props) => {
        const {refTables:{partyNameFormTypes, calendarTypes}, partyTypeId} = props;
        this.props.dispatch(refPartyNameFormTypesFetchIfNeeded());// nacteni seznamů typů forem jmen (uřední, ...)
        this.props.dispatch(refPartyTypesFetchIfNeeded());        // načtení seznamu typů jmen
        this.props.dispatch(getRegistryRecordTypesIfNeeded(partyTypeId));
        this.props.dispatch(calendarTypesFetchIfNeeded());


        partyNameFormTypes.fetched &&
        !partyNameFormTypes.isFetching &&
        calendarTypes.fetched &&
        !calendarTypes.isFetching &&
        this.loadData(props);
    };
    /**
     * Funkce vracející výchozí inicializační objekt
     * @param {object} props
     * @return {object}
     */
    getDefaultInitObject(props){
        const {refTables: {calendarTypes}} = props;
        let firstCalId = calendarTypes.items[0].id;
        return {
            validFrom: {calendarTypeId:firstCalId},
            validTo: {calendarTypeId:firstCalId}
        }
    }
    /**
     * Funkce načítající výchozí hodnoty formuláře
     * @param {object} props
     */
    loadData(props) {
        const {refTables: {partyNameFormTypes}, partyType, initData = {}} = props;
        if (!this.state.initialized) {
            this.setState({initialized: true, complementsTypes: partyType.complementTypes}, () => {
                let defaultInitData = {...this.getDefaultInitObject(props)};
                for(let f in defaultInitData){
                    if(!initData[f]){    // Přepsání prázdných položek výchozí hodnotou
                        initData[f] = defaultInitData[f];
                    }
                }
                this.props.load(initData);
            });
        }
    }

    submitReduxForm = (values, dispatch) => submitForm(PartyNameForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {complementsTypes} = this.state;

        const {
            fields: {
                partyNameComplements,
                nameFormType,
                degreeBefore,
                degreeAfter,
                mainPart,
                otherPart,
                note,
                validFrom,
                validTo
            },
            refTables:{partyNameFormTypes},
            handleSubmit,
            submitting,
            partyType,
            onClose
        } = this.props;

        const {initialized} = this.state;

        const complementsList = complementsTypes && complementsTypes.map(i => <option value={i.complementTypeId} key={'index' + i.complementTypeId}>{i.name}</option>);

        return initialized ? <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            <Modal.Body className="dialog-3-col party-name-form">
                <div className="flex">
                    <div className="flex-2 col">
                        <Row>
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
                                <label>{i18n('party.name.complements')}</label> <Button variant="action" onClick={() => {partyNameComplements.addField({complementTypeId:null, complement: null})}}><Icon glyph="fa-plus"/></Button>
                                {partyNameComplements.map((complement, index) => <div className="complement" key={'complement' + index}>
                                    <FormInput as="select" {...complement.complementTypeId}>
                                        <option key='0'/>
                                        {complementsList}
                                    </FormInput>
                                    <FormInput type="text" {...complement.complement}/>
                                    <Button className="btn-icon" onClick={() => {partyNameComplements.removeField(index)}}><Icon glyph="fa-times"/></Button>
                                </div>)}
                            </Col>
                            <Col xs={12}>
                                <FormInput as="select" label={i18n('party.name.nameFormType')} {...nameFormType.id}>
                                    <option key="null" />
                                    {partyNameFormTypes.items.map((i) => <option value={i.id} key={i.id}>{i.name}</option>)}
                                </FormInput>
                            </Col>
                        </Row>
                    </div>
                    <div className="datation-group flex-1 col">
                        <Row>
                            <Col xs={12}>
                                <Row>
                                    <Col xs={6} md={12}>
                                        <DatationField fields={validFrom} label={i18n('party.name.validFrom')} labelTextual={i18n('party.name.validFrom.textDate')} labelNote={i18n('party.name.validFrom.note')} />
                                    </Col>
                                    <Col xs={6} md={12}>
                                        <DatationField fields={validTo} label={i18n('party.name.validTo')} labelTextual={i18n('party.name.validTo.textDate')} labelNote={i18n('party.name.validTo.note')} />
                                    </Col>
                                </Row>
                            </Col>
                        </Row>
                    </div>
                    <div className="flex-1 col">
                        <Row>
                            <Col xs={12}>
                                <FormInput as="textarea" label={i18n('party.name.note')} {...note} />
                            </Col>
                        </Row>
                    </div>
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('global.action.store')}</Button>
                <Button variant="link" onClick={onClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form> : <HorizontalLoader />;
    }
}

export default reduxForm({
        form: 'partyNameForm',
        fields: PartyNameForm.fields,
        validate: PartyNameForm.validateInline
    }, state => ({
        initialValues: state.form.partyNameForm.initialValues,
        refTables: state.refTables,
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'partyNameForm', data}),}
)(PartyNameForm);
