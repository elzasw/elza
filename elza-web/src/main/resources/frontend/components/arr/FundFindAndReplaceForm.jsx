/**
 * Formulář hledání a nahrazení.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if (!values.findText) {
        errors.findText = i18n('global.validation.required');
    }
    if (!values.itemsArea) {
        errors.itemsArea = i18n('global.validation.required');
    }

    return errors;
};

var FundFindAndReplaceForm = class FundFindAndReplaceForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.dispatch(descItemTypesFetchIfNeeded());
    }

    render() {
        const {allItemsCount, checkedItemsCount, refType, fields: {findText, replaceText, itemsArea}, handleSubmit, onClose, descItemTypes} = this.props;
        const uncheckedItemsCount = allItemsCount - checkedItemsCount
        var submitForm = submitReduxForm.bind(this, validate)

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <Input label={i18n('arr.fund.findAndReplace.descItemType')} wrapperClassName='form-items-group'>
                            <div title={refType.name}>
                                {refType.shortcut}
                            </div>
                        </Input>
                        <Input type="text" label={i18n('arr.fund.findAndReplace.findText')} {...findText} {...decorateFormField(findText)} />
                        <Input type="text" label={i18n('arr.fund.findAndReplace.replaceText')} {...replaceText} {...decorateFormField(replaceText)} />
                        <Input label={i18n('arr.fund.findAndReplace.itemsArea')} {...decorateFormField(itemsArea)} wrapperClassName='form-items-group'>
                            <Input type="radio" label={i18n('arr.fund.findAndReplace.itemsArea.all', allItemsCount)} {...itemsArea} value='all' checked={itemsArea.value === 'all'} />
                            {checkedItemsCount > 0 && checkedItemsCount < allItemsCount && <Input type="radio" label={i18n('arr.fund.findAndReplace.itemsArea.selected', checkedItemsCount)} {...itemsArea} value='selected' checked={itemsArea.value === 'selected'} />}
                            {uncheckedItemsCount > 0 && checkedItemsCount > 0 && <Input type="radio" label={i18n('arr.fund.findAndReplace.itemsArea.unselected', uncheckedItemsCount)} {...itemsArea} value='unselected' checked={itemsArea.value === 'unselected'} />}
                        </Input>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('arr.fund.findAndReplace.action.replace')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'fundFindAndReplaceForm',
    fields: ['findText', 'replaceText', 'itemsArea'],
},state => ({
    initialValues: {findText: '', replaceText: '', itemsArea: ''},
    descItemTypes: state.refTables.descItemTypes
}),
{}
)(FundFindAndReplaceForm)



