/**
 * Komponenta seznamu vybraných uzlů pro verzi archivního souboru.  
 */

require ('./FundNodesList.less')

import React from 'react';
import {connect} from 'react-redux'
import {ListBox, Icon, AbstractReactComponent} from 'components/index.jsx';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Button} from 'react-bootstrap';
import {createReferenceMarkString, getGlyph} from 'components/arr/ArrUtils.jsx'

// Na kolik znaků se má název položky oříznout
const NODE_NAME_MAX_CHARS = 60

var FundNodesList = class FundNodesList extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelectItem', 'handleRenderItem', 'handleDeleteItem');
    }    
    
    handleSelectItem(node, index) {
    }

    handleDeleteItem(node) {
        const {onDeleteNode, readOnly} = this.props
        if (!readOnly) {
            onDeleteNode(node)
        }
    }

    handleRenderItem(node, index) {
        const {readOnly} = this.props

        const refMark = <div className="reference-mark">{createReferenceMarkString(node)}</div>

        var name = node.name ? node.name : <i>{i18n('fundTree.node.name.undefined', node.id)}</i>;
        if (name.length > NODE_NAME_MAX_CHARS) {
            name = name.substring(0, NODE_NAME_MAX_CHARS - 3) + '...'
        }
        name = <div title={name} className="name">{name}</div>

        return (
            <div className="item">
                <div className="item-container">
                    {refMark}
                    {name}
                </div>
                <div className="actions-container">
                    {!readOnly && <Button onClick={this.handleDeleteItem.bind(this, node)}><Icon glyph="fa-trash"/></Button>}
                </div>
            </div>
        )
    }
    
    render() {
        const {nodes} = this.props

        return (
            <ListBox
                className="fund-nodes-list-listbox"
                items={nodes}
                renderItemContent={this.handleRenderItem}
                onSelect={this.handleSelectItem}
                onDelete={this.handleDeleteItem}
            />
        )
    }
}

FundNodesList.propTypes = {
    nodes: React.PropTypes.array.isRequired,
    onDeleteNode: React.PropTypes.func.isRequired,
}

module.exports = connect()(FundNodesList);