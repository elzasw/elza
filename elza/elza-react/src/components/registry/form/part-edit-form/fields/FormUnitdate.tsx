import React, {FC} from 'react';
// import { Field} from 'redux-form';
import { Field } from 'react-final-form';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import UnitdateField from '../../../field/UnitdateField';

export const FormUnitdate:FC<{
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
        renderComponent={UnitdateField}
        />
}
