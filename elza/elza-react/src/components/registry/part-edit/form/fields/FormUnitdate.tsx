import React, { FC } from 'react';
import { Field, useForm, useField } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import UnitdateField from '../../../field/UnitdateField';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import {ApItemUnitdateVO} from 'api/ApItemUnitdateVO';
import { CommonFieldProps } from './types';

export const FormUnitdate:FC<CommonFieldProps<ApItemUnitdateVO>> = ({
    name,
    label,
    disabled = false,
    disableRevision,
    onDelete = () => {console.warn("'onDelete' not defined")},
}) => {
    const form = useForm();
    const field = useField<RevisionItem>(`${name}`);
    const {item, updatedItem} = field.input.value;
    const prevValue = (item as ApItemUnitdateVO | undefined)?.value;

    return <Field
        name={`${name}.updatedItem.value`}
    >
        {(props) => {
            const isNew = updatedItem ? updatedItem.changeType === "NEW" || !updatedItem.changeType : false;
            const isDeleted = updatedItem?.changeType === "DELETED";

            const handleChange = (e: any) => {
                props.input.onBlur(e)
                handleValueUpdate(form, props);
            }

            const handleRevert = () => {
                form.change(`${name}.updatedItem`, item)
                handleValueUpdate(form, props);
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
