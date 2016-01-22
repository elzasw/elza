/**
 * Formulář přidání nebo uzavření AP.
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

        //this.bindMethods('');

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.dispatch(refRuleSetFetchIfNeeded());
        this.props.load(this.props.initData);
    }

    render() {
        const {fields: {name, ruleSetId, rulArrTypeId}, handleSubmit, onClose} = this.props;
        var ruleSets = this.props.refTables.ruleSet.items;
        var currRuleSetId = this.props.values.ruleSetId;
        var currRuleSet = [];
        var ruleSetOptions;
        if (!ruleSetId.invalid) {
            currRuleSet = ruleSets[indexById(ruleSets, currRuleSetId)];
            ruleSetOptions = currRuleSet.arrangementTypes.map(i=> <option key={i.id} value={i.id}>{i.name}</option>);
        }
        return (
            <div>
                <Modal.Body>
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



