/**
 * Strom AP s jednotlivými verzemi.
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
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.opened) {
            this.dispatch(faFileTreeFetchIfNeeded());
        }
    }

    componentDidMount() {
        if (this.props.opened) {
            this.dispatch(faFileTreeFetchIfNeeded());
        }
    }

    /**
     * Vybrání verze AP pro zobrazení.
     * @param fa {Object} AP
     * @param version {Object} verze AP
     */
    handleSelect(fa, version) {
        var fa = Object.assign({}, fa, {faId: fa.id, versionId: version.id, id: version.id, activeVersion: version});

        this.dispatch(selectFaTab(fa));
        this.props.onSelect(fa);
    }

    /**
     * Převod datumu do řetězce - v budoucnu při více locale nahradit metodou pracující s locale.
     * @param date {Date} datum
     * @return {String} datum
     */
    dateToString(date) {
        var dd  = date.getDate().toString();
        var mm = (date.getMonth() + 1).toString();
        var yyyy = date.getFullYear().toString();
        return (dd[1]?dd:"0"+dd[0]) + "." + (mm[1]?mm:"0"+mm[0]) + "." + yyyy;
    }

    /**
     * Renderování otevřeného panelu.
     * @return {Object} view
     */
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

    /**
     * Renderování zavřeného panelu.
     * @return {Object} view
     */
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

FaFileTree.propTypes = {
    opened: React.PropTypes.bool.isRequired,
    onSelect: React.PropTypes.func.isRequired,
    items: React.PropTypes.array.isRequired,
    isFetching: React.PropTypes.bool.isRequired,
    fetched: React.PropTypes.bool.isRequired,
}

module.exports = connect()(FaFileTree);