import React from 'react';
import {Form} from 'react-bootstrap';
import AbstractReactComponent from '../../AbstractReactComponent';

class FormInput extends AbstractReactComponent {
    render() {
        let {error, touched, ...rest} = this.props;
        const {meta, children, type, label, value, inline, feedback, ...otherProps} = rest;

        if (meta) {
            error = meta.error;
            touched = meta.touched;
        }

        console.log(label, "::", error, touched);

        const hasError = !!(touched && error);
        let inlineProps = {};
        if (inline) {
            error && (inlineProps.title = error);
        }

        switch (type) {
            case 'static':
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <Form.Control.Static
                            ref="input"
                            value={value} {...otherProps} {...inlineProps}
                            isInvalid={hasError}
                        >
                            {children}
                        </Form.Control.Static>
                        {!inline && hasError && <Form.Control.Feedback>{error}</Form.Control.Feedback>}
                    </Form.Group>
                );
            case 'radio':
                return (
                    <div>
                        <Form.Check
                            type="radio"
                            ref="input"
                            label={label}
                            value={value}
                            isInvalid={hasError}
                            {...otherProps}
                            {...inlineProps}
                        />
                        {!inline && hasError && <Form.Control.Feedback>{error}</Form.Control.Feedback>}
                    </div>
                );
            case 'select':
                return (
                    <Form.Group>
                        {label && <Form.Label>{label}</Form.Label>}
                        <Form.Control
                            ref="input"
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
                            ref="input"
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
    }
}

export default FormInput;
