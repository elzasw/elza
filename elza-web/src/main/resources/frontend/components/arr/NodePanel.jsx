/**
 * Komponenta panelu formuláře jedné JP.
 */

import React from 'react';
import {connect} from 'react-redux'
import {Icon, AbstractReactComponent, i18n, Loading, SubNodeForm, Accordion} from 'components';
import {Button} from 'react-bootstrap';
import {faSubNodeFormFetchIfNeeded} from 'actions/arr/subNodeForm'
import {faSubNodeInfoFetchIfNeeded} from 'actions/arr/subNodeInfo'
import {faNodeInfoFetchIfNeeded} from 'actions/arr/nodeInfo'
import {faSelectSubNode, faSubNodesNext, faSubNodesPrev, faSubNodesNextPage, faSubNodesPrevPage} from 'actions/arr/nodes'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes'
import {indexById} from 'stores/app/utils.jsx'

require ('./NodePanel.less');

var NodePanel = class NodePanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderParents',
            'renderChildren', 'handleOpenItem',
            'handleCloseItem', 'handleParentNodeClick', 'handleChildNodeClick',
            'getParentNodes', 'getChildNodes', 'getSiblingNodes',
            'renderAccordion'
            );
        
    }

    componentDidMount() {
        this.requestData(this.props.versionId, this.props.node);
    }

    componentWillReceiveProps(nextProps) {
        this.requestData(nextProps.versionId, nextProps.node);
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
        return [this.props.node, ...this.props.node.parentNodes];
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
     * Renderování Accordion.
     * @param form {Object} editační formulář, pokud je k dispozici (k dispozici je, pokud je nějaká položka Accordion vybraná)
     * @return {Object} view
     */
    renderAccordion(form) {
        const {node} = this.props;

        var rows = [];

        if (node.viewStartIndex > 0) {
            rows.push(
                <Button onClick={()=>this.dispatch(faSubNodesPrev())}><Icon glyph="fa-chevron-left" />{i18n('arr.fa.prev')}</Button>
            )
        }

        for (var a=node.viewStartIndex; (a<node.viewStartIndex + node.pageSize) && (a < node.childNodes.length); a++) {
            var item = node.childNodes[a];

            if (node.selectedSubNodeId == item.id) {
                rows.push(
                    <div className='accordion-header opened' onClick={this.handleCloseItem.bind(this, item)}>
                        {item.name} [{item.id}]
                    </div>
                )
                rows.push(
                    <div className='accordion-body'>
                        {form}
                    </div>
                )
            } else {
                rows.push(
                    <div className='accordion-header closed' onClick={this.handleOpenItem.bind(this, item)}>
                        {item.name} [{item.id}]
                    </div>
                )
            }
        }

        if (node.viewStartIndex + node.pageSize/2 < node.childNodes.length) {
            rows.push(
                <Button onClick={()=>this.dispatch(faSubNodesNext())}><Icon glyph="fa-chevron-right" />{i18n('arr.fa.next')}</Button>
            )
        }

        return rows;
    }

    render() {
        const {calendarTypes, versionId, rulDataTypes, node, packetTypes, packets} = this.props;

        if (node.isFetching || !node.fetched) {
            return <Loading/>
        }

        var parents = this.renderParents(this.getParentNodes());
        var children;
        if (node.subNodeInfo.isFetching || !node.subNodeInfo.fetched) {
            children = <div className='children'><div className='content'><Loading/></div></div>
        } else {
            children = this.renderChildren(this.getChildNodes());
        }
        var siblings = this.getSiblingNodes().map(s => <span key={s.id}> {s.id}</span>);
        var actions = (
            <div className='actions'>
                <Button><Icon glyph="fa-plus-circle" />Přidat JP na konec</Button>
                <Button disabled={node.viewStartIndex == 0} onClick={()=>this.dispatch(faSubNodesPrevPage())}><Icon glyph="fa-backward" />{i18n('arr.fa.subNodes.prevPage')}</Button>
                <Button disabled={node.viewStartIndex + node.pageSize > node.childNodes.length} onClick={()=>this.dispatch(faSubNodesNextPage())}><Icon glyph="fa-forward" />{i18n('arr.fa.subNodes.nextPage')}</Button>

                <input type="text"/><Button>Hledat</Button>
            </div>
        )

        var form;
        if (!node.subNodeForm.isFetching && node.subNodeForm.fetched) {
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
                packets={packets}
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
}

NodePanel.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    node: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
}

module.exports = connect()(NodePanel);
