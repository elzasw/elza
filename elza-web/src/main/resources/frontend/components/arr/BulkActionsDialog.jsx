/**
 * Formulář přidání nebo uzavření AS.
 */

import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import BulkActionsTable from './BulkActionsTable'
import {Modal, Button} from 'react-bootstrap';

class BulkActionsDialog extends AbstractReactComponent {

    static PropTypes = {
        mandatory: React.PropTypes.bool.isRequired
    };

    render() {
        const {onClose, mandatory} = this.props;
        return (
            <div>
                <Modal.Body>
                    <BulkActionsTable mandatory={mandatory} versionValidate={false}/>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default BulkActionsDialog;



