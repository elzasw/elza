/**
 * Komponenta panelu formuláře jedné JP.
 */

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components';
import {nodeFormFetchIfNeeded} from 'actions/arr/nodeForm'
import {faSelectSubNode} from 'actions/arr/nodes'
import {indexById} from 'stores/app/utils.jsx'

require ('./NodePanel.less');

var NodePanel = class NodePanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderParents', 'renderChildren', 'handleParentNodeClick', 'handleChildNodeClick');

        this.dispatch(nodeFormFetchIfNeeded(props.node.selectedSubNodeId, props.versionId));
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(nodeFormFetchIfNeeded(nextProps.node.selectedSubNodeId, nextProps.versionId));
    }

    handleParentNodeClick(node) {
        var index = indexById(this.props.nodeForm.parentNodes, node.id);
        var subNodeId = node.id;
        var subNodeParentNode = index + 1 < this.props.nodeForm.parentNodes.length ? this.props.nodeForm.parentNodes[index + 1] : null;

        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode));
    }

    handleChildNodeClick(node) {
        var subNodeId = node.id;
        var subNodeParentNode = this.props.nodeForm.node;
        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode));
    }

    renderParents(parents) {
        var rows = parents.map(parent => {
            return (
                <div className='node' onClick={this.handleParentNodeClick.bind(this, parent)}>{parent.name}</div>
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
                <div className='node' onClick={this.handleChildNodeClick.bind(this, child)}>{child.name}</div>
            )
        });

        return (
            <div className='children'>
                {rows}
            </div>
        )
    }

    render() {
        var isLoading = this.props.nodeForm.isFetching || !this.props.nodeForm.fetched;

        if (isLoading) {
            return <Loading/>
        }

        var parents = this.renderParents(this.props.nodeForm.parentNodes);
        var children = this.renderChildren(this.props.nodeForm.childNodes);
        var siblings = this.props.nodeForm.siblingNodes.map(s => <span>{s.id}</span>);

        return (
            <div className='node-panel-container'>
                <div className='actions'>NODE [{this.props.node.id}] actions.......SUB NODE: {this.props.node.selectedSubNodeId}</div>
                {parents}
                <div className='content'>content{siblings}</div>
                {children}
            </div>
        );
    }
}

module.exports = connect()(NodePanel);