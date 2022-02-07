import React, { FC } from 'react';
import { Field, useForm } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample } from '../../../revision';

export const FormCheckbox:FC<{
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
    return <Field
        name={`${name}.value`}
        label={label}
    >
        {(props) => {
            const handleChange = (e: any) => {
                props.input.onBlur(e)
                handleValueUpdate(form, props);
            }

            return <RevisionFieldExample
                label={label}
                prevValue={prevValue}
                disableRevision={disableRevision}
                value={props.input.value ? "Ano" : "Ne"}
            >
                <ReduxFormFieldErrorDecorator
                    {...props as any}
                    input={{
                        ...props.input,
                        onBlur: handleChange // inject modified onChange handler
                    }}
                    disabled={disabled}
                    renderComponent={FormInput}
                    type="checkbox"
                    />
            </RevisionFieldExample>
        }}
    </Field>
}

