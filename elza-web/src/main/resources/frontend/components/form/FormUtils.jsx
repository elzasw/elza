/**
 * Utility pro formuláře s inplace editací.
 */

/**
 * Vrácení objektu pro dekoraci input prvku inplace editace.
 * @param field {Object} objekt s informací o inplace prvku
 * @param inline {Boolean} pokud je true, jedná se o inline editaci, kde se chyba nezobrazuje pod prvke, ale až po najetí myší jako title
 * @return {Object} objekt pro dekoraci input prvku
 */
export function decorateFormField(field, inline = false) {
    if (field.touched && field.error) {
        const result = {
            bsStyle: 'error',
            hasFeedback: true,
        }

        if (inline) {
            result.inline = true;
        } else {
            result.help = field.error;
        }

        return result;
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
