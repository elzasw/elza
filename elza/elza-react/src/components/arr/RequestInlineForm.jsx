import PropTypes from 'prop-types';
import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
import {initForm} from 'actions/form/inlineForm.jsx';
import FormInputField from "../shared/form/FormInputField";

/**
 * Formulář inline editace požadavku na externí systém.
 */
class RequestInlineForm extends AbstractReactComponent {

    /**
     * Validace formuláře.
     */
    static validate(values, props) {
        const errors = {};
        return errors;
    }

    static propTypes = {
        initData: PropTypes.object,
        reqType: PropTypes.object.isRequired,
        onSave: PropTypes.func.isRequired,
    };

    state = {};

    componentDidMount() {
        this.props.initForm(this.props.onSave);
    }

    render() {
        const {
            disabled,
        } = this.props;

        return (
            <div className="edit-request-form-container">
                <form>
                    <Field
                        name="description"
                        as="textarea"
                        component={FormInputField}
                        label={i18n('arr.request.title.description')}
                        disabled={disabled}
                    />
                </form>
            </div>
        );
    }
}

export default reduxForm(
    {
        form: 'requestEditForm',
        validate: RequestInlineForm.validate,
    },
    (state, props) => {
        return {
            initialValues: props.initData,
            outputTypes: state.refTables.outputTypes.items,
            allTemplates: state.refTables.templates.items,
        };
    },
    {initForm: onSave => initForm('requestEditForm', RequestInlineForm.validate, onSave)},
)(RequestInlineForm);
