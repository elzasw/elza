import { FormInputField, i18n } from "components/shared";
import { Field, Form as FinalForm } from "react-final-form"
import { Modal, Button, Form } from "react-bootstrap";
import { AipFilter } from "typings/store";
import { AipFilterFormProps, SelectionOptions } from "./AipFilterFormProps";
import { AipFilterCriteria } from "./EnumAipFilterCriteria";
import {AipDetailVO} from "elza-api";

const AipEnumFilterForm = ({item, selectValues, onSubmit, onClose}: AipFilterFormProps) => {
    const validate = (values: AipFilter) => {
        const errors: Partial<Record<keyof AipFilter, string>> = {};
        if(values.value == null) {
            errors.value = "Hodnota musí být vyplněna";
        }
        return errors;
    };


    return (
        <FinalForm<AipFilter>
            initialValues={{
                attr: item.key as keyof AipDetailVO,
                criteria: AipFilterCriteria.EQUALS,
                value: selectValues[0]?.value,
                path: item.path,
            }}
            onSubmit={onSubmit}
            validate={validate}
        >
        {({ submitting, handleSubmit }) => {
            return (
                <Form>
                    <Modal.Body>
                        <Field
                            component={FormInputField}
                            type="select"
                            label= {i18n('aip.form.value')}
                            name="value"

                        >
                            {selectValues && selectValues.map(({value, label}: SelectionOptions, index: number) =>
                                <option key={index} value={value.toString()}>
                                    {label}
                                </option>
                            )}
                        </Field>
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


export default AipEnumFilterForm;
