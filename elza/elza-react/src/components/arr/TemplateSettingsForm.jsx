import PropTypes from 'prop-types';
import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {i18n} from 'components/shared';
import {Form} from 'react-bootstrap';
import {Button} from '../ui';
import {WebApi} from '../../actions/WebApi';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';

import './TemplateSettingsForm.scss';
import FormInputField from "../shared/form/FormInputField";

class TemplateSettingsForm extends React.Component {
    static propTypes = {
        engine: PropTypes.string.isRequired,
        outputId: PropTypes.number.isRequired,
        outputSettings: PropTypes.object,
    };

    handleSubmit = settings => {
        const {outputId} = this.props;
        return WebApi.updateOutputSettings(outputId, settings).then(() => {
            this.props.dispatch(addToastrSuccess(i18n('arr.output.template.settings.success')));
        });
    };

    render() {
        const {
            handleSubmit,
            submitting,
            engine,
            readMode,
        } = this.props;

        const commonProps = {
            disabled: readMode || submitting,
        };

        if (engine && engine === 'JASPER') {
            return (
                <Form onSubmit={handleSubmit(this.handleSubmit)}>
                    <Field
                        name="evenPageOffsetX"
                        type="text"
                        component={FormInputField}
                        label={i18n('arr.output.template.oddPageOffsetX')}
                        {...commonProps}
                    />
                    <Field
                        name="evenPageOffsetY"
                        type="text"
                        component={FormInputField}
                        label={i18n('arr.output.template.oddPageOffsetY')}
                        {...commonProps}
                    />
                    <Field
                        name="oddPageOffsetX"
                        type="text"
                        component={FormInputField}
                        label={i18n('arr.output.template.evenPageOffsetX')}
                        {...commonProps}
                    />
                    <Field
                        name="oddPageOffsetY"
                        type="text"
                        component={FormInputField}
                        label={i18n('arr.output.template.evenPageOffsetY')}
                        {...commonProps}
                    />
                    {!readMode && (
                        <Button
                            className="output-settings-submit"
                            disabled={submitting}
                            type="submit"
                            variant="outline-secondary"
                        >
                            {i18n('arr.output.template.set')}
                        </Button>
                    )}
                </Form>
            );
        }

        return <div>{i18n('arr.output.panel.template.noSettings')}</div>;
    }
}

export default reduxForm(
    {
        form: 'templateSettingsForm',
    },
    (state, {outputSettings}) => {
        return {
            initialValues: outputSettings,
        };
    },
)(TemplateSettingsForm);
