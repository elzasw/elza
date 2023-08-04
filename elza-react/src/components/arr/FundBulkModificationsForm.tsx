import React, { useEffect } from 'react';
import { FILTER_NULL_VALUE } from 'actions/arr/fundDataGrid';
import { WebApi } from 'actions/index';
import { descItemTypesFetchIfNeeded } from 'actions/refTables/descItemTypes';
import { getSpecsIds } from 'components/arr/ArrUtils';
import { submitForm } from 'components/form/FormUtils';
import { i18n } from 'components/shared';
import { Form, FormGroup, FormLabel, Modal } from 'react-bootstrap';
import { Field, Form as FinalForm } from 'react-final-form';
import { objectById } from "stores/app/utils";
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
import { useThunkDispatch } from 'utils/hooks';
import { RulDataTypeVO } from 'api/RulDataTypeVO';
import { RulDataTypeCodeEnum } from 'api/RulDataTypeCodeEnum';
import { AppState } from 'typings/store';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import { FormState } from 'final-form';
import { useSelector } from 'react-redux';

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

    if (!values.operationType) {
        errors.operationType = i18n('global.validation.required');
    }

    if (props.refType.useSpecification) {
        const specsIds = values.specIds;
        if (specsIds.length === 0 && specsIds.indexOf(FILTER_NULL_VALUE) === -1) {
            errors.specs = i18n('global.validation.required');
        }
    }

    switch (values.operationType) {
        case OperationType.FIND_AND_REPLACE:
            if (!values.findText) {
                errors.findText = i18n('global.validation.required');
            }
            break;
        case OperationType.REPLACE:
        case OperationType.APPEND:
            if (!values.replaceText) {
                errors.replaceText = i18n('global.validation.required');
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
                        errors.replaceText = err && err.message ? err.message : i18n('global.validation.required');
                    }
                    break;

                case RulDataTypeCodeEnum.RECORD_REF:
                    // console.log(222222222, values);

                    break;
                default:
                    break;
            }

            if (props.refType.useSpecification && !values.replaceSpec) {
                errors.replaceSpec = i18n('global.validation.required');
            }
            break;
        case OperationType.DELETE:
            break;
        case OperationType.SET_SPECIFICATION:
            if (!values.replaceSpec) {
                errors.replaceSpec = i18n('global.validation.required');
            }
            break;
        case OperationType.SET_VALUE:
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
}

type SubmitValues = Omit<Fields, "replaceValueId" | "specs"> & {
    replaceValueId?: number;
    specIds: (number | string | null)[];
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
        // !useSpecification && (INT, STRING, TEXT, UNITDATE, UNITID, BIT, FORMATTED_TEXT, )
        switch (dataType.code) {
            case RulDataTypeCodeEnum.ENUM:
                return false;
            case RulDataTypeCodeEnum.TEXT:
            case RulDataTypeCodeEnum.STRING:
            case RulDataTypeCodeEnum.FORMATTED_TEXT:
            case RulDataTypeCodeEnum.UNITID:
            case RulDataTypeCodeEnum.INT:
            case RulDataTypeCodeEnum.DECIMAL:
            case RulDataTypeCodeEnum.DATE:
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

    const getIsSetValueSupported = () => {
        switch (dataType.code) {
            case RulDataTypeCodeEnum.STRUCTURED:
                if (structureTypes) {
                    let structTypes = objectById(structureTypes.data, versionId, 'versionId');
                    let structureType = structTypes ? objectById(structTypes.data, refType.structureTypeId) : null;
                    return structureType ? !structureType.anonymous : false;
                }
                return true;
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

        return {
            ...values,
            replaceValueId: values.replaceValueId?.id,
            specIds,
        }
    }

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

    const findStructureData = async (value: string = ""): Promise<StructureDataSimple[] | undefined> => {
        const _structureTypes = objectById(structureTypes?.data, versionId, 'versionId');
        const structureType = _structureTypes ? objectById(_structureTypes.data, refType.structureTypeId) : undefined;

        if (!structureType) { return []; }

        const { rows } = await WebApi.findStructureData(versionId, structureType.code, value, true, 0, 50);
        return rows.map(({ id, value }) => ({
            id,
            name: value,
        }));
    }

    const getSubmitButtonLabel = (operationType?: OperationType) => {
        switch (operationType) {
            case OperationType.FIND_AND_REPLACE:
            case OperationType.REPLACE:
            case OperationType.DELETE:
                return `arr.fund.bulkModifications.action.${operationType}`;
            case OperationType.SET_VALUE:
            case OperationType.SET_SPECIFICATION:
                return `arr.fund.bulkModifications.action.setSpecification`;
            default:
                return `global.action.store`;
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
                        as={'select'}
                        component={FormInputField}
                        label={i18n(
                            getIsEnumType()
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
            case OperationType.FIND_AND_REPLACE:
                operationInputs.push(
                    <Field
                        key={'findText'}
                        name="findText"
                        type={dataType.code === "TEXT" ? "textarea" : "text"}
                        component={FormInputField}
                        label={i18n('arr.fund.bulkModifications.findAndRFeplace.findText')}
                        disabled={submitting}
                    />,
                );
                operationInputs.push(
                    <Field
                        key={'replaceText'}
                        name="replaceText"
                        type={dataType.code === "TEXT" ? "textarea" : "text"}
                        component={FormInputField}
                        label={i18n('arr.fund.bulkModifications.findAndRFeplace.replaceText')}
                        disabled={submitting}
                    />,
                );
                break;
            case OperationType.APPEND:
            case OperationType.REPLACE:
                if (refType.useSpecification) {
                    operationInputs.push(
                        <Field
                            key="replaceSpec"
                            name="replaceSpec"
                            as={'select'}
                            component={FormInputField}
                            label={i18n(
                                getIsEnumType()
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
                    case RulDataTypeCodeEnum.UNITDATE:
                        operationInputs.push(
                            <Field
                                key="replaceText"
                                name="replaceText"
                                label={i18n('arr.fund.bulkModifications.replace.replaceText')}
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
                                label={i18n('arr.fund.bulkModifications.replace.replaceText')}
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
                    default:
                        operationInputs.push(
                            <Field
                                key="replaceText"
                                name="replaceText"
                                type={dataType.code === "TEXT" ? "textarea" : "text"}
                                component={FormInputField}
                                label={i18n('arr.fund.bulkModifications.replace.replaceText')}
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
                        label={i18n('arr.fund.bulkModifications.replace.replaceEnum')}
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
            initialValues={initialValues || {
                findText: "",
                replaceText: "",
                itemsArea: getDefaultItemsArea({ allItemsCount, checkedItemsCount }),
                specs: { type: 'unselected' },
            }}
        >
            {({ handleSubmit, form, submitting }) => {

                const formState = form.getState();
                const uncheckedItemsCount = allItemsCount - checkedItemsCount;
                const operationInputs = getOperationFields(submitting, formState);
                const submitButtonTitle = getSubmitButtonLabel(formState.values.operationType);

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
                                        <span>{i18n('arr.fund.bulkModifications.itemsArea.selected')}</span>
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
                                        <span>{i18n('arr.fund.bulkModifications.itemsArea.unselected')}</span>
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
                                    <span>{i18n('arr.fund.bulkModifications.itemsArea.page')}</span>
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
                                    <span>{i18n('arr.fund.bulkModifications.itemsArea.all')}</span>
                                </div>}
                                type="radio"
                                value={'all'}
                            />
                        </section>
                        <section>
                            <Field
                                key={'operationType'}
                                name="operationType"
                                as={'select'}
                                component={FormInputField}
                                disabled={submitting}
                                placeholder="Vyberte operaci..."
                            >
                                <option value={""} selected={true}>Vyberte operaci...</option>
                                {getIsFindAndReplaceSupported() && (
                                    <option key="findAndReplace" value={OperationType.FIND_AND_REPLACE}>
                                        {i18n('arr.fund.bulkModifications.operationType.findAndReplace')}
                                    </option>
                                )}
                                {getIsReplaceSupported() && (
                                    <option key="replace" value={OperationType.REPLACE}>
                                        {i18n('arr.fund.bulkModifications.operationType.replace')}
                                    </option>
                                )}
                                {getIsAppendSupported() && (
                                    <option key="append" value={OperationType.APPEND}>
                                        {i18n('arr.fund.bulkModifications.operationType.append')}
                                    </option>
                                )}
                                {getIsSetSpecificationSupported() && (
                                    <option key="setSpecification" value={OperationType.SET_SPECIFICATION}>
                                        {i18n(
                                            getIsEnumType()
                                                ? 'arr.fund.bulkModifications.operationType.setEnum'
                                                : 'arr.fund.bulkModifications.operationType.setSpecification',
                                        )}
                                    </option>
                                )}
                                {getIsSetValueSupported() && (
                                    <option key="setValue" value={OperationType.SET_VALUE}>
                                        {i18n('arr.fund.bulkModifications.operationType.setEnum')}
                                    </option>
                                )}
                                <option key="delete" value={OperationType.DELETE}>
                                    {i18n('arr.fund.bulkModifications.operationType.delete')}
                                </option>
                            </Field>
                            {operationInputs}
                        </section>


                        {refType.useSpecification && formState.values.operationType && (
                            <>
                                {refType.useSpecification && formState.values.operationType && <div className="separator" />}
                                <section>
                                    <FormLabel>
                                        {i18n(
                                            getIsEnumType()
                                                ? 'arr.fund.bulkModifications.values'
                                                : 'arr.fund.bulkModifications.specs',
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
                                                        name: i18n('arr.fund.filterSettings.value.empty')
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

export default FundBulkModificationsForm;
