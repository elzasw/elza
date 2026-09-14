import PropTypes from 'prop-types';
import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { fundFormMessages } from './fundFormMessages';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx';
import {connect} from 'react-redux';
import * as dms from '../../actions/global/dms';
import storeFromArea from '../../shared/utils/storeFromArea';
import FormInputField from "../shared/form/FormInputField";

/**
 * Formulář editace souboru s editovatelným typem.
 */
class EditableFileForm extends AbstractReactComponent {

    static FORM = 'outputEditForm';

    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};

        if (!values.name) {
            errors.name = this.props.intl.formatMessage(globalMessages.validationRequired);
        }
        if (!values.mimeType) {
            errors.mimeType = this.props.intl.formatMessage(globalMessages.validationRequired);
        }
        if (!values.fileName) {
            errors.fileName = this.props.intl.formatMessage(globalMessages.validationRequired);
        }
        if (!values.content) {
            errors.content = this.props.intl.formatMessage(globalMessages.validationRequired);
        }

        return errors;
    };

    static propTypes = {
        initData: PropTypes.object,
        onSubmitForm: PropTypes.func.isRequired,
    };

    state = {};

    UNSAFE_componentWillReceiveProps(nextProps) {}

    componentDidMount() {
//        this.props.load(this.props.initData);
        this.props.dispatch(dms.mimeTypesFetchIfNeeded());
    }

    submitReduxForm = (values, dispatch) =>
        submitForm(EditableFileForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {
            handleSubmit,
            onClose,
            dms,
            submitting,
            create,
        } = this.props;

        return (
            <div className="add-file-form-container">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <Field
                            disabled={submitting}
                            name="name"
                            type="text"
                            component={FormInputField}
                            label={<FormattedMessage {...fundFormMessages.dmsFileName} />}
                        />
                        <Field
                            disabled={submitting}
                            name="mimeType"
                            type="select"
                            component={FormInputField}
                            label={<FormattedMessage {...fundFormMessages.dmsFileMimeType} />}
                        >
                            <option value={''} key="no-select">
                                {<FormattedMessage {...fundFormMessages.globalActionSelect} />}
                            </option>
                            {dms.fetched && dms.rows.map(x => <option value={x}>{x}</option>)}
                        </Field>
                        <Field
                            disabled={submitting}
                            name="fileName"
                            type="text"
                            component={FormInputField}
                            label={<FormattedMessage {...fundFormMessages.dmsFileFileName} />}
                        />
                        <Field
                            name="content"
                            type="textarea"
                            component={FormInputField}
                            label={<FormattedMessage {...fundFormMessages.dmsFileContent} />}
                            disabled={submitting}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary">{this.props.intl.formatMessage(create ? globalMessages.add : globalMessages.save)}</Button>
                        <Button variant="link" onClick={onClose}>
                            {<FormattedMessage {...globalMessages.cancel} />}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

EditableFileForm.defaultProps = {
    initData: {},
};

const editableFileReduxForm = reduxForm(
    {form: EditableFileForm.FORM}
)(EditableFileForm);

function mapStateToProps(state) {
    return {
        dms: storeFromArea(state, dms.MIME_TYPES_AREA),
    };
}

export default connect(mapStateToProps)(injectIntl(editableFileReduxForm));
