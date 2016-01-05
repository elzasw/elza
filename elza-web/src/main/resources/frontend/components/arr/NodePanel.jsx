/**
 * Komponenta panelu formuláře jedné JP.
 */

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading, NodeForm} from 'components';
import {faNodeFormFetchIfNeeded} from 'actions/arr/nodeForm'
import {faNodeInfoFetchIfNeeded} from 'actions/arr/nodeInfo'
import {faSelectSubNode} from 'actions/arr/nodes'
import {indexById} from 'stores/app/utils.jsx'

require ('./NodePanel.less');
var NodePanel = class NodePanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderParents', 'renderChildren', 'handleParentNodeClick', 'handleChildNodeClick', 'getParentNodes', 'getChildNodes');
        
        if (props.node.selectedSubNodeId != null) {
            this.dispatch(faNodeFormFetchIfNeeded(props.faId, props.node.selectedSubNodeId, props.node.nodeKey));
        }
        this.dispatch(faNodeInfoFetchIfNeeded(props.faId, props.node.id, props.node.nodeKey));
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.node.selectedSubNodeId != null) {
            this.dispatch(faNodeFormFetchIfNeeded(nextProps.faId, nextProps.node.selectedSubNodeId, nextProps.node.nodeKey));
        }
        this.dispatch(faNodeInfoFetchIfNeeded(nextProps.faId, nextProps.node.id, nextProps.node.nodeKey));
    }

    handleParentNodeClick(node) {
        var parentNodes = this.getParentNodes();
        var index = indexById(parentNodes, node.id);
        var subNodeId = node.id;
        var subNodeParentNode = index + 1 < parentNodes.length ? parentNodes[index + 1] : null;

        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode));
    }

    handleChildNodeClick(node) {
        var subNodeId = node.id;
        var subNodeParentNode = this.props.node.nodeForm.node;
        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode));
    }

    renderParents(parents) {
        var rows = parents.map(parent => {
            return (
                <div key={parent.id} className='node' onClick={this.handleParentNodeClick.bind(this, parent)}>{parent.name}</div>
            )
        }).reverse();
        return (
            <div className='parents'>
                {rows}
            </div>
        )
    }

    renderChildren(children) {
        var rows = children.map(child => {
            return (
                <div key={child.id} className='node' onClick={this.handleChildNodeClick.bind(this, child)}>{child.name}</div>
            )
        });

        return (
            <div className='children'>
                {rows}
            </div>
        )
    }

    getParentNodes() {
        return [this.props.node, ...this.props.node.nodeInfo.parentNodes];
    }

    getChildNodes() {
        return [...this.props.node.nodeForm.childNodes];
    }

    render() {
        var isLoading = false;
        isLoading |= this.props.node.nodeInfo.isFetching || !this.props.node.nodeInfo.fetched;
        isLoading |= this.props.node.nodeForm.isFetching || !this.props.node.nodeForm.fetched;
//console.log("NODE_PANEL", this.props, this.props.node, 'isLoading', isLoading);

        if (isLoading) {
            return <Loading/>
        }

        var parents = this.renderParents(this.getParentNodes());
        var children = this.renderChildren(this.getChildNodes());
        var siblings = this.props.node.nodeInfo.childNodes.map(s => <span key={s.id}> {s.id}</span>);

        return (
            <div className='node-panel-container'>
                <div className='actions'>NODE [{this.props.node.id}] actions.......SUB NODE: {this.props.node.selectedSubNodeId}</div>
                {parents}
                <div className='content'>
                    content{siblings}
                    {false && <NodeForm levelExt={this.props.nodeForm.levelExt}/>}
                </div>
                {children}
            </div>
        );
    }
}

module.exports = connect()(NodePanel);