import React from 'react';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import PersistentSortForm from './PersistentSortForm';

/**
 * Dialog pro funkci perzistentního řazení
 */
class PersistentSortDialog extends AbstractReactComponent {


    handleSubmit = () => {
        this.refs.form.getWrappedInstance().submit();
    };

    render() {
        const {onClose, node, versionId} = this.props;

        return (
            <div>
                <Modal.Body>
                    <span><label>{i18n("arr.history.title.nodeChanges") + ":"} &nbsp;</label>{node.name}</span>
                    <PersistentSortForm versionId={versionId} node={node} ref="form"/>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" onClick={this.handleSubmit}>{i18n('global.action.run')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}


export default PersistentSortDialog;
