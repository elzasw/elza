/**
 * Komponenta záložek otevřených JP.
 */

import React from 'react';

import {Button, Glyphicon, Nav, NavItem} from 'react-bootstrap';
import {NodePanel, Tabs} from 'components';
import {FaAppStoreActions} from 'actions';

require ('./NodeTabs.less');

var NodeTabs = class NodeTabs extends React.Component {
    constructor(props) {
        super(props);
    }

    handleNodeSelect(item) {
        FaAppStoreActions.selectNode.asFunction(item.id);
    }

    handleNodeClose(item) {
        FaAppStoreActions.closeNode.asFunction(item.id);
    }

    render() {
        var tabs = this.props.nodes.map((node) => {
            return {
                id: node.id,
                title: <span>Node {node.id} <small>id:{node.id}</small></span>
            }
        });

        return (
            <Tabs.Container className='node-tabs-container'>
                <Tabs.Tabs items={tabs} activeItem={this.props.activeNode} onSelect={this.handleNodeSelect} onClose={this.handleNodeClose}/>
                <Tabs.Content>
                    {this.props.activeNode && <NodePanel node={this.props.activeNode} />}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = NodeTabs;