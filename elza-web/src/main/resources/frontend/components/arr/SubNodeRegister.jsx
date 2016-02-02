require('./SubNodeRegister.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, Loading, NoFocusButton} from 'components';
import {connect} from 'react-redux'

import {faSubNodeRegisterValueDelete, faSubNodeRegisterValueAdd,
        faSubNodeRegisterValueFocus, faSubNodeRegisterValueBlur, faSubNodeRegisterValueChange} from 'actions/arr/subNodeRegister'

import NodeRegister from './registerForm/NodeRegister'
import {routerNavigate} from 'actions/router'

var SubNodeRegister = class SubNodeRegister extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderLink', 'renderForm', 'handleAddClick');

    }

    componentDidMount() {

    }

    /**
     * Vytvoření nového hesla.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleCreateRecord(index) {
        // TODO: slapa - čeká se na dodělání REJSTŘÍKŮ
        console.warn("TODO: slapa - čeká se na dodělání REJSTŘÍKŮ - handleCreateRecord");

        // TODO: slapa - předělat => volat metodu na vyvoření jednotně
        /*this.dispatch(modalDialogShow(this, i18n('registry.addRegistry'),
         <AddRegistryForm create onSubmit={this.handleCreateRecordSubmit.bind(this, valueLocation)}/>));*/
    }

    /**
     * Vytvoření hesla po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreateRecordSubmit(index, data) {
        const {versionId, selectedSubNodeId, nodeKey} = this.props;

        // TODO: slapa - čeká se na dodělání REJSTŘÍKŮ
        console.warn("TODO: slapa - čeká se na dodělání REJSTŘÍKŮ - handleCreateRecordSubmit");
    }

    handleAddClick() {
        this.dispatch(faSubNodeRegisterValueAdd(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey));
    }

    handleChange(index, recordId) {
        this.dispatch(faSubNodeRegisterValueChange(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, index, recordId));
    }

    handleDetail(index, recordId) {
        // TODO: slapa - čeká se na dodělání REJSTŘÍKŮ
        //this.dispatch(recordSelect(recordId));
        console.warn("TODO: slapa - čeká se na dodělání REJSTŘÍKŮ - handleDetail");
        this.dispatch(routerNavigate('registry'));
    }

    handleFocus(index) {
        this.dispatch(faSubNodeRegisterValueFocus(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, index));
    }

    handleBlur(index) {
        this.dispatch(faSubNodeRegisterValueBlur(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, index));
    }

    handleRemove(index) {
        this.dispatch(faSubNodeRegisterValueDelete(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, index));
    }

    renderLink(link, index) {

        return (
                <div className="link" key={"link-" + index}>
                    <NodeRegister onFocus={this.handleFocus.bind(this, index)}
                                  onBlur={this.handleBlur.bind(this, index)}
                                  onDetail={this.handleDetail.bind(this, index)}
                                  onChange={this.handleChange.bind(this, index)}
                                  onCreateRecord={this.handleCreateRecord.bind(this, index)}
                                  item={link} /><NoFocusButton key="delete" onClick={this.handleRemove.bind(this, index)} ><Icon glyph="fa-times" /></NoFocusButton>
                </div>
        );
    }

    renderForm() {
        const {register} = this.props;

        var links = [];
        register.formData.nodeRegisters.forEach((link, index) => links.push(this.renderLink(link, index)));

        return (
                <div className="register-form">
                    <div className='links'>{links}</div>
                    <div className='action'><NoFocusButton onClick={this.handleAddClick}><Icon glyph="fa-plus" /></NoFocusButton></div>
                </div>
        );

    }

    render() {

        const {register} = this.props;

        var form;

        if (register.fetched) {
            form = this.renderForm();
        } else {
            form = <Loading value={i18n('global.data.loading.register')} />
        }

        return (
            <div className='node-register'>
                <div className='node-register-title'>{i18n('subNodeRegister.title')}</div>
                {form}
            </div>
        )
    }
}

SubNodeRegister.propTypes = {
    register: React.PropTypes.object.isRequired,
    selectedSubNodeId: React.PropTypes.number.isRequired,
    nodeKey: React.PropTypes.number.isRequired,
    nodeId: React.PropTypes.oneOfType(React.PropTypes.number, React.PropTypes.string),
}

module.exports = connect()(SubNodeRegister);
