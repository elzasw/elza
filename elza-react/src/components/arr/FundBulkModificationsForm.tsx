import React, { useEffect } from 'react';
import { FILTER_NULL_VALUE } from 'actions/arr/fundDataGrid';
import { WebApi } from 'actions/index';
import { descItemTypesFetchIfNeeded } from 'actions/refTables/descItemTypes';
import { getSpecsIds } from 'components/arr/ArrUtils';
import { submitForm } from 'components/form/FormUtils';
import {} from 'components/shared';
import { defineMessages, FormattedMessage, useIntl } from 'react-intl';
import { getIntl } from 'components/shared/lang/intlInstance';
import { globalMessages } from 'components/shared/lang/messages';
import { arrExtraMessages, arrMessages } from './messages';
import { Form, FormGroup, FormLabel, Modal } from 'react-bootstrap';
import { Field, Form as FinalForm } from 'react-final-form';
import { objectById } from "stores/app/utils";
import FormInputField from '../shared/form/FormInputField';
import ReduxFormFieldErrorDecorator from '../shared/form/ReduxFormFieldErrorDecorator';
import { Button } from '../ui';
import { validateDouble, validateInt } from '../validate';
import { getMapFromList } from './../../stores/app/utils';
import DatationField from './../party/DatationField';
import './FundBulkModificationsForm.scss';
import DescItemRecordRef from './nodeForm/DescItemRecordRef';
import DescItemUnitdate from './nodeForm/DescItemUnitdate';
import SimpleCheckListBox from './SimpleCheckListBox';
import { useThunkDispatch } from 'utils/hooks';
import { RulDataTypeVO } from 'api/RulDataTypeVO';
import { RulDataTypeCodeEnum } from 'api/RulDataTypeCodeEnum';
import { AppState } from 'typings/store';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import { FormState } from 'final-form';
import arrayMutators from 'final-form-arrays';
import { useSelector } from 'react-redux';
import DescItemLink from './nodeForm/DescItemLink';
import {
    BulkItemValue,
    DataBit,
    DataDecimal,
    DataEnum,
    DataFormattedText,
    DataInteger,
    DataRecordRef,
    DataString,
    DataStructureRef,
    DataText,
    DataType,
    DataUnitdate,
    DataUnitid,
    DataUriRef,
    ItemData,
} from 'elza-api';
import { BulkAppendLink, BulkAppendRecord, BulkAppendValue, BulkAppendValues } from './BulkAppendValues';

export enum OperationType {
    DELETE = "delete",
    SET_SPECIFICATION = "setSpecification",
    SET_VALUE = "setValue",
    FIND_AND_REPLACE = "findAndReplace",
    REPLACE = "replace",
    APPEND = "append",
}

const getDefaultItemsArea = ({
    allItemsCount,
    checkedItemsCount
}: {
    allItemsCount: number,
    checkedItemsCount: number
}) => {
    const showSelected = checkedItemsCount > 0 && checkedItemsCount < allItemsCount;

    if (showSelected) {
        return 'selected';
    } else {
        return 'page';
    }
};

/**
* Validace formuláře.
*/

const validate = (values: SubmitValues, props: { refType: RulDescItemTypeExtVO, dataType: RulDataTypeVO }) => {
    const errors: any = {};
    const isEnumType = props.dataType.code === RulDataTypeCodeEnum.ENUM;

    if (!values.operationType) {
        errors.operationType = getIntl().formatMessage(globalMessages.validationRequired);
    }

    // append adds values and filters nothing, so it has no specification list
    if (props.refType.useSpecification && values.operationType !== OperationType.APPEND) {
        const specsIds = values.specIds;
        if (specsIds.length === 0 && specsIds.indexOf(FILTER_NULL_VALUE) === -1) {
            errors.specs = getIntl().formatMessage(globalMessages.validationRequired);
        }
    }

    switch (values.operationType) {
        case OperationType.FIND_AND_REPLACE:
            if (!values.findText) {
                errors.findText = getIntl().formatMessage(globalMessages.validationRequired);
            }
            break;
        case OperationType.APPEND: {
            const rowErrors = values.appendValues.map(row => validateAppendValue(row, props));
            const hasRowErrors = rowErrors.some(rowError => Object.keys(rowError).length > 0);
            if (hasRowErrors) {
                errors.appendValues = rowErrors;
            }
            break;
        }
        case OperationType.REPLACE:
            if (!isEnumType && !values.replaceText) {
                errors.replaceText = getIntl().formatMessage(globalMessages.validationRequired);
            }

            switch (props.dataType.code) {
                case RulDataTypeCodeEnum.INT:
                    const result = validateInt(values.replaceText);
                    if (result) {
                        errors.replaceText = result;
                    }
                    break;

                case RulDataTypeCodeEnum.UNITDATE:
                    try {
                        DatationField.validate(values.replaceText);
                    } catch (err) {
                        errors.replaceText = err && err.message ? err.message : getIntl().formatMessage(globalMessages.validationRequired);
                    }
                    break;

                case RulDataTypeCodeEnum.RECORD_REF:
                    // console.log(222222222, values);

                    break;
                default:
                    break;
            }

            if (props.refType.useSpecification && !values.replaceSpec) {
                errors.replaceSpec = getIntl().formatMessage(globalMessages.validationRequired);
            }
            break;
        case OperationType.DELETE:
            break;
        case OperationType.SET_SPECIFICATION:
            if (!values.replaceSpec) {
                errors.replaceSpec = getIntl().formatMessage(globalMessages.validationRequired);
            }
            break;
        case OperationType.SET_VALUE:
            if (!values.replaceValueId) {
                errors.replaceValueId = getIntl().formatMessage(globalMessages.validationRequired);
            }
            break;
        default:
            break;
    }

    if (!values.itemsArea) {
        errors.itemsArea = getIntl().formatMessage(globalMessages.validationRequired);
    }

    return errors;
};

const bulkAppendValueMessages = defineMessages({
    notNumber: { id: 'arr.fund.bulkModifications.appendValues.notNumber', defaultMessage: 'Nejedná se o číslo' },
});

const isAppendRecord = (value: BulkAppendValue['value']): value is BulkAppendRecord =>
    typeof value === 'object' && value !== null && 'id' in value;

const isAppendLink = (value: BulkAppendValue['value']): value is BulkAppendLink =>
    typeof value === 'object' && value !== null && !('id' in value);

const getAppendText = (value: BulkAppendValue['value']) => (typeof value === 'string' ? value : '');

// the decimal comma is accepted as in the description item form
const parseAppendDecimal = (text: string) => Number(text.trim().replace(',', '.'));

const hasAppendValue = (value: BulkAppendValue['value'], dataTypeCode: string) => {
    switch (dataTypeCode) {
        case RulDataTypeCodeEnum.RECORD_REF:
        case RulDataTypeCodeEnum.STRUCTURED:
            return isAppendRecord(value) && !!value.id;
        case RulDataTypeCodeEnum.URI_REF:
            return isAppendLink(value) && !!value.value;
        default:
            return getAppendText(value).trim().length > 0;
    }
};

const validateAppendValue = (row: BulkAppendValue, props: { refType: RulDescItemTypeExtVO, dataType: RulDataTypeVO }) => {
    const rowErrors: { specId?: string, value?: string } = {};
    const requiredMessage = getIntl().formatMessage(globalMessages.validationRequired);

    if (props.refType.useSpecification && !row.specId) {
        rowErrors.specId = requiredMessage;
    }
    if (props.dataType.code === RulDataTypeCodeEnum.ENUM) {
        return rowErrors;
    }
    if (!hasAppendValue(row.value, props.dataType.code)) {
        rowErrors.value = requiredMessage;
        return rowErrors;
    }

    const text = getAppendText(row.value);
    switch (props.dataType.code) {
        case RulDataTypeCodeEnum.INT:
            rowErrors.value = validateInt(text) || undefined;
            break;
        case RulDataTypeCodeEnum.DECIMAL: {
            const isNumber = Number.isFinite(parseAppendDecimal(text));
            rowErrors.value = isNumber
                ? validateDouble(text) || undefined
                : getIntl().formatMessage(bulkAppendValueMessages.notNumber);
            break;
        }
        case RulDataTypeCodeEnum.UNITDATE:
            try {
                DatationField.validate(text);
            } catch (err) {
                rowErrors.value = err instanceof Error && err.message ? err.message : requiredMessage;
            }
            break;
        default:
            break;
    }
    if (!rowErrors.value) {
        delete rowErrors.value;
    }
    return rowErrors;
};

const createAppendItemData = (value: BulkAppendValue['value'], dataTypeCode: string): ItemData => {
    const text = getAppendText(value);
    switch (dataTypeCode) {
        case RulDataTypeCodeEnum.TEXT: {
            const data: DataText = { dataType: DataType.Text, textValue: text };
            return data;
        }
        case RulDataTypeCodeEnum.FORMATTED_TEXT: {
            const data: DataFormattedText = { dataType: DataType.FormattedText, value: text };
            return data;
        }
        case RulDataTypeCodeEnum.STRING: {
            const data: DataString = { dataType: DataType.String, stringValue: text };
            return data;
        }
        case RulDataTypeCodeEnum.UNITID: {
            const data: DataUnitid = { dataType: DataType.Unitid, unitId: text };
            return data;
        }
        case RulDataTypeCodeEnum.INT: {
            const data: DataInteger = { dataType: DataType.Int, integerValue: Number(text) };
            return data;
        }
        case RulDataTypeCodeEnum.DECIMAL: {
            const data: DataDecimal = { dataType: DataType.Decimal, value: parseAppendDecimal(text) };
            return data;
        }
        case RulDataTypeCodeEnum.UNITDATE: {
            const data: DataUnitdate = { dataType: DataType.Unitdate, value: text };
            return data;
        }
        case RulDataTypeCodeEnum.BIT: {
            const data: DataBit = { dataType: DataType.Bit, bitValue: text.toLowerCase() === 'true' };
            return data;
        }
        case RulDataTypeCodeEnum.RECORD_REF: {
            const data: DataRecordRef = { dataType: DataType.RecordRef, value: isAppendRecord(value) ? value.id : 0 };
            return data;
        }
        case RulDataTypeCodeEnum.STRUCTURED: {
            const data: DataStructureRef = {
                dataType: DataType.Structured,
                structuredObjectId: isAppendRecord(value) ? value.id : 0,
            };
            return data;
        }
        case RulDataTypeCodeEnum.URI_REF: {
            const link = isAppendLink(value) ? value : {};
            const data: DataUriRef = { dataType: DataType.UriRef, value: link.value ?? '', description: link.description };
            return data;
        }
        case RulDataTypeCodeEnum.ENUM: {
            const data: DataEnum = { dataType: DataType.Enum };
            return data;
        }
        default:
            throw new Error(`Bulk append does not support data type ${dataTypeCode}`);
    }
};

const createAppendItemValues = (rows: BulkAppendValue[], dataTypeCode: string): BulkItemValue[] =>
    rows.map(row => ({
        itemSpecId: row.specId ? Number(row.specId) : undefined,
        data: createAppendItemData(row.value, dataTypeCode),
    }));

interface Props {
    initialValues: Fields;
    dataType: RulDataTypeVO;
    refType: RulDescItemTypeExtVO;
    versionId: number;
    allItemsCount: number;
    checkedItemsCount: number;
    onClose: () => void;
    onSubmitForm: (values: SubmitValues) => void;
}

interface CheckboxListValue {
    type: "selected" | "unselected";
    ids: (number | string)[];
}

interface Fields {
    itemsArea: "page" | "selected" | "unselected" | "all";
    replaceSpec?: number;
    findText?: string;
    replaceText?: string;
    replaceValueId?: StructureDataSimple;
    operationType?: OperationType;
    specs: CheckboxListValue;
    appendValues: BulkAppendValue[];
}

type SubmitValues = Omit<Fields, "replaceValueId" | "specs"> & {
    replaceValueId?: number;
    specIds: (number | string | null)[];
    appendItemValues: BulkItemValue[];
}

interface StructureDataSimple {
    id: number,
    name: string,
}

/**
 * Formulář hledání a nahrazení.
 */
const FundBulkModificationsForm = ({
    initialValues,
    dataType,
    refType,
    versionId,
    allItemsCount,
    checkedItemsCount,
    onClose,
    onSubmitForm,
}: Props) => {
    const intl = useIntl();
    const structureTypes = useSelector((state: AppState) => state.refTables.structureTypes);

    const dispatch = useThunkDispatch();

    useEffect(() => {
        dispatch(descItemTypesFetchIfNeeded());
    }, [dispatch])

    const getIsFindAndReplaceSupported = () => {
        switch (dataType.code) {
            case RulDataTypeCodeEnum.TEXT:
            case RulDataTypeCodeEnum.STRING:
            case RulDataTypeCodeEnum.FORMATTED_TEXT:
            case RulDataTypeCodeEnum.UNITID:
                return true;
            default:
                return false;
        }
    };

    const getIsReplaceSupported = () => {
        switch (dataType.code) {
            case RulDataTypeCodeEnum.TEXT:
            case RulDataTypeCodeEnum.STRING:
            case RulDataTypeCodeEnum.FORMATTED_TEXT:
            case RulDataTypeCodeEnum.UNITID:
            case RulDataTypeCodeEnum.INT:
            case RulDataTypeCodeEnum.DECIMAL:
            case RulDataTypeCodeEnum.DATE:
            case RulDataTypeCodeEnum.UNITDATE:
            case RulDataTypeCodeEnum.RECORD_REF:
                return true;
            default:
                return false;
        }
    };

    const getIsAppendSupported = () => {
        switch (dataType.code) {
            case RulDataTypeCodeEnum.STRUCTURED:
                return getIsSharedStructureType();
            // the server does not append these data types
            case RulDataTypeCodeEnum.DATE:
            case RulDataTypeCodeEnum.COORDINATES:
            case RulDataTypeCodeEnum.FILE_REF:
            case RulDataTypeCodeEnum.JSON_TABLE:
                return false;
            case RulDataTypeCodeEnum.ENUM:
                return true;
            case RulDataTypeCodeEnum.TEXT:
            case RulDataTypeCodeEnum.STRING:
            case RulDataTypeCodeEnum.FORMATTED_TEXT:
            case RulDataTypeCodeEnum.UNITID:
            case RulDataTypeCodeEnum.INT:
            case RulDataTypeCodeEnum.DECIMAL:
            case RulDataTypeCodeEnum.UNITDATE:
            case RulDataTypeCodeEnum.RECORD_REF:
                return refType.useSpecification;
            default:
                return true;
        }
    };

    const getIsSetSpecificationSupported = () => {
        return refType.useSpecification;
    };

    /**
     * True when the objects of the item's structure type are shared, so one object can be assigned to many nodes.
     */
    const getIsSharedStructureType = () => {
        if (!structureTypes) {
            return true;
        }
        const structTypes = objectById(structureTypes.data, versionId, 'versionId');
        const structureType = structTypes ? objectById(structTypes.data, refType.structureTypeId) : null;
        return structureType ? !structureType.anonymous : false;
    };

    const getIsSetValueSupported = () => {
        switch (dataType.code) {
            case RulDataTypeCodeEnum.STRUCTURED:
                return getIsSharedStructureType();
            default:
                return false;
        }
    };

    /**
    * Transorm values for form submit
    */
    const transformValues = (values: Fields): SubmitValues => {

        // get specIds by selection type
        const specIds = getSpecsIds(
            refType,
            values.specs.type,
            values.specs.ids
        );

        // extend refType descItemSpecs with empty value only when it is ENUM
        if (
            (values.operationType != OperationType.SET_SPECIFICATION || getIsEnumType())
            && values.specs.type === "unselected"
            && refType.useSpecification
        ) {
            specIds.push(null);
        }

        const isAppend = values.operationType === OperationType.APPEND;
        return {
            ...values,
            replaceValueId: values.replaceValueId?.id,
            specIds,
            appendItemValues: isAppend ? createAppendItemValues(values.appendValues, dataType.code) : [],
        }
    }

    const validateForm = (values: Fields) => validate(transformValues(values), { refType, dataType });

    const handleSubmitForm = (values: Fields) => {
        const transformedValues = transformValues(values);
        submitForm(validate, transformedValues, { refType, dataType }, onSubmitForm, dispatch);
    }

    /**
     * Vrací true v případě, že atribut tvoří hodnotu pouze specifikací - enum.
     */
    const getIsEnumType = () => {
        return dataType.code === RulDataTypeCodeEnum.ENUM;
    };

    const findStructureData = async (value: string = ""): Promise<StructureDataSimple[]> => {
        const _structureTypes = objectById(structureTypes?.data, versionId, 'versionId');
        const structureType = _structureTypes ? objectById(_structureTypes.data, refType.structureTypeId) : undefined;

        if (!structureType) { return []; }

        const { rows } = await WebApi.findStructureData(versionId, structureType.code, value, true, 0, 50);
        return rows.map(({ id, value }) => ({
            id,
            name: value,
        }));
    }

    /**
     * Popisek potvrzovacího tlačítka podle typu úpravy.
     *
     * Klíč se dřív skládal z hodnoty enumu velkými písmeny
     * (`...action.FIND_AND_REPLACE`), ale v katalogu jsou klíče camelCase -
     * nikdy tedy nesedly a na tlačítku se vypisovalo "[klíč]".
     */
    const getSubmitButtonLabel = (operationType?: OperationType) => {
        switch (operationType) {
            case OperationType.FIND_AND_REPLACE:
                return arrMessages.fundBulkModificationsActionFindAndReplace;
            case OperationType.REPLACE:
                return arrMessages.fundBulkModificationsActionReplace;
            case OperationType.DELETE:
                return arrMessages.fundBulkModificationsActionDelete;
            case OperationType.SET_VALUE:
            case OperationType.SET_SPECIFICATION:
                return arrMessages.fundBulkModificationsActionSetSpecification;
            default:
                return globalMessages.save;
        }
    }

    const getOperationFields = (submitting: boolean, { values, errors }: FormState<Fields, Partial<Fields>>) => {
        const { operationType, replaceText, replaceSpec } = values;

        let operationInputs: React.ReactNode[] = [];

        switch (operationType) {
            case OperationType.SET_SPECIFICATION:
                operationInputs.push(
                    <Field
                        key={'replaceSpec'}
                        name="replaceSpec"
                        type={'select'}
                        component={FormInputField}
                        label={intl.formatMessage(
                            getIsEnumType()
                                ? arrMessages.fundBulkModificationsReplaceReplaceEnum
                                : arrMessages.fundBulkModificationsReplaceReplaceSpec,
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
            case OperationType.FIND_AND_REPLACE:
                operationInputs.push(
                    <Field
                        key={'findText'}
                        name="findText"
                        type={dataType.code === "TEXT" ? "textarea" : "text"}
                        component={FormInputField}
                        label={<FormattedMessage {...arrMessages.fundBulkModificationsFindAndRFeplaceFindText} />}
                        disabled={submitting}
                    />,
                );
                operationInputs.push(
                    <Field
                        key={'replaceText'}
                        name="replaceText"
                        type={dataType.code === "TEXT" ? "textarea" : "text"}
                        component={FormInputField}
                        label={<FormattedMessage {...arrMessages.fundBulkModificationsFindAndRFeplaceReplaceText} />}
                        disabled={submitting}
                    />,
                );
                break;
            case OperationType.APPEND:
                operationInputs.push(
                    <BulkAppendValues
                        key="appendValues"
                        name="appendValues"
                        dataType={dataType}
                        refType={refType}
                        getStructuredObjects={findStructureData}
                        disabled={submitting}
                    />,
                );
                break;
            case OperationType.REPLACE:
                if (refType.useSpecification) {
                    operationInputs.push(
                        <Field
                            key="replaceSpec"
                            name="replaceSpec"
                            type={'select'}
                            component={FormInputField}
                            label={intl.formatMessage(
                                getIsEnumType()
                                    ? arrMessages.fundBulkModificationsReplaceReplaceEnum
                                    : arrMessages.fundBulkModificationsReplaceReplaceSpec,
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
                    case RulDataTypeCodeEnum.UNITDATE:
                        operationInputs.push(
                            <Field
                                key="replaceText"
                                name="replaceText"
                                label={<FormattedMessage {...arrMessages.fundBulkModificationsReplaceReplaceText} />}
                            >{({ input }) => {
                                let data = {
                                    ...descItemProps,
                                    descItem: {
                                        error: {
                                            value: errors?.replaceText || null,
                                        },
                                        value: replaceText,
                                    },
                                };
                                const handleChange = (unitdateValue) => {
                                    input.onChange(unitdateValue.value);
                                }
                                return <DescItemUnitdate {...input} onChange={handleChange} {...data} />
                            }}
                            </Field>,
                        );
                        break;
                    case RulDataTypeCodeEnum.RECORD_REF:
                        operationInputs.push(
                            <Field
                                key="replaceText"
                                name="replaceText"
                                label={<FormattedMessage {...arrMessages.fundBulkModificationsReplaceReplaceText} />}
                            >
                                {({ input }) => {
                                    let specName = null;
                                    if (replaceSpec) {
                                        const specMap = getMapFromList(refType.descItemSpecs) as any;
                                        specName = specMap[replaceSpec].name;
                                    }

                                    let data = {
                                        ...descItemProps,
                                        itemTypeId: refType.id,
                                        itemName: refType.shortcut,
                                        specName: specName,
                                        descItem: {
                                            error: {
                                                value: errors?.replaceText || null,
                                            },
                                            record: replaceText,
                                            descItemSpecId: replaceSpec,
                                        },
                                    };
                                    return <DescItemRecordRef {...input} {...data} />
                                }}
                            </Field>
                        );
                        break;
                    case RulDataTypeCodeEnum.URI_REF:
                        operationInputs.push(
                            <Field
                                key="replaceText"
                                name="replaceText"
                                label={<FormattedMessage {...arrMessages.fundBulkModificationsReplaceReplaceText} />}
                            >
                                {({ input }) => {
                                    let data = {
                                        ...descItemProps,
                                        descItem: {
                                            error: {
                                                value: errors?.replaceText || null,
                                            },
                                            value: input.value.value,
                                            description: input.value.description,
                                        },
                                    };
                                    return <DescItemLink {...input} {...data} />
                                }}
                            </Field>
                        );
                        break;
                    case RulDataTypeCodeEnum.ENUM:
                        break;
                    default:
                        operationInputs.push(
                            <Field
                                key="replaceText"
                                name="replaceText"
                                type={dataType.code === "TEXT" ? "textarea" : "text"}
                                component={FormInputField}
                                label={<FormattedMessage {...arrMessages.fundBulkModificationsReplaceReplaceText} />}
                                disabled={submitting}
                            />,
                        );
                }

                break;
            case OperationType.SET_VALUE:
                operationInputs.push(
                    <Field
                        key={'replaceValueId'}
                        name="replaceValueId"
                        type={'asyncAutocomplete'}
                        component={FormInputField}
                        label={<FormattedMessage {...arrMessages.fundBulkModificationsReplaceReplaceEnum} />}
                        disabled={submitting}
                        getItems={(search: string) => findStructureData(search)}
                    />
                );
                break;
            default:
                break;
        }
        return operationInputs;
    }

    return (
        <FinalForm<Fields>
            onSubmit={handleSubmitForm}
            validate={validateForm}
            mutators={{ ...arrayMutators }}
            initialValues={initialValues || {
                findText: "",
                replaceText: "",
                itemsArea: getDefaultItemsArea({ allItemsCount, checkedItemsCount }),
                specs: { type: 'unselected', ids: [] },
                appendValues: [{ rowId: 0 }],
            }}
            keepDirtyOnReinitialize={true}
        >
            {({ handleSubmit, form, submitting }) => {

                const formState = form.getState();
                const uncheckedItemsCount = allItemsCount - checkedItemsCount;
                const operationInputs = getOperationFields(submitting, formState);
                const submitButtonTitle = getSubmitButtonLabel(formState.values.operationType);
                const showSpecsFilter = refType.useSpecification
                    && !!formState.values.operationType
                    && formState.values.operationType !== OperationType.APPEND;

                return <Form onSubmit={handleSubmit}>
                    <Modal.Body className="fund-bulk-modifications-container">
                        <h1>
                            {refType.name}
                        </h1>

                        <section>
                            {checkedItemsCount > 0 && checkedItemsCount < allItemsCount && (
                                <Field
                                    name="itemsArea"
                                    component={ReduxFormFieldErrorDecorator}
                                    renderComponent={Form.Check}
                                    label={<div>
                                        <span>{<FormattedMessage {...arrMessages.fundBulkModificationsItemsAreaSelected} />}</span>
                                        <span className="item-count-label">{checkedItemsCount}</span>
                                    </div>}
                                    type="radio"
                                    value={'selected'}
                                />
                            )}
                            {uncheckedItemsCount > 0 && checkedItemsCount > 0 && (
                                <Field
                                    name="itemsArea"
                                    component={ReduxFormFieldErrorDecorator}
                                    renderComponent={Form.Check}
                                    label={<div>
                                        <span>{<FormattedMessage {...arrMessages.fundBulkModificationsItemsAreaUnselected} />}</span>
                                        <span className="item-count-label">{uncheckedItemsCount}</span>
                                    </div>}
                                    type="radio"
                                    value={'unselected'}
                                />
                            )}
                            <Field
                                name="itemsArea"
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Form.Check}
                                label={<div>
                                    <span>{<FormattedMessage {...arrMessages.fundBulkModificationsItemsAreaPage} />}</span>
                                    <span className="item-count-label">{allItemsCount}</span>
                                </div>}
                                type="radio"
                                value={'page'}
                            />
                            <Field
                                name="itemsArea"
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Form.Check}
                                label={<div>
                                    <span>{<FormattedMessage {...arrMessages.fundBulkModificationsItemsAreaAll} />}</span>
                                </div>}
                                type="radio"
                                value={'all'}
                            />
                        </section>
                        <section>
                            <Field
                                key={'operationType'}
                                name="operationType"
                                type={'select'}
                                component={FormInputField}
                                disabled={submitting}
                                placeholder={intl.formatMessage(arrExtraMessages.fundBulkModificationsSelectOperation)}
                            >
                                <option value={""} selected={true}>{intl.formatMessage(arrExtraMessages.fundBulkModificationsSelectOperation)}</option>
                                {getIsFindAndReplaceSupported() && (
                                    <option key="findAndReplace" value={OperationType.FIND_AND_REPLACE}>
                                        {intl.formatMessage(arrMessages.fundBulkModificationsOperationTypeFindAndReplace)}
                                    </option>
                                )}
                                {getIsReplaceSupported() && (
                                    <option key="replace" value={OperationType.REPLACE}>
                                        {intl.formatMessage(arrMessages.fundBulkModificationsOperationTypeReplace)}
                                    </option>
                                )}
                                {getIsAppendSupported() && (
                                    <option key="append" value={OperationType.APPEND}>
                                        {intl.formatMessage(arrMessages.fundBulkModificationsOperationTypeAppend)}
                                    </option>
                                )}
                                {getIsSetSpecificationSupported() && (
                                    <option key="setSpecification" value={OperationType.SET_SPECIFICATION}>
                                        {intl.formatMessage(
                                            getIsEnumType()
                                                ? arrMessages.fundBulkModificationsOperationTypeSetEnum
                                                : arrMessages.fundBulkModificationsOperationTypeSetSpecification,
                                        )}
                                    </option>
                                )}
                                {getIsSetValueSupported() && (
                                    <option key="setValue" value={OperationType.SET_VALUE}>
                                        {intl.formatMessage(arrMessages.fundBulkModificationsOperationTypeSetEnum)}
                                    </option>
                                )}
                                <option key="delete" value={OperationType.DELETE}>
                                    {intl.formatMessage(arrMessages.fundBulkModificationsOperationTypeDelete)}
                                </option>
                            </Field>
                            {operationInputs}
                        </section>


                        {showSpecsFilter && (
                            <>
                                <div className="separator" />
                                <section>
                                    <FormLabel>
                                        {intl.formatMessage(
                                            getIsEnumType()
                                                ? arrMessages.fundBulkModificationsValues
                                                : arrMessages.fundBulkModificationsSpecs,
                                        )}
                                    </FormLabel>
                                    <Field
                                        name={'specs'}
                                    >{({ input }) => {
                                        return <SimpleCheckListBox
                                            {...input}
                                            items={
                                                formState.values.operationType != OperationType.SET_SPECIFICATION || getIsEnumType() ? [
                                                    {
                                                        id: FILTER_NULL_VALUE,
                                                        name: intl.formatMessage(arrMessages.fundFilterSettingsValueEmpty)
                                                    },
                                                    ...refType.descItemSpecs,
                                                ] : [...refType.descItemSpecs]
                                            }
                                        />
                                    }}</Field>
                                </section>
                            </>
                        )}

                    </Modal.Body>
                    <Modal.Footer>
                        <Button disabled={!formState.values.operationType} type="submit" variant="outline-secondary">
                            {intl.formatMessage(submitButtonTitle)}
                        </Button>
                        <Button variant="link" onClick={onClose}>
                            <FormattedMessage {...globalMessages.close} />
                        </Button>
                    </Modal.Footer>
                </Form>
            }}
        </FinalForm>
    );

}

export default FundBulkModificationsForm;
