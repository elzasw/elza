/**
 * Formulář přidání nebo uzavření AS.
 */

import React from 'react';
import {AbstractReactComponent, i18n, BulkActionsTable} from 'components/index.jsx';
import {Modal, Button} from 'react-bootstrap';

var BulkActionsDialog = class BulkActionsDialog extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

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
};


BulkActionsDialog.propTypes = {
    mandatory: React.PropTypes.bool.isRequired
};

export default BulkActionsDialog;



