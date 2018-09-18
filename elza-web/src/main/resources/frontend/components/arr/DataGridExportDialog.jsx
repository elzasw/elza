import React from 'react';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button} from 'react-bootstrap';
import DataGridExportForm from './DataGridExportForm';

/**
 * Dialog pro export tabulkového zobrazení.
 */
class DataGridExportDialog extends AbstractReactComponent {


    handleSubmit = () => {
        this.refs.form.getWrappedInstance().submit();
    };

    render() {
        const {onClose, versionId, fundDataGrid} = this.props;

        return (
            <div>
                <Modal.Body>
                    <span><label>{i18n("dataGrid.export.title")}</label></span>
                    <DataGridExportForm versionId={versionId} fundDataGrid={fundDataGrid} ref="form"/>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" onClick={this.handleSubmit}>{i18n('global.action.export')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}


export default DataGridExportDialog;
