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
        var tabs = this.props.nodes.map((node, i) => {
            return {
                id: node.id,
                index: i,
                key: node.id + "_" + i,
                title: <span>Node {node.id}</span>,
                desc: <span>id:{node.id}</span>
            }
        });

        var activeNode = this.props.nodes[this.props.activeIndex];
        var activeTab = tabs[this.props.activeIndex];

        return (
            <Tabs.Container className='node-tabs-container'>
                <Tabs.Tabs items={tabs} activeItem={activeTab}
                    onSelect={item=>this.dispatch(faSelectNodeTab(item.index))}
                    onClose={item=>this.dispatch(faCloseNodeTab(item.index))}
                />
                <Tabs.Content>
                    {activeNode && <NodePanel node={activeNode} nodeForm={this.props.nodeForm} />}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = connect()(NodeTabs);