/**
 * Formulář přidání nebo uzavření AP.
 */

require ('./FaForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, BulkActionsTable, Icon, Autocomplete, VersionValidationState} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet'
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if ((props.create || props.update) && !values.name) {
        errors.name = i18n('global.validation.required');
    }
    if ((props.create || props.approve) && !values.ruleSetId) {
        errors.ruleSetId = i18n('global.validation.required');
    }
    if ((props.create || props.approve) && !values.rulArrTypeId) {
        errors.rulArrTypeId = i18n('global.validation.required');
    }

    return errors;
};

var FaForm = class FaForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('isBulkActionRunning');

        this.state = {};
    }

    componentDidMount() {
        this.dispatch(refRuleSetFetchIfNeeded());
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
        this.props.bulkActions.states.forEach((item) => {
            if (item.state === 'RUNNING') {
                return true;
            }
        });
        return false;
    }


    /**
     * Zkontroluje zda existují nějaké nespuštěné hromadné akce
     *
     * @returns {boolean}
     */
    isMandatoryBulkActionsDone() {
        if (this.props.bulkActions.actions.length !== this.props.bulkActions.states.length) {
            return false;
        }
        this.props.bulkActions.states.forEach((item) => {
            if (typeof item.runChange !== 'object') {
                return false;
            }
        });
        return true;
    }

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
        const {fields: {name, ruleSetId, rulArrTypeId, regScopes}, handleSubmit, onClose} = this.props;
        var approveButton;
        if (this.props.approve) {
            if (this.isBulkActionRunning()) {
                approveButton = <span className="text-danger">{i18n('arr.fa.approveVersion.runningBulkAction')}</span>;
            } else if (this.isMandatoryBulkActionsDone()) {
                approveButton = <Button onClick={handleSubmit}>{i18n('arr.fa.approveVersion.approve')}</Button>
            } else {
                approveButton = <Button bsStyle="danger"
                                        onClick={handleSubmit}>{i18n('arr.fa.approveVersion.approveForce')}</Button>
            }
        }
        var ruleSets = this.props.refTables.ruleSet.items;
        var currRuleSetId = this.props.values.ruleSetId;
        var currRuleSet = [];
        var ruleSetOptions = [];
        if (!ruleSetId.invalid) {
            currRuleSet = ruleSets[indexById(ruleSets, currRuleSetId)];
            if (currRuleSet) {
                ruleSetOptions = currRuleSet.arrangementTypes.map(i=> <option key={i.id}
                                                                              value={i.id}>{i.name}</option>);
            }
        }
        return (
            <div>
                <Modal.Body>
                    {
                        this.props.approve &&
                        <div>
                            <BulkActionsTable mandatory={true} versionValidate={true}/>
                            <VersionValidationState count={this.props.versionValidation.count}
                                                    errExist={this.props.versionValidation.count > 0}
                                                    isFetching={this.props.versionValidation.isFetching}/>
                        </div>
                    }
                    <form onSubmit={handleSubmit}>
                        {(this.props.create || this.props.update) &&
                        <Input type="text" label={i18n('arr.fa.name')} {...name} {...decorateFormField(name)} />}
                        {(this.props.create || this.props.approve) && <Input type="select"
                                                                             label={i18n('arr.fa.ruleSet')} {...ruleSetId} {...decorateFormField(ruleSetId)}>
                            <option key='-ruleSetId'/>
                            {ruleSets.map(i=> {
                                return <option value={i.id}>{i.name}</option>
                            })}
                        </Input>}
                        {(this.props.create || this.props.approve) && <Input type="select" disabled={ruleSetId.invalid}
                                                                             label={i18n('arr.fa.arrType')} {...rulArrTypeId} {...decorateFormField(rulArrTypeId)}>
                            <option key='-rulArrTypeId'/>
                            {ruleSetOptions}
                        </Input>}
                        {this.props.update && <Autocomplete
                            tags
                            label={i18n('arr.fa.regScope')}// / Třída rejstříku
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
                    {this.props.create && <Button onClick={handleSubmit}>{i18n('global.action.create')}</Button>}
                    {this.props.approve && approveButton}
                    {this.props.update && <Button onClick={handleSubmit}>{i18n('global.action.update')}</Button>}
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};

FaForm.propTypes = {
    approve: React.PropTypes.bool,
    create: React.PropTypes.bool,
    update: React.PropTypes.bool,
    scopeList: React.PropTypes.array
};

module.exports = reduxForm({
        form: 'faForm',
        fields: ['name', 'ruleSetId', 'rulArrTypeId', 'regScopes[].id', 'regScopes[].name'],
        validate
    }, state => ({
        initialValues: state.form.faForm.initialValues,
        refTables: state.refTables,
        bulkActions: state.arrRegion.activeIndex !== null ? state.arrRegion.fas[state.arrRegion.activeIndex].bulkActions : undefined,
        versionValidation: state.arrRegion.activeIndex !== null ? state.arrRegion.fas[state.arrRegion.activeIndex].versionValidation : undefined,
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'faForm', data})}
)(FaForm);



