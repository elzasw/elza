/**
 * Formulář editace hodnoty v tabulce pro desc item typu tabulka.
 */

require ('./DescItemTableCellForm.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, SubNodeForm} from 'components/index.jsx';
import {Modal, Button, Input} from 'react-bootstrap';

const keyDownHandlers = {
    Enter: function (e) {
        e.stopPropagation();
        e.preventDefault();

        const {onClose} = this.props
        onClose();

    },
}

var DescItemTableCellForm = class DescItemTableCellForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        
        this.bindMethods("handleChange", 'handleKeyDown')
        
        this.state = {
            value: props.value
        }
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            value: nextProps.value,
        })
    }

    componentDidMount() {
        const {position} = this.props

        $('.desc-item-table-cell-edit .modal-dialog').css({
            top: position.y + 'px',
            left: position.x + 'px',
        })

        $(".modal-backdrop").hide();
        // $(".modal-backdrop").css({opacity: 0})
    }

    handleKeyDown(event) {
        if (keyDownHandlers[event.key]) {
            keyDownHandlers[event.key].call(this, event)
        }
    }

    handleChange(e) {
        const value = e.target.value;
        
        this.setState({
            value: value,
        })
        
        const {onChange} = this.props
        onChange(value);
    }
    
    render() {
        const {className} = this.props
        const {value} = this.state

        return (
            <div className={"cell-edit-container " + (className ? className : "")} onKeyDown={this.handleKeyDown}>
                <Input
                    type="text"
                    value={value}
                    onChange={this.handleChange}
                       />
            </div>
        )
    }
}

function mapStateToProps(state) {
    // const {arrRegion, refTables} = state
    return {
    }
}

module.exports = connect(mapStateToProps)(DescItemTableCellForm)