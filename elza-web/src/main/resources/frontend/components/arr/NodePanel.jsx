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

        this.bindMethods('renderParents', 'handleRenderAccordionItemHeader', 'handleRenderAccordionItemContent',
            'renderChildren', 'handleOpenItem', 'handleCloseItem', 'handleParentNodeClick', 'handleChildNodeClick',
            'getParentNodes', 'getChildNodes', 'getSiblingNodes',
            'renderAccordion'
            );
        
        if (props.node.selectedSubNodeId != null) {
            this.dispatch(faSubNodeFormFetchIfNeeded(props.versionId, props.node.selectedSubNodeId, props.node.nodeKey));
            this.dispatch(faSubNodeInfoFetchIfNeeded(props.versionId, props.node.selectedSubNodeId, props.node.nodeKey));
            this.dispatch(refRulDataTypesFetchIfNeeded());
        }
        this.dispatch(faNodeInfoFetchIfNeeded(props.versionId, props.node.id, props.node.nodeKey));
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.node.selectedSubNodeId != null) {
            this.dispatch(faSubNodeFormFetchIfNeeded(nextProps.versionId, nextProps.node.selectedSubNodeId, nextProps.node.nodeKey));
            this.dispatch(faSubNodeInfoFetchIfNeeded(nextProps.versionId, nextProps.node.selectedSubNodeId, nextProps.node.nodeKey));
            this.dispatch(refRulDataTypesFetchIfNeeded());
        }
        this.dispatch(faNodeInfoFetchIfNeeded(nextProps.versionId, nextProps.node.id, nextProps.node.nodeKey));
    }

    handleParentNodeClick(node) {
        var parentNodes = this.getParentNodes();
        var index = indexById(parentNodes, node.id);
        var subNodeId = node.id;
        var subNodeParentNode = index + 1 < parentNodes.length ? parentNodes[index + 1] : null;

        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode));
    }

    handleChildNodeClick(node) {
        var subNodeId = node.id;
        var subNodeParentNode = this.getSiblingNodes()[indexById(this.getSiblingNodes(), this.props.node.selectedSubNodeId)];
        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode));
    }

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

    getParentNodes() {
        return [this.props.node, ...this.props.node.parentNodes];
    }

    getChildNodes() {
        return [...this.props.node.subNodeInfo.childNodes];
    }

    getSiblingNodes() {
        return [...this.props.node.childNodes];
    }

    handleCloseItem(item) {
        this.dispatch(faSelectSubNode(null, this.props.node));
    }

    handleOpenItem(item) {
        var subNodeId = item.id;
        this.dispatch(faSelectSubNode(subNodeId, this.props.node));
    }

    handleRenderAccordionItemHeader(item, opened) {
        return (
            <div>{item.name}</div>
        );
    }

    handleRenderAccordionItemContent(item) {
        if (this.props.node.subNodeForm.isFetching || !this.props.node.subNodeForm.fetched) {
            return <Loading/>
        }
        if (this.props.rulDataTypes.isFetching || !this.props.rulDataTypes.fetched) {
            return <Loading/>
        }

        return (
            <SubNodeForm formData={this.props.node.subNodeForm.data} rulDataTypes={this.props.rulDataTypes} calendarTypes={this.props.calendarTypes}/>
        );
    }

    renderAccordion(form) {
        var {node} = this.props;

        var rows = [];

        if (node.viewStartIndex > 0) {
            rows.push(
                <Button onClick={()=>this.dispatch(faSubNodesPrev())}><Icon glyph="chevron-left" />{i18n('arr.fa.prev')}</Button>
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
                <Button onClick={()=>this.dispatch(faSubNodesNext())}><Icon glyph="chevron-right" />{i18n('arr.fa.next')}</Button>
            )
        }

        return rows;
    }

    render() {
        var {node} = this.props;

        //console.log("NODE_PANEL", this.props);
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
                <Button><Icon glyph="plus" />Přidat JP na konec</Button>
                <Button disabled={node.viewStartIndex == 0} onClick={()=>this.dispatch(faSubNodesPrevPage())}><Icon glyph="backward" />{i18n('arr.fa.subNodes.prevPage')}</Button>
                <Button disabled={node.viewStartIndex + node.pageSize > node.childNodes.length} onClick={()=>this.dispatch(faSubNodesNextPage())}><Icon glyph="forward" />{i18n('arr.fa.subNodes.nextPage')}</Button>

                <input type="text"/><Button>Hledat</Button>
            </div>
        )

        var form;
        if (!node.subNodeForm.isFetching && node.subNodeForm.fetched) {
            form = <SubNodeForm
                nodeId={this.props.node.id}
                versionId={this.props.versionId}
                selectedSubNodeId={node.selectedSubNodeId}
                nodeKey={node.nodeKey}
                formData={node.subNodeForm.formData}
                descItemTypeInfos={node.subNodeForm.descItemTypeInfos}
                rulDataTypes={this.props.rulDataTypes}
                calendarTypes={this.props.calendarTypes}
            />
        } else {
            form = <Loading/>
        }

        var content = (
            <div className='content'>
                {this.renderAccordion(form)}
                {false && form}
                {false && <Accordion
                    closeItem={this.handleCloseItem}
                    openItem={this.handleOpenItem}
                    selectedId={node.selectedSubNodeId}
                    items={this.getSiblingNodes()}
                    renderItemHeader={this.handleRenderAccordionItemHeader}
                    renderItemContent={this.handleRenderAccordionItemContent}
                />}
            </div>
        )

        var accordionInfo = <div>
            {node.viewStartIndex}-{node.viewStartIndex + node.pageSize} [{node.childNodes.length}]
        </div>

        return (
            <div className='node-panel-container'>
                {false && accordionInfo}
                {actions}
                {parents}
                {content}
                {children}
            </div>
        );
    }
}

module.exports = connect()(NodePanel);
