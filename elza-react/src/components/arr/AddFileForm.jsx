import PropTypes from 'prop-types';
import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
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
            errors.name = i18n('global.validation.required');
        }
        if (!values.file) {
            errors.file = i18n('global.validation.required');
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
                        <FF label={i18n('dms.file.name')} name={"name"} />
                        <FF field={FileInput} name={"file"} />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="outline-secondary" type="submit">{i18n('global.action.add')}</Button>
                        <Button variant="link" onClick={onClose}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

export default reduxForm({
    form: 'addFileForm'
})(AddFileForm);
