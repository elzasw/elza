import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {connect} from 'react-redux';
import {submit} from 'redux-form';
import {Modal} from 'react-bootstrap';
import {Button} from '../ui';
import DataGridExportForm from './DataGridExportForm';
import {FORM_DATA_GRID_EXPORT} from "../../constants";

/**
 * Dialog pro export tabulkového zobrazení.
 */
class DataGridExportDialog extends AbstractReactComponent {
    handleSubmit = () => {
        this.props.dispatch(submit(FORM_DATA_GRID_EXPORT));
    };

    render() {
        const {onClose, versionId, fundDataGrid} = this.props;

        return (
            <div>
                <Modal.Body>
                    <span>
                        <label>{i18n('dataGrid.export.title')}</label>
                    </span>
                    <DataGridExportForm versionId={versionId} fundDataGrid={fundDataGrid} />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" onClick={this.handleSubmit}>
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

export default connect()(DataGridExportDialog);
