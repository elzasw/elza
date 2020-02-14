import PropTypes from 'prop-types';
import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'


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
        onSubmitForm: PropTypes.func.isRequired
    };

    state = {};

    UNSAFE_componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.props.load(this.props.initData);
    }

    submitReduxForm = (values, dispatch) => submitForm(AddFileForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {fields: {name, file}, handleSubmit, onClose} = this.props;

        return (
            <div className="add-file-form-container">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <FormInput type="text" label={i18n('dms.file.name')} {...name} {...decorateFormField(name)} />
                        <FormInput type="file" {...file} {...decorateFormField(file)} value={null}/>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit">{i18n('global.action.add')}</Button>
                        <Button variant="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }
}

AddFileForm.defaultProps = {
    initData: {}
};

export default reduxForm(
    {form: 'addFileForm', fields: ['name', 'file']},
    null,
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addFileForm', data})}
)(AddFileForm);
