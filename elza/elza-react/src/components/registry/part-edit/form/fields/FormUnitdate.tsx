import React, { FC } from 'react';
import { Field, useForm } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import UnitdateField from '../../../field/UnitdateField';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample } from './RevisionFieldExample';

export const FormUnitdate:FC<{
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
    >
        {(props) => {
            const handleChange = (e: any) => { 
                props.input.onBlur(e)
                handleValueUpdate(form, props);
            }

            return <RevisionFieldExample 
                label={label} 
                prevValue="1234" 
                value={props.input.value}
            >
                <ReduxFormFieldErrorDecorator
                    {...props as any}
                    input={{
                        ...props.input,
                        onBlur: handleChange // inject modified onChange handler
                    }}
                    disabled={disabled}
                    renderComponent={UnitdateField}
                    />
            </RevisionFieldExample>

        }}
    </Field>
}
