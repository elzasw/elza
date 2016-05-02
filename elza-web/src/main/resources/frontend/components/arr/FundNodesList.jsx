/**
 * Komponenta seznamu vybraných uzlů pro verzi archivního souboru.  
 */

require ('./FundNodesList.less')

import React from 'react';
import {connect} from 'react-redux'
import {ListBox, AbstractReactComponent} from 'components/index.jsx';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Button} from 'react-bootstrap';

var FundNodesList = class FundNodesList extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelectItem', 'handleRenderItem');
    }    
    
    handleSelectItem(node, index) {
        
    }
    
    handleRenderItem(node, index) {
        return (
            <div>
                <div>1|223||23||2</div>
                <div>{item.name}</div>
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
            />
        )
    }
    
}

module.exports = connect()(FundNodesList);