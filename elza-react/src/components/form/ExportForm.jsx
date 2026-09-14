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
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    exportFilter: { id: 'export.exportFilter', defaultMessage: 'Exportní filtr' },
    includeUUID: { id: 'export.includeUUID', defaultMessage: 'Exportovat UUID' },
    includeAccessPoints: { id: 'export.includeAccessPoints', defaultMessage: 'Exportovat entity' },
    includeDaos: {
        id: 'export.includeDaos',
        defaultMessage: 'Exportovat digitální archivní objekty (DAO)',
    },
    exportAction: { id: 'global.action.export', defaultMessage: 'Exportovat' },
});
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
                                    name="exportFilter"
                                    component={FormInputField}
                                    type="select"
                                    label={this.props.intl.formatMessage(messages.exportFilter)}
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
                                    label={this.props.intl.formatMessage(messages.includeUUID)}
                                />
                                <Field
                                    name="includeAccessPoints"
                                    component={FormInputField}
                                    type="checkbox"
                                    label={this.props.intl.formatMessage(messages.includeAccessPoints)}
                                />
                                <Field
                                    name="includeDaos"
                                    component={FormInputField}
                                    type="checkbox"
                                    label={this.props.intl.formatMessage(messages.includeDaos)}
                                />
                            </>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary"><FormattedMessage {...messages.exportAction} /></Button>
                    <Button variant="link" onClick={onClose}>
                        <FormattedMessage {...globalMessages.cancel} />
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
})(injectIntl(form));
