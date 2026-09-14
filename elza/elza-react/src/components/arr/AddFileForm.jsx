import PropTypes from 'prop-types';
import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { fundFormMessages } from './fundFormMessages';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import FileInput from '../shared/form/FileInput';
import FF from '../shared/form/FF';

/**
 * Formulář přidání souboru.
 */
class AddFileForm extends AbstractReactComponent {
    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};

        if (!values.name) {
            errors.name = this.props.intl.formatMessage(globalMessages.validationRequired);
        }
        if (!values.file) {
            errors.file = this.props.intl.formatMessage(globalMessages.validationRequired);
        }

        return errors;
    };

    static propTypes = {
        initData: PropTypes.object,
        onSubmitForm: PropTypes.func.isRequired,
    };

    state = {};


    submitReduxForm = (values, dispatch) =>
        submitForm(AddFileForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {
            handleSubmit,
            onClose,
        } = this.props;

        return (
            <div className="add-file-form-container">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <FF label={<FormattedMessage {...fundFormMessages.dmsFileName} />} name={"name"} />
                        <FF field={FileInput} name={"file"} />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="outline-secondary" type="submit">{<FormattedMessage {...globalMessages.add} />}</Button>
                        <Button variant="link" onClick={onClose}>
                            {<FormattedMessage {...globalMessages.cancel} />}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

export default reduxForm({
    form: 'addFileForm'
})(injectIntl(AddFileForm));
