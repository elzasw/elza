import { FormInputField, i18n } from "components/shared";
import { Field, Form as FinalForm } from "react-final-form"
import { DaAipDetailVO } from "api/DaAipDetailVO";
import { Modal, Button, Form } from "react-bootstrap";
import { AipFilter } from "typings/store";
import { AipFilterFormProps, SelectionOptions } from "./AipFilterFormProps";
import { AipFilterCriteria } from "./EnumAipFilterCriteria";
import { AREA_ADMIN_FUNDS, fundsFetchIfNeeded } from "actions/admin/fund";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { useEffect } from "react";
import { useThunkDispatch } from "utils/hooks";
import { AREA_ACCESS_POINTS, accessPointsFetchIfNeeded } from "actions/ap/accessPoints";

const AipRefFilterForm = ({item, onSubmit, onClose}: AipFilterFormProps) => {
    const funds = useSelector((state: any) => storeFromArea(state, AREA_ADMIN_FUNDS));
    const accessPoints = useSelector((state: any) => storeFromArea(state, AREA_ACCESS_POINTS));
    const dispatch = useThunkDispatch();

    const validate = (values: AipFilter) => {
        const errors: Partial<Record<keyof AipFilter, string>> = {};
        if(values.value == null) {
            errors.value = i18n("aip.form.error.value");
        }
        return errors;
    };

    useEffect(() => {
        if (item.key == "fund.name") {
            dispatch(fundsFetchIfNeeded());
        } else {
            dispatch(accessPointsFetchIfNeeded());
        }
    }, []);

    let selectValues;
    if (item.key == "fund.name") {
        selectValues = funds.fetched && funds.rows.map(item => ({value: item.id, label: item.name}));
    } else {
        selectValues = accessPoints.fetched && accessPoints.rows.map(item => ({value: item.id, label: item.name}));
    }

    if (!selectValues) {
        return <span>Načítání..</span>
    }

    return (
        <FinalForm<AipFilter>
            initialValues={{
                attr: item.key as keyof DaAipDetailVO,
                criteria: AipFilterCriteria.EQUALS,
                value: selectValues[0].value,
                path: item.path,
                label: selectValues[0].label,
            }}
            onSubmit={onSubmit}
            validate={validate}
        >
        {({ submitting, handleSubmit, values, form }) => {
            const inputVisible =
                values.criteria == AipFilterCriteria.EQUALS ||
                values.criteria == AipFilterCriteria.DOES_NOT_CONTAIN;

            return (
                <Form>
                    <Modal.Body>
                        <Form.Label>{i18n("aip.form.content")}</Form.Label>
                        <Field name="criteria">
                            {({ input }) => (
                                <FormInputField
                                    {...input}
                                    // @ts-ignore
                                    type="radio"
                                    label={i18n("aip.form.contain")}
                                    checked={values.criteria == AipFilterCriteria.EQUALS}
                                    value={AipFilterCriteria.EQUALS}
                                    onChange={() => form.change("criteria", AipFilterCriteria.EQUALS)}
                                />
                            )}
                        </Field>
                        <Field name="criteria">
                            {({ input }) => (
                                <FormInputField
                                    {...input}
                                    // @ts-ignore
                                    type="radio"
                                    label={i18n("aip.form.notContain")}
                                    checked={values.criteria == AipFilterCriteria.DOES_NOT_CONTAIN}
                                    value={AipFilterCriteria.DOES_NOT_CONTAIN}
                                    onChange={() => form.change("criteria", AipFilterCriteria.DOES_NOT_CONTAIN)}
                                />
                            )}
                        </Field>
                        <Field name="criteria">
                            {({ input }) => (
                                <FormInputField
                                    {...input}
                                    // @ts-ignore
                                    type="radio"
                                    label={i18n("aip.form.notNull")}
                                    checked={values.criteria == AipFilterCriteria.IS_NOT_NULL}
                                    value={AipFilterCriteria.IS_NOT_NULL}
                                    onChange={() => form.change("criteria", AipFilterCriteria.IS_NOT_NULL)}
                                />
                            )}
                        </Field>
                        <Field name="criteria">
                            {({ input }) => (
                                <FormInputField
                                    {...input}
                                    // @ts-ignore
                                    type="radio"
                                    label={i18n("aip.form.null")}
                                    checked={values.criteria == AipFilterCriteria.IS_NULL}
                                    value={AipFilterCriteria.IS_NULL}
                                    onChange={() => form.change("criteria", AipFilterCriteria.IS_NULL)}
                                />
                            )}
                        </Field>

                        {inputVisible && <Field name="criteria">
                            {({ input, value }) => (
                                // @ts-ignore
                                <FormInputField
                                    {...input}
                                    type="select"
                                    label={i18n("aip.form.value")}
                                    value={value}
                                    onChange={(e) => {
                                        form.change("value", e.target.value);
                                        form.change("label", e.target.options[e.target.selectedIndex].label);
                                    }}
                                >
                                    {!selectValues && <span>Načítání...</span>}
                                    {selectValues && selectValues.map(({value, label}: SelectionOptions, index: number) =>
                                        <option key={index} value={Number(value)} label={label}>
                                            {label}
                                        </option>
                                    )}
                                </FormInputField>
                            )}
                        </Field> }

                    </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={handleSubmit} variant="outline-secondary" disabled={submitting}>
                            OK
                        </Button>
                        <Button onClick={onClose} variant="link">
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </Form>
            )
        }}
        </FinalForm>
    )
}

export default AipRefFilterForm;
