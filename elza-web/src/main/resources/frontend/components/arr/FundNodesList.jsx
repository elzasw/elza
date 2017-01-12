/**
 * Komponenta seznamu vybraných uzlů pro verzi archivního souboru.
 */

require('./FundNodesList.less')

import React from 'react';
import {connect} from 'react-redux'
import {NodeLabel, AbstractReactComponent, AddRemoveList, Icon, i18n} from 'components/index.jsx';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Button} from 'react-bootstrap';

export default class extends AbstractReactComponent {
    static propTypes = {
        nodes: React.PropTypes.array.isRequired,
        onDeleteNode: React.PropTypes.func,
        onAddNode: React.PropTypes.func,
        readOnly: React.PropTypes.bool
    };

    handleDeleteItem = (node) => {
        const {onDeleteNode, readOnly} = this.props;
        if (!readOnly) {
            onDeleteNode(node)
        }
    };

    handleRenderItem = (node) => {
        return <NodeLabel node={node} />
    };

    render() {
        const {nodes, onAddNode, readOnly} = this.props;

        return (
            <AddRemoveList
                className="fund-nodes-list-container"
                readOnly={readOnly}
                items={nodes}
                onAdd={onAddNode}
                onRemove={this.handleDeleteItem}
                addTitle="arr.fund.nodes.title.select"
                removeTitle="arr.fund.nodes.title.remove"
                renderItem={this.handleRenderItem}
            />
        )
    }
}
