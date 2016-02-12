/**
 * Komponenta záložek otevřených AP.
 */

require ('./FaTreeTabs.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Tabs, FaTreeLazy, FaTreeMain, Icon} from 'components';
import * as types from 'actions/constants/ActionTypes';
import {AppActions} from 'stores';
import {Button} from 'react-bootstrap';
import {MenuItem} from 'react-bootstrap';
import {fasFetchIfNeeded, selectFaTab, closeFaTab, faExtendedView} from 'actions/arr/fa'
import {faTreeFocusNode, faTreeFetchIfNeeded, faTreeNodeExpand, faTreeNodeCollapse} from 'actions/arr/faTree'
import {faSelectSubNode} from 'actions/arr/nodes'
import {createFaRoot, getParentNode} from './ArrUtils.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu'
import {propsEquals, dateToString} from 'components/Utils'

var FaTreeTabs = class FaTreeTabs extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callFaSelectSubNode', 'handleNodeClick', 'handleSelectInNewTab',
            'handleContextMenu', 'handleToggleExtendedView');
    }

    componentDidMount() {
        const {activeFa} = this.props;

        this.dispatch(fasFetchIfNeeded());
        if (activeFa) {
            this.requestFaTreeData(activeFa);
        }
    }

    componentWillReceiveProps(nextProps) {
        const {activeFa} = nextProps;
        
        this.dispatch(fasFetchIfNeeded());
        if (activeFa) {
            this.requestFaTreeData(activeFa);
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['fas', 'activeFa', 'focus']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    requestFaTreeData(activeFa) {
        this.dispatch(faTreeFetchIfNeeded(types.FA_TREE_AREA_MAIN, activeFa.versionId, activeFa.faTree.expandedIds, activeFa.faTree.selectedId));
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
                <MenuItem onClick={this.handleSelectInNewTab.bind(this, node)}>{i18n('faTree.action.openInNewTab')}</MenuItem>
            </ul>
        )

        this.dispatch(faTreeFocusNode(types.FA_TREE_AREA_MAIN, node));
        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    /**
     * Otevření uzlu v nové záložce.
     * @param node {Object} uzel
     */
    handleSelectInNewTab(node) {
        this.dispatch(contextMenuHide());

        this.callFaSelectSubNode(node, true);
    }

    /**
     * Otevření uzlu v záložce.
     * @param node {Object} uzel
     * @param openNewTab {Boolean} true, pokud se má otevřít v nové záložce
     */
    callFaSelectSubNode(node, openNewTab) {
        var parentNode = getParentNode(node, this.props.activeFa.faTree.nodes);
        if (parentNode == null) {   // root
            parentNode = createFaRoot(this.props.activeFa);
        }
        this.dispatch(faSelectSubNode(node.id, parentNode, openNewTab, null, false));
    }

    /**
     * Otevření uzlu v aktuální záložce (pokud aktuální není, otevře se v nové).
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, e) {
        this.callFaSelectSubNode(node, false);
    }

    handleToggleExtendedView() {
        this.dispatch(faExtendedView(true));
    }

    render() {
        const {fas, activeFa, focus} = this.props;

        if (fas.length == 0) {
            return <div></div>
        }

        var tabs = fas.map((fa) => {
            return {
                id: fa.id,
                key: fa.id,
                title: fa.name,
                desc: fa.lockDate ? dateToString(new Date(fa.lockDate)) : ''
            }
        });

        return (
            <Tabs.Container className='fa-tabs-container'>
                <Button onClick={this.handleToggleExtendedView} className='extended-view-toggle'><Icon glyph='fa-expand'/></Button>

                <Tabs.Tabs closable items={tabs} activeItem={activeFa}
                    onSelect={item=>this.dispatch(selectFaTab(item))}
                    onClose={item=>this.dispatch(closeFaTab(item))}
                />
                <Tabs.Content>
                    <FaTreeMain
                        fa = {activeFa}
                        versionId={activeFa.versionId}
                        {...activeFa.faTree}
                        ref='tree'
                        focus={focus}
                    />
                    {false && <FaTreeLazy 
                        fa={activeFa}
                        {...activeFa.faTree}
                        versionId={this.props.activeFa.versionId}
                        onOpenCloseNode={(node, expand) => {expand ? this.dispatch(faTreeNodeExpand(types.FA_TREE_AREA_MAIN, node)) : this.dispatch(faTreeNodeCollapse(types.FA_TREE_AREA_MAIN, node))}}
                        onContextMenu={this.handleContextMenu}
                        onNodeClick={this.handleNodeClick}
                    /> }
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

FaTreeTabs.propTypes = {
    fas: React.PropTypes.array.isRequired,
    activeFa: React.PropTypes.object,
    focus: React.PropTypes.object.isRequired,
}

module.exports = connect()(FaTreeTabs);
