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

var AdminFulltextReindex = class AdminFulltextReindex extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            reindexing: false
        };
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
        this.setState({reindexing: true});
        WebApi.reindex();
    }

    reindexingFinished() {
        this.setState({reindexing: false});
    }

    render() {
        return this.state.reindexing ? this.renderReindexing() : this.renderNotReindexing();
    }
}

module.exports = connect()(AdminFulltextReindex);
