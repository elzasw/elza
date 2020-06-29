import React, {memo, PropsWithChildren} from 'react';
import {BaseFieldProps, Field, WrappedFieldProps} from 'redux-form';
import {decorateFormField} from '../../form/FormUtils';
import FormInput from './FormInput';

interface IFFProps {
    decorator?: boolean;
    field?: any;
}

const Base: React.FC<IFFProps & WrappedFieldProps> = memo(props => {
    const {input, meta, decorator, field, ...rest} = props;
    let ComponentClass = field;
    if (!ComponentClass) {
        ComponentClass = FormInput;
    }
    return <ComponentClass {...rest} {...input} {...meta} {...(decorator !== false ? decorateFormField(props) : {})} />;
});

const FF: React.FC<IFFProps &
    Partial<Omit<BaseFieldProps<{[key: string]: any}>, 'component'>> & {[key: string]: any}> = props => {
    return <Field component={Base} {...props} />;
};

export default FF;
