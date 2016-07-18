import React from 'react';
import {FormControl, FormGroup, ControlLabel, HelpBlock} from 'react-bootstrap'
import {AbstractReactComponent} from 'components/index.jsx'

const FormInput = class FormInput extends AbstractReactComponent {
    render() {
        const {label, error, touched, value, inline, ...otherProps} = this.props;
        const hasError = touched && error;
        let inlineProps = {};
        if (inline) {
            error && (inlineProps.title = error);
        }
        return <FormGroup validationState={hasError ? 'error' : null}>
            {label && <ControlLabel>{label}</ControlLabel>}
            <FormControl
                ref='input'
                value={value}
                onChange={this.handleChange}
                {...otherProps}
                {...inlineProps}
            />
            {!inline && hasError && <HelpBlock>{error}</HelpBlock>}
        </FormGroup>
    }

    static defaultProps = {
        inline: false
    }
}

FormInput.propsTypes = {
    label: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.object]),
    error: React.PropTypes.string,
    touched: React.PropTypes.bool.isRequired,
    feedback: React.PropTypes.bool,
    placeholder: React.PropTypes.bool,
};

FormInput.defaultProps = {
    feedback: false
};

module.exports = FormInput;