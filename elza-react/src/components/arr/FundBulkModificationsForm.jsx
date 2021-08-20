//
import React from 'react';
import {formValueSelector, Field, getFormMeta, reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {connect} from 'react-redux';
import {Col, Form, FormCheck, FormGroup, FormLabel, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {decorateFormField, submitForm} from 'components/form/FormUtils';
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes';
import {getSpecsIds, hasDescItemTypeValue} from 'components/arr/ArrUtils';
import './FundBulkModificationsForm.scss';
import SimpleCheckListBox from './SimpleCheckListBox';
import ValueCheckListBox from './ValueCheckListBox';
import {validateInt} from '../validate';
import DescItemUnitdate from './nodeForm/DescItemUnitdate';
import DescItemRecordRef from './nodeForm/DescItemRecordRef';
import DatationField from './../party/DatationField';
import {FILTER_NULL_VALUE} from 'actions/arr/fundDataGrid';
import {getMapFromList} from './../../stores/app/utils';
import FormInputField from '../shared/form/FormInputField';
import FF from '../shared/form/FF';
import ReduxFormFieldErrorDecorator from '../shared/form/ReduxFormFieldErrorDecorator';
import {WebApi} from 'actions/index';

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
            break;
    }

    return result;
};

const getDefaultItemsArea = props => {
    const {allItemsCount, checkedItemsCount} = props;

    const showSelected = checkedItemsCount > 0 && checkedItemsCount < allItemsCount;

    if (showSelected) {
        return 'selected';
    } else {
        return 'page';
    }
};

/**
 * Formulář hledání a nahrazení.
 */
class FundBulkModificationsForm extends AbstractReactComponent {

    state = {
        allValueItems: [],
        valueItems: [],
        valueSearchText: '',
    };

    /**
     * Validace formuláře.
     */

    static validate = (values, props) => {
        const errors = {};

        if (!values.operationType) {
            errors.operationType = i18n('global.validation.required');
        }

        if (props.refType.useSpecification) {
            const refType = {
                ...props.refType,
                descItemSpecs: [
                    {id: FILTER_NULL_VALUE, name: i18n('arr.fund.filterSettings.value.empty')},
                    ...props.refType.descItemSpecs,
                ],
            };
            const specsIds = getSpecsIds(refType, values.specs.type, values.specs.ids);
            if (specsIds.length === 0 && values.specs.ids.indexOf(FILTER_NULL_VALUE) === -1) {
                errors.specs = i18n('global.validation.required');
            }
        }

        switch (values.operationType) {
            case 'findAndReplace':
                if (!values.findText) {
                    errors.findText = i18n('global.validation.required');
                }
                break;
            case 'replace':
                if (!values.replaceText) {
                    errors.replaceText = i18n('global.validation.required');
                }

                switch (props.dataType.code) {
                    case 'INT':
                        const result = validateInt(values.replaceText);
                        if (result) {
                            errors.replaceText = result;
                        }
                        break;

                    case 'UNITDATE':
                        try {
                            DatationField.validate(values.replaceText);
                        } catch (err) {
                            errors.replaceText = err && err.message ? err.message : ' ';
                        }

                        if (!errors.replaceText && values.replaceText) {
                            errors.replaceText = i18n('global.validation.required');
                        }
                        break;

                    case 'RECORD_REF':
                        // console.log(222222222, values);

                        break;
                    default:
                        break;
                }

                if (props.refType.useSpecification && !values.replaceSpec) {
                    errors.replaceSpec = i18n('global.validation.required');
                }
                break;
            case 'delete':
                break;
            case 'setSpecification':
                if (!values.replaceSpec) {
                    errors.replaceSpec = i18n('global.validation.required');
                }
                break;
            case 'setValue':
                if (!values.replaceValue) {
                    errors.replaceValue = i18n('global.validation.required');
                }
            default:
                break;
        }

        if (!values.itemsArea) {
            errors.itemsArea = i18n('global.validation.required');
        }

        return errors;
    };

    UNSAFE_componentWillReceiveProps(nextProps) {}

    componentDidMount() {
        this.props.dispatch(descItemTypesFetchIfNeeded());

        if (this.supportSetValue) {
            this.callValueSearch('');
        }
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
                break;
        }

        return result;
    };

    supportReplace = () => {
        const {dataType} = this.props;

        let result;

        switch (dataType.code) {
            case 'TEXT':
            case 'STRING':
            case 'FORMATTED_TEXT':
            case 'UNITID':
            case 'INT':
            case 'DATE':
            case 'UNITDATE':
            case 'RECORD_REF':
                result = true;
                break;
            default:
                result = false;
                break;
        }

        return result;
    };

    supportSetSpecification = () => {
        const {refType} = this.props;
        return refType.useSpecification;
    };

    supportSetValue = () => {
        const {dataType} = this.props;
        let result;

        switch (dataType.code) {
            case 'STRUCTURED':
                result = true;
                break;
            default:
                result = false;
                break;
        }

        return result;
    };

    submitReduxForm = (values, dispatch) =>
        submitForm(FundBulkModificationsForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    /**
     * Vrací true v případě, že atribut tvoří hodnotu pouze specifikací - enum.
     */
    isEnumType = () => {
        const {dataType} = this.props;
        return dataType.code === 'ENUM';
    };

    callValueSearch() {
        const {versionId, refType, dataType} = this.props;
        const {valueSearchText} = this.state;

        if (!hasDescItemTypeValue(dataType)) {
            // pokud nemá hodnotu, nemůžeme volat
            return;
        }

        if (dataType.code === "STRUCTURED") {

            WebApi.getDescItemTypeValues(versionId, refType.id, valueSearchText, null, 200).then(json => {
                var valueItems = json.map(i => ({id: i, name: i}));

                if (
                    valueSearchText === '' ||
                    i18n('arr.fund.filterSettings.value.empty').toLowerCase().indexOf(valueSearchText) !== -1
                ) {

                    this.setState({
                        allValueItems: valueItems,
                    });
                    // u prázdného hledání a případně u hledání prázdné hodnoty doplňujeme null položku
                    valueItems = [
                        {
                            id: 'NULL',
                            name: i18n('arr.fund.filterSettings.value.empty'),
                        },
                        ...valueItems,
                    ];
                }

                this.setState({
                    valueItems: valueItems,
                });
            });
        }
    }

    render() {
        const {
            allItemsCount,
            checkedItemsCount,
            refType,
            handleSubmit,
            onClose,
            dataType,
            meta,
            replaceSpec,
            replaceText,
            operationType,
            submitting,
            versionId,
        } = this.props;
        const {allValueItems} = this.state;
        const uncheckedItemsCount = allItemsCount - checkedItemsCount;

        let operationInputs = [];
        let submitButtonTitle;

        switch (operationType) {
            case 'setSpecification':
                submitButtonTitle = 'arr.fund.bulkModifications.action.setSpecification';
                operationInputs.push(
                    <Field
                        key={'replaceSpec'}
                        name="replaceSpec"
                        as={'select'}
                        component={FormInputField}
                        label={i18n(
                            this.isEnumType()
                                ? 'arr.fund.bulkModifications.replace.replaceEnum'
                                : 'arr.fund.bulkModifications.replace.replaceSpec',
                        )}
                        disabled={submitting}
                    >
                        <option />
                        {refType.descItemSpecs.map(i => (
                            <option key={i.id} value={i.id}>
                                {i.name}
                            </option>
                        ))}
                    </Field>,
                );
                break;
            case 'findAndReplace':
                submitButtonTitle = 'arr.fund.bulkModifications.action.findAndReplace';
                operationInputs.push(
                    <Field
                        key={'findText'}
                        name="findText"
                        type="text"
                        component={FormInputField}
                        label={i18n('arr.fund.bulkModifications.findAndRFeplace.findText')}
                        disabled={submitting}
                    />,
                );
                operationInputs.push(
                    <Field
                        key={'replaceText'}
                        name="replaceText"
                        type="text"
                        component={FormInputField}
                        label={i18n('arr.fund.bulkModifications.findAndRFeplace.replaceText')}
                        disabled={submitting}
                    />,
                );
                break;
            case 'replace':
                submitButtonTitle = 'arr.fund.bulkModifications.action.replace';
                if (refType.useSpecification) {
                    operationInputs.push(
                        <Field
                            key={'replaceSpec'}
                            name="replaceSpec"
                            as={'select'}
                            component={FormInputField}
                            label={i18n(
                                this.isEnumType()
                                    ? 'arr.fund.bulkModifications.replace.replaceEnum'
                                    : 'arr.fund.bulkModifications.replace.replaceSpec',
                            )}
                            disabled={submitting}
                        >
                            <option />
                            {refType.descItemSpecs.map(i => (
                                <option key={i.id} value={i.id}>
                                    {i.name}
                                </option>
                            ))}
                        </Field>,
                    );
                }

                // Pomocné props pro předávání na hodnoty typu desc item
                const descItemProps = {
                    hasSpecification: refType.useSpecification,
                    locked: false,
                    readMode: false,
                    cal: false,
                    readOnly: false,
                };

                switch (dataType.code) {
                    case 'UNITDATE':
                        {
                            let data = {
                                ...descItemProps,
                                descItem: {
                                    error: {
                                        value: meta.replaceText ? meta.replaceText : null,
                                    },
                                    value: replaceText.value,
                                },
                            };
                            operationInputs.push(
                                <FF
                                    name={'replaceText'}
                                    field={DescItemUnitdate}
                                    label={i18n('arr.fund.bulkModifications.replace.replaceText')}
                                    {...data}
                                />,
                            );
                        }
                        break;
                    case 'RECORD_REF':
                        {
                            let specName = null;
                            if (replaceSpec) {
                                const map = getMapFromList(refType.descItemSpecs);
                                specName = map[replaceSpec].name;
                            }

                            let data = {
                                ...descItemProps,
                                itemTypeId: refType.id,
                                itemName: refType.shortcut,
                                specName: specName,
                                descItem: {
                                    error: {
                                        value: meta.replaceText ? meta.replaceText : null,
                                    },
                                    record: replaceText,
                                    descItemSpecId: replaceSpec,
                                },
                            };
                            operationInputs.push(
                                <FF
                                    name={'replaceText'}
                                    field={DescItemRecordRef}
                                    label={i18n('arr.fund.bulkModifications.replace.replaceText')}
                                    {...data}
                                />,
                            );
                        }
                        break;
                    default:
                        operationInputs.push(
                            <Field
                                key={'replaceText'}
                                name="replaceText"
                                type="text"
                                component={FormInputField}
                                label={i18n('arr.fund.bulkModifications.replace.replaceText')}
                                disabled={submitting}
                            />,
                        );
                }

                break;
            case 'setValue':
                submitButtonTitle = 'arr.fund.bulkModifications.action.setSpecification';
                operationInputs.push(
                    <Field
                        key={'replaceValue'}
                        name="replaceValue"
                        as={'select'}
                        component={FormInputField}
                        label={i18n('arr.fund.bulkModifications.replace.replaceEnum')}
                        disabled={submitting}
                    >
                        <option />
                        {allValueItems.map(i => (
                            <option key={i.id} value={i.id}>
                                {i.name}
                            </option>
                        ))}
                    </Field>,
                );
                break;
            case 'delete':
                submitButtonTitle = 'arr.fund.bulkModifications.action.delete';
                break;
            default:
                break;
        }

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body className="fund-bulk-modifications-container">
                    <FormInput
                        type="static"
                        label={i18n('arr.fund.bulkModifications.descItemType')}
                        wrapperClassName="form-items-group"
                    >
                        {refType.shortcut}
                    </FormInput>

                    {refType.useSpecification && (
                        <FormGroup>
                            <FormLabel>
                                {i18n(
                                    this.isEnumType()
                                        ? 'arr.fund.bulkModifications.values'
                                        : 'arr.fund.bulkModifications.specs',
                                )}
                            </FormLabel>
                            <FF
                                field={SimpleCheckListBox}
                                ref="specsListBox"
                                items={[
                                    {id: FILTER_NULL_VALUE, name: i18n('arr.fund.filterSettings.value.empty')},
                                    ...refType.descItemSpecs,
                                ]}
                                name={'specs'}
                            />
                        </FormGroup>
                    )}

                    {dataType.code === "STRUCTURED" && (
                        <FormGroup>
                            <FormLabel>
                                {i18n('arr.fund.bulkModifications.values')}
                            </FormLabel>
                            <FF
                                field={ValueCheckListBox}
                                ref="valuesListBox"
                                refType={refType}
                                versionId={versionId}
                                name={'values'}
                            />
                        </FormGroup>
                    )}

                    <FormGroup>
                        <FormLabel>{i18n('arr.fund.bulkModifications.itemsArea')}</FormLabel>
                        <Field
                            name="itemsArea"
                            component={ReduxFormFieldErrorDecorator}
                            renderComponent={Form.Check}
                            label={i18n('arr.fund.bulkModifications.itemsArea.page', allItemsCount)}
                            type="radio"
                            value={'page'}
                        />
                        {checkedItemsCount > 0 && checkedItemsCount < allItemsCount && (
                            <Field
                                name="itemsArea"
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Form.Check}
                                label={i18n('arr.fund.bulkModifications.itemsArea.selected', checkedItemsCount)}
                                type="radio"
                                value={'selected'}
                            />
                        )}
                        {uncheckedItemsCount > 0 && checkedItemsCount > 0 && (
                            <Field
                                name="itemsArea"
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Form.Check}
                                label={i18n('arr.fund.bulkModifications.itemsArea.unselected', checkedItemsCount)}
                                type="radio"
                                value={'unselected'}
                            />
                        )}
                        <Field
                            name="itemsArea"
                            component={ReduxFormFieldErrorDecorator}
                            renderComponent={Form.Check}
                            label={i18n('arr.fund.bulkModifications.itemsArea.all')}
                            type="radio"
                            value={'all'}
                        />
                    </FormGroup>

                    <Field
                        key={'operationType'}
                        name="operationType"
                        as={'select'}
                        component={FormInputField}
                        label={i18n('arr.fund.bulkModifications.operationType')}
                        disabled={submitting}
                    >
                        {this.supportFindAndReplace() && (
                            <option key="findAndReplace" value="findAndReplace">
                                {i18n('arr.fund.bulkModifications.operationType.findAndReplace')}
                            </option>
                        )}
                        {this.supportReplace() && (
                            <option key="replace" value="replace">
                                {i18n('arr.fund.bulkModifications.operationType.replace')}
                            </option>
                        )}
                        {this.supportSetSpecification() && (
                            <option key="setSpecification" value="setSpecification">
                                {i18n(
                                    this.isEnumType()
                                        ? 'arr.fund.bulkModifications.operationType.setEnum'
                                        : 'arr.fund.bulkModifications.operationType.setSpecification',
                                )}
                            </option>
                        )}
                        {this.supportSetValue() && (
                            <option key="setValue" value="setValue">
                                {i18n('arr.fund.bulkModifications.operationType.setEnum')}
                            </option>
                        )}
                        <option key="delete" value="delete">
                            {i18n('arr.fund.bulkModifications.operationType.delete')}
                        </option>
                    </Field>
                    {operationInputs}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary">
                        {i18n(submitButtonTitle)}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.close')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const formName = 'fundBulkModificationsForm';

const RF = reduxForm({
    form: formName,
})(FundBulkModificationsForm);

const formSelector = formValueSelector(formName);

export default connect((state, props) => {
    let val = '';

    if (props.dataType.code === 'UNITDATE') {
        val = {
            value: '',
        };
    }

    return {
        initialValues: props.initialValues || {
            findText: '',
            replaceText: val,
            itemsArea: getDefaultItemsArea(props),
            operationType: getDefaultOperationType(props),
            specs: {type: 'unselected'},
            values: {type: 'unselected'},
        },
        replaceText: formSelector(state, 'replaceText'),
        replaceSpec: formSelector(state, 'replaceSpec'),
        replaceValue: formSelector(state, 'replaceValue'),
        operationType: formSelector(state, 'operationType'),
        meta: getFormMeta(formName)(state),
        descItemTypes: state.refTables.descItemTypes,
    };
})(RF);
