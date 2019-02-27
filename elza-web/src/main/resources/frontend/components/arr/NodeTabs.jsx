/**
 * Komponenta záložek otevřených JP.
 */

import './NodeTabs.less';

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Tabs} from 'components/shared';
import {fundCloseNodeTab, fundSelectNodeTab} from 'actions/arr/nodes.jsx'
import {nodesFetchIfNeeded} from 'actions/arr/node.jsx'
import {propsEquals} from 'components/Utils.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {createReferenceMarkString, getGlyph} from 'components/arr/ArrUtils.jsx'
import {canSetFocus, focusWasSet, isFocusFor, setFocus} from 'actions/global/focus.jsx'
import NodePanel from "./NodePanel";
import {FOCUS_KEYS} from "../../constants.tsx";

class NodeTabs extends AbstractReactComponent {
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
            if (isFocusFor(focus, FOCUS_KEYS.ARR, 2, 'tabs')) {
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
        var eqProps = ['versionId', 'fund', 'nodes', 'activeIndex', 'fundId', 'descItemTypes',
            'rulDataTypes', 'calendarTypes', 'showRegisterJp', 'showDaosJp', 'closed']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    handleTabSelect(item) {
        const {versionId} = this.props

        this.dispatch(fundSelectNodeTab(versionId, item.id, item.key, item.index))
        // this.dispatch(fundSelectNodeTab(item.index))
        this.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'tabs'))
    }

    render() {
        const {fund, nodes, activeIndex, versionId, rulDataTypes, showRegisterJp, showDaosJp,
                calendarTypes, descItemTypes, fundId, closed, displayAccordion} = this.props;

        if (nodes.length == 0) {
            return <div></div>
        }

        var tabs = nodes.map((node, i) => {
            var name = node.name ? node.name : i18n('fundTree.node.name.undefined', node.id);
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

        const noTabs = !displayAccordion && tabs.length <= 1;

        return (
            <Tabs.Container ref='tabs' className={`node-tabs-container ${noTabs ? 'node-no--tabs' : ''}`}>
                {!noTabs &&
                    <Tabs.Tabs
                        closable
                        items={tabs} activeItem={activeTab}
                        onSelect={this.handleTabSelect}
                        onClose={item=>this.dispatch(fundCloseNodeTab(versionId, item.id, item.key, item.index))}
                    />
                }
                <Tabs.Content>
                    {activeNode && 
                        <NodePanel 
                            versionId={versionId}
                            fund={fund}
                            closed={closed}
                            fundId={fundId}
                            node={activeNode}
                            rulDataTypes={rulDataTypes}
                            calendarTypes={calendarTypes}
                            descItemTypes={descItemTypes}
                            showRegisterJp={showRegisterJp}
                            showDaosJp={showDaosJp}
                            displayAccordion={displayAccordion}
                        />
                    }
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

NodeTabs.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fund: React.PropTypes.object.isRequired,
    nodes: React.PropTypes.array.isRequired,
    activeIndex: React.PropTypes.number,
    fundId: React.PropTypes.number,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    showRegisterJp: React.PropTypes.bool.isRequired,
    showDaosJp: React.PropTypes.bool.isRequired,
    displayAccordion: React.PropTypes.bool.isRequired,
    closed: React.PropTypes.bool.isRequired
}

function mapStateToProps(state) {
    const {focus} = state
    return {
        focus,
    }
}

export default connect(mapStateToProps)(NodeTabs);
