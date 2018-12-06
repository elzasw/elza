require('./SubNodeRegister.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, HorizontalLoader, NoFocusButton} from 'components/shared';
import {connect} from 'react-redux'

import {
    fundSubNodeRegisterValueDelete,
    fundSubNodeRegisterValueAdd,
    fundSubNodeRegisterValueFocus,
    fundSubNodeRegisterValueBlur,
    fundSubNodeRegisterValueChange
} from 'actions/arr/subNodeRegister.jsx'
import NodeRegister from './registerForm/NodeRegister.jsx'
import {routerNavigate} from 'actions/router.jsx'
import DescItemLabel from './nodeForm/DescItemLabel.jsx'
import RegistrySelectPage from 'pages/select/RegistrySelectPage.jsx'
import {registryDetailFetchIfNeeded, registryListFilter, registryDetailClear, AREA_REGISTRY_LIST, registryAdd} from 'actions/registry/registry.jsx'
import {partyDetailFetchIfNeeded, partyListFilter, partyDetailClear, AREA_PARTY_LIST} from 'actions/party/party.jsx'
import {storeFromArea, objectById} from 'shared/utils'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import classNames from 'classnames'
import {MODAL_DIALOG_VARIANT} from '../../constants.tsx'

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
        this.dispatch(registryAdd(versionId, this.handleCreatedRecord.bind(this, index), true));
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
    handleSelectModule = ({onSelect, filterText, value}) => {
        const {hasSpecification, registryList, partyList, fundName, nodeName} = this.props;
        //console.log(filterText, registryList, partyList, fundName, nodeName, specName, "aaa");
        const open = (hasParty = false) => {
            if (hasParty) {
                //this.dispatch(partyListFilter({...partyList.filter, text: filterText, itemSpecId: null}));
                this.dispatch(partyDetailClear());
            }
            //this.dispatch(registryListFilter({text: filterText}));
            this.dispatch(registryDetailFetchIfNeeded(value ? value.id : null));

            this.dispatch(modalDialogShow(this, null, <RegistrySelectPage
                titles={[fundName, nodeName]}
                hasParty={hasParty} onSelect={(data) => {
                    onSelect(data);
                    if (hasParty) {
                        this.dispatch(partyListFilter({text:null, type:null, itemSpecId: null}));
                        this.dispatch(partyDetailClear());
                    }
                    this.dispatch(registryListFilter({text: null, registryParentId: null, registryTypeId: null, versionId: null, itemSpecId: null, parents: [], typesToRoot: null}));
                    this.dispatch(registryDetailClear());
                    this.dispatch(modalDialogHide());
            }}
            />, classNames(MODAL_DIALOG_VARIANT.FULLSCREEN, MODAL_DIALOG_VARIANT.NO_HEADER)));
        };
        open(true);

    };

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
                                  //onDetail={this.handleDetail.bind(this, index)}
                                  onSelectModule={this.handleSelectModule}
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
            {register.fetched ? this.renderForm() : <HorizontalLoader />}
        </div>
    }
};

function mapStateToProps(state,props) {
    const {arrRegion} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }
    const registryList = storeFromArea(state, AREA_REGISTRY_LIST);
    const partyList = storeFromArea(state, AREA_PARTY_LIST);
    let fundName = null, nodeName = null;
    if (props.typePrefix != "output") {
        const {arrRegion:{activeIndex,funds}} = state;
        const fund = funds[activeIndex];
        const {nodes} = fund;
        fundName = fund.name;
        const node = nodes.nodes[nodes.activeIndex];
        const {selectedSubNodeId} = node;
        const subNode = objectById(node.childNodes, selectedSubNodeId);
        subNode && subNode.name && (nodeName = subNode.name);
    }
    return {
        fund,
        registryList,
        partyList,
        fundName,
        nodeName
    }
}

export default connect(mapStateToProps)(SubNodeRegister);
