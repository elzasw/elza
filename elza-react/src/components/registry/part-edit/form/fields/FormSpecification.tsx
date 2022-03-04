import React, { FC } from 'react';
import { Field, useForm, useField } from 'react-final-form';
import { RulDescItemTypeExtVO } from '../../../../../api/RulDescItemTypeExtVO';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import SpecificationField from '../../../field/SpecificationField';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import { ApItemVO } from 'api/ApItemVO';
import { CommonFieldProps } from './types';

export const FormSpecification:FC<CommonFieldProps<ApItemVO> & {
    itemSpecIds?: number[];
    itemType: RulDescItemTypeExtVO;
    getSpecName?: (id:number) => string;
}> = ({
    name,
    label,
    disabled = false,
    itemSpecIds = [],
    itemType,
    getSpecName = () => "-",
    disableRevision,
}) => {
    const form = useForm();
    const field = useField<RevisionItem>(`${name}`);
    const {item, updatedItem} = field.input.value;

    return <Field
        name={`${name}.updatedItem.specId`}
    >
        {(props) => {
            const handleChange = (e: any) => { 
                props.input.onChange(e)
                handleValueUpdate(form);
            }

            const handleRevert = () => {
                form.change(`${name}.updatedItem`, item)
                handleValueUpdate(form);
            }

            const handleDelete = () => {
                form.change(`${name}.updatedItem`, {
                    ...updatedItem,
                    changeType: "DELETED",
                    specId: undefined,
                })
                handleValueUpdate(form);
            }

            const isNew = updatedItem ? updatedItem.changeType === "NEW" || !updatedItem.changeType : false;
            const isDeleted = updatedItem?.changeType === "DELETED";
            const prevValue = item?.specId != null ? getSpecName(item.specId) : undefined

            return <RevisionFieldExample 
                label={label} 
                prevValue={prevValue} 
                value={isDeleted ? undefined : getSpecName(parseInt(props.input.value))}
                disableRevision={disableRevision}
                onRevert={!isNew ? handleRevert : undefined}
                onDelete={disableRevision || isNew || isDeleted ? undefined : handleDelete}
                isDeleted={isDeleted}
            >
                <ReduxFormFieldErrorDecorator
                    {...props as any}
                    input={{
                        ...props.input,
                        onChange: handleChange // inject modified onChange handler
                    }}
                    disabled={disabled}
                    itemTypeId={itemType.id}
                    itemSpecIds={itemSpecIds}
                    renderComponent={SpecificationField}
                    spellcheck={false}
                    />
            </RevisionFieldExample>
        }}
    </Field>
}
