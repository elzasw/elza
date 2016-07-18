import React from 'react';
import {FormControl, FormGroup, ControlLabel, HelpBlock} from 'react-bootstrap'
import {AbstractReactComponent} from 'components/index.jsx'

const FormInput = class FormInput extends AbstractReactComponent {
    render() {
        const {label, error, touched, value, ...otherProps} = this.props;
        const hasError = touched && error;
        return <FormGroup validationState={hasError ? 'error' : null}>
            {label && <ControlLabel>{label}</ControlLabel>}
            <FormControl
                ref='input'
                value={value}
                onChange={this.handleChange}
                {...otherProps}
            />
            {hasError && <HelpBlock>{error}</HelpBlock>}
        </FormGroup>
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