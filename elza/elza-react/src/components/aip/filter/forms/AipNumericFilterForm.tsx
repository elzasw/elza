import { FormInputField, i18n } from "components/shared";
import { Field, Form as FinalForm } from "react-final-form"
import { Modal, Button, Form } from "react-bootstrap";
import { AipFilter } from "typings/store";
import { AipFilterFormProps } from "./AipFilterFormProps";
import { AipFilterCriteria } from "./EnumAipFilterCriteria";
import {AipDetailVO} from "elza-api";

const AipNumericFilterForm = ({item, onSubmit, onClose}: AipFilterFormProps) => {
    const validate = (values: AipFilter) => {
        const errors: Partial<Record<keyof AipFilter, string>> = {};

        if(values.criteria != AipFilterCriteria.BETWEEN) return errors;
        if(item.type == "number") {
            if(!Number(values.from)) errors.from = i18n("aip.form.error.nan");
            if(Number(values.from) < 0) errors.from =  i18n("aip.form.error.positiveNum");
            if(!Number(values.to)) errors.to = i18n("aip.form.error.nan");
            if(Number(values.to) < 0) errors.to = i18n("aip.form.error.positiveNum");
            if(Number(values.from) > Number(values.to)) errors.to = i18n("aip.form.error.between");
        } else if(item.type == "date") {
            if(values.from == null) errors.from = i18n("aip.form.error.emptyDate");
            if(values.to == null) errors.to = i18n("aip.form.error.emptyDate");
        }

        return errors;
    };


    return (
        <FinalForm<AipFilter>
            initialValues={{
                attr: item.key as keyof AipDetailVO,
                criteria: AipFilterCriteria.BETWEEN,
                path: item.path,
            }}
            onSubmit={onSubmit}
            validate={validate}
        >
        {({ submitting, handleSubmit, form, values }) => {
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
                                    label={i18n("aip.form.between")}
                                    checked={values.criteria == AipFilterCriteria.BETWEEN}
                                    value={AipFilterCriteria.BETWEEN}
                                    onChange={() => form.change("criteria", AipFilterCriteria.BETWEEN)}
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

                        {values.criteria == AipFilterCriteria.BETWEEN && <>
                            <Field
                                component={FormInputField}
                                type={item.type == "date" ? "date" : "number"}
                                label={i18n("aip.form.from")}
                                name="from"
                            />
                            <Field
                                component={FormInputField}
                                type={item.type == "date" ? "date" : "number"}
                                label={i18n("aip.form.to")}
                                name="to"
                            />
                        </>}

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


export default AipNumericFilterForm;
