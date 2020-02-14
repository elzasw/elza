/**
 * Formulář editace hodnoty v tabulce pro desc item typu tabulka.
 */

import './DescItemJsonTableCellForm.scss'
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, SubNodeForm, FormInput, Utils} from 'components/shared';
import {validateInt, normalizeInt} from 'components/validate.jsx';
import {Shortcuts} from 'react-shortcuts';
import {PropTypes} from 'prop-types';
import defaultKeymap from './DescItemJsonTableCellFormKeymap.jsx';

import $ from 'jquery';

var DescItemJsonTableCellForm = class DescItemJsonTableCellForm extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }
    constructor(props) {
        super(props);

        this.bindMethods("handleChange")

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
    handleFormClose = ()=>{
        const {onClose} = this.props;
        onClose();
    }
    actionMap = {
        "FORM_CLOSE": this.handleFormClose
    }
    handleShortcuts(action,e){
        e.stopPropagation();
        e.preventDefault();
        this.actionMap[action](e);
    }

    handleChange(e) {
        const value = e.target.value;

        const {dataType} = this.props;
        var normalizedValue = value;
        switch (dataType) {
            case "INTEGER":
                normalizedValue = normalizeInt(normalizedValue);
                break;
            case "TEXT":
                break;
        }

        this.setState({
            value: normalizedValue,
        })

        const {onChange} = this.props
        onChange(normalizedValue);
    }

    render() {
        const {className} = this.props
        const {value} = this.state

        return (
            <Shortcuts name="DescItemJsonTableCellForm" handler={(action,e)=>this.handleShortcuts(action,e)} className={"cell-edit-container " + (className ? className : "")} stopPropagation={false}>
                <FormInput
                    type="text"
                    value={value}
                    onChange={this.handleChange}
                       />
            </Shortcuts>
        )
    }
}

function mapStateToProps(state) {
    // const {arrRegion, refTables} = state
    return {
    }
}

export default connect(mapStateToProps)(DescItemJsonTableCellForm)
