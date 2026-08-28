import { FormInputField } from "components/shared";
import { Field, Form as FinalForm } from "react-final-form";
import { Modal, Button, Form } from "react-bootstrap";
import { AdminFunds, AipFilterEntry, ApAccessPoints } from "typings/store";
import { AREA_ADMIN_FUNDS, fundsFetchIfNeeded } from "actions/admin/fund";
import { AREA_ACCESS_POINTS, accessPointsFetchIfNeeded } from "actions/ap/accessPoints";
import { storeFromArea } from "shared/utils";
import { useEffect } from "react";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useAppThunkDispatch } from "utils/hooks";
import { AipFieldName, AipProblemType, QueueItemState } from "elza-api";
import { generateUUID } from "utils/uuid";
import { AipColumn } from "../../columns";
import { boolMessages, filterErrorMessages, filterMessages, problemMessages, queueStateMessages } from "../../messages";
import { globalMessages } from "components/shared/lang/messages";
import { IntlShape, defineMessages, useIntl } from "react-intl";
import {
    AipOperation,

    OPERATIONS,
    buildFilter,
    isNullaryOperation,
    isRangeOperation,
} from "../aipFilterModel";

interface Props {
    item: AipColumn;
    onSubmit: (entry: AipFilterEntry) => void;
    onClose: () => void;
}

export type AipFilterFormProps = Props;

interface FormValues {
    operation: AipOperation;
    value?: string;
    from?: string;
    to?: string;
}

interface Option {
    value: string;
    label: string;
}

const localMessages = defineMessages({
    loading: { id: "aip.filter.loading", defaultMessage: "Načítání…" },
});

const PROBLEM_TYPES = [
    AipProblemType.MetadataError,
    AipProblemType.UnknownFund,
    AipProblemType.UnknownInstitution,
];

const IMPORT_STATES = [
    QueueItemState.ImportError, QueueItemState.ImportNew, QueueItemState.ImportOk, QueueItemState.Update,
];

const EXPORT_STATES = [
    QueueItemState.ExportError, QueueItemState.ExportNew, QueueItemState.ExportOk,
];



function operationMessage(operation: AipOperation) {
    switch (operation) {
        case "EQ": return filterMessages.equals;
        case "NEQ": return filterMessages.notEquals;
        case "CONTAINS": return filterMessages.contain;
        case "NOT_CONTAINS": return filterMessages.notContain;
        case "BETWEEN": return filterMessages.between;
        case "IS_NULL": return filterMessages.isNull;
        case "NOT_NULL": return filterMessages.notNull;
    }
}

function selectOptions(item: AipColumn, funds: AdminFunds, accessPoints: ApAccessPoints, intl: IntlShape): Option[] | null {
    switch (item.valueType) {
        case "bool":
            return [
                {value: "true", label: intl.formatMessage(boolMessages.yes)},
                {value: "false", label: intl.formatMessage(boolMessages.no)},
            ];
        case "importState":
            return IMPORT_STATES.map(state => ({value: state, label: intl.formatMessage(queueStateMessages[state])}));
        case "exportState":
            return EXPORT_STATES.map(state => ({value: state, label: intl.formatMessage(queueStateMessages[state])}));
        case "problemType":
            return PROBLEM_TYPES.map(type => ({value: type, label: intl.formatMessage(problemMessages[type])}));
        case "ref": {
            const source = item.field === AipFieldName.Fund ? funds : accessPoints;
            if (!source.fetched || !source.rows) {
                return null;
            }
            return source.rows.map(row => ({value: String(row.id), label: row.name ?? String(row.id)}));
        }
        default:
            return null;
    }
}

function labelFor(values: FormValues, options: Option[] | null): string | undefined {
    if (isNullaryOperation(values.operation) || isRangeOperation(values.operation) || !options) {
        return undefined;
    }
    return options.find(option => option.value === String(values.value))?.label;
}

export function AipFilterForm({item, onSubmit, onClose}: Props) {
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const funds = useAppSelector(state => storeFromArea(state, AREA_ADMIN_FUNDS) as AdminFunds);
    const accessPoints = useAppSelector(state => storeFromArea(state, AREA_ACCESS_POINTS) as ApAccessPoints);
    const isRef = item.valueType === "ref";

    useEffect(() => {
        if (!isRef) {
            return;
        }
        dispatch(item.field === AipFieldName.Fund ? fundsFetchIfNeeded() : accessPointsFetchIfNeeded());
    }, [dispatch, isRef, item.field]);

    const options = selectOptions(item, funds, accessPoints, intl);
    if (isRef && options == null) {
        return <span>{intl.formatMessage(localMessages.loading)}</span>;
    }

    const operations = OPERATIONS[item.valueType];
    const isDate = item.valueType === "date";

    const validate = (values: FormValues) => {
        const errors: Partial<Record<keyof FormValues, string>> = {};
        if (isNullaryOperation(values.operation)) {
            return errors;
        }
        if (isRangeOperation(values.operation)) {
            if (isDate) {
                if (!values.from) errors.from = intl.formatMessage(filterErrorMessages.emptyDate);
                if (!values.to) errors.to = intl.formatMessage(filterErrorMessages.emptyDate);
            } else {
                if (!Number(values.from)) errors.from = intl.formatMessage(filterErrorMessages.nan);
                if (Number(values.from) < 0) errors.from = intl.formatMessage(filterErrorMessages.positiveNum);
                if (!Number(values.to)) errors.to = intl.formatMessage(filterErrorMessages.nan);
                if (Number(values.to) < 0) errors.to = intl.formatMessage(filterErrorMessages.positiveNum);
                if (Number(values.from) > Number(values.to)) errors.to = intl.formatMessage(filterErrorMessages.between);
            }
            return errors;
        }
        if (!values.value) {
            errors.value = intl.formatMessage(filterErrorMessages.value);
        }
        return errors;
    };

    const handleSubmit = (values: FormValues) => {
        onSubmit({
            id: generateUUID(),
            field: item.field,
            filter: buildFilter(item.field, item.valueType, values),
            label: labelFor(values, options),
        });
    };

    return (
        <FinalForm<FormValues>
            initialValues={{operation: operations[0], value: options?.[0]?.value}}
            onSubmit={handleSubmit}
            validate={validate}
        >
            {({submitting, handleSubmit, form, values}) => (
                <Form>
                    <Modal.Body>
                        <Form.Label>{intl.formatMessage(filterMessages.content)}</Form.Label>
                        {operations.map(operation => (
                            <Field name="operation" key={operation}>
                                {({input}) => (
                                    <FormInputField
                                        {...input}
                                        // FormInputField does not declare the radio variant
                                        // @ts-ignore
                                        type="radio"
                                        label={intl.formatMessage(operationMessage(operation))}
                                        checked={values.operation === operation}
                                        value={operation}
                                        onChange={() => form.change("operation", operation)}
                                    />
                                )}
                            </Field>
                        ))}

                        {isRangeOperation(values.operation) && (
                            <>
                                <Field
                                    component={FormInputField}
                                    type={isDate ? "date" : "number"}
                                    label={intl.formatMessage(filterMessages.from)}
                                    name="from"
                                />
                                <Field
                                    component={FormInputField}
                                    type={isDate ? "date" : "number"}
                                    label={intl.formatMessage(filterMessages.to)}
                                    name="to"
                                />
                            </>
                        )}

                        {!isNullaryOperation(values.operation) && !isRangeOperation(values.operation) && (
                            options ? (
                                <Field
                                    component={FormInputField}
                                    type="select"
                                    label={intl.formatMessage(filterMessages.value)}
                                    name="value"
                                >
                                    {options.map(({value, label}) => (
                                        <option key={value} value={value}>{label}</option>
                                    ))}
                                </Field>
                            ) : (
                                <Field
                                    component={FormInputField}
                                    type={item.valueType === "number" ? "number" : "text"}
                                    label={intl.formatMessage(filterMessages.value)}
                                    name="value"
                                />
                            )
                        )}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={handleSubmit} variant="outline-secondary" disabled={submitting}>
                            OK
                        </Button>
                        <Button onClick={onClose} variant="link">
                            {intl.formatMessage(globalMessages.cancel)}
                        </Button>
                    </Modal.Footer>
                </Form>
            )}
        </FinalForm>
    );
}
