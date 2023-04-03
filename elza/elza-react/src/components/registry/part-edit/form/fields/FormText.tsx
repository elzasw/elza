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
    const field = useField<RevisionItem<ApItemTextVO>>(`${name}`);
    const {updatedItem, item} = field.input.value;
    const prevValue = item?.value;

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

                const newUpdatedItem: ApItemTextVO = {...updatedItem, value: item?.value, changeType: "ORIGINAL"};
                form.change(`${name}.updatedItem`, newUpdatedItem);
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
                        onChange: handleChange,
                        onBlur: handleBlur // inject modified onChange handler
                    }}
                    disabled={disabled}
                    maxLength={limitLength}
                    renderComponent={FormInput}
                    />
            </RevisionFieldExample>

        }}
    </Field>
}
