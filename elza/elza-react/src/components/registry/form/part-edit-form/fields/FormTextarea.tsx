import React, {FC} from 'react';
// import { Field} from 'redux-form';
import { Field, useForm } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import FormInput from '../../../../shared/form/FormInput';

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
/*
    return <Field
        name={`${name}.value`}
        label={label}
        disabled={disabled}
        maxLength={limitLength}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
        type={'textarea'}
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
                maxLength={limitLength}
                renderComponent={FormInput}
                type="textarea"
                />

        }}
    </Field>
}
