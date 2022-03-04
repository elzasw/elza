import React, { FC } from 'react';
import { Field, useForm, useField } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import { ApItemFormattedTextVO } from 'api/ApItemFormattedTextVO';
import { CommonFieldProps } from './types';

export const FormTextarea:FC<CommonFieldProps<ApItemFormattedTextVO> & {
    limitLength?: boolean;
}> = ({
    name,
    label,
    disabled = false,
    limitLength,
    disableRevision,
}) => {
    const form = useForm();
    const field = useField<RevisionItem>(`${name}`);
    const {item, updatedItem} = field.input.value;
    const prevValue = (item as ApItemFormattedTextVO | undefined)?.value;

    return <Field
        name={`${name}.updatedItem.value`}
    >
        {(props) => {
            const handleChange = (e: any) => {
                props.input.onBlur(e)
                handleValueUpdate(form, props);
            }

            const handleRevert = () => {
                form.change(`${name}.updatedItem`, item)
                handleValueUpdate(form, props);
            }

            const handleDelete = () => {
                form.change(`${name}.updatedItem`, {
                    ...updatedItem,
                    changeType: "DELETED",
                    value: null,
                })
                handleValueUpdate(form);
            }

            const isNew = updatedItem ? updatedItem.changeType === "NEW" || !updatedItem.changeType : false;
            const isDeleted = updatedItem?.changeType === "DELETED";

            return <RevisionFieldExample
                label={label}
                prevValue={prevValue}
                value={props.input.value}
                disableRevision={disableRevision}
                alignTop={true}
                equalSplit={true}
                onRevert={!isNew ? handleRevert : undefined}
                onDelete={disableRevision || isNew || isDeleted ? undefined : handleDelete}
                isDeleted={isDeleted}
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
