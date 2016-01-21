/**
 * Strom archivních souborů.
 */

require ('./FaFileTree.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components';
import {Nav, NavItem} from 'react-bootstrap';

import {faFileTreeFetchIfNeeded} from 'actions/arr/faFileTree'
import {selectFaTab} from 'actions/arr/fa'

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
        var fa = Object.assign({}, fa, {faId: fa.id, versionId: version.id, id: version.id, activeVersion: version});

        this.dispatch(selectFaTab(fa));
        this.props.onSelect(fa);
    }

    dateToString(date) {
        var dd  = date.getDate().toString();
        var mm = (date.getMonth() + 1).toString();
        var yyyy = date.getFullYear().toString();
        return (dd[1]?dd:"0"+dd[0]) + "." + (mm[1]?mm:"0"+mm[0]) + "." + yyyy;
    }

    renderOpened() {
        var rows = [];
        this.props.items.each(item=>{
            rows.push(
                <NavItem className='finding-aid' key={item.id} disabled>
                    {item.name}
                </NavItem>
            )
            item.versions.each(ver => {
                rows.push(
                    <NavItem className='version' key={item.id + '_' + ver.id} onClick={this.handleSelect.bind(this, item, ver)}>
                        {ver.lockDate ? this.dateToString(ver.lockDate) : i18n('arr.fa.currentVersion')}
                    </NavItem>
                )
            });
        });

        var navRows = (
            <Nav>
                {rows}
            </Nav>
        )

        return (
            <div className='finding-aid-file-tree-container'>
                {(this.props.isFetching || !this.props.fetched) && <Loading/>}
                {(!this.props.isFetching && this.props.fetched) && navRows}
            </div>
        );
    }

    renderClosed() {
        return (
            <div className='finding-aid-file-tree-container'>
                ...
            </div>
        );
    }

    render() {
        return this.props.opened ? this.renderOpened() : this.renderClosed();
    }
}

module.exports = connect()(FaFileTree);