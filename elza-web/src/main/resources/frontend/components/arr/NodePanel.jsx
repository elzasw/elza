/**
 * Komponenta panelu formuláře jedné JP.
 */

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components';
import {nodeFormFetchIfNeeded} from 'actions/arr/nodeForm'

require ('./NodePanel.less');

var NodePanel = class NodePanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderParents', 'renderChildren');

        this.dispatch(nodeFormFetchIfNeeded(props.node.selectedSubNodeId, props.versionId));
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(nodeFormFetchIfNeeded(nextProps.node.selectedSubNodeId, nextProps.versionId));
    }

    renderParents(parents) {
        var rows = parents.map(parent => {
            return (
                <div>{parent.name}</div>
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
                <div>{child.name}</div>
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

        return (
            <div className='node-panel-container'>
                <div className='actions'>NODE [{this.props.node.id}] actions.......SUB NODE: {this.props.node.selectedSubNodeId}</div>
                {parents}
                <div className='parents'>parents<br/>parents<br/>parents<br/>parents<br/></div>
                <div className='content'>content</div>
                {children}
            </div>
        );
    }
}

module.exports = connect()(NodePanel);