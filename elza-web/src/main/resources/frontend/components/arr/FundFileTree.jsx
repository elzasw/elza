/**
 * Strom AS s jednotlivými verzemi.
 */

require ('./FundFileTree.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Icon, Loading} from 'components';
import {Nav, NavItem} from 'react-bootstrap';
import {fundFileTreeFetchIfNeeded} from 'actions/arr/fundFileTree'
import {selectFundTab} from 'actions/arr/fund'
import {propsEquals, dateToString} from 'components/Utils'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils'

var FundFileTree = class FundFileTree extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelect');
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.opened) {
            this.dispatch(fundFileTreeFetchIfNeeded());
        }
    }

    componentDidMount() {
        if (this.props.opened) {
            this.dispatch(fundFileTreeFetchIfNeeded());
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['opened', 'items', 'isFetching', 'fetched']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    /**
     * Vybrání verze AS pro zobrazení.
     * @param fund {Object} AS
     * @param version {Object} verze AS
     */
    handleSelect(fund, version) {
        var fund = getFundFromFundAndVersion(fund, version);

        this.dispatch(selectFundTab(fund));
        this.props.onSelect(fund);
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
                    <Icon glyph='fa-align-justify'/>
                    {item.name}
                </NavItem>
            )
            item.versions.each(ver => {
                var glyph = '';
                rows.push(
                    <NavItem className='version' key={item.id + '_' + ver.id} onClick={this.handleSelect.bind(this, item, ver)}>
                        <Icon glyph={glyph}/>
                        {ver.lockDate ? dateToString(new Date(ver.lockDate)) : i18n('arr.fund.currentVersion')}
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

FundFileTree.propTypes = {
    opened: React.PropTypes.bool.isRequired,
    onSelect: React.PropTypes.func.isRequired,
    items: React.PropTypes.array.isRequired,
    isFetching: React.PropTypes.bool.isRequired,
    fetched: React.PropTypes.bool.isRequired,
}

module.exports = connect()(FundFileTree);