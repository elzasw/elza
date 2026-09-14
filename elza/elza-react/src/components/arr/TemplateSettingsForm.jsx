import PropTypes from 'prop-types';
import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { templateMessages } from './templateMessages';
import {Form} from 'react-bootstrap';
import {Button} from '../ui';
import {WebApi} from '../../actions/WebApi';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';

import './TemplateSettingsForm.scss';
import FormInputField from '../shared/form/FormInputField';

class TemplateSettingsForm extends React.Component {
    static propTypes = {
        engine: PropTypes.string.isRequired,
        outputId: PropTypes.number.isRequired,
        outputSettings: PropTypes.object,
    };

    handleSubmit = settings => {
        const {outputId} = this.props;
        return WebApi.updateOutputSettings(outputId, settings).then(() => {
            this.props.dispatch(addToastrSuccess(this.props.intl.formatMessage(templateMessages.outputTemplateSettingsSuccess)));
        });
    };

    render() {
        const {handleSubmit, submitting, engine, readMode} = this.props;

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
                        label={<FormattedMessage {...templateMessages.outputTemplateOddPageOffsetX} />}
                        {...commonProps}
                    />
                    <Field
                        name="evenPageOffsetY"
                        type="text"
                        component={FormInputField}
                        label={<FormattedMessage {...templateMessages.outputTemplateOddPageOffsetY} />}
                        {...commonProps}
                    />
                    <Field
                        name="oddPageOffsetX"
                        type="text"
                        component={FormInputField}
                        label={<FormattedMessage {...templateMessages.outputTemplateEvenPageOffsetX} />}
                        {...commonProps}
                    />
                    <Field
                        name="oddPageOffsetY"
                        type="text"
                        component={FormInputField}
                        label={<FormattedMessage {...templateMessages.outputTemplateEvenPageOffsetY} />}
                        {...commonProps}
                    />
                    {!readMode && (
                        <Button
                            className="output-settings-submit"
                            disabled={submitting}
                            type="submit"
                            variant="outline-secondary"
                        >
                            {<FormattedMessage {...templateMessages.outputTemplateSet} />}
                        </Button>
                    )}
                </Form>
            );
        }

        return <div>{<FormattedMessage {...templateMessages.outputPanelTemplateNoSettings} />}</div>;
    }
}

export default reduxForm({
    form: 'templateSettingsForm',
})(injectIntl(TemplateSettingsForm));
