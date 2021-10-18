import React, {FC} from 'react';
import { Field, useForm } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import FormInput from '../../../../shared/form/FormInput';

export const FormCheckbox:FC<{
    name: string;
    label: string;
    disabled?: boolean;
}> = ({
    name,
    label,
    disabled = false,
}) => {
    const form = useForm();
    return <Field
        name={`${name}.value`}
        label={label}
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
                renderComponent={FormInput}
                type="checkbox"
                />
        }}
    </Field>
}

