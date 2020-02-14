import PropTypes from 'prop-types';
import React from 'react';
import {FormControl, FormGroup, FormLabel, Form, FormCheck} from 'react-bootstrap';
import AbstractReactComponent from "../../AbstractReactComponent";

class FormInput extends AbstractReactComponent {
    static defaultProps = {
        inline: false,
        feedback: false
    };

    static propTypes = {
        label: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
        error: PropTypes.string,
        touched: PropTypes.bool.isRequired,
        feedback: PropTypes.bool,
        placeholder: PropTypes.bool,
        staticInput: PropTypes.bool,  // má se renderovat jako FormControl.Static?
    };

    render() {
        const {children, type, label, error, touched, value, inline, feedback, ...otherProps} = this.props;

        const hasError = touched && error;
        let inlineProps = {};
        if (inline) {
            error && (inlineProps.title = error);
        }

        switch (type) {
            case "static":
                return <FormGroup validationState={hasError ? 'error' : null}>
                    {label && <FormLabel>{label}</FormLabel>}
                    <FormControl.Static
                        ref='input'
                        value={value}
                        {...otherProps}
                        {...inlineProps}
                    >{children}</FormControl.Static>
                    {!inline && hasError && <Form.Control.Feedback>{error}</Form.Control.Feedback>}
                </FormGroup>;
            case "radio":
                return <div>
                    <FormCheck
                        type="radio"
                        ref='input'
                        label={label}
                        value={value}
                        {...otherProps}
                        {...inlineProps} />
                    {!inline && hasError && <Form.Control.Feedback>{error}</Form.Control.Feedback>}
                </div>;
            default:
                return <FormGroup validationState={hasError ? 'error' : null}>
                    {label && <FormLabel>{label}</FormLabel>}
                    <FormControl
                        ref='input'
                        value={value}
                        children={children}
                        type={type}
                        {...otherProps}
                        {...inlineProps}
                    />
                    {!inline && hasError && <Form.Control.Feedback>{error}</Form.Control.Feedback>}
                </FormGroup>
        }
    }
}

export default FormInput;
