/**
 * Formulář importu rejstříkových hesel
 * <ImportForm fund onSubmit={this.handleCallImportRegistry} />
 */
import React from 'react';
import {Field, reduxForm} from 'redux-form';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import AbstractReactComponent from '../AbstractReactComponent';
import HorizontalLoader from '../shared/loading/HorizontalLoader';
import i18n from '../i18n';
import FormInputField from '../shared/form/FormInputField';
import {connect} from "react-redux";
import * as exportFilters from "../../actions/refTables/exportFilters";

class ExportForm extends AbstractReactComponent {
    static propTypes = {};

    componentDidMount() {
        this.fetch();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.fetch();
    }

    fetch = () => {
        const {dispatch} = this.props;
        dispatch(exportFilters.fetchIfNeeded());
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
            exportFilters,
        } = this.props;
        const isFetching = exportFilters.isFetching;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    {isFetching ? (
                        <HorizontalLoader />
                    ) : (
                            <>
                                <Field
                                    name="exportFilterId"
                                    component={FormInputField}
                                    type="select"
                                    label={i18n('export.exportFilter')}
                                >
                                    <option key="blankName" />
                                    {exportFilters.data && exportFilters.data.map((i, index) => {
                                        return (
                                            <option key={index + 'name'} value={i.id}>
                                                {i.name}
                                            </option>
                                        );
                                    })}
                                </Field>
                                <Field
                                    name="includeUUID"
                                    component={FormInputField}
                                    type="checkbox"
                                    label={i18n('export.includeUUID')}
                                />
                                <Field
                                    name="includeAccessPoints"
                                    component={FormInputField}
                                    type="checkbox"
                                    label={i18n('export.includeAccessPoints')}
                                />
                            </>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary">{i18n('global.action.export')}</Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const form = reduxForm({
    form: 'exportForm',
})(ExportForm);

export default connect((state, props) => {
    return {
        exportFilters: state.refTables.exportFilters,
    };
})(form);
