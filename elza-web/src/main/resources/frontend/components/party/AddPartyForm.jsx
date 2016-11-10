import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n, Scope, Icon, FormInput, Loading} from 'components/index.jsx';
import {Modal, Button, HelpBlock, FormGroup, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRegionList.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
import {submitReduxForm} from 'components/form/FormUtils.jsx'
import {getTreeItemById} from "./../../components/registry/registryUtils";

/**
 * ADD PARTY FORM
 * *********************************************
 * formulář nové osoby
 */
class AddPartyForm extends AbstractReactComponent {

    static fields = [
        'recordTypeId',
        'scopeId',
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
        let errors = AddPartyForm.requireFields('mainPart', 'nameFormTypeId', 'recordTypeId')(values);
        errors.complements = values.complements.map(AddPartyForm.requireFields('complementTypeId', 'complement'));
        if (errors.complements.filter(i => Object.keys(i).length !== 0).length === 0) {
            delete errors.complements;
        }
        return errors;
    };

    static PropTypes = {
        versionId: React.PropTypes.number,
        partyTypeId: React.PropTypes.number.isRequired,
        partyTypeCode: React.PropTypes.string.isRequired,
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

        const loop = (item) => {
            if (item.addRecord) {
                return item;
            }

            if (item.children && item.children.length > 0) {
                for (let child of item.children) {
                    const found = loop(child);
                    if (found) {
                        return found;
                    }
                }
            }
            return null;
        };

        let found;
        for (let type of items) {
            found = loop(type);
            if (found) {
                break;
            }
        }

        return found ? found.id : null;
    }

    dataRefresh = (props = this.props) => {
        const {recordTypes, refTables:{partyTypes, partyNameFormTypes, scopesData:{scopes}}, partyTypeId} = props;
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());// nacteni seznamů typů forem jmen (uřední, ...)
        this.dispatch(refPartyTypesFetchIfNeeded());        // načtení seznamu typů jmen
        this.dispatch(getRegistryRecordTypesIfNeeded(partyTypeId));
        this.dispatch(requestScopesIfNeeded(null));

        const scope = scopes.filter(i => i.versionId === null);

        // Inicializace pokud všechny podmínky OK pak spustí loadData
        !this.state.initialized &&
            recordTypes.fetched &&
            !recordTypes.isFetching &&
            recordTypes.registryTypeId === partyTypeId &&
            partyNameFormTypes.fetched &&
            !partyNameFormTypes.isFetching &&
            partyTypes.fetched &&
            !partyTypes.isFetching &&
            scopes.length > 0 &&
            scope.length > 0 &&
            !scope[0].isFetching &&
            this.loadData(props);
    };

    /**
     * Pokud nejsou nastaveny hodnoty - nastavíme hodnotu do pole nameFormTypeId a scopeId
     */
    loadData(props) {
        const {recordTypes, refTables: {partyNameFormTypes, scopesData:{scopes}, partyTypes}, partyTypeId} = props;

        const recordTypeId = this.getPreselectRecordTypeId(recordTypes);
        const nameFormTypeId = partyNameFormTypes.items[0].nameFormTypeId;
        const scopeId = scopes.filter(i => i.versionId === null)[0].scopes[0].id;
        const complementsTypes = partyTypes.items.filter(i => i.partyTypeId == partyTypeId)[0].complementTypes;

        this.setState({initialized: true, complementsTypes});
        this.props.load({nameFormTypeId, scopeId, recordTypeId});
    }

    /**
     * HANDLE CLOSE
     * *********************************************
     * Zavření dialogového okénka formuláře
     */
    handleClose = () => {
        this.dispatch(modalDialogHide());
    };

    customSubmitReduxForm = (validate, store, values, dispatch) => {
        return new Promise((resolve, reject) => {
            const errors = validate(values, this.props);
            if (Object.keys(errors).length > 0) {
                reject(errors)
            } else {
                const {partyTypeId} = this.props;
                this.props.onSubmitForm(store, {...values, partyTypeId});
                resolve()
            }
        })
    };

    /**
     * RENDER
     * *********************************************
     * Vykreslení formuláře
     */
    render() {
        const {complementsTypes} = this.state;

        const submit = this.customSubmitReduxForm.bind(this, AddPartyForm.validate);

        const {
            submitFailed,
            fields: {
                complements,
                recordTypeId,
                scopeId,
                nameFormTypeId,
                degreeBefore,
                degreeAfter,
                mainPart,
                otherPart
            },
            refTables:{partyNameFormTypes},
            partyTypeCode,
            versionId,
            handleSubmit,
            showSubmitTypes,
            recordTypes,
            submitting
        } = this.props;

        const {initialized} = this.state;

        const treeItems = recordTypes.fetched ? recordTypes.item : [];
        const value = recordTypeId.value === null ? null : getTreeItemById(recordTypeId.value, treeItems);

        return (
            <div>
                {initialized ? <div>
                    <Form>
                    <Modal.Body>
                        <div className="line">
                            <FormGroup validationState={recordTypeId.touched && recordTypeId.error ? 'error' : null}>
                                <Autocomplete
                                    label={i18n('party.recordType')}
                                    items={treeItems}
                                    tree
                                    allowSelectItem={(id, item) => item.addRecord}
                                    {...recordTypeId}
                                    value={value}
                                    onChange={(id, item) => recordTypeId.onChange(id)}
                                    onBlur={(id, item) => recordTypeId.onBlur(id)}
                                />
                                {recordTypeId.touched && recordTypeId.error && <HelpBlock>{recordTypeId.error}</HelpBlock>}
                            </FormGroup>
                            <Scope versionId={versionId} label={i18n('party.recordScope')} {...scopeId} />
                        </div>

                        <FormInput componentClass="select" label={i18n('party.nameFormType')} {...nameFormTypeId}>
                            <option key="0"/>
                            {partyNameFormTypes.items.map((i)=> {
                                return <option value={i.nameFormTypeId} key={i.nameFormTypeId}>{i.name}</option>
                            })}
                        </FormInput>

                        <hr/>
                        {partyTypeCode == "PERSON" ? <div className="line">
                            <FormInput type="text" label={i18n('party.degreeBefore')} {...degreeBefore}/>
                            <FormInput type="text" label={i18n('party.degreeAfter')} {...degreeAfter}/>
                        </div> : ""}

                        <FormInput type="text" label={i18n('party.nameMain')} {...mainPart} />
                        <FormInput type="text" label={i18n('party.nameOther')} {...otherPart} />
                        <hr/>
                        <div>
                            <label>{i18n('party.nameComplements')}</label>
                            {complements.map((complement, index) => <div className="block complement" key={'complement' + index}>
                                    <div className="line">
                                        <FormInput type="text" {...complement.complement}  /> {/*error={submitFailed && complement.complement.touched && !complement.complement.value ? i18n('global.validation.required') : null}*/}
                                        <FormInput componentClass="select" {...complement.complementTypeId} > {/*error={submitFailed && complement.complementTypeId.touched && !complement.complementTypeId.value ? i18n('global.validation.required') : null}*/}
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
                        {showSubmitTypes && <Button type="submit" onClick={handleSubmit(submit.bind(this, 'storeAndViewDetail'))} disabled={submitting}>{i18n('global.action.storeAndViewDetail')}</Button>}
                        <Button type="submit" onClick={handleSubmit(submit.bind(this,'store'))} disabled={submitting}>{i18n('global.action.store')}</Button>
                        <Button bsStyle="link" onClick={this.handleClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                    </Form>
                </div> : <Loading />}
            </div>
        )
    }
}

export default reduxForm({
        form: 'addPartyForm',
        fields: AddPartyForm.fields,
    }, state => ({
        initialValues: state.form.addPartyForm.initialValues,
        refTables: state.refTables,
        recordTypes: state.registryRegionRecordTypes
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyForm', data}),}
)(AddPartyForm);