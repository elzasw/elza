import React, { FC } from 'react';
import { Field, useForm, useField } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import { ApItemBitVO } from 'api/ApItemBitVO';
import { CommonFieldProps } from './types';

export const FormCheckbox:FC<CommonFieldProps<ApItemBitVO>> = ({
    name,
    label,
    disabled = false,
    disableRevision,
    onDelete = () => {console.warn("'onDelete' not defined")},
}) => {
    const form = useForm();
    const field = useField<RevisionItem<ApItemBitVO>>(`${name}`);
    const {updatedItem, item} = field.input.value;

    const getValue = (item?: ApItemBitVO) => {
        if(!item || item.value == null){return undefined}
        return item.value ? "Ano" : "Ne";
    }
    return <Field
        name={`${name}.updatedItem.value`}
    >
        {(props) => {
            const isNew = updatedItem ? updatedItem.changeType === "NEW" || (!item && !!updatedItem) : false;
            const isDeleted = updatedItem?.changeType === "DELETED";

            const handleBlur = (e: any) => {
                props.input.onBlur(e)
                handleValueUpdate(form, props);
            }

            const handleChange = (e: any) => {
                if(updatedItem?.changeType === "ORIGINAL"){
                    form.change(`${name}.updatedItem`, {...updatedItem, changeType: "UPDATED"})
                }
                props.input.onChange(e)
            }

            const handleRevert = () => {
                if(!updatedItem){ throw Error("No updated item to revert."); }
                if(!item){ throw Error("No original item to revert to."); }

                const newUpdatedItem: ApItemBitVO = {...updatedItem, value: item?.value, changeType: "ORIGINAL"};
                form.change(`${name}.updatedItem`, newUpdatedItem)
                handleValueUpdate(form, props);
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
                prevValue={getValue(item as ApItemBitVO)}
                disableRevision={disableRevision}
                value={getValue(updatedItem as ApItemBitVO)}
                onRevert={!isNew ? handleRevert : undefined}
                onDelete={isDeleted ? undefined : handleDelete}
                isDeleted={isDeleted}
            >
                <ReduxFormFieldErrorDecorator
                    {...props as any}
                    input={{
                        ...props.input,
                        checked: props.input.value,
                        onChange: handleChange,
                        onBlur: handleBlur // inject modified onChange handler
                    }}
                    disabled={disabled}
                    renderComponent={FormInput}
                    type="checkbox"
                    />
            </RevisionFieldExample>
        }}
    </Field>
}

