import React, { FC } from 'react';
import { Field, useForm, useField } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import { ApItemTextVO } from 'api/ApItemTextVO';
import { CommonFieldProps } from './types';

export const FormText:FC<CommonFieldProps<ApItemTextVO> & {
    limitLength?: boolean;
}> = ({
    name,
    label,
    disabled = false,
    limitLength,
    disableRevision,
    onDelete = () => {console.warn("'onDelete' not defined")},
}) => {
    const form = useForm();
    const field = useField<RevisionItem>(`${name}`);
    const {updatedItem, item} = field.input.value;
    const prevValue = (item as ApItemTextVO | undefined)?.value;

    return <Field
        name={`${name}.updatedItem.value`}
    >
        {(props) => {
            const isNew = updatedItem ? updatedItem.changeType === "NEW" || (!item && !!updatedItem) : false;
            const isDeleted = updatedItem?.changeType === "DELETED";

            const handleChange = (e: any) => {
                props.input.onBlur(e)
                handleValueUpdate(form, props);
            }

            const handleRevert = () => {
                form.change(`${name}.updatedItem`, item)
                handleValueUpdate(form);
            }
            
            const handleDelete = () => {
                if(disableRevision || isNew){onDelete()}
                else {
                    form.change(`${name}.updatedItem`, {
                        ...updatedItem,
                        changeType: "DELETED",
                        value: null,
                    })
                }
                handleValueUpdate(form);
            }

            return <RevisionFieldExample
                label={label}
                prevValue={prevValue}
                value={props.input.value}
                disableRevision={disableRevision}
                equalSplit={true}
                onRevert={!isNew ? handleRevert : undefined}
                onDelete={isDeleted ? undefined : handleDelete}
                isDeleted={isDeleted}
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
