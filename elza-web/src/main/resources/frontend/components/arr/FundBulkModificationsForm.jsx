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

    if (!values.operationType) {
        errors.operationType = i18n('global.validation.required');
    }

    switch (values.operationType) {
        case 'findAndReplace':
            if (!values.findText) {
                errors.findText = i18n('global.validation.required');
            }
            break
        case 'replace':
            if (!values.replaceText) {
                errors.replaceText = i18n('global.validation.required');
            }
            break
        case 'delete':
            break
    }

    if (!values.itemsArea) {
        errors.itemsArea = i18n('global.validation.required');
    }

    return errors;
};

const getDefaultItemsArea = props => {
    const {allItemsCount, checkedItemsCount} = props
    const uncheckedItemsCount = allItemsCount - checkedItemsCount

    const showSelected = checkedItemsCount > 0 && checkedItemsCount < allItemsCount

    if (showSelected) {
        return 'selected'
    } else {
        return 'all'
    }
}

var FundBulkModificationsForm = class FundBulkModificationsForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.dispatch(descItemTypesFetchIfNeeded());
    }

    render() {
        const {allItemsCount, checkedItemsCount, refType, fields: {findText, replaceText, itemsArea, operationType}, handleSubmit, onClose, descItemTypes} = this.props;
        const uncheckedItemsCount = allItemsCount - checkedItemsCount
        var submitForm = submitReduxForm.bind(this, validate)

        let operationInputs = []
        switch (operationType.value) {
            case 'findAndReplace':
                operationInputs.push(<Input type="text" label={i18n('arr.fund.bulkModifications.findText')} {...findText} {...decorateFormField(findText)} />)
                operationInputs.push(<Input type="text" label={i18n('arr.fund.bulkModifications.replaceText')} {...replaceText} {...decorateFormField(replaceText)} />)
                break
            case 'replace':
                operationInputs.push(<Input type="text" label={i18n('arr.fund.bulkModifications.replaceText')} {...replaceText} {...decorateFormField(replaceText)} />)
                break
            case 'delete':
                break
        }

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <Input label={i18n('arr.fund.bulkModifications.descItemType')} wrapperClassName='form-items-group'>
                            <div title={refType.name}>
                                {refType.shortcut}
                            </div>
                        </Input>
                        <Input label={i18n('arr.fund.bulkModifications.itemsArea')} {...decorateFormField(itemsArea)} wrapperClassName='form-items-group'>
                            <Input type="radio" label={i18n('arr.fund.bulkModifications.itemsArea.all', allItemsCount)} {...itemsArea} value='all' checked={itemsArea.value === 'all'} />
                            {checkedItemsCount > 0 && checkedItemsCount < allItemsCount && <Input type="radio" label={i18n('arr.fund.bulkModifications.itemsArea.selected', checkedItemsCount)} {...itemsArea} value='selected' checked={itemsArea.value === 'selected'} />}
                            {uncheckedItemsCount > 0 && checkedItemsCount > 0 && <Input type="radio" label={i18n('arr.fund.bulkModifications.itemsArea.unselected', uncheckedItemsCount)} {...itemsArea} value='unselected' checked={itemsArea.value === 'unselected'} />}
                        </Input>
                        <Input type='select' label={i18n('arr.fund.bulkModifications.operationType')} {...operationType} {...decorateFormField(operationType)}>
                            <option key='findAndReplace' value='findAndReplace'>{i18n('arr.fund.bulkModifications.operationType.findAndReplace')}</option>
                            <option key='replace' value='replace'>{i18n('arr.fund.bulkModifications.operationType.replace')}</option>
                            <option key='delete' value='delete'>{i18n('arr.fund.bulkModifications.operationType.delete')}</option>
                        </Input>
                        {operationInputs}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('arr.fund.bulkModifications.action.replace')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'fundBulkModificationsForm',
    fields: ['findText', 'replaceText', 'itemsArea', 'operationType'],
}, (state, props) => {
        return {
            initialValues: {findText: '', replaceText: '', itemsArea: getDefaultItemsArea(props), operationType: 'findAndReplace'}, descItemTypes: state.refTables.descItemTypes
        }
    },
{}
)(FundBulkModificationsForm)



