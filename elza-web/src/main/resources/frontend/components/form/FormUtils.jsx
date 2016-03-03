/**
 * Utility pro formuláře s inplace editací.
 */

/**
 * Vrácení objektu pro dekoraci input prvku inplace editace.
 * @param field {Object} objekt s informací o inplace prvku
 * @return {Object} objekt pro dekoraci input prvku
 */
export function decorateFormField(field) {
    if (field.touched && field.error) {
        return {
            bsStyle: 'error',
            hasFeedback: true,
            help: field.error
        }
    }
}

export function submitReduxForm(validate, values, dispatch) {
    return new Promise((resolve, reject) => {
        var errors = validate(values, this.props)
        if (Object.keys(errors).length > 0) {
            reject(errors)
        } else {
            this.props.onSubmitForm(values)
            resolve()
        }
    })
}

export function submitReduxFormWithProp(validate, submitProp, values, dispatch) {
    return new Promise((resolve, reject) => {
        var errors = validate(values, this.props)
        if (Object.keys(errors).length > 0) {
            reject(errors)
        } else {
            this.props.onSubmitForm(values, submitProp)
            resolve()
        }
    })
}

export function getBootstrapInputComponentInfo(props) {
    var cls = 'form-group';
    var feedbackIcon = '';
    if (props.hasFeedback) {
        cls += ' has-feedback';
    }
    if (props.bsStyle) {
        cls += ' has-' + props.bsStyle;
        switch (props.bsStyle) {
            case 'success':
                feedbackIcon = 'ok'
                break;
            case 'warning':
                feedbackIcon = 'warning-sign'
                break;
            case 'error':
                feedbackIcon = 'remove'
                break;
        }
    }
    return {
        cls,
        feedbackIcon
    }
}
