import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {FormControl, Modal, Button, Input, Radio, ControlLabel, Form, FormGroup} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx';
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx';
import {getSpecsIds} from 'components/arr/ArrUtils.jsx';
import  './FundBulkModificationsForm.less';

const SimpleCheckListBox = require('./SimpleCheckListBox');


const getDefaultOperationType = props => {
    const {dataType} = props;

    let result;

    switch (dataType.code) {
        case 'TEXT':
        case 'STRING':
        case 'FORMATTED_TEXT':
        case 'UNITID':
            result = 'findAndReplace';
            break;
        default:
            result = 'delete';
            break
    }

    return result
};

const getDefaultItemsArea = props => {
    const {allItemsCount, checkedItemsCount} = props;
    const uncheckedItemsCount = allItemsCount - checkedItemsCount;

    const showSelected = checkedItemsCount > 0 && checkedItemsCount < allItemsCount;

    if (showSelected) {
        return 'selected'
    } else {
        return 'all'
    }
};

/**
 * Formulář hledání a nahrazení.
 */

class FundBulkModificationsForm extends AbstractReactComponent {

    /**
     * Validace formuláře.
     */

    static validate = (values, props) => {
        const errors = {};

        if (!values.operationType) {
            errors.operationType = i18n('global.validation.required');
        }

        if (props.refType.useSpecification) {
            const specsIds = getSpecsIds(props.refType, values.specs.type, values.specs.ids)
            if (specsIds.length === 0) {
                errors.specs = i18n('global.validation.required');
            }
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
                if (props.refType.useSpecification && !values.replaceSpec) {
                    errors.replaceSpec = i18n('global.validation.required');
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

    constructor(props) {
        super(props);

        this.bindMethods('supportFindAndReplace', 'supportReplace')
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.dispatch(descItemTypesFetchIfNeeded());
    }

    supportFindAndReplace = () => {
        const {dataType} = this.props;

        let result;

        switch (dataType.code) {
            case 'TEXT':
            case 'STRING':
            case 'FORMATTED_TEXT':
            case 'UNITID':
                result = true;
                break;
            default:
                result = false;
                break
        }

        return result
    };

    supportReplace = () => {
        const {dataType} = this.props;

        let result;

        switch (dataType.code) {
            case 'TEXT':
            case 'STRING':
            case 'FORMATTED_TEXT':
            case 'UNITID':
                result = true;
                break;
            default:
                result = false;
                break
        }

        return result
    };

    submitReduxForm = (values, dispatch) => submitForm(FundBulkModificationsForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {allItemsCount, checkedItemsCount, refType, fields: {findText, replaceText, itemsArea, operationType, specs, replaceSpec}, handleSubmit, onClose, descItemTypes} = this.props;
        const uncheckedItemsCount = allItemsCount - checkedItemsCount;

        let operationInputs = [];
        let submitButtonTitle;
        switch (operationType.value) {
            case 'findAndReplace':
                submitButtonTitle = 'arr.fund.bulkModifications.action.findAndReplace';
                operationInputs.push(<FormInput type="text" label={i18n('arr.fund.bulkModifications.findAndRFeplace.findText')} {...findText} {...decorateFormField(findText)} />);
                operationInputs.push(<FormInput type="text" label={i18n('arr.fund.bulkModifications.findAndRFeplace.replaceText')} {...replaceText} {...decorateFormField(replaceText)} />);
                break;
            case 'replace':
                submitButtonTitle = 'arr.fund.bulkModifications.action.replace';
                if (refType.useSpecification) {
                    operationInputs.push(
                        <FormInput componentClass='select' label={i18n('arr.fund.bulkModifications.replace.replaceSpec')} {...replaceSpec} {...decorateFormField(replaceSpec)}>
                            <option />
                            {refType.descItemSpecs.map(i => (
                                <option key={i.id} value={i.id}>{i.name}</option>
                            ))}
                        </FormInput>
                    )
                }

                operationInputs.push(<FormInput type="text" label={i18n('arr.fund.bulkModifications.replace.replaceText')} {...replaceText} {...decorateFormField(replaceText)} />);
                break;
            case 'delete':
                submitButtonTitle = 'arr.fund.bulkModifications.action.delete';
                break
        }

        return <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            <Modal.Body className='fund-bulk-modifications-container'>
                <FormInput type="static" label={i18n('arr.fund.bulkModifications.descItemType')} wrapperClassName='form-items-group'>
                    {refType.shortcut}
                </FormInput>

                {refType.useSpecification && <FormGroup>
                    <ControlLabel>{i18n('arr.fund.bulkModifications.specs')}</ControlLabel>
                    <SimpleCheckListBox
                        ref='specsListBox'
                        items={refType.descItemSpecs}
                        {...specs}
                    />
                </FormGroup>}

                <FormGroup>
                    <ControlLabel>{i18n('arr.fund.bulkModifications.itemsArea')}</ControlLabel>
                    <Radio
                        {...itemsArea} value='all' checked={itemsArea.value === 'all'}
                    >{i18n('arr.fund.bulkModifications.itemsArea.all', allItemsCount)}</Radio>
                    {checkedItemsCount > 0 && checkedItemsCount < allItemsCount && <Radio {...itemsArea} value='selected' checked={itemsArea.value === 'selected'}>{i18n('arr.fund.bulkModifications.itemsArea.selected', checkedItemsCount)}</Radio>}
                    {uncheckedItemsCount > 0 && checkedItemsCount > 0 && <Radio {...itemsArea} value='unselected' checked={itemsArea.value === 'unselected'}>{i18n('arr.fund.bulkModifications.itemsArea.unselected', uncheckedItemsCount)}</Radio>}
                </FormGroup>
                <FormInput componentClass='select' label={i18n('arr.fund.bulkModifications.operationType')} {...operationType} {...decorateFormField(operationType)}>
                    {this.supportFindAndReplace() && <option key='findAndReplace' value='findAndReplace'>{i18n('arr.fund.bulkModifications.operationType.findAndReplace')}</option>}
                    {this.supportReplace() && <option key='replace' value='replace'>{i18n('arr.fund.bulkModifications.operationType.replace')}</option>}
                    <option key='delete' value='delete'>{i18n('arr.fund.bulkModifications.operationType.delete')}</option>
                </FormInput>
                {operationInputs}
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit">{i18n(submitButtonTitle)}</Button>
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
            </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
    form: 'fundBulkModificationsForm',
    fields: ['findText', 'replaceText', 'itemsArea', 'operationType', 'specs', 'replaceSpec'],
}, (state, props) => {
        return {
            initialValues: {findText: '', replaceText: '', itemsArea: getDefaultItemsArea(props), operationType: getDefaultOperationType(props), specs: {type: 'unselected'}},
            descItemTypes: state.refTables.descItemTypes
        }
    },
{}
)(FundBulkModificationsForm)



