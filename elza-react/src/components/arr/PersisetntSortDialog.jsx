import React from 'react';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { fundFormMessages } from './fundFormMessages';
import {Modal} from 'react-bootstrap';
import {Button} from '../ui';
import PersistentSortForm from './PersistentSortForm';
import {connect} from 'react-redux';
import { submit } from 'redux-form'

/**
 * Dialog pro funkci perzistentního řazení
 */
class PersistentSortDialog extends AbstractReactComponent {
    handleSubmit = () => {
        this.props.dispatch(submit('persistentSortForm'));
    };

    render() {
        const {onClose, node, versionId, fund} = this.props;

        return (
            <div>
                <Modal.Body>
                    <span>
                        <label>{this.props.intl.formatMessage(fundFormMessages.historyTitleNodeChanges) + ':'} &nbsp;</label>
                        {node.name}
                    </span>
                    <PersistentSortForm fund={fund} versionId={versionId} node={node} />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" onClick={this.handleSubmit}>
                        {<FormattedMessage {...globalMessages.run} />}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {<FormattedMessage {...globalMessages.cancel} />}
                    </Button>
                </Modal.Footer>
            </div>
        );
    }
}

export default connect()(injectIntl(PersistentSortDialog));
