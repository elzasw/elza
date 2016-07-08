/**
 * Formulář přidání nebo uzavření AS.
 */

require ('./FundForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, BulkActionsTable, Icon, Autocomplete, VersionValidationState} from 'components/index.jsx';
import {Modal, Button, Input} from 'react-bootstrap';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if ((props.create || props.update) && !values.name) {
        errors.name = i18n('global.validation.required');
    }
    if ((props.create || props.ruleSet) && !values.ruleSetId) {
        errors.ruleSetId = i18n('global.validation.required');
    }
    if ((props.create || props.update) && !values.institutionId) {
        errors.institutionId = i18n('global.validation.required');
    }

    return errors;
};

var FundForm = class FundForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('isBulkActionRunning');

        this.state = {};
    }

    componentDidMount() {
        this.dispatch(refRuleSetFetchIfNeeded());
        this.dispatch(refInstitutionsFetchIfNeeded());
        if (this.props.initData) {
            this.props.load(this.props.initData);
        }
    }

    /**
     * Zkontroluje zda některé z hromadných akcí běží
     *
     * @returns {boolean}
     */
    isBulkActionRunning() {
        var result = false;
        this.props.bulkActions.states.forEach((item) => {
            if (item.state !== 'ERROR' && item.state !== 'FINISH') {
                result = true;
            }
        });
        return result;
    }

    /**
     * Vyhledávání v redux form fields - prohledává index.value
     * @param arr prohledávaný objekt
     * @param id hodnota
     * @param attrName index ve kterém se hledá
     * @returns {*}
     */
    findIndexInFields(arr, id, attrName = 'id') {
        if (arr == null) {
            return null;
        }

        if (attrName !== null) {
            for (var a = 0; a < arr.length; a++) {
                if (arr[a][attrName].value == id) {
                    return a;
                }
            }
        } else {
            for (var a = 0; a < arr.length; a++) {
                if (arr[a]['id'].value == id) {
                    return a;
                }
            }
        }
        return null;
    }

    render() {
        const {fields: {name, ruleSetId, regScopes, institutionId, internalCode, dateRange}, handleSubmit, onClose} = this.props;

        var submitForm = submitReduxForm.bind(this, validate);

        var approveButton;
        if (this.props.approve) {
            if (this.isBulkActionRunning()) {
                approveButton = <span className="text-danger">{i18n('arr.fund.approveVersion.runningBulkAction')}</span>;
            } else {
                approveButton = <Button onClick={handleSubmit(submitForm)}>{i18n('arr.fund.approveVersion.approve')}</Button>
            }
        }
        var ruleSets = this.props.refTables.ruleSet.items;
        var institutions = this.props.refTables.institutions.items;

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        {(this.props.create || this.props.update) &&
                        <Input type="text" label={i18n('arr.fund.name')} {...name} {...decorateFormField(name)} />}

                        {(this.props.create || this.props.update) &&
                        <Input type="text" label={i18n('arr.fund.internalCode')} {...internalCode} {...decorateFormField(internalCode)} />}

                        {(this.props.create || this.props.update) &&
                        <Input type="select" label={i18n('arr.fund.institution')} {...institutionId} {...decorateFormField(institutionId)}>
                            <option key='-institutionId'/>
                            {institutions.map(i=> {
                                return <option value={i.id}>{i.name}</option>
                            })}
                        </Input>}

                        {(this.props.create || this.props.ruleSet) &&
                        <Input type="select" label={i18n('arr.fund.ruleSet')} {...ruleSetId} {...decorateFormField(ruleSetId)}>
                            <option key='-ruleSetId'/>
                            {ruleSets.map(i=> {
                                return <option value={i.id}>{i.name}</option>
                            })}
                        </Input>}

                        {this.props.approve &&
                        <Input type="textarea" label={i18n('arr.fund.dateRange')} {...dateRange} {...decorateFormField(dateRange)} />}

                        {this.props.update && <Autocomplete
                            tags
                            label={i18n('arr.fund.regScope')}
                            items={this.props.scopeList}
                            getItemId={(item) => item ? item.id : null}
                            getItemName={(item) => item ? item.name : ''}
                            onChange={
                                (id, value) => {
                                    if (value.name.trim() == '') {
                                        return;
                                    }
                                    let index = this.findIndexInFields(this.props.fields.regScopes, value.name, 'name');
                                    if (index == null) {
                                        this.props.fields.regScopes.addField(value);
                                    } else {
                                        this.props.fields.regScopes.removeField(index);
                                    }
                                }
                            }
                            value={this.state.autocompleteValue}
                        />}
                        {this.props.update && <div className="selected-data-container">
                            {regScopes.map((scope, scopeIndex) => (
                                <div className="selected-data" key={scopeIndex}>
                                    {scope.name.value}<Button onClick={() => {regScopes.removeField(scopeIndex)}}>
                                    <Icon glyph="fa-times"/>
                                </Button>
                                </div>))}
                        </div>}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    {this.props.create && <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.create')}</Button>}
                    {this.props.approve && approveButton}
                    {(this.props.update || this.props.ruleSet) && <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.update')}</Button>}
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};

FundForm.propTypes = {
    approve: React.PropTypes.bool,
    create: React.PropTypes.bool,
    update: React.PropTypes.bool,
    ruleSet: React.PropTypes.bool,
    scopeList: React.PropTypes.array
};

module.exports = reduxForm({
        form: 'fundForm',
        fields: ['name', 'ruleSetId', 'institutionId', 'internalCode', 'dateRange', 'regScopes[].id', 'regScopes[].name'],
    }, state => ({
        initialValues: state.form.fundForm.initialValues,
        refTables: state.refTables,
        bulkActions: state.arrRegion.activeIndex !== null ? state.arrRegion.funds[state.arrRegion.activeIndex].bulkActions : undefined,
        versionValidation: state.arrRegion.activeIndex !== null ? state.arrRegion.funds[state.arrRegion.activeIndex].versionValidation : undefined,
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'fundForm', data})}
)(FundForm);



