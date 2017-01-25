require('./SubNodeRegister.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, Loading, NoFocusButton} from 'components/index.jsx';
import {connect} from 'react-redux'

import {
    fundSubNodeRegisterValueDelete,
    fundSubNodeRegisterValueAdd,
    fundSubNodeRegisterValueFocus,
    fundSubNodeRegisterValueBlur,
    fundSubNodeRegisterValueChange
} from 'actions/arr/subNodeRegister.jsx'
import {registryDetailFetchIfNeeded, registryAdd} from 'actions/registry/registry.jsx'
import NodeRegister from './registerForm/NodeRegister.jsx'
import {routerNavigate} from 'actions/router.jsx'
import DescItemLabel from './nodeForm/DescItemLabel.jsx'

const SubNodeRegister = class SubNodeRegister extends AbstractReactComponent {
    static PropTypes = {
        register: React.PropTypes.object.isRequired,
        selectedSubNodeId: React.PropTypes.number.isRequired,
        routingKey: React.PropTypes.number.isRequired,
        closed: React.PropTypes.bool.isRequired,
        readMode: React.PropTypes.bool.isRequired,
        nodeId: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string]),
    };

    /**
     * Vytvoření nového hesla.
     *
     * @param index {number} index hodnoty seznamu
     */
    handleCreateRecord(index) {
        const {versionId} = this.props;
        this.dispatch(registryAdd(null, versionId, this.handleCreatedRecord.bind(this, index), '', true));
    }

    /**
     * Vytvoření hesla po vyplnění formuláře.
     *
     * @param index {number} pozice hodnoty atributu
     * @param data {Object} data z formuláře
     * @param submitType {String} typ odeslání
     */
    handleCreatedRecord(index, data, submitType) {
        const {versionId, selectedSubNodeId, routingKey, fund} = this.props;

        // TODO: sjednoceni od Pavla - ELZA-591
        this.dispatch(fundSubNodeRegisterValueFocus(versionId, selectedSubNodeId, routingKey, index));
        this.dispatch(fundSubNodeRegisterValueChange(versionId, selectedSubNodeId, routingKey, index, data));
        this.dispatch(fundSubNodeRegisterValueBlur(versionId, selectedSubNodeId, routingKey, index));

        // Akce po vytvoření
        if (submitType === 'storeAndViewDetail') {  // přesměrování na detail
            this.dispatch(registryDetailFetchIfNeeded(data.id));
            this.dispatch(routerNavigate('registry'));
        } else {    // nastavení focus zpět na prvek
        }
    }

    handleAddClick = () => {
        this.dispatch(fundSubNodeRegisterValueAdd(this.props.versionId, this.props.selectedSubNodeId, this.props.routingKey));
    }

    handleChange(index, record) {
        this.dispatch(fundSubNodeRegisterValueChange(this.props.versionId, this.props.selectedSubNodeId, this.props.routingKey, index, record));
    }

    handleDetail(recordId) {
        this.dispatch(registryDetailFetchIfNeeded(recordId));
        this.dispatch(routerNavigate('registry'));
    }

    handleFocus(index) {
        this.dispatch(fundSubNodeRegisterValueFocus(this.props.versionId, this.props.selectedSubNodeId, this.props.routingKey, index));
    }

    handleBlur(index) {
        this.dispatch(fundSubNodeRegisterValueBlur(this.props.versionId, this.props.selectedSubNodeId, this.props.routingKey, index));
    }

    handleRemove(index) {
        this.dispatch(fundSubNodeRegisterValueDelete(this.props.versionId, this.props.selectedSubNodeId, this.props.routingKey, index));
    }

    renderRegister = (register, index) => {
        const {closed, versionId, readMode} = this.props;

        if (readMode) {
            return register.value ?
                <DescItemLabel key={"link-" + index} onClick={this.handleDetail.bind(this, register.record.id)} value={register.record.record} /> :
                <DescItemLabel key={"link-" + index} value="" />;
        } else {
            const deletable = (register.id && register.value === register.prevValue) || (!register.id && !register.value);
            return <div className="link" key={"link-" + index}>
                    <NodeRegister onFocus={this.handleFocus.bind(this, index)}
                                  onBlur={this.handleBlur.bind(this, index)}
                                  onDetail={this.handleDetail.bind(this, index)}
                                  onChange={this.handleChange.bind(this, index)}
                                  closed={closed}
                                  onCreateRecord={this.handleCreateRecord.bind(this, index)}
                                  item={register}
                                  value={register.value ? register.value : null}
                                  versionId={versionId} />
                {!closed && <NoFocusButton style={{visibility: deletable ? 'visible' : 'hidden'}} key="delete" onClick={this.handleRemove.bind(this, index)} ><Icon glyph="fa-times" /></NoFocusButton>}
            </div>;
        }
    };

    renderForm = () => {
        const {register, closed, readMode} = this.props;

        return <div className="register-form">
            <div className='links'>{register.data.map(this.renderRegister)}</div>
            {!closed && !readMode && <div className='action'><NoFocusButton onClick={this.handleAddClick}><Icon glyph="fa-plus" /></NoFocusButton></div>}
        </div>
    };

    render() {

        const {register} = this.props;

        return <div className='node-register'>
            <div className='node-register-title'>{i18n('subNodeRegister.title')}</div>
            {register.fetched ? this.renderForm() : <Loading value={i18n('global.data.loading.register')} />}
        </div>
    }
};

function mapStateToProps(state) {
    const {arrRegion} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    return {
        fund
    }
}

export default connect(mapStateToProps)(SubNodeRegister);
