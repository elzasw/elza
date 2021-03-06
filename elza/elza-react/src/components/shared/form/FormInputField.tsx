import React, {memo} from 'react';
import { WrappedFieldProps } from 'redux-form';
import {decorateFormField} from '../../form/FormUtils';
import FormInput from './FormInput';

interface IFormInputField extends WrappedFieldProps {
    decorator?: boolean
}

const FormInputField: React.FC<IFormInputField> = memo(props => {
    const {input, meta, decorator, ...rest} = props;
    return <FormInput {...rest} {...input} {...meta} {...decorator !== false ? decorateFormField(props) : {}}/>;
});

export default FormInputField;
