/**
 * Formulář přidání souboru.
 */

import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {Modal, Button} from 'react-bootstrap';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    if (!values.name) {
        errors.name = i18n('global.validation.required');
    }
    if (!values.file) {
        errors.file = i18n('global.validation.required');
    }

    return errors;
};

const AddFileForm = class AddFileForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.props.load(this.props.initData);
    }

    render() {
        const {fields: {name, file}, handleSubmit, onClose} = this.props;

        var submitForm = submitReduxForm.bind(this, validate);

        return (
            <div className="add-file-form-container">
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <FormInput type="text" label={i18n('dms.file.name')} {...name} {...decorateFormField(name)} />
                        <FormInput type="file" {...file} {...decorateFormField(file)} value={null}/>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.add')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};

AddFileForm.propTypes = {
    initData: React.PropTypes.object,
    onSubmitForm: React.PropTypes.func.isRequired
};

AddFileForm.defaultProps = {
    initData: {}
};

module.exports = reduxForm(
    {form: 'addFileForm', fields: ['name', 'file']},
    null,
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addFileForm', data})}
)(AddFileForm);
