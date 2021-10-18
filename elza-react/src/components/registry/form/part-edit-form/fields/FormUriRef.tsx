import React, {FC} from 'react';
import { Field, useForm } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import {Col, Row} from 'react-bootstrap';
import FormInput from '../../../../shared/form/FormInput';

export const FormUriRef:FC<{
    name: string;
    label: string;
    disabled?: boolean;
}> = ({
    name,
    label,
    disabled = false,
}) => {
    const form = useForm();
    const validate = (value:string) => {
        if(!value?.match(/^.+:.+$/g)){
            return "Nesprávný formát odkazu";
        }
        return undefined;
    }
    return <Row>
        <Col xs={6}>
            <Field
                name={`${name}.value`}
                label={label}
                validate={validate}
            >
                {(props) => {
                    const handleChange = (e: any) => { 
                        props.input.onBlur(e)
                        form.mutators.attributes?.(name);
                    }

                    return <ReduxFormFieldErrorDecorator
                        {...props as any}
                        input={{
                            ...props.input,
                            onBlur: handleChange // inject modified onChange handler
                        }}
                        disabled={disabled}
                        maxLength={1000}
                        renderComponent={FormInput}
                        />

                }}
            </Field>
        </Col>
        <Col xs={6}>
            <Field
                name={`${name}.description`}
                label="Název odkazu"
            >
                {(props) => {
                    const handleChange = (e: any) => { 
                        props.input.onBlur(e)
                        form.mutators.attributes?.(name);
                    }

                    return <ReduxFormFieldErrorDecorator
                        {...props as any}
                        input={{
                            ...props.input,
                            onBlur: handleChange // inject modified onChange handler
                        }}
                        disabled={disabled}
                        maxLength={250}
                        renderComponent={FormInput}
                        />

                }}
            </Field>
        </Col>
    </Row>
}
