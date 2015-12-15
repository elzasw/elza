/**
 * Komponenta záložek otevřených JP.
 */

require ('./NodeTabs.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, NodePanel, Tabs} from 'components';
import {AppActions} from 'stores';

import {faSelectNodeTab, faCloseNodeTab} from 'actions/arr/nodes'

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
                    onSelect={item=>this.dispatch(faSelectNodeTab(item))}
                    onClose={item=>this.dispatch(faCloseNodeTab(item))}
                />
                <Tabs.Content>
                    {this.props.activeNode && <NodePanel node={this.props.activeNode} nodeForm={this.props.nodeForm} />}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = connect()(NodeTabs);