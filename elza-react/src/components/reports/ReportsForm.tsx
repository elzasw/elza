import {
    Input,
    Label,
    Button,
    // Select,
    Dropdown,
    Option,
    OptionGroup,
    // SelectionEvents,
} from "@fluentui/react-components";
// import { DatePicker } from "@fluentui/react-datepicker-compat";
// import type { InputProps } from "@fluentui/react-components";
import { Field, Form } from "react-final-form";
import { /* ChangeEvent, */ useEffect, useState } from "react";
import { Api } from "api";
import {
    ReportReportCategory,
    ReportReportDefinition,
    ReportReportParamDefinition,
    ReportReportParameters,
    ReportValue,
    // ReportValueAccesspointId,
    ReportValueDate,
    // ReportValueFondId,
    ReportValueInteger,
    ReportValueString,
    ReportValueType,
} from "elza-api";
import { FormattedMessage, useIntl, defineMessages } from "react-intl";

const messages = defineMessages({
    selectReport: {
        id: "admin_reports_select_report",
        defaultMessage: "Vyberte přehled...",
    },
    reset: {
        id: "admin_reports_form_reset",
        defaultMessage: "Reset",
    },
    show: {
        id: "admin_reports_form_show",
        defaultMessage: "Zobrazit",
    },
    reportDefinition: {
        id: "admin_reports_form_reportDefinition",
        defaultMessage: "Přehled"
    }
})

// export type Fields = Record<string, ReportValueAccesspointId | ReportValueFondId | ReportValueString | ReportValueInteger | ReportValueDate> & { definitionCode: string }
export type _Fields = Record<string, string | number> & { definitionCode: string };

export interface Props {
    onSubmit: (definitionCode: string, values: ReportReportParameters) => void;
}

interface FieldProps {
    label: string;
    name: string;
    isRequired: boolean;
}

export function FluentNumberField({ name, label, isRequired }: FieldProps) {
    return (
        <Field name={name}>
            {({ input }) => {
                const { /* type, */ ..._input } = input;
                return (
                    <div style={{ display: "flex", flexDirection: "column" }}>
                        <Label htmlFor={name}>
                            {label}
                            {isRequired ? "*" : ""}
                        </Label>
                        <Input {..._input} id={name} type="number"></Input>
                    </div>
                );
            }}
        </Field>
    );
}

export function FluentStringField({ name, label, isRequired }: FieldProps) {
    return (
        <Field name={name}>
            {({ input }) => {
                const { /* type, */ ..._input } = input;
                return (
                    <div style={{ display: "flex", flexDirection: "column" }}>
                        <Label htmlFor={name}>
                            {label}
                            {isRequired ? "*" : ""}
                        </Label>
                        <Input {..._input} id={name} type="text"></Input>
                    </div>
                );
            }}
        </Field>
    );
}

export function FluentDateField({ name, label, isRequired }: FieldProps) {
    return (
        <Field name={name}>
            {({ input }) => {
                const { /* type, */ ..._input } = input;
                return (
                    <div style={{ display: "flex", flexDirection: "column" }}>
                        <Label htmlFor={name}>
                            {label}
                            {isRequired ? "*" : ""}
                        </Label>
                        <Input {..._input} id={name} type="date"></Input>
                    </div>
                );
            }}
        </Field>
    );
}

function transformValue(value: any, paramDefinition: ReportReportParamDefinition) {
    if (!paramDefinition) {
        return undefined;
    }
    const { code, paramType } = paramDefinition;

    const transformedValue: ReportValue = { valueType: paramType };
    if (paramType === ReportValueType.Int) {
        return {
            ...transformedValue,
            intValue: value,
        } as ReportValueInteger;
    }
    if (paramType === ReportValueType.String) {
        return {
            ...transformedValue,
            textValue: value,
        } as ReportValueString;
    }
    if (paramType === ReportValueType.Date) {
        return {
            ...transformedValue,
            dateValue: new Date(value).toISOString(),
        } as ReportValueDate;
    }
}

export function ReportsForm({ onSubmit }: Props) {
    const [reportCategories, setReportCategories] = useState<ReportReportCategory[]>([]);
    const { formatMessage } = useIntl();

    useEffect(() => {
        (async () => {
            const { data: categories } = await Api.report.reportGetDefinitions();
            setReportCategories(categories);
        })();
    }, []);

    function getReportDefinition(definitionCode: string) {
        let reportDefinition: ReportReportDefinition | undefined;
        reportCategories?.forEach(({ reportDefinitions }) => {
            reportDefinition = reportDefinitions?.find(({ code }) => code === definitionCode);
        });
        return reportDefinition;
    }

    function handleSubmit(values: _Fields) {
        const transformedValues: ReportReportParameters = { params: [] };
        const { params } = getReportDefinition(values.definitionCode);

        Object.entries(values).forEach(([key, value]) => {
            const paramDefinition = params.find(({ code }) => code === key);
            // skip unknown/added types e.g. definitionCode;
            if(!paramDefinition) {
                return;
            }
            const _value = transformValue(value, paramDefinition);
            transformedValues.params.push({
                code: key,
                values: _value ? [_value] : [],
            });
        });

        onSubmit(values.definitionCode, transformedValues);
    }

    function validate(values: _Fields) {
        const errors: Record<string, string> = {}
        try {
            if (!values.definitionCode) {
                errors.definitionCode = "Required"
                return errors;
            }

            const { params } = getReportDefinition(values.definitionCode);
            params?.forEach(({ code, required }) => {
                if (!values[code] && required) {
                    errors[code] = `${code} is required`
                }
            })
        } catch (error) {
            console.error(error);
        }
        return errors;
    }

    return (
        <div style={{ overflow: "auto", height: "100%", padding: "20px" }}>
            <Form<_Fields> onSubmit={handleSubmit} validate={validate}>
                {({ handleSubmit, values, form, valid }) => {
                    function handleReset() {
                        form.reset();
                    }
                    let params: ReportReportParamDefinition[] = [];
                    let selectedValue: string = values.definitionCode;

                    try {
                        const { name, params: _params } = getReportDefinition(values.definitionCode);
                        selectedValue = name;
                        params = _params;
                    } catch (error) {
                        console.error(error);
                    }

                    return (
                        <form style={{ display: "flex", flexDirection: "column" }}>
                            <Field name="definitionCode">
                                {({ input }) => {
                                    function handleChange(e: any, data: any) {
                                        form.change("definitionCode", data.optionValue);
                                    }

                                    return (
                                        <div style={{ display: "flex", flexDirection: "column" }}>
                                            <Label htmlFor="definitionCode">
                                                <FormattedMessage {...messages.reportDefinition} />
                                            </Label>
                                            <Dropdown
                                                {...input}
                                                value={selectedValue}
                                                // selectedOptions={[selectedValue]}
                                                onOptionSelect={handleChange}
                                                type="button"
                                                id="definitionCode"
                                                placeholder={formatMessage(messages.selectReport)}
                                            >
                                                {reportCategories.map(({ name, code, reportDefinitions }) => {
                                                    return (
                                                        <OptionGroup key={code} label={name}>
                                                            {reportDefinitions.map(({ name, code }) => {
                                                                return <Option value={code}>{name}</Option>;
                                                            })}
                                                        </OptionGroup>
                                                    );
                                                })}
                                            </Dropdown>
                                        </div>
                                    );
                                }}
                            </Field>
                            {params?.map(({ code, name, required, paramType }) => {
                                if (paramType === ReportValueType.Int) {
                                    return <FluentNumberField
                                        name={code}
                                        label={formatMessage({ id: `admin_reports_form_property_${name}` })}
                                        isRequired={required}
                                    />;
                                }
                                if (paramType === ReportValueType.String) {
                                    return <FluentStringField
                                        name={code}
                                        label={formatMessage({ id: `admin_reports_form_property_${name}` })}
                                        isRequired={required}
                                    />;
                                }
                                if (paramType === ReportValueType.Date) {
                                    return <FluentDateField
                                        name={code}
                                        label={formatMessage({ id: `admin_reports_form_property_${name}` })}
                                        isRequired={required}
                                    />;
                                }
                                return (
                                    <div>
                                        <Label>{name}</Label>
                                        <div>unknown type {paramType}</div>
                                    </div>
                                );
                            })}
                            <div style={{ margin: "-8px", marginTop: "32px", display: "flex", justifyContent: "flex-end" }}>
                                <Button style={{ margin: "8px" }} onClick={handleReset}>
                                    <FormattedMessage {...messages.reset} />
                                </Button>
                                <Button style={{ margin: "8px" }} appearance="primary" onClick={handleSubmit} disabled={!valid}>
                                    <FormattedMessage {...messages.show} />
                                </Button>
                            </div>
                        </form>
                    );
                }}
            </Form>
        </div>
    );
}

export default ReportsForm;
