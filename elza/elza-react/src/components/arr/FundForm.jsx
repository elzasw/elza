import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Icon, Autocomplete, VersionValidationState, FormInput} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'

import './FundForm.scss';
import UserAndGroupField from "../admin/UserAndGroupField";
import {WebApi} from "../../actions/WebApi";
import {renderUserOrGroupItem, renderUserOrGroupLabel} from "../admin/adminRenderUtils";
import TagsField from "../TagsField";

/**
 * Formulář přidání nebo uzavření AS.
 */
class FundForm extends AbstractReactComponent {

    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const {userDetail} = props;
        const admin = userDetail.isAdmin();

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

        if (props.create && !admin && (!values.fundAdmins || values.fundAdmins.length === 0)) {
            errors.fundAdmins = i18n('global.validation.required');
        }

        return errors;
    };

    static propTypes = {
        approve: PropTypes.bool,
        create: PropTypes.bool,
        update: PropTypes.bool,
        ruleSet: PropTypes.bool,
        scopeList: PropTypes.array
    };

    state = {};

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
    isBulkActionRunning = () => {
        let result = false;
        this.props.bulkActions && this.props.bulkActions.states.forEach((item) => {
            if (item.state !== 'ERROR' && item.state !== 'FINISH') {
                result = true;
            }
        });
        return result;
    };

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

        for (let a = 0; a < arr.length; a++) {
            if (arr[a][attrName !== null ? attrName : 'id'].value == id) {
                return a;
            }
        }
        return null;
    }
    submitReduxForm = (values, dispatch) => submitForm(FundForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {error, userDetail, fields: {fundAdmins, name, ruleSetId, apScopes: apScopes, institutionId, internalCode, dateRange}, handleSubmit, onClose, create, update, approve, ruleSet, refTables, submitting} = this.props;
        let approveButton;
        if (approve) {
            if (this.isBulkActionRunning()) {
                approveButton = <span className="text-danger">{i18n('arr.fund.approveVersion.runningBulkAction')}</span>;
            } else {
                approveButton = <Button type="submit" disabled={submitting}>{i18n('arr.fund.approveVersion.approve')}</Button>
            }
        }
        const ruleSets = refTables.ruleSet.items;
        const institutions = refTables.institutions.items;
        const admin = userDetail.isAdmin();

        return <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            <Modal.Body>
                {(create || update) &&
                <FormInput type="text" label={i18n('arr.fund.name')} {...name} {...decorateFormField(name)} />}

                {(create || update) &&
                <FormInput type="text" label={i18n('arr.fund.internalCode')} {...internalCode} {...decorateFormField(internalCode)} />}

                {(create || update) &&
                <FormInput componentClass="select" label={i18n('arr.fund.institution')} {...institutionId} {...decorateFormField(institutionId)}>
                    <option key='-institutionId'/>
                    {institutions.map(i=> {
                        return <option value={i.id}>{i.name}</option>
                    })}
                </FormInput>}

                {(create || ruleSet) &&
                <FormInput componentClass="select" label={i18n('arr.fund.ruleSet')} {...ruleSetId} {...decorateFormField(ruleSetId)}>
                    <option key='-ruleSetId'/>
                    {ruleSets.map(i=> {
                        return <option value={i.id}>{i.name}</option>
                    })}
                </FormInput>}

                {approve &&
                <FormInput componentClass="textarea" label={i18n('arr.fund.dateRange')} {...dateRange} {...decorateFormField(dateRange)} />}

                {update && <Autocomplete
                    tags
                    label={i18n('arr.fund.regScope')}
                    items={this.props.scopeList}
                    getItemId={(item) => item ? item.id : null}
                    getItemName={(item) => item ? item.name : ''}
                    onChange={
                        (value) => {
                            if (!value || value.name.trim() == '') {
                                return;
                            }
                            let index = this.findIndexInFields(this.props.fields.apScopes, value.name, 'name');
                            if (index === null) {
                                this.props.fields.apScopes.addField(value);
                            } else {
                                this.props.fields.apScopes.removeField(index);
                            }
                        }
                    }
                    value={this.state.autocompleteValue}
                />}
                {update && <div className="selected-data-container">
                    {apScopes.map((scope, scopeIndex) => (
                        <div className="selected-data" key={scopeIndex}>
                            <span>{scope.name.value}</span><Button onClick={() => {apScopes.removeField(scopeIndex)}}>
                            <Icon glyph="fa-times"/>
                        </Button>
                        </div>))}
                </div>}

                {create && <FormInput
                    componentClass={TagsField}
                    label={i18n('arr.fund.fundAdmins')}
                    {...fundAdmins}
                    {...decorateFormField(fundAdmins)}
                    renderTagItem={renderUserOrGroupLabel}
                    fieldComponent={UserAndGroupField}
                    fieldComponentProps={{
                        findUserApi: WebApi.findUserWithFundCreate,
                        findGroupApi: WebApi.findGroupWithFundCreate
                    }}
                />}
            </Modal.Body>
            <Modal.Footer>
                {create && <Button type="submit"  disabled={submitting}>{i18n('global.action.create')}</Button>}
                {approve && approveButton}
                {(update || ruleSet) && <Button type="submit" disabled={submitting}>{i18n('global.action.update')}</Button>}
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

function mapStateToProps(state) {
    const {userDetail} = state;
    return {
        userDetail,
    }
}

export default connect(mapStateToProps)(reduxForm({
        form: 'fundForm',
        fields: ['name', 'ruleSetId', 'institutionId', 'internalCode', 'dateRange', 'apScopes[].id', 'apScopes[].name', "fundAdmins"]
    }, state => ({
        initialValues: state.form.fundForm.initialValues,
        refTables: state.refTables,
        bulkActions: state.arrRegion.activeIndex !== null ? state.arrRegion.funds[state.arrRegion.activeIndex].bulkActions : undefined,
        versionValidation: state.arrRegion.activeIndex !== null ? state.arrRegion.funds[state.arrRegion.activeIndex].versionValidation : undefined,
    }),
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'fundForm', data})}
)(FundForm));



