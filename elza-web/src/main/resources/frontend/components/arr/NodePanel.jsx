/**
 * Komponenta panelu formuláře jedné JP.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Icon, AbstractReactComponent, i18n, Loading, SubNodeForm, Accordion, SubNodeRegister, AddNodeDropdown} from 'components';
import {Button, Tooltip, OverlayTrigger, Input} from 'react-bootstrap';
import {faSubNodeFormFetchIfNeeded} from 'actions/arr/subNodeForm'
import {faSubNodeRegisterFetchIfNeeded} from 'actions/arr/subNodeRegister'
import {faSubNodeInfoFetchIfNeeded} from 'actions/arr/subNodeInfo'
import {faNodeInfoFetchIfNeeded} from 'actions/arr/nodeInfo'
import {faSelectSubNode, faSubNodesNext, faSubNodesPrev, faSubNodesNextPage, faSubNodesPrevPage} from 'actions/arr/nodes'
import {faNodeSubNodeFulltextSearch} from 'actions/arr/node'
import {addNode} from 'actions/arr/node'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes'
import {indexById} from 'stores/app/utils.jsx'
import {createFaRoot, isFaRootId} from './ArrUtils.jsx'
import {propsEquals} from 'components/Utils'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
const scrollIntoView = require('dom-scroll-into-view')

require ('./NodePanel.less');

var NodePanel = class NodePanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderParents',
            'renderChildren', 'handleOpenItem',
            'handleCloseItem', 'handleParentNodeClick', 'handleChildNodeClick',
            'getParentNodes', 'getChildNodes', 'getSiblingNodes',
            'renderAccordion', 'renderState', 'transformConformityInfo', 'handleAddNodeAtEnd',
            'handleChangeFilterText'
            );

        this.state = {
            filterText: props.node.filterText
        }
    }

    componentDidMount() {
        this.requestData(this.props.versionId, this.props.node);
        this.dispatch(calendarTypesFetchIfNeeded());
        this.ensureItemVisible();
    }

    componentWillReceiveProps(nextProps) {
        this.requestData(nextProps.versionId, nextProps.node, nextProps.showRegisterJp);
        this.dispatch(calendarTypesFetchIfNeeded());

        var newState = {
            filterText: nextProps.node.filterText
        }

        var scroll = false;
        if (!this.props.node.fetched && nextProps.node.fetched) {
            scroll = true;
        } else if (nextProps.node.selectedSubNodeId !== null && this.props.node.selectedSubNodeId !== nextProps.node.selectedSubNodeId) {
            scroll = true;
        }
        if (scroll) {
            this.setState(newState, this.ensureItemVisible)
        } else {
            this.setState(newState);
        }
    }

    handleChangeFilterText(e) {
        this.setState({
            filterText: e.target.value
        })
    }

    ensureItemVisible() {
        if (this.props.node.selectedSubNodeId !== null) {
            var itemNode = ReactDOM.findDOMNode(this.refs['accheader-' + this.props.node.selectedSubNodeId])
            if (itemNode !== null) {
                var contentNode = ReactDOM.findDOMNode(this.refs.content)
                scrollIntoView(itemNode, contentNode, { onlyScrollIfNeeded: true, alignWithTop:true })
            }
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['versionId', 'fa', 'node', 'calendarTypes',
            'packetTypes', 'packets', 'rulDataTypes', 'findingAidId', 'showRegisterJp']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AP
     * @param node {Object} node
     * @param showRegisterJp {bool} zobrazení rejstřílů vázené k jednotce popisu
     */
    requestData(versionId, node, showRegisterJp) {
        if (node.selectedSubNodeId != null) {
            this.dispatch(faSubNodeFormFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            this.dispatch(faSubNodeInfoFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            this.dispatch(refRulDataTypesFetchIfNeeded());

            if (showRegisterJp) {
                this.dispatch(faSubNodeRegisterFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            }

        }
        this.dispatch(faNodeInfoFetchIfNeeded(versionId, node.id, node.nodeKey));
    }

    /**
     * Kliknutí na položku v seznamu nadřízených NODE.
     * @param node {Object} node na který se kliklo
     */
    handleParentNodeClick(node) {
        var parentNodes = this.getParentNodes();
        var index = indexById(parentNodes, node.id);
        var subNodeId = node.id;
        var subNodeParentNode = index + 1 < parentNodes.length ? parentNodes[index + 1] : null;
        if (subNodeParentNode == null) {
            subNodeParentNode = createFaRoot(this.props.fa, node);
        }

        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode, false, null, true));
    }

    /**
     * Kliknutí na položku v seznamu podřízených NODE.
     * @param node {Object} node na který se kliklo
     */
    handleChildNodeClick(node) {
        var subNodeId = node.id;
        var subNodeParentNode = this.getSiblingNodes()[indexById(this.getSiblingNodes(), this.props.node.selectedSubNodeId)];
        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode, false, null, true));
    }

    /**
     * Renderování seznamu nadřízených NODE.
     * @param parents {Array} seznam node pro vyrenderování
     * @return {Object} view
     */
    renderParents(parents) {
        var rows = parents.map(parent => {
            return (
                <div key={parent.id} className='node' onClick={this.handleParentNodeClick.bind(this, parent)}>{parent.name}</div>
            )
        }).reverse();
        return (
            <div className='parents'>
                {rows}
            </div>
        )
    }

    /**
     * Renderování seznamu podřízených NODE.
     * @param children {Array} seznam node pro vyrenderování
     * @return {Object} view
     */
    renderChildren(children) {
        var rows = children.map(child => {
            return (
                <div key={child.id} className='node' onClick={this.handleChildNodeClick.bind(this, child)}>{child.name}</div>
            )
        });

        return (
            <div className='children'>
                {rows}
            </div>
        )
    }

    /**
     * Načtení seznamu nadřízených NODE.
     * @return {Array} seznam NODE
     */
    getParentNodes() {
        const {node} = this.props;
        if (isFaRootId(node.id)) {
            return [...node.parentNodes];
        } else {
            return [node, ...node.parentNodes];
        }
    }

    /**
     * Načtení seznamu podřízených NODE.
     * @return {Array} seznam NODE
     */
    getChildNodes() {
        return [...this.props.node.subNodeInfo.childNodes];
    }

    /**
     * Načtení seznamu souroyeneckých NODE.
     * @return {Array} seznam NODE
     */
    getSiblingNodes() {
        return [...this.props.node.childNodes];
    }

    /**
     * Zavření položky Accordion.
     * @param item {Object} na který node v Accordion se kliklo
     */
    handleCloseItem(item) {
        this.dispatch(faSelectSubNode(null, this.props.node, false, null, false));
    }

    /**
     * Vybrání a otevření položky Accordion.
     * @param item {Object} na který node v Accordion se kliklo
     */
    handleOpenItem(item) {
        var subNodeId = item.id;
        this.dispatch(faSelectSubNode(subNodeId, this.props.node, false, null, true));
    }

    /**
     * Přidání JP na konec do aktuálního node
     * Využito v dropdown buttonu pro přidání node
     *
     * @param event Event selectu
     * @param scenario name vybraného scénáře
     */
    handleAddNodeAtEnd(event, scenario) {
        this.dispatch(addNode(this.props.node, this.props.node, this.props.fa.versionId, "CHILD", this.getDescItemTypeCopyIds(), scenario));
    }

    /**
     * Vrátí pole ke zkopírování
     */
    getDescItemTypeCopyIds() {
        var itemsToCopy = null;
        if (this.props.nodeSettings != "undefined") {
            var nodeIndex = indexById(this.props.nodeSettings.nodes, this.props.node.id);
            if (nodeIndex != null) {
                itemsToCopy = this.props.nodeSettings.nodes[nodeIndex].descItemTypeCopyIds;
            }
        }
        return itemsToCopy;
    }

    /**
     * Renderování stavu.
     * @param item {object} na který node v Accordion se kliklo
     * @return {Object} view
     */
    renderState(item) {
        var icon;
        var tooltip;

        if (item.nodeConformity) {
            var description = (item.nodeConformity.description) ? "<br />" + item.nodeConformity.description : "";
            var messages = new Array();

            var errors = item.nodeConformity.errorList;
            var missings = item.nodeConformity.missingList;

            if (errors && errors.length > 0) {
                messages.push(<div key="errors" className="error">Chyby</div>);
                errors.forEach(error => { messages.push(<div key={error.id} className="message">{error.description}</div>) });
            }

            if (missings && missings.length > 0) {
                messages.push(<div key="missings" className="missing">Chybějící</div>);
                missings.forEach(missing => { messages.push(<div key={missing.id}  className="message">{missing.description}</div>) });
            }

            if (item.nodeConformity.state === "OK") {
                icon = <Icon glyph="fa-check" />
                tooltip = <Tooltip id="status-ok">{i18n('arr.node.status.ok') + description}</Tooltip>
            } else {
                icon = <Icon glyph="fa-exclamation-circle" />
                tooltip = <Tooltip id="status-err">{i18n('arr.node.status.err')} {description} {messages}</Tooltip>
            }
        } else {
            icon = <Icon glyph="fa-exclamation-triangle" />
            tooltip = <Tooltip id="status-undefined">{i18n('arr.node.status.undefined')}</Tooltip>
        }

        return (
                <OverlayTrigger placement="left" overlay={tooltip}>
                    <div className="status">
                        {icon}
                    </div>
                </OverlayTrigger>
        );
    }

    /**
     * Renderování Accordion.
     * @param form {Object} editační formulář, pokud je k dispozici (k dispozici je, pokud je nějaká položka Accordion vybraná)
     * @return {Object} view
     */
    renderAccordion(form, recordInfo) {
        const {node} = this.props;

        var rows = [];

        if (node.viewStartIndex > 0) {
            rows.push(
                <Button key="prev" onClick={()=>this.dispatch(faSubNodesPrev())}><Icon glyph="fa-chevron-left" />{i18n('arr.fa.prev')}</Button>
            )
        }

        for (var a=node.viewStartIndex; (a<node.viewStartIndex + node.pageSize) && (a < node.childNodes.length); a++) {
            var item = node.childNodes[a];

            var state = this.renderState(item);

            if (node.selectedSubNodeId == item.id) {
                rows.push(
                    <div key={item.id} ref={'accheader-' + item.id} className='accordion-item opened'>
                        <div className='accordion-header' onClick={this.handleCloseItem.bind(this, item)}>
                            {item.name} [{item.id}] {state}
                        </div>
                        <div key="body" className='accordion-body'>
                            {form}
                            {recordInfo}
                        </div>
                    </div>
                )
                if (false) rows.push(
                    <div key="body" className='accordion-body'>
                        {form}
                        {recordInfo}
                    </div>
                )
            } else {
                rows.push(
                    <div key={item.id} ref={'accheader-' + item.id} className='accordion-item closed'>
                        <div className='accordion-header' onClick={this.handleOpenItem.bind(this, item)}>
                            {item.name} [{item.id}] {state}
                        </div>
                    </div>
                )
            }
        }

        if (node.viewStartIndex + node.pageSize/2 < node.childNodes.length) {
            rows.push(
                <Button key="next" onClick={()=>this.dispatch(faSubNodesNext())}><Icon glyph="fa-chevron-right" />{i18n('arr.fa.next')}</Button>
            )
        }

        return rows;
    }

    render() {
        const {calendarTypes, versionId, rulDataTypes, node, packetTypes, packets, findingAidId, showRegisterJp} = this.props;

        if (!node.fetched) {
            return <Loading value={i18n('global.data.loading.node')}/>
        }

        var parents = this.renderParents(this.getParentNodes());
        var children;
        if (node.subNodeInfo.fetched || node.selectedSubNodeId == null) {
            children = this.renderChildren(this.getChildNodes());
        } else {
            children = <div className='children'><Loading value={i18n('global.data.loading.node.children')} /></div>
        }
        var siblings = this.getSiblingNodes().map(s => <span key={s.id}> {s.id}</span>);
        var actions = (
            <div className='actions'>
                {
                    node.fetched && !isFaRootId(node.id) &&
                    <AddNodeDropdown key="end"
                                     title="Přidat JP na konec"
                                     glyph="fa-plus-circle"
                                     action={this.handleAddNodeAtEnd}
                                     node={this.props.node}
                                     version={this.props.fa.versionId}
                                     direction="CHILD"
                    />
                }
                <div className='btn btn-default' disabled={node.viewStartIndex == 0} onClick={()=>this.dispatch(faSubNodesPrevPage())}><Icon glyph="fa-backward" />{i18n('arr.fa.subNodes.prevPage')}</div>
                <div className='btn btn-default' disabled={node.viewStartIndex + node.pageSize >= node.childNodes.length} onClick={()=>this.dispatch(faSubNodesNextPage())}><Icon glyph="fa-forward" />{i18n('arr.fa.subNodes.nextPage')}</div>

                <Input type="text" onChange={this.handleChangeFilterText} value={this.state.filterText}/>
                <Button onClick={() => {this.dispatch(faNodeSubNodeFulltextSearch(this.state.filterText))}}>Hledat</Button>
            </div>
        )

        var form;
        if (node.subNodeForm.fetched && calendarTypes.fetched) {
            var conformityInfo = this.transformConformityInfo(node);
            form = <SubNodeForm
                nodeId={node.id}
                versionId={versionId}
                selectedSubNodeId={node.selectedSubNodeId}
                nodeKey={node.nodeKey}
                formData={node.subNodeForm.formData}
                descItemTypeInfos={node.subNodeForm.descItemTypeInfos}
                rulDataTypes={rulDataTypes}
                calendarTypes={calendarTypes}
                packetTypes={packetTypes}
                conformityInfo={conformityInfo}
                packets={packets}
                parentNode={node}
                findingAidId={findingAidId}
                selectedSubNode={node.subNodeForm.data.node}
            />
        } else {
            form = <Loading value={i18n('global.data.loading.form')}/>
        }

        var record;

        if (showRegisterJp) {
            record = <SubNodeRegister
                        nodeId={node.id}
                        versionId={versionId}
                        selectedSubNodeId={node.selectedSubNodeId}
                        nodeKey={node.nodeKey}
                        register={node.subNodeRegister} />
        }

        var accordionInfo = <div>
            {node.viewStartIndex}-{node.viewStartIndex + node.pageSize} [{node.childNodes.length}]
        </div>

        return (
            <div className='node-panel-container'>
                {false && accordionInfo}
                {actions}
                {parents}
                <div className='content' ref='content'>
                    {this.renderAccordion(form, record)}
                </div>
                {children}
            </div>
        );
    }

    /**
     * Převedení dat do lepších struktur.
     *
     * @param node {object} JP
     * @returns {{errors: {}, missings: {}}}
     */
    transformConformityInfo(node) {
        var nodeId = node.subNodeForm.nodeId;

        var nodeState;

        for (var i = 0; i < node.childNodes.length; i++) {
            if (node.childNodes[i].id == nodeId) {
                nodeState = node.childNodes[i].nodeConformity;
                break;
            }
        }

        var conformityInfo = {
            errors: {},
            missings: {}
        };

        if (nodeState) {
            var errors = nodeState.errorList;
            if (errors && errors.length > 0) {
                errors.forEach(error => {
                    if (conformityInfo.errors[error.descItemObjectId] == null) {
                        conformityInfo.errors[error.descItemObjectId] = new Array();
                    }
                    conformityInfo.errors[error.descItemObjectId].push(error);
                });
            }

            var missings = nodeState.missingList;
            if (missings && missings.length > 0) {
                missings.forEach(missing => {
                    if (conformityInfo.missings[missing.descItemTypeId] == null) {
                        conformityInfo.missings[missing.descItemTypeId] = new Array();
                    }
                    conformityInfo.missings[missing.descItemTypeId].push(missing);
                });
            }
        }
        return conformityInfo;
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state
    return {
        nodeSettings: arrRegion.nodeSettings
    }
}

NodePanel.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fa: React.PropTypes.object.isRequired,
    node: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    nodeSettings: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    findingAidId: React.PropTypes.number,
    showRegisterJp: React.PropTypes.bool.isRequired,
}

module.exports = connect(mapStateToProps)(NodePanel);
