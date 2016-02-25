/**
 * Komponenta záložek otevřených JP.
 */

require ('./NodeTabs.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, NodePanel, Tabs, i18n} from 'components';
import {AppActions} from 'stores';
import {faSelectNodeTab, faCloseNodeTab} from 'actions/arr/nodes'
import {nodesFetchIfNeeded} from 'actions/arr/node'
import {propsEquals} from 'components/Utils'
import {createReferenceMarkString, getGlyph} from 'components/arr/ArrUtils'
import {setFocus, canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'

var NodeTabs = class NodeTabs extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleTabSelect', 'trySetFocus')
    }

    componentDidMount() {
        this.dispatch(nodesFetchIfNeeded(this.props.versionId));
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(nodesFetchIfNeeded(nextProps.versionId));
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'arr', 2, 'tabs')) {
                this.setState({}, () => {
                   ReactDOM.findDOMNode(this.refs.tabs).focus()
                   focusWasSet()
                })
            }
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
return true
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['versionId', 'fa', 'nodes', 'activeIndex', 'findingAidId', 'descItemTypes',
            'rulDataTypes', 'calendarTypes', 'packetTypes', 'packets', 'showRegisterJp', 'closed']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    handleTabSelect(item) {
        this.dispatch(faSelectNodeTab(item.index))
        this.dispatch(setFocus('arr', 2, 'tabs'))
    }

    render() {
        const {fa, nodes, activeIndex, versionId, rulDataTypes, showRegisterJp,
                calendarTypes, descItemTypes, packetTypes, packets, findingAidId, closed} = this.props;

        if (nodes.length == 0) {
            return <div></div>
        }

        var tabs = nodes.map((node, i) => {
            var name = node.name ? node.name : i18n('faTree.node.name.undefined', node.id);
            return {
                id: node.id,
                index: i,
                key: node.id + "_" + i,
                title: <span title={name} className="node-tab-title">{name}</span>,
                desc: <span className="node-tab-desc">{createReferenceMarkString(node)}</span>
            }
        });

        var activeNode = nodes[activeIndex];
        var activeTab = tabs[activeIndex];

        return (
            <Tabs.Container ref='tabs' className='node-tabs-container'>
                <Tabs.Tabs closable items={tabs} activeItem={activeTab}
                    onSelect={this.handleTabSelect}
                    onClose={item=>this.dispatch(faCloseNodeTab(item.index))}
                />
                <Tabs.Content>
                    {activeNode && <NodePanel versionId={versionId}
                                              fa={fa}
                                              closed={closed}
                                              findingAidId={findingAidId}
                                              node={activeNode}
                                              rulDataTypes={rulDataTypes}
                                              calendarTypes={calendarTypes}
                                              descItemTypes={descItemTypes}
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
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    showRegisterJp: React.PropTypes.bool.isRequired,
    closed: React.PropTypes.bool.isRequired,
}

function mapStateToProps(state) {
    const {focus} = state
    return {
        focus,
    }
}

module.exports = connect(mapStateToProps)(NodeTabs);