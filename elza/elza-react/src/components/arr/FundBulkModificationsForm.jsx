//
import { FILTER_NULL_VALUE } from 'actions/arr/fundDataGrid';
import { WebApi } from 'actions/index';
import { descItemTypesFetchIfNeeded } from 'actions/refTables/descItemTypes';
import { getSpecsIds, hasDescItemTypeValue } from 'components/arr/ArrUtils';
import { submitForm } from 'components/form/FormUtils';
import { AbstractReactComponent, FormInput, i18n } from 'components/shared';
import React from 'react';
import { Form, FormGroup, FormLabel, Modal } from 'react-bootstrap';
import { Field, Form as FinalForm } from 'react-final-form';
import { connect } from 'react-redux';
import objectById from "../../shared/utils/objectById";
import FormInputField from '../shared/form/FormInputField';
import ReduxFormFieldErrorDecorator from '../shared/form/ReduxFormFieldErrorDecorator';
import { Button } from '../ui';
import { validateInt } from '../validate';
import { getMapFromList } from './../../stores/app/utils';
import DatationField from './../party/DatationField';
import './FundBulkModificationsForm.scss';
import DescItemRecordRef from './nodeForm/DescItemRecordRef';
import DescItemUnitdate from './nodeForm/DescItemUnitdate';
import SimpleCheckListBox from './SimpleCheckListBox';
import Tags from 'components/form/Tags';

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

const StructuredTypeField = ({refType, versionId, name}) => {
    return <Field name={name} >{({input}) => {
        const handleAddItem = (item) => {
            if(!(input.value.items || []).find(({id}) => (item.id === id))){
                input.onChange({
                    type: input.value.type,
                    items: [...(input.value.items || []), item]
                });
            }
        }

        const handleRemoveItem = (_item, index) => {
            if(index !== -1){
                const _array = [...input.value.items];
                _array.splice(index, 1)
                input.onChange(_array);
            }
        }

        const getItems = async (search) => {
            const response = await WebApi.getDescItemTypeValues(versionId, refType.id, search, null, 50)
            const valueItems = response.map(({id, value}) => ({id, name: value}));
            if(
                search === '' 
                    || i18n('arr.fund.filterSettings.value.empty').toLowerCase().indexOf(search) !== -1
            ) {
                valueItems.unshift({
                    id: -1,
                    name: i18n('arr.fund.filterSettings.value.empty'),
                })
            }
            return valueItems;
        }

        const renderTagItem = ({item}) => {
            return item.name
        }

        return <div>
            <FormInputField 
                {...input}
                type={'asyncAutocomplete'}
                disabled={false}
                getItems={getItems}
                onChange={handleAddItem}
                />
            <Tags 
                items={input.value?.items || []} 
                onRemove={handleRemoveItem} 
                renderItem={renderTagItem} 
                />
        </div>
    }}</Field>
}

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
                            errors.replaceText = err && err.message ? err.message : i18n('global.validation.required');
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
                if (!values.replaceValueId) {
                    errors.replaceValueId = i18n('global.validation.required');
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
        const {dataType, structureTypes, refType, versionId} = this.props;
        let result;

        switch (dataType.code) {
            case 'STRUCTURED':
                if (structureTypes) {
                    let structTypes = objectById(structureTypes.data, versionId, 'versionId');
                    let structureType = structTypes ? objectById(structTypes.data, refType.structureTypeId) : null;
                    return structureType ? !structureType.anonymous : false;
                }
                result = true;
                break;
            default:
                result = false;
                break;
        }

        return result;
    };

    /**
    * Transorm values for form submit
    */
    transformValues = (values) => {
        return {
            ...values,
            replaceValueId: values.replaceValueId?.id,
            values: {
                type: values.values.type,
                ids: (values.values?.items || []).map(({id}) => (id)),
            }
        }
    }

    handleSubmitForm = (values) =>{
        const transformedValues = this.transformValues(values);
        submitForm(FundBulkModificationsForm.validate, transformedValues, this.props, this.props.onSubmitForm, this.props.dispatch);
    }

    /**
     * Vrací true v případě, že atribut tvoří hodnotu pouze specifikací - enum.
     */
    isEnumType = () => {
        const {dataType} = this.props;
        return dataType.code === 'ENUM';
    };

    callValueSearch() {
        const {versionId, refType, dataType, structureTypes} = this.props;
        const {valueSearchText} = this.state;

        if (!hasDescItemTypeValue(dataType)) {
            // pokud nemá hodnotu, nemůžeme volat
            return;
        }

        if (dataType.code === "STRUCTURED") {

            WebApi.getDescItemTypeValues(versionId, refType.id, valueSearchText, null, 200).then(json => {
                var valueItems = json.map(i => ({id: i.id, name: i.value}));

                if (
                    valueSearchText === '' ||
                    i18n('arr.fund.filterSettings.value.empty').toLowerCase().indexOf(valueSearchText) !== -1
                ) {
                    // u prázdného hledání a případně u hledání prázdné hodnoty doplňujeme null položku
                    valueItems = [
                        {
                            id: -1,
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

    findStructureData = async (value = "") => {
        const {versionId, structureTypes: structureTypesRef, refType} = this.props;
        let structureTypes = objectById(structureTypesRef?.data, versionId, 'versionId');
        let structureType = objectById(structureTypes?.data, refType.structureTypeId);

        if (!structureType) { return; }

        const {rows} = await WebApi.findStructureData(versionId, structureType.code, value, true, 0, 50);
        return rows.map(({id, value}) => ({
            id, 
            name: value,
        }));
    }

    render() {
        const {
            allItemsCount,
            checkedItemsCount,
            refType,
            onClose,
            dataType,
            versionId,
        } = this.props;

        const getSubmitButtonLabel = (operationType) => {
            let submitButtonTitle;

            switch (operationType) {
                case 'setSpecification':
                    submitButtonTitle = 'arr.fund.bulkModifications.action.setSpecification';
                    break;
                case 'findAndReplace':
                    submitButtonTitle = 'arr.fund.bulkModifications.action.findAndReplace';
                    break;
                case 'replace':
                    submitButtonTitle = 'arr.fund.bulkModifications.action.replace';
                    break;
                case 'setValue':
                    submitButtonTitle = 'arr.fund.bulkModifications.action.setSpecification';
                    break;
                case 'delete':
                    submitButtonTitle = 'arr.fund.bulkModifications.action.delete';
                    break;
                default:
                break;
            }
            return submitButtonTitle;
        }

        const getOperationFields = (submitting, formState) => {
            const {operationType, replaceText, replaceSpec} = formState.values;
            const meta = formState.errors;
            const {allValueItems} = this.state;

            let operationInputs = [];

            console.log("bulk modification form", operationType, allValueItems)

            switch (operationType) {
                case 'setSpecification':
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
                    if (refType.useSpecification) {
                        operationInputs.push(
                            <Field
                                key="replaceSpec"
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
                                operationInputs.push(
                                    <Field
                                        key="replaceText"
                                        name="replaceText"
                                        label={i18n('arr.fund.bulkModifications.replace.replaceText')}
                                    >{({input}) => {
                                            let data = {
                                                ...descItemProps,
                                                descItem: {
                                                    error: {
                                                        value: meta.replaceText || null,
                                                    },
                                                    value: replaceText,
                                                },
                                            };
                                            const handleChange = (unitdateValue) => {
                                                input.onChange(unitdateValue.value);
                                            }
                                            return <DescItemUnitdate {...input} onChange={handleChange} {...data}/>
                                        }}
                                    </Field>,
                                );
                            }
                            break;
                        case 'RECORD_REF':
                        {
                                operationInputs.push(
                                    <Field
                                        key="replaceText"
                                        name="replaceText"
                                        label={i18n('arr.fund.bulkModifications.replace.replaceText')}
                                    >{({input}) => {
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
                                                        value: meta.replaceText || null,
                                                    },
                                                    record: replaceText,
                                                    descItemSpecId: replaceSpec,
                                                },
                                            };
                                            return <DescItemRecordRef {...input} {...data}/>
                                        }}</Field>
                                );
                            }
                            break;
                        default:
                        operationInputs.push(
                            <Field
                                key="replaceText"
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
                    operationInputs.push(
                        <Field
                            key={'replaceValueId'}
                            name="replaceValueId"
                            type={'asyncAutocomplete'}
                            component={FormInputField}
                            label={i18n('arr.fund.bulkModifications.replace.replaceEnum')}
                            disabled={submitting}
                            getItems={(search) => this.findStructureData(search)}
                        />
                    );
                    break;
                default:
                break;
            }
            return operationInputs;
        }

        return (
            <FinalForm 
                onSubmit={this.handleSubmitForm}
                initialValues={this.props.initialValues || {
                    findText: "",
                    replaceText: "",
                    itemsArea: getDefaultItemsArea(this.props),
                    operationType: getDefaultOperationType(this.props),
                    specs: {type: 'unselected'},
                    values: {type: 'selected'},
                }}
            >
                {({handleSubmit, pristine, form, submitting})=>{

                    const formState = form.getState();
                    const uncheckedItemsCount = allItemsCount - checkedItemsCount;
                    const operationInputs = getOperationFields(submitting, formState);
                    const submitButtonTitle = getSubmitButtonLabel(formState.values.operationType);

                    return <Form onSubmit={handleSubmit}>
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
                                    <Field
                                        name={'specs'}
                                    >{({input})=>{
                                            return <SimpleCheckListBox 
                                                {...input}
                                                items={[
                                                    {id: FILTER_NULL_VALUE, name: i18n('arr.fund.filterSettings.value.empty')},
                                                    ...refType.descItemSpecs,
                                                ]}
                                                />
                                        }}</Field>
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
                                        label={i18n('arr.fund.bulkModifications.itemsArea.unselected', uncheckedItemsCount)}
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
                }}
            </FinalForm>
        );
    }
}

export default connect((state) => {
    return {
        descItemTypes: state.refTables.descItemTypes,
        structureTypes: state.refTables.structureTypes,
    };
})(FundBulkModificationsForm);
