/**
 * Komponenta pro reindexaci fuultextu.
 *
 * @author Jiří Vaněk
 * @since 21.1.2016
 */
import React from 'react';
import {connect} from 'react-redux';
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, i18n} from 'components/shared';
import {getIndexStateFetchIfNeeded, reindex} from 'actions/admin/fulltext.jsx';

var AdminFulltextReindex = class AdminFulltextReindex extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    componentWillReceiveProps(nextProps) {
        if (!nextProps.fetched) {
            this.dispatch(getIndexStateFetchIfNeeded());
        }
    }

    componentDidMount() {
        if (!this.props.fetched) {
            this.dispatch(getIndexStateFetchIfNeeded());
        }
    }

    renderReindexing() {
        return (
            <div>{i18n("admin.fulltext.message.reindexing")}</div>
        );
    }

    renderNotReindexing() {
        return (
            <Button onClick={this.startReindexing.bind(this)} bsSize="xsmall">{i18n("admin.fulltext.action.reindex")}</Button>
        );
    }

    startReindexing() {
        this.dispatch(reindex());
    }

    render() {
        return this.props.indexing ? this.renderReindexing() : this.renderNotReindexing();
    }
}

export default connect()(AdminFulltextReindex);
