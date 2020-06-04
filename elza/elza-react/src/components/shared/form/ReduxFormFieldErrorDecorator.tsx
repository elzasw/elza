import React from "react";
import {WrappedFieldProps} from "redux-form";

type Props = WrappedFieldProps & {
    renderComponent: React.ComponentType
    passOnly: boolean;
}

/**
 * @param renderComponent Komponenta, ktera se ma zaobalit
 * @param passOnly Pouze preda meta hodnoty fieldu do podrizene komponenty, ale neresi zobrazovani chyb - pouziti napr. u Autocomplete
 */
const ReduxFormFieldErrorDecorator = React.memo((props: Props) => {
    const {renderComponent, meta: {touched, error}, ...other} = props;
    const passOnly = props.passOnly;
    return <>
        {React.createElement(renderComponent, passOnly ? {...other, ...props.meta} : {...other} as any)}
        {!passOnly && touched && error && <span className={'text-danger'}>{error}</span>}
    </>;
})

export default ReduxFormFieldErrorDecorator;

