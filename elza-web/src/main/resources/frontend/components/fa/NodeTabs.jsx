/**
 * Komponenta záložek otevřených JP.
 */

import React from 'react';

import {Button, Glyphicon, Nav, NavItem} from 'react-bootstrap';
import {NodePanel, Tabs} from 'components';
import {MainNodesStore, FaAppStore} from 'stores';
import {FaAppStoreActions} from 'actions';

require ('./NodeTabs.less');

var NodeTabs = class NodeTabs extends React.Component {
    constructor(props) {
        super(props);

        this.state = this.getStateFromStore(FaAppStore);

        FaAppStore.listen(status => {
            this.setState(this.getStateFromStore(status));
        });
    }

    getStateFromStore(store) {
        var activeFa = store.getActiveFa();
        if (activeFa != null) {
            return {
                activeNode: activeFa.getActiveNode(),
                nodes: activeFa.getAllNodes()
            };
        } else {
            return {
                activeNode: null,
                nodes: []
            };
        }
    }

    handleNodeSelect(item) {
        FaAppStoreActions.selectNode.asFunction(item.id);
    }

    handleNodeClose(item, newActiveItem) {
        FaAppStoreActions.closeNode.asFunction(item.id, newActiveItem ? newActiveItem.id : null);
    }

    render() {
        var tabs = this.state.nodes.map((node) => {
            return {
                id: node.id,
                title: <span>Node {node.id} <small>id:{node.id}</small></span>
            }
        });

        return (
            <Tabs.Container className='node-tabs-container'>
                <Tabs.Tabs items={tabs} activeItem={this.state.activeNode} onSelect={this.handleNodeSelect} onClose={this.handleNodeClose}/>
                <Tabs.Content>
                    {this.state.activeNode && <NodePanel node={this.state.activeNode} />}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = NodeTabs;