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
        const {fa, nodes, activeIndex, versionId, rulDataTypes, calendarTypes, packetTypes, packets} = this.props;

        if (nodes.length == 0) {
            return <div></div>
        }

        var tabs = nodes.map((node, i) => {
            return {
                id: node.id,
                index: i,
                key: node.id + "_" + i,
                title: <span>{node.name}</span>,
                desc: <span>id:{node.id}</span>
            }
        });

        var activeNode = nodes[activeIndex];
        var activeTab = tabs[activeIndex];

        return (
            <Tabs.Container className='node-tabs-container'>
                <Tabs.Tabs items={tabs} activeItem={activeTab}
                    onSelect={item=>this.dispatch(faSelectNodeTab(item.index))}
                    onClose={item=>this.dispatch(faCloseNodeTab(item.index))}
                />
                <Tabs.Content>
                    {activeNode && <NodePanel versionId={versionId}
                                              fa={fa}
                                              node={activeNode}
                                              rulDataTypes={rulDataTypes}
                                              calendarTypes={calendarTypes}
                                              packetTypes={packetTypes}
                                              packets={packets} />}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

NodeTabs.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fa: React.PropTypes.object.isRequired,
    nodes: React.PropTypes.array.isRequired,
    activeIndex: React.PropTypes.number,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.object.isRequired,
}

module.exports = connect()(NodeTabs);