import React, {FC} from 'react';
// import { Field} from 'redux-form';
import { Field, useForm } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import Scope from '../../../../shared/scope/Scope';
import { ScopeData } from "typings/store";

export const FormScope:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    items: ScopeData[];
}> = ({
    name,
    label,
    disabled = false,
    items,
}) => {
    const form = useForm();
    return <Field
        name={name}
        label={label}
    >
        {(props) => {
            const handleChange = (e: any) => { 
                props.input.onChange(e)
                form.mutators.attributes?.(name);
            }

            return <ReduxFormFieldErrorDecorator
                {...props as any}
                input={{
                    ...props.input,
                    onChange: handleChange // inject modified onChange handler
                }}
                disabled={disabled}
                renderComponent={Scope}
                passOnly={true}
                items={items}
            />
        }}
    </Field>
}
