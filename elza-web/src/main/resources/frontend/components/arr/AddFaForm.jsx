/**
 * Formulář přidání nové AP.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet'
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'

const validate = values => {
    const errors = {};

    if (!values.name) {
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

        //this.bindMethods('pokus');

        this.dispatch(refRuleSetFetchIfNeeded());

        this.props.load(props.initData);

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {fields: {name, ruleSetId, rulArrTypeId}, handleSubmit, onClose} = this.props;
        var ruleSets = this.props.refTables.ruleSet.items;
        var currRuleSetId = this.props.values.ruleSetId;
        var currRuleSet;
        if (!ruleSetId.invalid) {
            currRuleSet = ruleSets[indexById(ruleSets, currRuleSetId)];
        }
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <Input type="text" label={i18n('arr.fa.name')} {...name} {...decorateFormField(name)} />
                        <Input type="select" label={i18n('arr.fa.ruleSet')} {...ruleSetId} {...decorateFormField(ruleSetId)}>
                            <option></option>
                            {ruleSets.map(i=> {return <option value={i.id}>{i.name}</option>})}
                        </Input>
                        {!ruleSetId.invalid && (
                            <Input type="select" label={i18n('arr.fa.ruleSet')} {...rulArrTypeId} {...decorateFormField(rulArrTypeId)}>
                                <option></option>
                                {currRuleSet.rulArrTypes.map(i=> <option value={i.id}>{i.name}</option>)}
                            </Input>
                        )}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit}>{i18n('global.action.create')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'addFaForm',
    fields: ['name', 'ruleSetId', 'rulArrTypeId'],
    validate
},state => ({
    initialValues: state.form.addFaForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addFaForm', data})}
)(AddFaForm)



