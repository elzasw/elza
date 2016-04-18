/**
 * Komponenta záložek otevřených AS.
 */

require ('./FundTreeTabs.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, NoFocusButton, i18n, Tabs, FundTreeLazy, FundTreeMain, Icon} from 'components';
import * as types from 'actions/constants/ActionTypes';
import {AppActions} from 'stores';
import {MenuItem} from 'react-bootstrap';
import {fundsFetchIfNeeded, selectFundTab, closeFundTab, fundExtendedView} from 'actions/arr/fund'
import {fundTreeFocusNode, fundTreeFetchIfNeeded, fundTreeNodeExpand, fundTreeNodeCollapse} from 'actions/arr/fundTree'
import {fundSelectSubNode} from 'actions/arr/nodes'
import {createFundRoot, getParentNode} from './ArrUtils.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu'
import {propsEquals, dateToString} from 'components/Utils'
import {indexById} from 'stores/app/utils.jsx'

var FundTreeTabs = class FundTreeTabs extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callFundSelectSubNode', 'handleNodeClick', 'handleSelectInNewTab',
            'handleContextMenu', 'handleToggleExtendedView');
    }

    componentDidMount() {
        const {activeFund} = this.props;

        this.dispatch(fundsFetchIfNeeded());
        if (activeFund) {
            this.requestFundTreeData(activeFund);
        }
    }

    componentWillReceiveProps(nextProps) {
        const {activeFund} = nextProps;
        
        this.dispatch(fundsFetchIfNeeded());
        if (activeFund) {
            this.requestFundTreeData(activeFund);
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['funds', 'activeFund', 'focus']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    requestFundTreeData(activeFund) {
        this.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_MAIN, activeFund.versionId, activeFund.fundTree.expandedIds, activeFund.fundTree.selectedId));
    }

    /**
     * Zobrazení kontextového menu pro daný uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleContextMenu(node, e) {
        e.preventDefault();
        e.stopPropagation();

        var menu = (
            <ul className="dropdown-menu">
                <MenuItem onClick={this.handleSelectInNewTab.bind(this, node)}>{i18n('fundTree.action.openInNewTab')}</MenuItem>
            </ul>
        )

        this.dispatch(fundTreeFocusNode(types.FUND_TREE_AREA_MAIN, this.props.activeFund.versionId, node));
        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    /**
     * Otevření uzlu v nové záložce.
     * @param node {Object} uzel
     */
    handleSelectInNewTab(node) {
        this.dispatch(contextMenuHide());

        this.callFundSelectSubNode(node, true);
    }

    /**
     * Otevření uzlu v záložce.
     * @param node {Object} uzel
     * @param openNewTab {Boolean} true, pokud se má otevřít v nové záložce
     */
    callFundSelectSubNode(node, openNewTab) {
        var parentNode = getParentNode(node, this.props.activeFund.fundTree.nodes);
        if (parentNode == null) {   // root
            parentNode = createFundRoot(this.props.activeFund);
        }
        this.dispatch(fundSelectSubNode(this.props.activeFund.versionId, node.id, parentNode, openNewTab, null, false));
    }

    /**
     * Otevření uzlu v aktuální záložce (pokud aktuální není, otevře se v nové).
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, e) {
        this.callFundSelectSubNode(node, false);
    }

    handleToggleExtendedView() {
        this.dispatch(fundExtendedView(true));
    }

    render() {
        const {funds, activeFund, focus} = this.props;

        if (funds.length == 0) {
            return <div></div>
        }

        var tabs = funds.map((fund) => {
            return {
                id: fund.id,
                versionId: fund.versionId,
                key: fund.versionId,
                title: fund.name,
                desc: fund.lockDate ? dateToString(new Date(fund.lockDate)) : ''
            }
        });

        var activeItem = tabs[indexById(tabs, activeFund.versionId, "versionId")]

        return (
            <Tabs.Container className='fa-tabs-container'>
                <NoFocusButton onClick={this.handleToggleExtendedView} className='extended-view-toggle'><Icon glyph='fa-expand'/></NoFocusButton>

                <Tabs.Tabs closable items={tabs} activeItem={activeItem}
                    onSelect={item=>this.dispatch(selectFundTab(item))}
                    onClose={item=>this.dispatch(closeFundTab(item))}
                />
                <Tabs.Content>
                    <FundTreeMain
                        fund = {activeFund}
                        cutLongLabels={true}
                        versionId={activeFund.versionId}
                        {...activeFund.fundTree}
                        ref='tree'
                        focus={focus}
                    />
                    {false && <FundTreeLazy 
                        fund={activeFund}
                        {...activeFund.fundTree}
                        versionId={this.props.activeFund.versionId}
                        onOpenCloseNode={(node, expand) => {expand ? this.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_MAIN, node)) : this.dispatch(fundTreeNodeCollapse(types.FUND_TREE_AREA_MAIN, this.props.activeFund.versionId, node))}}
                        onContextMenu={this.handleContextMenu}
                        onNodeClick={this.handleNodeClick}
                    /> }
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

FundTreeTabs.propTypes = {
    funds: React.PropTypes.array.isRequired,
    activeFund: React.PropTypes.object,
    focus: React.PropTypes.object.isRequired,
}

module.exports = connect()(FundTreeTabs);
