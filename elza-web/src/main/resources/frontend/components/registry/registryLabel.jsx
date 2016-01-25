/**
 * Komponenta pro vytvoření stejných poliček pro editaci záznamu
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {Input} from 'react-bootstrap';
import {AbstractReactComponent, DropDownTree} from 'components';
import {connect} from 'react-redux'

var RegistryLabel = class RegistryLabel extends AbstractReactComponent {
    constructor(props){
        super(props);


    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        var body = null;
        switch (this.props.type) {
            case 'selectWithChild':
                body = <DropDownTree
                    items = {this.props.items}
                    selectedItemID = {this.props.value}
                    onSelect = {this.props.onSelect}
                    />
                break;
            case 'textarea':
                body = <Input type="textarea" value={this.props.value}/>
                break;
        }

        var actions = [];
        return (
            <div className='desc-item-type-label'>
                <div className='title' title={this.props.label}>
                    {this.props.label}
                </div>
                <div>
                    {body}
                </div>
                <div className='actions'>
                    {actions}
                </div>
            </div>
        )
    }


}

module.exports = connect()(RegistryLabel);