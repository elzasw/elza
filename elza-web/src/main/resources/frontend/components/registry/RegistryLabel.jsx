/**
 * Komponenta pro vytvoření stejných poliček pro editaci záznamu
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {Input, Button} from 'react-bootstrap';
import {Icon, NoFocusButton, AbstractReactComponent, DropDownTree} from 'components';
import {connect} from 'react-redux'

var RegistryLabel = class RegistryLabel extends AbstractReactComponent {
    constructor(props){
        super(props);
        this.handleVariantChange = this.handleVariantChange.bind(this);
        this.handleVariantKeyUp = this.handleVariantKeyUp.bind(this);
        this.focus = this.focus.bind(this);
        this.state = {
            variant: this.props.value,
        }

    }

    componentWillReceiveProps(nextProps){

    }

    handleVariantKeyUp(e){
        if (e.keyCode == 13 && this.props.onEnter){
            this.props.onEnter(e);
        }
    }


    handleVariantChange(e){
        this.setState({
            variant: e.target.value                                  // uložení zadaného řezezce ve stavu komponenty
        });
    }

    focus() {
        this.refs.input.getInputDOMNode().focus()
    }

    render() {
        var body = null;

        switch (this.props.type) {
            case 'selectWithChild':
                body = <DropDownTree
                    disabled={this.props.disabled}
                    items = {this.props.items}
                    selectedItemID = {this.props.value}
                    onSelect = {this.props.onSelect}
                    />
                break;
            case 'variant':

                body = <div className="desc-item-value-container">
                    <span>
                        <Input
                            disabled={this.props.disabled}
                            ref='input'
                            type='text'
                            value={this.state.variant}
                            onChange={this.handleVariantChange}
                            onKeyUp={this.handleVariantKeyUp}
                            onBlur={this.props.onBlur}
                            />
                        </span>
                    {this.props.onClickDelete && <NoFocusButton disabled={this.props.disabled} onClick = {this.props.onClickDelete}><Icon glyph='fa-times' /></NoFocusButton>}

                </div>
                break;
        }

        var actions = [];
        return (
            <div className="registry-label">
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

module.exports = connect(null, null, null, { withRef: true })(RegistryLabel);