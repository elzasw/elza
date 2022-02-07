import React, { FC } from 'react';
import { Col, Row } from 'react-bootstrap';
import { Field, useForm, useField } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample } from '../../../revision';

export const FormUriRef:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    prevValue?: string;
    disableRevision?: boolean;
}> = ({
    name,
    label,
    disabled = false,
    prevValue,
    disableRevision,
}) => {
    const form = useForm();
    const descriptionField = useField(`${name}.description`);
    const valueField = useField(`${name}.value`)
    const validate = (value:string) => {
        if(!value?.match(/^.+:.+$/g)){
            return "Nesprávný formát odkazu";
        }
        return undefined;
    }
    return <Row>
        <Col xs={10}>
            <RevisionFieldExample
                label={label}
                prevValue={prevValue}
                value={`${descriptionField.input.value}: ${valueField.input.value}`}
                disableRevision={disableRevision}
            >
                <div style={{display: "flex"}}>
                    <Field
                        name={`${name}.description`}
                        label="Název"
                    >
                        {(props) => {
                            const handleChange = (e: any) => {
                                props.input.onBlur(e)
                                handleValueUpdate(form, props);
                            }

                            return <div style={{display: "flex", flexGrow: 1, flexDirection: "column"}}>
                            <ReduxFormFieldErrorDecorator
                                {...props as any}
                                input={{
                                    ...props.input,
                                    onBlur: handleChange // inject modified onChange handler
                                }}
                                disabled={disabled}
                                maxLength={250}
                                renderComponent={FormInput}
                                />
                                </div>

                        }}
                    </Field>
                    <Field
                        name={`${name}.value`}
                        label={"Odkaz"}
                        validate={validate}
                    >
                        {(props) => {
                            const handleChange = (e: any) => {
                                props.input.onBlur(e)
                                handleValueUpdate(form, props);
                            }

                            return <div style={{display: "flex", flexGrow: 2, flexDirection: "column"}}>
                                <ReduxFormFieldErrorDecorator
                                {...props as any}
                                input={{
                                    ...props.input,
                                    onBlur: handleChange // inject modified onChange handler
                                }}
                                disabled={disabled}
                                maxLength={1000}
                                renderComponent={FormInput}
                                />
                            </div>

                        }}
                    </Field>
                </div>
            </RevisionFieldExample>
        </Col>
    </Row>
}
