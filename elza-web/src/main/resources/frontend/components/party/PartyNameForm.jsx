import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {DropDownTree, AbstractReactComponent, i18n, Scope, Icon, FormInput, Loading} from 'components/index.jsx';
import {Modal, Button, HelpBlock, FormGroup, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRegionList.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
import {submitReduxForm} from 'components/form/FormUtils.jsx'

const PARTY_TYPE_PERSON = 'PERSON';

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
        let errors = PartyNameForm.requireFields('mainPart')(values);
        errors.partyNameComplements = values.partyNameComplements.map(PartyNameForm.requireFields('complementTypeId', 'complement'));
        if (errors.partyNameComplements.filter(i => Object.keys(i).length !== 0).length === 0) {
            delete errors.partyNameComplements;
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
        const {refTables:{partyNameFormTypes}, partyTypeId} = props;
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());// nacteni seznamů typů forem jmen (uřední, ...)
        this.dispatch(refPartyTypesFetchIfNeeded());        // načtení seznamu typů jmen
        this.dispatch(getRegistryRecordTypesIfNeeded(partyTypeId));

        partyNameFormTypes.fetched &&
        !partyNameFormTypes.isFetching &&
        this.loadData(props);
    };

    /**
     * Pokud nejsou nastaveny hodnoty - nastavíme hodnotu do pole nameFormTypeId a scopeId
     */
    loadData(props) {
        const {refTables: {partyNameFormTypes}, partyType, initData} = props;
        const nameFormTypeId = partyNameFormTypes.items[0].id;
        if (!this.state.initialized) {
            this.setState({initialized: true, complementsTypes: partyType.complementTypes}, () => {
                let newLoad = null;
                if (initData) {
                    newLoad = {
                        ...initData
                    }
                } else {
                    newLoad = {nameFormType:{id: nameFormTypeId}}
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
                otherPart
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
            <Modal.Body>
                <FormInput componentClass="select" label={i18n('party.nameFormType')} {...nameFormType.id}>
                    {partyNameFormTypes.items.map((i) => <option value={i.id} key={i.id}>{i.name}</option>)}
                </FormInput>

                <hr/>
                {partyType.code == PARTY_TYPE_PERSON && <div className="line">
                    <FormInput type="text" label={i18n('party.degreeBefore')} {...degreeBefore}/>
                    <FormInput type="text" label={i18n('party.degreeAfter')} {...degreeAfter}/>
                </div>}

                <FormInput type="text" label={i18n('party.nameMain')} {...mainPart} />
                <FormInput type="text" label={i18n('party.nameOther')} {...otherPart} />
                <hr/>
                <div>
                    <label>{i18n('party.nameComplements')}</label>
                    {partyNameComplements.map((complement, index) => <div className="block complement" key={'complement' + index}>
                            <div className="line">
                                <FormInput type="text" {...complement.complement}/>
                                <FormInput componentClass="select" {...complement.complementTypeId}>
                                    <option key='0'/>
                                    {complementsList}
                                </FormInput>
                                <Button className="btn-icon" onClick={() => {partyNameComplements.removeField(index)}}><Icon glyph="fa-trash"/></Button>
                            </div>
                        </div>
                    )}
                    <Button className="btn-icon block" onClick={() => {partyNameComplements.addField({complementTypeId:null, complement: null})}}><Icon glyph="fa-plus"/></Button>
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
    }, state => ({
        initialValues: state.form.partyNameForm.initialValues,
        refTables: state.refTables,
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'partyNameForm', data}),}
)(PartyNameForm);