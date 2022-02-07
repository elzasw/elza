import React, { FC } from 'react';
import { Field, useForm } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample } from '../../../revision';

export const FormText:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    limitLength?: boolean;
    prevValue?: string;
    disableRevision?: boolean;
}> = ({
    name,
    label,
    disabled = false,
    limitLength,
    prevValue,
    disableRevision,
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
                prevValue={prevValue}
                value={props.input.value}
                disableRevision={disableRevision}
                equalSplit={true}
            >
                <ReduxFormFieldErrorDecorator
                    {...props as any}
                    input={{
                        ...props.input,
                        onBlur: handleChange // inject modified onChange handler
                    }}
                    disabled={disabled}
                    maxLength={limitLength}
                    renderComponent={FormInput}
                    />
            </RevisionFieldExample>

        }}
    </Field>
}
