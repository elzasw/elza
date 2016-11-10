import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {DropDownTree, AbstractReactComponent, i18n, Scope, Icon, FormInput, Loading} from 'components/index.jsx';
import {Modal, Button, HelpBlock, FormGroup, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRegionList.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
import {submitReduxForm} from 'components/form/FormUtils.jsx'

const PARTY_TYPE_PERSON = 'PERSON';

/**
 * Formulář formy jména osoby
 */
class AddPartyNameForm extends AbstractReactComponent {

    static fields = [
        'nameFormTypeId',
        'degreeBefore',
        'degreeAfter',
        'mainPart',
        'otherPart',
        'complements[].complementTypeId',
        'complements[].complement',
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
        let errors = AddPartyNameForm.requireFields('mainPart', 'nameFormTypeId')(values);
        errors.complements = values.complements.map(AddPartyNameForm.requireFields('complementTypeId', 'complement'));
        if (errors.complements.filter(i => Object.keys(i).length !== 0).length === 0) {
            delete errors.complements;
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

    componentWillReceiveProps(nextProps) {
        //this.dataRefresh(nextProps);
    }

    componentDidMount() {
        this.dataRefresh();
    }

    dataRefresh = (props = this.props) => {
        const {refTables:{partyNameFormTypes}, partyTypeId} = props;
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
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
        const {refTables: {partyNameFormTypes}, partyType} = props;
        const nameFormTypeId = partyNameFormTypes.items[0].nameFormTypeId;
        this.setState({initialized: true, complementsTypes: partyType.complementTypes});
        this.props.load({nameFormTypeId});
    }

    render() {
        const {complementsTypes} = this.state;

        const submit = submitReduxForm.bind(this, AddPartyNameForm.validate);

        const {
            fields: {
                complements,
                nameFormTypeId,
                degreeBefore,
                degreeAfter,
                mainPart,
                otherPart
            },
            refTables:{partyNameFormTypes},
            handleSubmit,
            showSubmitTypes,
            submitting,
            partyType,
            onClose
        } = this.props;

        const {initialized} = this.state;

        return (
            <div>
                {initialized ? <Form>
                    <Modal.Body>
                        <FormInput componentClass="select" label={i18n('party.nameFormType')} {...nameFormTypeId}>
                            <option key="0"/>
                            {partyNameFormTypes.items.map((i)=> {
                                return <option value={i.nameFormTypeId} key={i.nameFormTypeId}>{i.name}</option>
                            })}
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
                            {complements.map((complement, index) => <div className="block complement" key={'complement' + index}>
                                    <div className="line">
                                        <FormInput type="text" {...complement.complement}/>
                                        <FormInput componentClass="select" {...complement.complementTypeId}>
                                            <option key='0'/>
                                            {complementsTypes && complementsTypes.map(i => <option value={i.complementTypeId} key={'index' + i.complementTypeId}>{i.name}</option>)}
                                        </FormInput>
                                        <Button className="btn-icon" onClick={() => {complements.removeField(index)}}><Icon glyph="fa-trash"/></Button>
                                    </div>
                                </div>
                            )}
                            <Button className="btn-icon block" onClick={() => {complements.addField({complementTypeId:null, complement: null})}}><Icon glyph="fa-plus"/></Button>
                        </div>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" onClick={handleSubmit(submit)} disabled={submitting}>{i18n('global.action.store')}</Button>
                        <Button bsStyle="link" onClick={onClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>: <Loading />}
            </div>
        )
    }
}

export default reduxForm({
        form: 'addPartyNameForm',
        fields: AddPartyNameForm.fields,
    }, state => ({
        initialValues: state.form.addPartyNameForm.initialValues,
        refTables: state.refTables,
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyNameForm', data}),}
)(AddPartyNameForm);