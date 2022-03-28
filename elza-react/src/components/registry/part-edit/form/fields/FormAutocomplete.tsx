import React from 'react';
import { Field, useForm } from 'react-final-form';
import { Autocomplete } from '../../../../shared';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';

interface FormAutocompleteProps<T> {
    name: string;
    label: string;
    disabled?: boolean;
    items: T[];
    allowSelectItem?: (item: T) => boolean;
    alwaysExpanded?: boolean;
    tree?: boolean;
}

export const FormAutocomplete = <ValueType,>({
    name,
    label,
    disabled = false,
    items,
    allowSelectItem = () => true,
    alwaysExpanded = false,
    tree = false,
}:FormAutocompleteProps<ValueType>) => {
    const form = useForm();
    return <Field
        name={name}
        label={label}
    >
        {(props) => {
            const handleChange = (e: any) => { 
                props.input.onChange(e)
                handleValueUpdate(form);
            }

            return <ReduxFormFieldErrorDecorator
                {...props as any}
                input={{
                    ...props.input,
                    onChange: handleChange // inject modified onChange handler
                }}
                disabled={disabled}
                renderComponent={Autocomplete}
                passOnly={true}
                items={items}
                allowSelectItem={allowSelectItem}
                alwaysExpanded={alwaysExpanded}
                tree={tree}
            />
        }}
    </Field>
}
