/**
 * Komponenta panelu formuláře jedné JP.
 */

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading, SubNodeForm, Accordion} from 'components';
import {Glyphicon, Button} from 'react-bootstrap';
import {faSubNodeFormFetchIfNeeded} from 'actions/arr/subNodeForm'
import {faSubNodeInfoFetchIfNeeded} from 'actions/arr/subNodeInfo'
import {faNodeInfoFetchIfNeeded} from 'actions/arr/nodeInfo'
import {faSelectSubNode} from 'actions/arr/nodes'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes'
import {indexById} from 'stores/app/utils.jsx'

require ('./NodePanel.less');
var NodePanel = class NodePanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderParents', 'handleRenderAccordionItemHeader', 'handleRenderAccordionItemContent', 'renderChildren', 'handleOpenItem', 'handleCloseItem', 'handleParentNodeClick', 'handleChildNodeClick', 'getParentNodes', 'getChildNodes', 'getSiblingNodes');
        
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
        return [this.props.node, ...this.props.node.nodeInfo.parentNodes];
    }

    getChildNodes() {
        return [...this.props.node.subNodeInfo.childNodes];
    }

    getSiblingNodes() {
        return [...this.props.node.nodeInfo.childNodes];
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

    render() {
        //console.log("NODE_PANEL", this.props);
        if (this.props.node.nodeInfo.isFetching || !this.props.node.nodeInfo.fetched) {
            return <Loading/>
        }

        var parents = this.renderParents(this.getParentNodes());
        var children;
        if (this.props.node.subNodeInfo.isFetching || !this.props.node.subNodeInfo.fetched) {
            children = <div className='children'><div className='content'><Loading/></div></div>
        } else {
            children = this.renderChildren(this.getChildNodes());
        }
        var siblings = this.getSiblingNodes().map(s => <span key={s.id}> {s.id}</span>);
        var actions = (
            <div className='actions'>
                <Button><Glyphicon glyph="plus" />Přidat JP na konec</Button>
                <input type="text"/><Button>Hledat</Button>
            </div>
        )

        var form;
        if (!this.props.node.subNodeForm.isFetching && this.props.node.subNodeForm.fetched) {
            form = <SubNodeForm
                versionId={this.props.versionId}
                selectedSubNodeId={this.props.node.selectedSubNodeId}
                nodeKey={this.props.node.nodeKey}
                formData={this.props.node.subNodeForm.formData}
                descItemTypeInfos={this.props.node.subNodeForm.descItemTypeInfos}
                rulDataTypes={this.props.rulDataTypes}
                calendarTypes={this.props.calendarTypes}
            />
        }

        var content = (
            <div className='content'>
                {form}
                {false && <Accordion
                    closeItem={this.handleCloseItem}
                    openItem={this.handleOpenItem}
                    selectedId={this.props.node.selectedSubNodeId}
                    items={this.getSiblingNodes()}
                    renderItemHeader={this.handleRenderAccordionItemHeader}
                    renderItemContent={this.handleRenderAccordionItemContent}
                />}
            </div>
        )

        return (
            <div className='node-panel-container'>
                {actions}
                {parents}
                {content}
                {children}
            </div>
        );
    }
}

module.exports = connect()(NodePanel);