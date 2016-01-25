/**
 * Komponenta pro reindexaci fuultextu.
 *
 * @author Jiří Vaněk
 * @since 21.1.2016
 */
import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, i18n} from 'components';
import {WebApi} from 'actions'
import {getIndexStateFetchIfNeeded, actionReindex} from 'actions/admin/fulltext';

var AdminFulltextReindex = class AdminFulltextReindex extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.dispatch(getIndexStateFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(getIndexStateFetchIfNeeded());
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
        this.dispatch(actionReindex());
    }

    render() {
        return this.props.indexing ? this.renderReindexing() : this.renderNotReindexing();
    }
}

module.exports = connect()(AdminFulltextReindex);
