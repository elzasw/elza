/**
 * Formulář přidání nebo uzavření AP.
 */

import React from 'react';
import {AbstractReactComponent, i18n, BulkActionsTable} from 'components';
import {Modal, Button} from 'react-bootstrap';

var BulkActionsDialog = class BulkActionsDialog extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    <BulkActionsTable mandatory={this.props.mandatory} versionValidate={false}/>
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

module.exports = BulkActionsDialog;



