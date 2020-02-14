import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import { reduxForm } from 'redux-form';
import { i18n } from 'components/shared';
import { Button, Form } from 'react-bootstrap';
import FormInput from '../shared/form/FormInput';
import { WebApi } from '../../actions/WebApi';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';

import './TemplateSettingsForm.scss';

const fields = ['evenPageOffsetX', 'evenPageOffsetY', 'oddPageOffsetX', 'oddPageOffsetY'];

class TemplateSettingsForm extends React.Component {
    static propTypes = {
        engine: PropTypes.string.isRequired,
        outputId: PropTypes.number.isRequired,
        outputSettings: PropTypes.object
    };

    handleSubmit = settings => {
        const { outputId } = this.props;
        return WebApi.updateOutputSettings(outputId, settings).then(()=>{
            this.props.dispatch(addToastrSuccess(i18n("arr.output.template.settings.success")));
        });
    };

    render() {
        const {
            handleSubmit,
            submitting,
            fields: { evenPageOffsetX, evenPageOffsetY, oddPageOffsetX, oddPageOffsetY },
            engine,
            readMode
        } = this.props;

        const commonProps = {
            disabled: readMode || submitting
        };

        if (engine && engine === 'JASPER') {
            return (
                <Form onSubmit={handleSubmit(this.handleSubmit)}>
                    <FormInput  {...evenPageOffsetX} {...commonProps} label={i18n('arr.output.template.oddPageOffsetX')} />
                    <FormInput  {...evenPageOffsetY} {...commonProps} label={i18n('arr.output.template.oddPageOffsetY')} />
                    <FormInput  {...oddPageOffsetX}  {...commonProps} label={i18n('arr.output.template.evenPageOffsetX')} />
                    <FormInput  {...oddPageOffsetY}  {...commonProps} label={i18n('arr.output.template.evenPageOffsetY')} />
                    {!readMode && <Button className="output-settings-submit" disabled={submitting} type="submit">
                        {i18n('arr.output.template.set')}
                    </Button>}
                </Form>
            );
        }

        return (
            <div>
                {i18n('arr.output.panel.template.noSettings')}
            </div>
        );
    }
}

export default reduxForm(
    {
        form: 'templateSettingsForm',
        fields
    },
    (state, { outputSettings }) => {
        return {
            initialValues: outputSettings

        };
    }
)(TemplateSettingsForm);
