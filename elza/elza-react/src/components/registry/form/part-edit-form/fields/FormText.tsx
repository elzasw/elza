import React, {FC} from 'react';
// import { Field} from 'redux-form';
import { Field } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import FormInput from '../../../../shared/form/FormInput';

export const FormText:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    limitLength?: boolean;
}> = ({
    name,
    label,
    disabled = false,
    limitLength,
}) => {
    return <Field
        name={`${name}.value`}
        label={label}
        disabled={disabled}
        maxLength={limitLength}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
        />
}
