import PropTypes from 'prop-types';
import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx'
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx'
import {initForm} from "actions/form/inlineForm.jsx"
import {indexById} from 'stores/app/utils.jsx'

/**
 * Formulář inline editace požadavku na externí systém.
 */
class RequestInlineForm extends AbstractReactComponent {

    static fields = ['description'];

    /**
     * Validace formuláře.
     */
    static validate(values, props) {
        const errors = {};
        return errors;
    }

    static PropTypes = {
        initData: PropTypes.object,
        reqType: PropTypes.object.isRequired,
        onSave: PropTypes.func.isRequired,
    };

    state = {};

    componentDidMount() {
        this.props.initForm(this.props.onSave)
    }

    render() {
        const {fields: {description}, disabled} = this.props;

        return <div className="edit-request-form-container">
            <form>
                <FormInput componentClass="textarea" label={i18n('arr.request.title.description')}
                           disabled={disabled} {...description} {...decorateFormField(description, true)} />
            </form>
        </div>;
    }
}

export default reduxForm({
        form: 'requestEditForm',
        fields: RequestInlineForm.fields,
        validate: RequestInlineForm.validate,
    }, (state, props) => {
        return {
            initialValues: props.initData,
            outputTypes: state.refTables.outputTypes.items,
            allTemplates: state.refTables.templates.items,
        }
    },
    {initForm: (onSave) => (initForm("requestEditForm", RequestInlineForm.validate, onSave))}
)(RequestInlineForm);
