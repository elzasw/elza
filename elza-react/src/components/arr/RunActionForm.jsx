/**
 * Formulář přidání výstupu.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { fundFormMessages } from './fundFormMessages';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx';
import {fundActionFetchConfigIfNeeded} from 'actions/arr/fundAction.jsx';
import FF from '../shared/form/FF';

const validate = (values, props) => {
    const errors = {};

    if (!values.code) {
        errors.code = this.props.intl.formatMessage(globalMessages.validationRequired);
    }

    return errors;
};

class RunActionForm extends AbstractReactComponent {
    static propTypes = {
        initData: PropTypes.object,
        onSubmitForm: PropTypes.func.isRequired,
        versionId: PropTypes.number.isRequired,
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(fundActionFetchConfigIfNeeded(nextProps.versionId));
    }

    componentDidMount() {
        this.props.dispatch(fundActionFetchConfigIfNeeded(this.props.versionId));
    }

    submitOptions = {
        closeOnSubmit: true,
    };

    submitReduxForm = (values, dispatch) =>
        submitForm(validate, values, this.props, this.props.onSubmitForm, dispatch, this.submitOptions);

    render() {
        const {handleSubmit, onClose, actionConfig, submitting} = this.props;
        return (
            <div className="run-action-form-container">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <FF name="code" type="select" label={<FormattedMessage {...fundFormMessages.fundActionFormType} />}>
                            <option key="novalue" value={null} />
                            {actionConfig.map(item => (
                                <option key={item.code} value={item.code}>
                                    {item.name}
                                </option>
                            ))}
                        </FF>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary" disabled={submitting}>
                            {<FormattedMessage {...globalMessages.run} />}
                        </Button>
                        <Button variant="link" onClick={onClose}>
                            {<FormattedMessage {...globalMessages.cancel} />}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

const form = reduxForm({
    form: 'RunActionForm',
    validate,
})(injectIntl(RunActionForm));

export default connect((state, props) => {
    const {
        arrRegion: {funds, activeIndex},
    } = state;

    let actionConfig = null;
    if (activeIndex !== null && funds[activeIndex].fundAction) {
        const {
            fundAction: {
                config: {fetched, data},
            },
        } = funds[activeIndex];
        if (fetched) {
            actionConfig = data;
        }
    }
    return {
        actionConfig,
    };
})(form);
