/**
 * Formulář importu rejstříkových hesel
 * <ImportForm fund onSubmit={this.handleCallImportRegistry} />
 */
import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import {WebApi} from 'actions/index.jsx';
import AbstractReactComponent from '../AbstractReactComponent';
import HorizontalLoader from '../shared/loading/HorizontalLoader';
import i18n from '../i18n';
import FormInputField from '../shared/form/FormInputField';

class ExportForm extends AbstractReactComponent {
    static propTypes = {};

    state = {
        defaultScopes: [],
        transformationNames: [],
        iExportFormsFetching: true,
        isFetching: true,
    };

    componentDidMount() {
        WebApi.getExportTransformations().then(json => {
            this.setState({
                transformationNames: json,
                isFetching: false,
            });
        });
    }

    validate = (values, props) => {
        const errors = {};
        return errors;
    };

    submitOptions = {finishOnSubmit: true};

    submitReduxForm = (values, dispatch) =>
        submitForm(this.validate, values, this.props, this.props.onSubmitForm, dispatch, this.submitOptions);

    render() {
        const {
            onClose,
            handleSubmit,
        } = this.props;
        const {isFetching} = this.state;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    {isFetching ? (
                        <HorizontalLoader />
                    ) : (
                        <Field
                            name="code"
                            component={FormInputField}
                            as="select"
                            label={i18n('export.transformationName')}
                        >
                            <option key="blankName" />
                            {this.state.transformationNames.map((i, index) => {
                                return (
                                    <option key={index + 'name'} value={i}>
                                        {i}
                                    </option>
                                );
                            })}
                        </Field>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit">{i18n('global.action.export')}</Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm({
    form: 'exportForm',
})(ExportForm);
