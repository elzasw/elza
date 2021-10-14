import React, {FC} from 'react';
// import { Field} from 'redux-form';
import { Field, useForm } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import {RulDescItemTypeExtVO} from '../../../../../api/RulDescItemTypeExtVO';
import SpecificationField from '../../../field/SpecificationField';

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
        label={label}
    >
        {(props) => {
            const handleChange = (e: any) => { 
                props.input.onChange(e)
                form.mutators.attributes?.(name);
            }

            return <ReduxFormFieldErrorDecorator
                {...props as any}
                input={{
                    ...props.input,
                    onChange: handleChange // inject modified onChange handler
                }}
                disabled={disabled}
                itemTypeId={itemType.id}
                itemSpecIds={itemSpecIds}
                renderComponent={SpecificationField}
                />

        }}
    </Field>
}
