import React, { FC } from 'react';
import { Field, useForm } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample } from './RevisionFieldExample';

export const FormTextarea:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    limitLength?: boolean;
}> = ({
    name,
    label,
    disabled = false,
    limitLength,
}) => {
    const form = useForm();
    return <Field
        name={`${name}.value`}
    >
        {(props) => {
            const handleChange = (e: any) => { 
                props.input.onBlur(e)
                handleValueUpdate(form, props);
            }

            return <RevisionFieldExample 
                label={label} 
                prevValue="Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Duis risus. Aenean placerat." 
                value={props.input.value}
                alignTop={true}
            >
                <ReduxFormFieldErrorDecorator
                    {...props as any}
                    input={{
                        ...props.input as any,
                        onBlur: handleChange // inject modified onChange handler
                    }}
                    disabled={disabled}
                    maxLength={limitLength}
                    renderComponent={FormInput}
                    type="textarea"
                    />
            </RevisionFieldExample>
        }}
    </Field>
}
