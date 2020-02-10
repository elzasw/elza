import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import {connect} from 'react-redux';
import * as dms from "../../actions/global/dms";
import storeFromArea from "../../shared/utils/storeFromArea";

/**
 * Formulář editace souboru s editovatelným typem.
 */
class EditableFileForm extends AbstractReactComponent {
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

    static PropTypes = {
        initData: React.PropTypes.object,
        onSubmitForm: React.PropTypes.func.isRequired
    };

    state = {};

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.props.load(this.props.initData);
        this.props.dispatch(dms.mimeTypesFetchIfNeeded());
    }

    submitReduxForm = (values, dispatch) => submitForm(EditableFileForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {fields: {name, content, fileName, mimeType}, handleSubmit, onClose, dms, create} = this.props;

        return (
            <div className="add-file-form-container">
                <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                    <Modal.Body>
                        <FormInput type="text" label={i18n('dms.file.name')} {...name} {...decorateFormField(name)} />
                        <FormInput label={i18n('dms.file.mimeType')} componentClass="select" {...mimeType} {...decorateFormField(mimeType)}>
                            <option value={""}></option>
                            {dms.fetched && dms.rows.map(x => <option value={x}>{x}</option>)}
                        </FormInput>
                        <FormInput type="text" label={i18n('dms.file.fileName')} {...fileName} {...decorateFormField(fileName)} />
                        <FormInput componentClass="textarea" label={i18n('dms.file.content')} {...content} {...decorateFormField(content)} />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit">{i18n(create ? 'global.action.add' : 'global.action.update')}</Button>
                        <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }
}

EditableFileForm.defaultProps = {
    initData: {}
};

const editableFileReduxForm = reduxForm(
    {form: 'addFileForm', fields: ['name', 'content', 'fileName', 'mimeType']},
    null,
    {load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addFileForm', data})}
)(EditableFileForm);

function mapStateToProps(state) {
    return {
        dms: storeFromArea(state, dms.MIME_TYPES_AREA)
    };
}

export default connect(mapStateToProps)(editableFileReduxForm);
