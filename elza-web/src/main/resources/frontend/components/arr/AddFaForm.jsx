/**
 * Formulář přidání nebo uzavření AP.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, BulkActionsTable, Icon} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet'
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if (props.create && !values.name) {
        errors.name = i18n('global.validation.required');
    }
    if (!values.ruleSetId) {
        errors.ruleSetId = i18n('global.validation.required');
    }
    if (!values.rulArrTypeId) {
        errors.rulArrTypeId = i18n('global.validation.required');
    }

    return errors;
};

var AddFaForm = class AddFaForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('isBulkActionRunning');

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.dispatch(refRuleSetFetchIfNeeded());
        if (this.props.initData) {
            this.props.load(this.props.initData);
        }
    }

    isBulkActionRunning() {
        this.props.bulkActions.states.forEach((item) => {
            if (item.state === 'RUNNING') {
                return true;
            }
        });
        return false;
    }

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

    render() {
        const {fields: {name, ruleSetId, rulArrTypeId}, handleSubmit, onClose} = this.props;
        var ruleSets = this.props.refTables.ruleSet.items;
        var currRuleSetId = this.props.values.ruleSetId;
        var currRuleSet = [];
        var ruleSetOptions = [];
        if (!ruleSetId.invalid) {
            currRuleSet = ruleSets[indexById(ruleSets, currRuleSetId)];
            if (currRuleSet) {
                ruleSetOptions = currRuleSet.arrangementTypes.map(i=> <option key={i.id} value={i.id}>{i.name}</option>);
            }
        }
        return (
            <div>
                <Modal.Body>
                    {this.props.isApproveDialog && <div>
                        <BulkActionsTable mandatory={true} versionValidate={true}/>
                        <div>
                            {
                                this.props.versionValidation.isFetching ?
                                    <span><Icon
                                        glyph="fa-refresh"/> {i18n('arr.fa.versionValidation.running')}</span> : (
                                    this.props.versionValidation.errors.length > 0 ?
                                        <span><Icon glyph="fa-check"/> {i18n('arr.fa.versionValidation.ok')}</span> :
                                        <span><Icon
                                            glyph="fa-exclamation-triangle"/> {i18n('arr.fa.versionValidation.err')}</span>
                                )

                            }
                        </div>
                    </div>}
                    <form onSubmit={handleSubmit}>
                        {this.props.create && <Input type="text" label={i18n('arr.fa.name')} {...name} {...decorateFormField(name)} />}
                        <Input type="select" label={i18n('arr.fa.ruleSet')} {...ruleSetId} {...decorateFormField(ruleSetId)}>
                            <option key='-ruleSetId'></option>
                            {ruleSets.map(i=> {return <option value={i.id}>{i.name}</option>})}
                        </Input>
                        <Input type="select" disabled={ruleSetId.invalid} label={i18n('arr.fa.arrType')} {...rulArrTypeId} {...decorateFormField(rulArrTypeId)}>
                            <option key='-rulArrTypeId'></option>
                            {ruleSetOptions}
                        </Input>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    {this.props.create && <Button onClick={handleSubmit}>{i18n('global.action.create')}</Button>}
                    {this.props.isApproveDialog && <span>
                        {
                            this.isBulkActionRunning() ?
                                <span
                                    className="text-danger">{i18n('arr.fa.approveVersion.runningBulkAction')}</span> : (
                                this.isMandatoryBulkActionsDone() ?
                                    <Button onClick={handleSubmit}>{i18n('arr.fa.approveVersion.approve')}</Button> :
                                    <Button bsStyle="danger"
                                            onClick={handleSubmit}>{i18n('arr.fa.approveVersion.approveForce')}</Button>
                            )

                        }
                    </span>}
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

AddFaForm.propTypes = {
    isApproveDialog: React.PropTypes.bool.isRequired
}

module.exports = reduxForm({
    form: 'addFaForm',
    fields: ['name', 'ruleSetId', 'rulArrTypeId'],
    validate
},state => ({
    initialValues: state.form.addFaForm.initialValues,
        refTables: state.refTables,
        bulkActions: state.arrRegion.activeIndex !== null ? state.arrRegion.fas[state.arrRegion.activeIndex].bulkActions : undefined,
        versionValidation: state.arrRegion.activeIndex !== null ? state.arrRegion.fas[state.arrRegion.activeIndex].versionValidation : undefined,
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addFaForm', data})}
)(AddFaForm)



