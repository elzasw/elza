// @ts-nocheck
import React, { memo, PropsWithChildren, ReactElement } from 'react';
import { Form } from 'react-bootstrap';

interface IFormInputProps {
    name?: string
    onChange?: (d: any) => void
    value?: any
    as?: string
    feedback?: boolean
    inline?: boolean
    label?: string | ReactElement
    type?: string
    error?: string
    touched?: boolean
    placeholder?: boolean
    staticInput?: boolean
}

export const FormInput: React.FC<PropsWithChildren<IFormInputProps>> = memo((props) => {

    const {error, touched, children, type, label, value, inline, feedback, ...otherProps} = props;

    const hasError = !!(touched && error);
    let inlineProps = {...(inline ? error ? {title: error} : {} : {})};

    switch (type) {
        case 'static':
            return (
                <Form.Group>
                    {label && <Form.Label>{label}</Form.Label>}
                    <div
                        {...otherProps}
                        {...inlineProps}
                    >
                        {children}
                    </div>
                </Form.Group>
            );
        case 'radio':
            return (
                <Form.Group>
                    <Form.Check
                        type="radio"
                        label={label}
                        value={value}
                        isInvalid={hasError}
                        {...otherProps}
                        {...inlineProps}
                    />
                    {!inline && hasError && <Form.Control.Feedback>{error}</Form.Control.Feedback>}
                </Form.Group>
            );
        case 'checkbox':
            return (
                <Form.Group>
                    <Form.Check
                        label={label}
                        value={value}
                        isInvalid={hasError}
                        {...otherProps}
                        {...inlineProps}
                    />
                    {!inline && hasError && <Form.Control.Feedback>{error}</Form.Control.Feedback>}
                </Form.Group>
            );
        case 'select':
            return (
                <Form.Group>
                    {label && <Form.Label>{label}</Form.Label>}
                    <Form.Control
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
        default:
            return (
                <Form.Group>
                    {label && <Form.Label>{label}</Form.Label>}
                    <Form.Control
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
});

export default FormInput;
