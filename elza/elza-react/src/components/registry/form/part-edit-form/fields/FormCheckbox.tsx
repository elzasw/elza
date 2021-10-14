import React, {FC} from 'react';
import { Field} from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import FormInput from '../../../../shared/form/FormInput';

export const FormCheckbox:FC<{
    name: string;
    label: string;
    disabled?: boolean;
}> = ({
    name,
    label,
    disabled = false,
}) => {
    return <Field
        name={`${name}.value`}
        label={label}
        disabled={disabled}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
        type={'checkbox'}
    />
}

