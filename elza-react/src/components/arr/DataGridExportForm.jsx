/**
 * Formulář nastavení funkce pro perzistentní řazení
 *
 */
import PropTypes from 'prop-types';

import React from 'react';
import {reduxForm, Field} from 'redux-form';
import {Form} from 'react-bootstrap';
import AbstractReactComponent from '../AbstractReactComponent';
import i18n from '../i18n';
import {connect} from 'react-redux';
import {UrlFactory} from '../../actions/WebApi';
import {modalDialogHide} from '../../actions/global/modalDialog';
import {downloadFile} from '../../actions/global/download';
import FormInputField from "../shared/form/FormInputField";
import {FORM_DATA_GRID_EXPORT} from "../../constants";

const transformSubmitData = values => {
    return {
        exportType: values.exportType === EXPORT_TYPE.TABLE,
    };
};

const handleSubmit = (values, dispatch, versionId, fundDataGrid) => {
    dispatch(modalDialogHide());
    let columns = [];
    if (fundDataGrid.columnsOrder && fundDataGrid.columnsOrder.length === 0) {
        columns = Object.keys(fundDataGrid.visibleColumns)
            .filter(key => fundDataGrid.visibleColumns[key])
            .map(key => parseInt(key));
    } else {
        columns = fundDataGrid.columnsOrder;
    }
    let url = UrlFactory.exportGridData(versionId, values.exportType, columns);
    dispatch(downloadFile(url));
};

const EXPORT_TYPE = {
    TABLE: 'TABLE',
    DATA: 'DATA',
};

class DataGridExportForm extends AbstractReactComponent {
    static propTypes = {
        versionId: PropTypes.number.isRequired,
    };

    render() {
        return (
            <Form>
                <Field
                    component={FormInputField}
                    type="radio"
                    name="exportType"
                    value={EXPORT_TYPE.TABLE}
                    label={i18n('dataGrid.export.exportType.table')}
                    inline
                />

                <Field
                    component={FormInputField}
                    type="radio"
                    name="exportType"
                    value={EXPORT_TYPE.DATA}
                    label={i18n('dataGrid.export.exportType.data')}
                    inline
                />
            </Form>
        );
    }
}

const formComponent = reduxForm({
    form: FORM_DATA_GRID_EXPORT,
})(DataGridExportForm);

export default connect((state, props) => {
    const {initialValues} = props;

    //zpětné poskládání dat pokud je form použit na /action, pro zapamatované nastavení akce
    const transformedInitialValues = initialValues
        ? {
              exportType: initialValues.exportType === 'TABLE' ? EXPORT_TYPE.TABLE : EXPORT_TYPE.DATA,
          }
        : {
              exportType: EXPORT_TYPE.TABLE,
          };

    return {
        onSubmit: (values, dispatch) =>
            props.onSubmit
                ? props.onSubmit(props.versionId, transformSubmitData(values))
                : handleSubmit(values, dispatch, props.versionId, props.fundDataGrid),
        validate: validate,
        initialValues: transformedInitialValues,
        enableReinitialize: true,
    };
})(formComponent);

const validate = values => {
    const errors = {};
    if (!values.exportType) {
        errors.itemType = i18n('dataGrid.export.exportType.noSelection.item');
    }

    return errors;
};
