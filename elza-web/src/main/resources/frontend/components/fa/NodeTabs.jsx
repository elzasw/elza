/**
 * Komponenta záložek otevřených JP.
 */

require ('./NodeTabs.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, NodePanel, Tabs} from 'components';
import {AppActions} from 'stores';

import {selectNode, closeNode} from 'actions/fa/nodes'

var NodeTabs = class NodeTabs extends AbstractReactComponent {
    constructor(props) {
        super(props);
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
                <Tabs.Tabs items={tabs} activeItem={this.props.activeNode}
                    onSelect={item=>this.dispatch(selectNode(item))}
                    onClose={item=>this.dispatch(closeNode(item))}
                />
                <Tabs.Content>
                    {this.props.activeNode && <NodePanel node={this.props.activeNode} />}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = connect()(NodeTabs);