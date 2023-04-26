import React, { FC, useRef } from 'react';
import { Field, useForm, useField } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import UnitdateField, { validateUnitDate, convertToEstimateWithConfirmation } from '../../../field/UnitdateField';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import {ApItemUnitdateVO} from 'api/ApItemUnitdateVO';
import { CommonFieldProps } from './types';
import { useThunkDispatch } from 'utils/hooks';

export const FormUnitdate:FC<CommonFieldProps<ApItemUnitdateVO>> = ({
    name,
    label,
    disabled = false,
    disableRevision,
    onDelete = () => {console.warn("'onDelete' not defined")},
}) => {
    const form = useForm();
    const field = useField<RevisionItem<ApItemUnitdateVO>>(`${name}`);
    const inputRef = useRef<HTMLInputElement>();
    const {item, updatedItem} = field.input.value;
    const prevValue = item?.value;
    const dispatch = useThunkDispatch();

    return <Field
        name={`${name}.updatedItem.value`}
    >
        {(props) => {
            const isNew = updatedItem ? updatedItem.changeType === "NEW" || !updatedItem.changeType : false;
            const isDeleted = updatedItem?.changeType === "DELETED";

            const handleBlur = async (e: any) => {
                props.input.onBlur(e)
                try {
                    const value = e.target.value;
                    const validated = validateUnitDate(value);

                    if(validated.valid){
                        const newValue = await convertToEstimateWithConfirmation(value, dispatch)
                        if(newValue){
                            form.change(`${name}.updatedItem`, {...updatedItem, value: newValue})
                        } else {
                            inputRef.current?.focus();
                            return;
                        }
                    }
                } catch (e) { throw e; }
                handleValueUpdate(form, props);
            }

            const handleRevert = () => {
                if(!updatedItem){ throw Error("No updated item to revert."); }
                if(!item){ throw Error("No original item to revert to."); }

                const newUpdatedItem: ApItemUnitdateVO = {...updatedItem, value: item?.value, changeType: "ORIGINAL"};
                form.change(`${name}.updatedItem`, newUpdatedItem);
                handleValueUpdate(form, props);
            }

            const handleChange = (e: any) => {
                if(updatedItem?.changeType === "ORIGINAL"){
                    form.change(`${name}.updatedItem`, {...updatedItem, changeType: "UPDATED"})
                }
                props.input.onChange(e)
            }

            const handleDelete = () => {
                if(disableRevision || isNew){onDelete()}
                else {
                    form.change(`${name}.updatedItem`, {
                        ...updatedItem,
                        changeType: "DELETED",
                        value: null,
                        specId: undefined,
                    })
                }
                handleValueUpdate(form);
            }

            return <RevisionFieldExample
                label={label}
                prevValue={prevValue}
                disableRevision={disableRevision}
                value={props.input.value}
                onRevert={!isNew ? handleRevert : undefined}
                onDelete={isDeleted ? undefined : handleDelete}
                isDeleted={isDeleted}
            >
                <ReduxFormFieldErrorDecorator
                    {...props as any}
                    ref={inputRef}
                    input={{
                        ...props.input,
                        onChange: handleChange,
                        onBlur: handleBlur, // inject modified onChange handler
                    }}
                    disabled={disabled}
                    renderComponent={UnitdateField}
                    />
            </RevisionFieldExample>

        }}
    </Field>
}
