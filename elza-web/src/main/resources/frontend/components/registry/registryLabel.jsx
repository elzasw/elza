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
        this.handleVariantChange = this.handleVariantChange.bind(this);
        this.handleVariantKeyUp = this.handleVariantKeyUp.bind(this);
        this.state = {
            variant: this.props.value,
        }

    }
    componentWillReceiveProps(nextProps) {
    }
    handleVariantKeyUp(e){
        if (e.keyCode == 13){
            this.handleSearch(e);
        }
    }


    handleVariantChange(e){
        this.setState({
            variant: e.target.value                                  // uložení zadaného řezezce ve stavu komponenty
        });

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
            case 'variant':

                body = <div className="desc-item-value-container">
                    <span>
                        <Input
                            type='text'
                            value={this.state.variant}
                            onChange={this.handleVariantChange}
                            onKeyUp={this.handleVariantKeyUp}
                            onBlur={this.props.onBlur}
                            />
                        </span>
                    <span className = 'btn glyphicon glyphicon-remove-sign' onClick = {this.props.onClickDelete} />
                </div>
                break;
        }

        var actions = [];
        return (
            <div>
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