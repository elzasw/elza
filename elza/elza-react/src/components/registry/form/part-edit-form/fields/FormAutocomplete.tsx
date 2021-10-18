import React from 'react';
// import { Field} from 'redux-form';
import { Field, useForm } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { Autocomplete } from '../../../../shared';

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
                form.mutators.attributes?.(name);
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
