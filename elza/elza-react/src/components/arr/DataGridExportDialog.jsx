import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Modal} from 'react-bootstrap';
import {Button} from '../ui';
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
                    <span>
                        <label>{i18n('dataGrid.export.title')}</label>
                    </span>
                    <DataGridExportForm versionId={versionId} fundDataGrid={fundDataGrid} ref="form" />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" onClick={this.handleSubmit}>
                        {i18n('global.action.export')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </div>
        );
    }
}

export default DataGridExportDialog;
