/**
 * Komponenta záložek otevřených JP.
 */

require ('./NodeTabs.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, NodePanel, Tabs} from 'components';
import {AppActions} from 'stores';
import {faSelectNodeTab, faCloseNodeTab} from 'actions/arr/nodes'
import {propsEquals} from 'components/Utils'

var NodeTabs = class NodeTabs extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nextProps, nextState) {
        var eqProps = ['versionId', 'fa', 'nodes', 'activeIndex', 'findingAidId',
            'rulDataTypes', 'calendarTypes', 'packetTypes', 'packets', 'showRegisterJp']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    render() {
        const {fa, nodes, activeIndex, versionId, rulDataTypes, showRegisterJp,
                calendarTypes, packetTypes, packets, findingAidId} = this.props;

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
                <Tabs.Tabs closable items={tabs} activeItem={activeTab}
                    onSelect={item=>this.dispatch(faSelectNodeTab(item.index))}
                    onClose={item=>this.dispatch(faCloseNodeTab(item.index))}
                />
                <Tabs.Content>
                    {activeNode && <NodePanel versionId={versionId}
                                              fa={fa}
                                              findingAidId={findingAidId}
                                              node={activeNode}
                                              rulDataTypes={rulDataTypes}
                                              calendarTypes={calendarTypes}
                                              packetTypes={packetTypes}
                                              showRegisterJp={showRegisterJp}
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
    findingAidId: React.PropTypes.number,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    showRegisterJp: React.PropTypes.bool.isRequired,
}

module.exports = connect()(NodeTabs);