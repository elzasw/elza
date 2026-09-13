/**
 * Komponenta pro reindexaci fuultextu.
 *
 * @author Jiří Vaněk
 * @since 21.1.2016
 */
import React from 'react';
import {connect} from 'react-redux';
import {Button} from '../ui';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage, defineMessages } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    reindexing: { id: 'admin.fulltext.message.reindexing', defaultMessage: 'Probíhá reindexace...' },
    reindex: { id: 'admin.fulltext.action.reindex', defaultMessage: 'Reindexovat' },
});
import {getIndexStateFetchIfNeeded, reindex} from 'actions/admin/fulltext.jsx';

class AdminFulltextReindex extends AbstractReactComponent {
    UNSAFE_componentWillReceiveProps(nextProps) {
        if (!nextProps.fetched) {
            this.props.dispatch(getIndexStateFetchIfNeeded());
        }
    }

    componentDidMount() {
        if (!this.props.fetched) {
            this.props.dispatch(getIndexStateFetchIfNeeded());
        }
    }

    renderReindexing() {
        return <div><FormattedMessage {...messages.reindexing} /></div>;
    }

    renderNotReindexing() {
        return (
            <Button onClick={this.startReindexing.bind(this)} bsSize="xsmall">
                <FormattedMessage {...messages.reindex} />
            </Button>
        );
    }

    startReindexing() {
        this.props.dispatch(reindex());
    }

    render() {
        return this.props.indexing ? this.renderReindexing() : this.renderNotReindexing();
    }
}

export default connect()(AdminFulltextReindex);
