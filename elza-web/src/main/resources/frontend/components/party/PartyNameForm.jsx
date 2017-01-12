import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {DropDownTree, AbstractReactComponent, i18n, Scope, Icon, FormInput, Loading, DatationField} from 'components/index.jsx';
import {Modal, Button, Row, Col, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRegionList.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
import {submitReduxForm} from 'components/form/FormUtils.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {PARTY_TYPE_CODES} from 'actions/party/party.jsx'


import './RelationForm.less'

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

    static PropTypes = {
        partyType: React.PropTypes.object.isRequired,
    };

    state = {
        complementsTypes: [],
        initialized: false,
    };

    componentDidMount() {
        this.dataRefresh();
    }

    componentWillReceiveProps(nextProps) {
        this.dataRefresh(nextProps);
    }

    dataRefresh = (props = this.props) => {
        const {refTables:{partyNameFormTypes, calendarTypes}, partyTypeId} = props;
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());// nacteni seznamů typů forem jmen (uřední, ...)
        this.dispatch(refPartyTypesFetchIfNeeded());        // načtení seznamu typů jmen
        this.dispatch(getRegistryRecordTypesIfNeeded(partyTypeId));
        this.dispatch(calendarTypesFetchIfNeeded());


        partyNameFormTypes.fetched &&
        !partyNameFormTypes.isFetching &&
        calendarTypes.fetched &&
        !calendarTypes.isFetching &&
        this.loadData(props);
    };

    /**
     * Pokud nejsou nastaveny hodnoty - nastavíme hodnotu do pole nameFormTypeId a scopeId
     */
    loadData(props) {
        const {refTables: {partyNameFormTypes, calendarTypes}, partyType, initData} = props;
        const firstCalId = calendarTypes.items[0].id;
        if (!this.state.initialized) {
            this.setState({initialized: true, complementsTypes: partyType.complementTypes}, () => {
                let newLoad = null;
                if (initData) {
                    newLoad = {
                        ...initData
                    }
                } else {
                    newLoad = {validFrom:{calendarTypeId:firstCalId}, validTo:{calendarTypeId:firstCalId}}
                }
                this.props.load(newLoad);
            });
        }
    }

    render() {
        const {complementsTypes} = this.state;

        const submit = submitReduxForm.bind(this, PartyNameForm.validate);

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

        return initialized ? <Form onSubmit={handleSubmit(submit)}>
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
                                <FormInput componentClass="textarea" label={i18n('party.name.note')} {...note} />
                            </Col>
                        </Row>
                    </div>
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('global.action.store')}</Button>
                <Button bsStyle="link" onClick={onClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form> : <Loading />;
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
