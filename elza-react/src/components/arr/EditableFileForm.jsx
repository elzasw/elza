import PropTypes from 'prop-types';
import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
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
            errors.name = i18n('global.validation.required');
        }
        if (!values.mimeType) {
            errors.mimeType = i18n('global.validation.required');
        }
        if (!values.fileName) {
            errors.fileName = i18n('global.validation.required');
        }
        if (!values.content) {
            errors.content = i18n('global.validation.required');
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
                            label={i18n('dms.file.name')}
                        />
                        <Field
                            disabled={submitting}
                            name="mimeType"
                            type="select"
                            component={FormInputField}
                            label={i18n('dms.file.mimeType')}
                        >
                            <option value={''} key="no-select">
                                {i18n('global.action.select')}
                            </option>
                            {dms.fetched && dms.rows.map(x => <option value={x}>{x}</option>)}
                        </Field>
                        <Field
                            disabled={submitting}
                            name="fileName"
                            type="text"
                            component={FormInputField}
                            label={i18n('dms.file.fileName')}
                        />
                        <Field
                            name="content"
                            as="textarea"
                            component={FormInputField}
                            label={i18n('dms.file.content')}
                            disabled={submitting}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary">{i18n(create ? 'global.action.add' : 'global.action.update')}</Button>
                        <Button variant="link" onClick={onClose}>
                            {i18n('global.action.cancel')}
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

export default connect(mapStateToProps)(editableFileReduxForm);
