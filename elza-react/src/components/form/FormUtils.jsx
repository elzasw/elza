/**
 * Utility pro formuláře s inplace editací.
 */

/**
 * Vrácení objektu pro dekoraci input prvku inplace editace.
 * @param field {Object} objekt s informací o inplace prvku
 * @param inline {Boolean} pokud je true, jedná se o inline editaci, kde se chyba nezobrazuje pod prvke, ale až po najetí myší jako title
 * @return {Object} objekt pro dekoraci input prvku
 */

import {modalDialogHide} from 'actions/global/modalDialog.jsx';

export function decorateFormField(field, inline = false) {
    if (field.touched && field.error) {
        const result = {
            variant: 'error',
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

/**
 * @deprecated user submitForm()
 */
export function submitReduxForm(validate, values, dispatch) {
    return submitForm.bind(this)(validate, values);
}


/**
 * Default form options
 */
const defaultOptions = {
    closeOnSubmit:false,
    closeOnFinished: true,
    closeOnRejected: false
}

/**
 * Function called after the form has been submitted
 *
 * @param {function} dispatch - redux dispatch function
 * @param {object} options - configuration object
 */
const formSubmitted = (dispatch,options) => {
    var finishOnSubmit = false;
    if(options.closeOnSubmit){
        dispatch(modalDialogHide());
    }
    if(options.onSubmitted){
        options.onSubmitted();
    }
    if(options.finishOnSubmit){
        finishOnSubmit = true;
    }
    return finishOnSubmit;
}

/**
 * Function called after the form operation has successfully ended
 *
 * @param {function} dispatch - redux dispatch function
 * @param {object} options - configuration object
 */
const formFinished = (dispatch,options) => {
    if(options.closeOnFinished){
        dispatch(modalDialogHide());
    }
    if(options.onFinished){
        options.onFinished();
    }
}

/**
 * Function called in case of form rejection, but before the rejection itself
 *
 * @param {function} dispatch - redux dispatch function
 * @param {object} options - configuration object
 */
const formRejected = (dispatch,options,reason) => {
    if(options.closeOnRejected){
        dispatch(modalDialogHide());
    }
    if(options.onRejected){
        options.onRejected(reason);
    }
}

/**
 * Function creating a Promise of submitted form
 *
 * @param {function} validate - validation function expecting 2 parameters, values and props
 * @param {object} values - object containing key:value pairs of the submitted form fields
 * @param {object} props - object containing props of the form component
 * @param {function} onSubmit - submit function called after the form is successfully validated
 * @param {function} dispatch - redux dispatch function
 * @param {object} options - optional configuration object
 *
 * Available options:
 * @option {bool} closeOnFinished - the form will close when successfully finished (default true)
 * @option {function} onFinished - function called after successfully finished form operation
 * @option {bool} closeOnSubmit - the form will close on submit (default false)
 * @option {bool} finishOnSubmit - the promise will be resolved on submit - one-way form (default false)
 * @option {function} onSubmitted - function called when submitted
 * @option {bool} closeOnRejected - the form will close when rejected (default false)
 * @option {function} onRejected - function called when rejected
 *
 * @returns Promise
 */
export function submitForm(validate,values,props,onSubmit,dispatch,options=defaultOptions) {
    for(var o in defaultOptions){
        if(typeof options[o] == "undefined"){
            options[o] = defaultOptions[o];
        }
    }
    var promise = new Promise((resolve, reject) => {
        var errors = validate(values,props);
        if(Object.keys(errors).length>0){
            formRejected(dispatch,options);
            reject(errors);
        } else {
            var submit = onSubmit(values);
            if(formSubmitted(dispatch,options)){
                formFinished(dispatch,options);
                resolve(values);
            }
            if(typeof submit == "object"){ // if the onSubmit function returns promise object
                submit.then((result)=>{
                    formFinished(dispatch,options);
                    resolve(result);
                }).catch(e=>{ // if an error happens during the submit promise
                    console.error(e);
                    formRejected(dispatch,options,e);
                    reject(); // rejected without parameters to prevent accidental mapping of error object keys to form fields
                });
            }
            return submit;
        }
    })
    return promise;
}

export function submitReduxFormWithRemote(validate, values) {
    return new Promise((resolve, reject) => {
        const errors = validate(values, this.props);
        if (Object.keys(errors).length > 0) {
            reject(errors)
        } else {
            this.props.onSubmit(values, resolve, reject)
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
    let cls = 'form-group';
    let feedbackIcon = '';
    if (props.hasFeedback) {
        cls += ' has-feedback';
    }
    if (props.variant) {
        cls += ' has-' + props.variant;
        switch (props.variant) {
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
