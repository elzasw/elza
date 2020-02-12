import PropTypes from 'prop-types';
import React from 'react';
import {Radio, FormControl, FormGroup, ControlLabel, HelpBlock} from 'react-bootstrap'
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
                    {label && <ControlLabel>{label}</ControlLabel>}
                    <FormControl.Static
                        ref='input'
                        value={value}
                        {...otherProps}
                        {...inlineProps}
                    >{children}</FormControl.Static>
                    {!inline && hasError && <HelpBlock>{error}</HelpBlock>}
                </FormGroup>;
            case "radio":
                return <div>
                    <Radio
                        ref='input'
                        value={value}
                        {...otherProps}
                        {...inlineProps}
                    >
                        {label}
                    </Radio>
                    {!inline && hasError && <HelpBlock>{error}</HelpBlock>}
                </div>;
            default:
                return <FormGroup validationState={hasError ? 'error' : null}>
                    {label && <ControlLabel>{label}</ControlLabel>}
                    <FormControl
                        ref='input'
                        value={value}
                        children={children}
                        type={type}
                        {...otherProps}
                        {...inlineProps}
                    />
                    {!inline && hasError && <HelpBlock>{error}</HelpBlock>}
                </FormGroup>
        }
    }
}

export default FormInput;
