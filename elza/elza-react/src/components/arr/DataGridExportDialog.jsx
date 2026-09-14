import React from 'react';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { fundFormMessages } from './fundFormMessages';
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
                        <label>{<FormattedMessage {...fundFormMessages.dataGridExportTitle} />}</label>
                    </span>
                    <DataGridExportForm versionId={versionId} fundDataGrid={fundDataGrid} />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" onClick={this.handleSubmit}>
                        {<FormattedMessage {...globalMessages.export} />}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {<FormattedMessage {...globalMessages.cancel} />}
                    </Button>
                </Modal.Footer>
            </div>
        );
    }
}

export default connect()(DataGridExportDialog);
