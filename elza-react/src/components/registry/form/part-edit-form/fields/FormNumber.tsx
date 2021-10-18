import React, {FC} from 'react';
// import { Field} from 'redux-form';
import { Field, useForm } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import FormInput from '../../../../shared/form/FormInput';

export const FormNumber:FC<{
    name: string;
    label: string;
    disabled?: boolean;
}> = ({
    name,
    label,
    disabled = false,
}) => {
/*
    return <Field
        name={`${name}.value`}
        label={label}
        disabled={disabled}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
    />
    */
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
                />

        }}
    </Field>
}

