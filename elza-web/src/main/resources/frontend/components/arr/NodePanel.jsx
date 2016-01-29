/**
 * Komponenta panelu formuláře jedné JP.
 */

import React from 'react';
import {connect} from 'react-redux'
import {Icon, AbstractReactComponent, i18n, Loading, SubNodeForm, Accordion} from 'components';
import {Button, Tooltip, OverlayTrigger} from 'react-bootstrap';
import {faSubNodeFormFetchIfNeeded} from 'actions/arr/subNodeForm'
import {faSubNodeInfoFetchIfNeeded} from 'actions/arr/subNodeInfo'
import {faNodeInfoFetchIfNeeded} from 'actions/arr/nodeInfo'
import {faSelectSubNode, faSubNodesNext, faSubNodesPrev, faSubNodesNextPage, faSubNodesPrevPage} from 'actions/arr/nodes'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes'
import {indexById} from 'stores/app/utils.jsx'
import {createFaRoot, isFaRootId} from './ArrUtils.jsx'
import {propsEquals} from 'components/Utils'

require ('./NodePanel.less');

var NodePanel = class NodePanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderParents',
            'renderChildren', 'handleOpenItem',
            'handleCloseItem', 'handleParentNodeClick', 'handleChildNodeClick',
            'getParentNodes', 'getChildNodes', 'getSiblingNodes',
            'renderAccordion', 'renderState', 'transformConformityInfo'
            );

    }

    componentDidMount() {
        this.requestData(this.props.versionId, this.props.node);
    }

    componentWillReceiveProps(nextProps) {
        this.requestData(nextProps.versionId, nextProps.node);
    }

    shouldComponentUpdate(nextProps, nextState) {
        var eqProps = ['versionId', 'fa', 'node', 'calendarTypes', 'packetTypes', 'packets', 'rulDataTypes', 'findingAidId']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AP
     * @param node {Object} node
     */
    requestData(versionId, node) {
        if (node.selectedSubNodeId != null) {
            this.dispatch(faSubNodeFormFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            this.dispatch(faSubNodeInfoFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            this.dispatch(refRulDataTypesFetchIfNeeded());
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

        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode));
    }

    /**
     * Kliknutí na položku v seznamu podřízených NODE.
     * @param node {Object} node na který se kliklo
     */
    handleChildNodeClick(node) {
        var subNodeId = node.id;
        var subNodeParentNode = this.getSiblingNodes()[indexById(this.getSiblingNodes(), this.props.node.selectedSubNodeId)];
        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode));
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
        this.dispatch(faSelectSubNode(null, this.props.node));
    }

    /**
     * Vybrání a otevření položky Accordion.
     * @param item {Object} na který node v Accordion se kliklo
     */
    handleOpenItem(item) {
        var subNodeId = item.id;
        this.dispatch(faSelectSubNode(subNodeId, this.props.node));
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
                messages.push(<div className="error">Chyby</div>);
                errors.forEach(error => { messages.push(<div className="message">{error.description}</div>) });
            }

            if (missings && missings.length > 0) {
                messages.push(<div className="missing">Chybějící</div>);
                missings.forEach(missing => { messages.push(<div className="message">{missing.description}</div>) });
            }

            if (item.nodeConformity.state === "OK") {
                icon = <Icon glyph="fa-check" />
                tooltip = <Tooltip>{i18n('arr.node.status.ok') + description}</Tooltip>
            } else {
                icon = <Icon glyph="fa-exclamation-circle" />
                tooltip = <Tooltip>{i18n('arr.node.status.err')} {description} {messages}</Tooltip>
            }
        } else {
            icon = <Icon glyph="fa-exclamation-triangle" />
            tooltip = <Tooltip>{i18n('arr.node.status.undefined')}</Tooltip>
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
    renderAccordion(form) {
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
                    <div key={item.id} className='accordion-header opened' onClick={this.handleCloseItem.bind(this, item)}>
                        {item.name} [{item.id}] {state}
                    </div>
                )
                rows.push(
                    <div key="body" className='accordion-body'>
                        {form}
                    </div>
                )
            } else {
                rows.push(
                    <div key={item.id} className='accordion-header closed' onClick={this.handleOpenItem.bind(this, item)}>
                        {item.name} [{item.id}] {state}
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
        const {calendarTypes, versionId, rulDataTypes, node, packetTypes, packets, findingAidId} = this.props;

        if (!node.fetched) {
            return <Loading/>
        }

        var parents = this.renderParents(this.getParentNodes());
        var children;
        if ((node.subNodeInfo.isFetching && !node.subNodeInfo.dirty) || !node.subNodeInfo.fetched) {
            children = <div className='children'><div className='content'><Loading/></div></div>
        } else {
            children = this.renderChildren(this.getChildNodes());
        }
        var siblings = this.getSiblingNodes().map(s => <span key={s.id}> {s.id}</span>);
        var actions = (
            <div className='actions'>
                <div className='btn btn-default'><Icon glyph="fa-plus-circle" />Přidat JP na konec</div>
                <div className='btn btn-default' disabled={node.viewStartIndex == 0} onClick={()=>this.dispatch(faSubNodesPrevPage())}><Icon glyph="fa-backward" />{i18n('arr.fa.subNodes.prevPage')}</div>
                <div className='btn btn-default' disabled={node.viewStartIndex + node.pageSize > node.childNodes.length} onClick={()=>this.dispatch(faSubNodesNextPage())}><Icon glyph="fa-forward" />{i18n('arr.fa.subNodes.nextPage')}</div>

                <input className="form-control" type="text"/><Button>Hledat</Button>
            </div>
        )

        var form;
        if (node.subNodeForm.fetched) {
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
            form = <Loading/>
        }

        var accordionInfo = <div>
            {node.viewStartIndex}-{node.viewStartIndex + node.pageSize} [{node.childNodes.length}]
        </div>

        return (
            <div className='node-panel-container'>
                {false && accordionInfo}
                {actions}
                {parents}
                <div className='content'>
                    {this.renderAccordion(form)}
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

NodePanel.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fa: React.PropTypes.object.isRequired,
    node: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    findingAidId: React.PropTypes.number,
}

module.exports = connect()(NodePanel);
