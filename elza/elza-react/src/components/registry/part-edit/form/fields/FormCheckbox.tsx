import React, { FC } from 'react';
import { Field, useForm } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample } from '../../../revision';
import { ApItemBitVO } from 'api/ApItemBitVO';
import { CommonFieldProps } from './types';

export const FormCheckbox:FC<CommonFieldProps<ApItemBitVO>> = ({
    name,
    label,
    disabled = false,
    prevItem,
    disableRevision,
}) => {
    const form = useForm();
    const getValue = (item?: ApItemBitVO) => {
        if(!item){return undefined}
        return item.value ? "Ano" : "Ne";
    }
    return <Field
        name={`${name}.updatedItem.value`}
        label={label}
    >
        {(props) => {
            const handleChange = (e: any) => {
                props.input.onBlur(e)
                handleValueUpdate(form, props);
            }

            const handleRevert = () => {
                form.change(`${name}.updatedItem`, prevItem)
                handleValueUpdate(form, props);
            }

            return <RevisionFieldExample
                label={label}
                prevValue={getValue(prevItem)}
                disableRevision={disableRevision}
                value={getValue(props.input.value)}
                onRevert={handleRevert}
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

