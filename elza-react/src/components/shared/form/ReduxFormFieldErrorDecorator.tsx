import React, { forwardRef } from 'react';
import {WrappedFieldProps} from 'redux-form';

type Props = WrappedFieldProps & {
    renderComponent: React.ComponentType;
    passOnly: boolean;
    [key: string]: any;
};

/**
 * @param renderComponent Komponenta, ktera se ma zaobalit
 * @param passOnly Pouze preda meta hodnoty fieldu do podrizene komponenty, ale neresi zobrazovani chyb - pouziti napr. u Autocomplete
 */
const ReduxFormFieldErrorDecorator = React.memo(forwardRef((props: Props, ref) => {
    const {
        renderComponent,
        meta: {touched, error},
        input,
        passOnly,
        ...other
    } = props;

    return (
        <>
            {React.createElement(
                renderComponent,
                passOnly ? {...other, ...input, ...props.meta, ref} : ({...other, ...input, ref} as any),
            )}
            {!passOnly && touched && error && <span className={'text-danger'}>{error}</span>}
        </>
    );
}));

export default ReduxFormFieldErrorDecorator;
