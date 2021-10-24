import React, { FC } from 'react';
import { Field, useForm } from 'react-final-form';
import { RulDescItemTypeExtVO } from '../../../../../api/RulDescItemTypeExtVO';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import SpecificationField from '../../../field/SpecificationField';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample } from '../../../revision';

export const FormSpecification:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    itemSpecIds?: number[];
    itemType: RulDescItemTypeExtVO;
}> = ({
    name,
    label,
    disabled = false,
    itemSpecIds = [],
    itemType
}) => {
    const form = useForm();
    return <Field
        name={`${name}.specId`}
    >
        {(props) => {
            const handleChange = (e: any) => { 
                props.input.onChange(e)
                handleValueUpdate(form);
            }

            return <RevisionFieldExample 
                label={label} 
                prevValue={(59).toString()} 
                value={props.input.value.toString()}
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
