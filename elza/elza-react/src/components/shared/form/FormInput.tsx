// @ts-nocheck
import React, {memo, PropsWithChildren, ReactElement} from 'react';
import {Form} from 'react-bootstrap';
import {Autocomplete, AsyncAutocomplete} from '../index';

interface IFormInputProps {
    name?: string;
    onChange?: (eventOrValue: React.ChangeEvent<HTMLInputElement> | string) => void;
    value?: any;
    as?: string;
    feedback?: boolean;
    inline?: boolean;
    label?: string | ReactElement;
    type?: string;
    error?: string;
    touched?: boolean;
    placeholder?: string;
    staticInput?: boolean;
    disabled?: boolean;
}
type Props = Partial<Omit<React.ComponentProps<typeof Form.Control>, "onChange">> & PropsWithChildren<IFormInputProps>;

const FormInput: React.ForwardRefExoticComponent<Props> = memo(
    React.forwardRef((props, ref) => {
        const {
            error,
            touched,
            children,
            type,
            label,
            value,
            inline,
            feedback,
            active,
            asyncValidating,
            valid,
            visited,
            autofilled,
            dirty,
            invalid,
            pristine,
            submitting,
            submitFailed,
            dispatch,
            initial,
            className,
            ...otherProps
        } = props;

        const hasError = !!(touched && error);
        let inlineProps = {...(inline ? (error ? {title: error} : {}) : {})};

        switch (type) {
            case 'static':
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <div ref={ref} className={className} {...otherProps} {...inlineProps}>
                            {children}
                        </div>
                    </Form.Group>
                );
            case 'radio':
                return (
                    <Form.Group>
                        <Form.Check
                            className={className}
                            ref={ref}
                            type="radio"
                            label={label}
                            value={value}
                            isInvalid={hasError}
                            {...otherProps}
                            {...inlineProps}
                        />
                        {!inline && hasError && <Form.Control.Feedback type="invalid">{error}</Form.Control.Feedback>}
                    </Form.Group>
                );
            case 'checkbox':
                return (
                    <Form.Group>
                        <Form.Check
                            className={className}
                            ref={ref}
                            label={label}
                            value={value}
                            isInvalid={hasError}
                            {...otherProps}
                            {...inlineProps}
                        />
                        {!inline && hasError && <Form.Control.Feedback type="invalid">{error}</Form.Control.Feedback>}
                    </Form.Group>
                );
            case 'select':
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <Form.Control
                            className={`${className} form-select`}
                            ref={ref}
                            as="select"
                            value={value}
                            isInvalid={hasError}
                            {...otherProps}
                            {...inlineProps}
                        >
                            {children}
                        </Form.Control>
                        {!inline && hasError && <Form.Control.Feedback type="invalid">{error}</Form.Control.Feedback>}
                    </Form.Group>
                );
            case 'textarea':
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <Form.Control
                            className={className}
                            ref={ref}
                            as="textarea"
                            value={value}
                            isInvalid={hasError}
                            {...otherProps}
                            {...inlineProps}
                        >
                            {children}
                        </Form.Control>
                        {!inline && hasError && <Form.Control.Feedback type="invalid">{error}</Form.Control.Feedback>}
                    </Form.Group>
                );
            case 'autocomplete':
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <Autocomplete
                            className={className}
                            ref={ref}
                            value={value}
                            error={hasError && error}
                            touched={touched}
                            {...otherProps}
                            {...inlineProps}
                        >
                            {children}
                        </Autocomplete>
                    </Form.Group>
                );
            case 'asyncAutocomplete':
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <AsyncAutocomplete
                            className={className}
                            ref={ref}
                            value={value}
                            error={hasError && error}
                            touched={touched}
                            {...otherProps}
                            {...inlineProps}
                        >
                            {children}
                        </AsyncAutocomplete>
                    </Form.Group>
                );
            case 'simple':
                return (
                    <Form.Group
                        className={className}
                        ref={ref}
                        label={label}
                        value={value}
                        children={children}
                        isInvalid={hasError}
                        {...otherProps}
                        {...inlineProps}
                    />
                );
            default:
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <Form.Control
                            ref={ref}
                            className={className}
                            value={value}
                            children={children}
                            type={type}
                            isInvalid={hasError}
                            {...otherProps}
                            {...inlineProps}
                        />
                        {!inline && hasError && <Form.Control.Feedback type="invalid">{error}</Form.Control.Feedback>}
                    </Form.Group>
                );
        }
    }),
);

export default FormInput;
