import React from 'react';
import {Field, formValueSelector, reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import {connect} from "react-redux";
import ReduxFormFieldErrorDecorator from "../shared/form/ReduxFormFieldErrorDecorator";

/**
 * Formulář editace rejstříkového hesla
 * <EditRegistryForm create onSubmit={this.handleCallEditRegistry} />
 */

class EditRegistryForm extends AbstractReactComponent {
    static validate = (values, props) => {
        const errors = {};

        if (!values.typeId) {
            errors.typeId = i18n('global.validation.required');
        }
        return errors;
    };

    state = {};

    componentDidMount() {
        if (this.props.initData) {
            this.props.load(this.props.initData);
        }
    }

    submitReduxForm = (values, dispatch) => {
        return submitForm(EditRegistryForm.validate, values, this.props, this.props.onSubmitForm, dispatch);
    }

    render() {
        const {
            handleSubmit,
            change,
            blur,
            onClose,
            submitting,
            refTables: {apTypes},
        } = this.props;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    <Field
                        label={i18n('registry.update.type')}
                        items={apTypes.items}
                        tree
                        alwaysExpanded
                        allowSelectItem={item => item.addRecord}
                        onChange={item => {
                            change('typeId', item ? item.id : null);
                        }}
                        // onBlur={item => {
                        //     blur('typeId', item ? item.id : null);
                        // }}
                        disabled={submitting}
                        name={'typeId'}
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={Autocomplete}
                        passOnly
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n('global.action.store')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const mapStateToProps = (state) => {
    const selector = formValueSelector('editRegistryForm');
    return {
        initialValues: {}, //state.form.editRegistryForm.initialValues,
        refTables: state.refTables,
    };
}

export default connect(mapStateToProps)(reduxForm(
    {
        form: 'editRegistryForm',
    }
)(EditRegistryForm));
