// @ts-nocheck
import React, {memo, PropsWithChildren, ReactElement} from 'react';
import {Form} from 'react-bootstrap';
import {Autocomplete} from "../index";

interface IFormInputProps {
    name?: string;
    onChange?: (d: any) => void;
    value?: any;
    as?: string;
    feedback?: boolean;
    inline?: boolean;
    label?: string | ReactElement;
    type?: string;
    error?: string;
    touched?: boolean;
    placeholder?: boolean;
    staticInput?: boolean;
}

const FormInput: React.FC<PropsWithChildren<IFormInputProps>> = memo(
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
            ...otherProps
        } = props;

        const hasError = !!(touched && error);
        let inlineProps = {...(inline ? (error ? {title: error} : {}) : {})};

        switch (type) {
            case 'static':
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <div ref={ref} {...otherProps} {...inlineProps}>
                            {children}
                        </div>
                    </Form.Group>
                );
            case 'radio':
                return (
                    <Form.Group>
                        <Form.Check
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
            case 'autocomplete':
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <Autocomplete
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
            default:
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <Form.Control
                            ref={ref}
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
