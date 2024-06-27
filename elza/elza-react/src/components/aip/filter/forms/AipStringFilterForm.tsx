import { FormInputField, i18n } from "components/shared";
import { Field, Form as FinalForm } from "react-final-form"
import { DaAipDetailVO } from "api/DaAipDetailVO";
import { colDef } from "components/aip/utils";
import { Modal, Button, Form } from "react-bootstrap";
import { AipFilter } from "typings/store";
import { AipFilterFormProps } from "./AipFilterFormProps";
import { AipFilterCriteria } from "./EnumAipFilterCriteria";

const AipStringFilterForm = ({item, onSubmit, onClose}: AipFilterFormProps) => {
    const validate = (values: AipFilter) => {
        const errors: Partial<Record<keyof AipFilter, string>> = {};
        const needsValue = 
            values.criteria == AipFilterCriteria.CONTAINS ||
            values.criteria == AipFilterCriteria.DOES_NOT_CONTAIN ||
            values.criteria == AipFilterCriteria.EQUALS;

        if(needsValue && (values.value == "" || values.value == null)) {
            errors.value = i18n("aip.form.error.value");
        }

        return errors;
    };

return (
        <FinalForm<AipFilter>
            initialValues={{
                attr: item.key as keyof DaAipDetailVO,
                criteria: AipFilterCriteria.CONTAINS,
                path: item.path
            }}
            onSubmit={onSubmit}
            validate={validate}
        >
        {({ submitting, handleSubmit, form, values }) => {
            const inputVisible = 
                values.criteria == AipFilterCriteria.CONTAINS || 
                values.criteria == AipFilterCriteria.DOES_NOT_CONTAIN || 
                values.criteria == AipFilterCriteria.EQUALS;

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
                                    checked={values.criteria == AipFilterCriteria.CONTAINS}
                                    value={AipFilterCriteria.CONTAINS}
                                    onChange={() => form.change("criteria", AipFilterCriteria.CONTAINS)}
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
                                    label={i18n("aip.form.equals")}
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
                                    hecked={values.criteria == AipFilterCriteria.IS_NULL}
                                    value={AipFilterCriteria.IS_NULL}
                                    onChange={() => form.change("criteria", AipFilterCriteria.IS_NULL)}
                                />
                            )}
                        </Field>
                        {inputVisible && <Field
                            component={FormInputField}
                            type="text"
                            label={i18n("aip.form.value")}
                            name="value"
                        />}
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
            )}}
        </FinalForm>
    )
}

export default AipStringFilterForm;