/**
 * Komponenta seznamu vybraných uzlů pro verzi archivního souboru.
 */

import React from 'react';
import NodeLabel from "./NodeLabel";
import {AbstractReactComponent, AddRemoveList, Icon, i18n} from 'components/shared';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';

import './FundNodesList.less';

class FuncNodesList extends AbstractReactComponent {
    static PropTypes = {
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

    handleRenderItem = (props) => {
        const {item} = props;
        return <NodeLabel node={item} />;
    };

    render() {
        const {nodes, onAddNode, readOnly, ...other} = this.props;

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
                {...other}
            />
        )
    }
}

export default FuncNodesList;
