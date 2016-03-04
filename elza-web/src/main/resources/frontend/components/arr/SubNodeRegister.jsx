require('./SubNodeRegister.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, Loading, NoFocusButton} from 'components';
import {connect} from 'react-redux'

import {faSubNodeRegisterValueDelete, faSubNodeRegisterValueAdd,
        faSubNodeRegisterValueFocus, faSubNodeRegisterValueBlur, faSubNodeRegisterValueChange} from 'actions/arr/subNodeRegister'
import {registrySelect, registryAdd} from 'actions/registry/registryRegionList'
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
     * @param index {Integer} index hodnoty seznamu
     */
    handleCreateRecord(index) {
        const {versionId} = this.props;
        this.dispatch(registryAdd(null, versionId, this.handleCreatedRecord.bind(this, index), '', true));
    }

    /**
     * Vytvoření hesla po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreatedRecord(index, data, submitType) {
        const {versionId, selectedSubNodeId, nodeKey, fa} = this.props;

        // TODO: sjednoceni od Pavla - ELZA-591
        this.dispatch(faSubNodeRegisterValueFocus(versionId, selectedSubNodeId, nodeKey, index));
        this.dispatch(faSubNodeRegisterValueChange(versionId, selectedSubNodeId, nodeKey, index, data.recordId));
        this.dispatch(faSubNodeRegisterValueBlur(versionId, selectedSubNodeId, nodeKey, index));

        // Akce po vytvoření
        if (submitType === 'storeAndViewDetail') {  // přesměrování na detail
            this.dispatch(registrySelect(data.recordId, fa));
            this.dispatch(routerNavigate('registry'));
        } else {    // nastavení focus zpět na prvek
        }
    }

    handleAddClick() {
        this.dispatch(faSubNodeRegisterValueAdd(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey));
    }

    handleChange(index, recordId) {
        this.dispatch(faSubNodeRegisterValueChange(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, index, recordId));
    }

    handleDetail(index, recordId) {
        const {fa} = this.props;
        this.dispatch(registrySelect(recordId, fa));
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
        const {closed, versionId} = this.props;
        return (
                <div className="link" key={"link-" + index}>
                    <NodeRegister onFocus={this.handleFocus.bind(this, index)}
                                  onBlur={this.handleBlur.bind(this, index)}
                                  onDetail={this.handleDetail.bind(this, index)}
                                  onChange={this.handleChange.bind(this, index)}
                                  closed={closed}
                                  onCreateRecord={this.handleCreateRecord.bind(this, index)}
                                  item={link}
                                  versionId={versionId} />
                    {!closed && <NoFocusButton key="delete" onClick={this.handleRemove.bind(this, index)} ><Icon glyph="fa-times" /></NoFocusButton>}
                </div>
        );
    }

    renderForm() {
        const {register, closed} = this.props;

        var links = [];
        register.formData.nodeRegisters.forEach((link, index) => links.push(this.renderLink(link, index)));

        return (
                <div className="register-form">
                    <div className='links'>{links}</div>
                    {!closed && <div className='action'><NoFocusButton onClick={this.handleAddClick}><Icon glyph="fa-plus" /></NoFocusButton></div>}
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

function mapStateToProps(state) {
    const {arrRegion} = state
    var fa = null;
    if (arrRegion.activeIndex != null) {
        fa = arrRegion.fas[arrRegion.activeIndex];
    }

    return {
        fa: fa
    }
}

SubNodeRegister.propTypes = {
    register: React.PropTypes.object.isRequired,
    selectedSubNodeId: React.PropTypes.number.isRequired,
    nodeKey: React.PropTypes.number.isRequired,
    closed: React.PropTypes.bool.isRequired,
    nodeId: React.PropTypes.oneOfType(React.PropTypes.number, React.PropTypes.string),
}

module.exports = connect(mapStateToProps)(SubNodeRegister);
