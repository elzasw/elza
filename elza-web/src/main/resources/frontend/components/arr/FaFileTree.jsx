/**
 * Strom archivních souborů.
 */

require ('./FaFileTree.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components';
import {Nav, NavItem} from 'react-bootstrap';

import {faFileTreeFetchIfNeeded} from 'actions/arr/faFileTree'
import {selectFa} from 'actions/arr/fa'

var FaFileTree = class FaFileTree extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelect');

        if (props.opened) {
            this.dispatch(faFileTreeFetchIfNeeded());
        }
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.opened) {
            this.dispatch(faFileTreeFetchIfNeeded());
        }
    }

    handleSelect(fa, version) {
        var fa = Object.assign({}, fa, {versionId: version.id});

        this.dispatch(selectFa(fa));
        this.props.onSelect(fa);
    }

    renderOpened() {
        var rows = this.props.items.map(item=>{
            var verRows = item.versions.map(ver => {
                return <NavItem key={ver.id} onClick={this.handleSelect.bind(this, item, ver)}>{ver.name}</NavItem>
            });
            return (
                <NavItem key={item.id} disabled>
                    {item.name}
                    {verRows}
                </NavItem>
            )
        });

        var navRows = (
            <Nav>
                {rows}
            </Nav>
        )

        return (
            <div className='finding-aid-file-tree-conteiner'>
                {(this.props.isFetching || !this.props.fetched) && <Loading/>}
                {(!this.props.isFetching && this.props.fetched) && navRows}
            </div>
        );
    }

    renderClosed() {
        return (
            <div className='finding-aid-file-tree-conteiner'>
                ...
            </div>
        );
    }

    render() {
        return this.props.opened ? this.renderOpened() : this.renderClosed();
    }
}

module.exports = connect()(FaFileTree);