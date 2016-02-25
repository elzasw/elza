/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

require('./SubNodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, AddPacketForm, AddPartyForm, AddRegistryForm,
    AddPartyEventForm, AddPartyGroupForm, AddPartyDynastyForm, AddPartyOtherForm, AddNodeDropdown} from 'components';
import {connect} from 'react-redux'
import {indexById} from 'stores/app/utils.jsx'
import {faSubNodeFormDescItemTypeAdd, faSubNodeFormValueChange, faSubNodeFormDescItemTypeDelete,
        faSubNodeFormValueChangeSpec,faSubNodeFormValueBlur, faSubNodeFormValueFocus, faSubNodeFormValueAdd,
        faSubNodeFormValueDelete, faSubNodeFormValuesCopyFromPrev, faSubNodeFormValueChangePosition} from 'actions/arr/subNodeForm'
var classNames = require('classnames');
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import DescItemString from './nodeForm/DescItemString'
import DescItemType from './nodeForm/DescItemType'
import AddDescItemTypeForm from './nodeForm/AddDescItemTypeForm'
import {lockDescItemType, unlockDescItemType, unlockAllDescItemType,
        copyDescItemType, nocopyDescItemType} from 'actions/arr/nodeSetting'
import {addNode,deleteNode} from '../../actions/arr/node'
import {createPacket} from 'actions/arr/packets'
import faSelectSubNode from 'actions/arr/nodes'
import {isFaRootId} from './ArrUtils.jsx'
import {partySelect, partyAdd} from 'actions/party/party'
import {registrySelect, registryAdd} from 'actions/registry/registryRegionList'
import {routerNavigate} from 'actions/router'
import {setInputFocus} from 'components/Utils'
//import {} from './AddNodeDropdown.jsx'
var Shortcuts = require('react-shortcuts/component')
import {setFocus, canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'

var SubNodeForm = class SubNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderDescItemGroup', 'handleAddDescItemType', 'renderDescItemType', 'handleChange', 'handleChangePosition',
            'handleChangeSpec', 'handleDescItemTypeRemove', 'handleBlur', 'handleFocus', 'renderFormActions',
            'handleDescItemAdd', 'handleDescItemRemove', 'handleDescItemTypeLock',
            'handleDescItemTypeUnlockAll', 'handleDescItemTypeCopy', 'handleAddNodeBefore', 'handleAddNodeAfter',
            'handleCreatePacket', 'handleCreatePacketSubmit', 'handleAddChildNode', 'handleCreateParty',
            'handleCreatedParty', 'handleCreateRecord', 'handleCreatedRecord', 'handleDeleteNode',
            'handleDescItemTypeCopyFromPrev', 'trySetFocus'
        );
    }

    componentDidMount() {
/*
        this.setState({}, () => {
            if (this.refs.nodeForm) {
                var el = ReactDOM.findDOMNode(this.refs.nodeForm);
                if (el) {
                    //setInputFocus(el, false);
                }
            }
        })*/

        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus, node} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'arr', 2, 'subNodeForm')) {
                if (focus.item) {   // položka
                } else {    // obecně formulář
                    this.setState({}, () => {
                        var el = ReactDOM.findDOMNode(this.refs.nodeForm);
                        if (el) {
                            setInputFocus(el, false);
                        }
                        focusWasSet()
                    })
                }
            }
        }
    }

    handleShortcuts(action) {
    }

    /**
     * Renderování skupiny atributů.
     * @param descItemGroup {Object} skupina
     * @param descItemGroupIndex {Integer} index skupiny v seznamu
     * @return {Object} view
     */
    renderDescItemGroup(descItemGroup, descItemGroupIndex) {
        var descItemTypes = descItemGroup.descItemTypes.map((descItemType, descItemTypeIndex) => (
            this.renderDescItemType(descItemType, descItemTypeIndex, descItemGroupIndex)
        ));
        var cls = classNames({
            'desc-item-group': true,
            active: descItemGroup.hasFocus
        });

        return (
            <div key={'type-' + descItemGroup.code + '-' + descItemGroupIndex} className={cls}>
                <div className='desc-item-types'>
                    {descItemTypes}
                </div>
            </div>
        )
    }

   
    /**
     * Odebrání hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     */
    handleDescItemRemove(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        }

        this.dispatch(faSubNodeFormValueDelete(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    /**
     * Odebrání atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     */
    handleDescItemTypeRemove(descItemGroupIndex, descItemTypeIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        }

        this.dispatch(faSubNodeFormDescItemTypeDelete(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    /**
     * Přidání/odebrání zámku pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param locked {Boolean} true, pokud se má zámek povolit
     */
    handleDescItemTypeLock(descItemTypeId, locked) {
        if (locked) {
            this.dispatch(lockDescItemType(this.props.nodeId, descItemTypeId));
        } else {
            this.dispatch(unlockDescItemType(this.props.nodeId, descItemTypeId));
        }
    }

    /**
     * Přidání/odebrání opakovaného pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param copy {Boolean} true, pokud se má opakované kopírování povolit
     */
    handleDescItemTypeCopy(descItemTypeId, copy) {
        if (copy) {
            this.dispatch(copyDescItemType(this.props.nodeId, descItemTypeId));
        } else {
            this.dispatch(nocopyDescItemType(this.props.nodeId, descItemTypeId));
        }
    }

    /**
     * Odebrání všech zámků pro všechny atributy
     */
    handleDescItemTypeUnlockAll() {
        this.dispatch(unlockAllDescItemType(this.props.nodeId));
    }

    /**
     * Vrátí pole ke zkopírování
     */
    getDescItemTypeCopyIds() {
        var itemsToCopy = null;
        if (this.props.nodeSettings != "undefined") {
            var nodeIndex = indexById(this.props.nodeSettings.nodes, this.props.nodeId);
            if (nodeIndex != null) {
                itemsToCopy = this.props.nodeSettings.nodes[nodeIndex].descItemTypeCopyIds;
            }
        }
        return itemsToCopy;
    }

    /**
     * @param event Event selectu
     * @param scenario id vybraného scénáře
     *
     * Přidání node před aktuální node a následovné vybrání
     * Využito v dropdown buttonu pro přidání node
     */
    handleAddNodeBefore(event, scenario) {
        this.dispatch(addNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId, "BEFORE", this.getDescItemTypeCopyIds(), scenario));
    }

    /**
     * @param event Event selectu
     * @param scenario name vybraného scénáře
     *
     * Přidání node za aktuální node a následovné vybrání
     * Využito v dropdown buttonu pro přidání node
     */
    handleAddNodeAfter(event, scenario) {
        this.dispatch(addNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId, "AFTER", this.getDescItemTypeCopyIds(), scenario))
    }

    /**
     * @param event Event selectu
     * @param scenario id vybraného scénáře
     *
     * Přidání podřízeného záznamu
     */
    handleAddChildNode(event, scenario) {
        this.dispatch(addNode(this.props.selectedSubNode, this.props.selectedSubNode, this.props.versionId, "CHILD", this.getDescItemTypeCopyIds(), scenario));
    }

    /**
     * Přidání nové hodnoty vícehodnotového atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     */
    handleDescItemAdd(descItemGroupIndex, descItemTypeIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        }

        this.dispatch(faSubNodeFormValueAdd(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    /**
     * Vytvoření nového obalu.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleCreatePacket(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        const {findingAidId} = this.props;

        var initData = {
            packetTypeId: null,
            storageNumber: "",
            invalidPacket: false
        };

        this.dispatch(modalDialogShow(this, i18n('arr.packet.title.add'), <AddPacketForm initData={initData} create findingAidId={findingAidId} onSubmitForm={this.handleCreatePacketSubmit.bind(this, valueLocation)} />));
    }

    /**
     * Vytvoření obalu po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreatePacketSubmit(valueLocation, form) {
        const {findingAidId, versionId, selectedSubNodeId, nodeKey} = this.props;

        var storageNumber = form.storageNumber;
        var packetTypeId = form.packetTypeId === "" ? null : parseInt(form.packetTypeId);
        var invalidPacket = form.invalidPacket;

        this.dispatch(createPacket(findingAidId, storageNumber, packetTypeId, invalidPacket, valueLocation,
                versionId, selectedSubNodeId, nodeKey));
    }

    /**
     * Vytvoření nového hesla.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleCreateRecord(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const {versionId} = this.props;
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }
        this.dispatch(registryAdd(null, versionId, this.handleCreatedRecord.bind(this, valueLocation)));
    }

    /**
     * Vytvoření hesla po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreatedRecord(valueLocation, data) {
        const {versionId, selectedSubNodeId, nodeKey, fa} = this.props;

        this.dispatch(faSubNodeFormValueChange(versionId, selectedSubNodeId, nodeKey, valueLocation, data, true));

        this.dispatch(registrySelect(data.recordId, fa));
        this.dispatch(routerNavigate('registry'));
    }

    /**
     * Zobrazení detailu rejstříku.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param recordId {Integer} identifikátor rejstříku
     */
    handleDetailRecord(descItemGroupIndex, descItemTypeIndex, descItemIndex, recordId) {
        const {versionId, fa} = this.props;
        this.dispatch(registrySelect(recordId, fa));
        this.dispatch(routerNavigate('registry'));
    }

    /**
     * Vytvoření nové osoby.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param partyTypeId {Integer} identifikátor typu osoby
     */
    handleCreateParty(descItemGroupIndex, descItemTypeIndex, descItemIndex, partyTypeId) {
        const {versionId} = this.props;
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }
        this.dispatch(partyAdd(partyTypeId, versionId, this.handleCreatedParty.bind(this, valueLocation)));
    }

    /**
     * Vytvoření obalu po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreatedParty(valueLocation, data) {
        const {versionId, selectedSubNodeId, nodeKey, fa} = this.props;

        this.dispatch(faSubNodeFormValueChange(versionId, selectedSubNodeId, nodeKey, valueLocation, data, true));

        this.dispatch(partySelect(data.partyId, fa));
        this.dispatch(routerNavigate('party'));
    }

    /**
     * Zobrazení detailu osoby.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param partyId {Integer} identifikátor osoby
     */
    handleDetailParty(descItemGroupIndex, descItemTypeIndex, descItemIndex, partyId) {
        const {fa} = this.props;
        this.dispatch(partySelect(partyId, fa));
        this.dispatch(routerNavigate('party'));
    }

    /**
     * Opuštění hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleBlur(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(faSubNodeFormValueBlur(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    /**
     * Nový focus do hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleFocus(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(faSubNodeFormValueFocus(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    /**
     * Změna hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param value {Object} nová hodnota atributu
     */
    handleChange(descItemGroupIndex, descItemTypeIndex, descItemIndex, value) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(faSubNodeFormValueChange(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation, value, false));
    }

    /**
     * Změna pozice hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param newDescItemIndex {Integer} nová pozice - nový index atributu
     */
    handleChangePosition(descItemGroupIndex, descItemTypeIndex, descItemIndex, newDescItemIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(faSubNodeFormValueChangePosition(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation, newDescItemIndex));
    }

    /**
     * Změna specifikace u hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param value {Object} nová hodnota specifikace u atributu
     */
    handleChangeSpec(descItemGroupIndex, descItemTypeIndex, descItemIndex, value) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(faSubNodeFormValueChangeSpec(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation, value));
    }

    /**
     * Akce okamžitého kopírování hodnot atributu z předcházející JP.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemTypeId {Integer} id desc item type
     */
    handleDescItemTypeCopyFromPrev(descItemGroupIndex, descItemTypeIndex, descItemTypeId) {
        const {nodeKey} = this.props

        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        }
        this.dispatch(faSubNodeFormValuesCopyFromPrev(this.props.versionId, this.props.selectedSubNode.id, this.props.selectedSubNode.version, descItemTypeId, nodeKey, valueLocation));
    }

    handleDeleteNode() {
        if (window.confirm('Opravdu chcete smazat tento JP?')) {
            this.dispatch(deleteNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId));
        }
    }

    /**
     * Renderování atributu.
     * @param descItemType {Object} atribut
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @return {Object} view
     */
    renderDescItemType(descItemType, descItemTypeIndex, descItemGroupIndex) {
        const {subNodeForm, descItemCopyFromPrevEnabled, rulDataTypes, calendarTypes, closed,
                nodeSettings, nodeId, packetTypes, packets, conformityInfo, versionId} = this.props;

        var refType = subNodeForm.refTypesMap[descItemType.id]
        var infoType = subNodeForm.infoTypesMap[descItemType.id]
        var rulDataType = refType.dataType

        var locked = false;
        var copy = false;

        // existují nějaké nastavení o JP
        if (nodeSettings) {
            var nodeSetting = nodeSettings.nodes[nodeSettings.nodes.map(function (node) {
                return node.id;
            }).indexOf(nodeId)];

            // existuje nastavení o JP - zamykání
            if (nodeSetting && nodeSetting.descItemTypeLockIds) {
                var index = nodeSetting.descItemTypeLockIds.indexOf(descItemType.id);

                // existuje type mezi zamknutými
                if (index >= 0) {
                    locked = true;
                }

            }

            // existuje nastavení o JP - kopírování
            if (nodeSetting && nodeSetting.descItemTypeCopyIds) {
                var index = nodeSetting.descItemTypeCopyIds.indexOf(descItemType.id);

                // existuje type mezi kopírovanými
                if (index >= 0) {
                    copy = true;
                }
            }
        }

        return (
            <DescItemType key={descItemType.id}
                descItemType={descItemType}
                refType={refType}
                infoType={infoType}
                rulDataType={rulDataType}
                calendarTypes={calendarTypes}
                packetTypes={packetTypes}
                packets={packets}
                onCreatePacket={this.handleCreatePacket.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onCreateParty={this.handleCreateParty.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDetailParty={this.handleDetailParty.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onCreateRecord={this.handleCreateRecord.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDetailRecord={this.handleDetailRecord.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemAdd={this.handleDescItemAdd.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemRemove={this.handleDescItemRemove.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onChange={this.handleChange.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onChangePosition={this.handleChangePosition.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onChangeSpec={this.handleChangeSpec.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onBlur={this.handleBlur.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onFocus={this.handleFocus.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemTypeRemove={this.handleDescItemTypeRemove.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemTypeLock={this.handleDescItemTypeLock.bind(this, descItemType.id)}
                onDescItemTypeCopy={this.handleDescItemTypeCopy.bind(this, descItemType.id)}
                onDescItemTypeCopyFromPrev={this.handleDescItemTypeCopyFromPrev.bind(this, descItemGroupIndex, descItemTypeIndex, descItemType.id)}
                locked={locked}
                closed={closed}
                copy={copy}
                conformityInfo={conformityInfo}
                descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                versionId={versionId}
            />
        )
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType() {
        const {subNodeForm, versionId, selectedSubNodeId, nodeKey} = this.props;

        const formData = subNodeForm.formData

        // Pro přidání chceme jen ty, které zatím ještě nemáme
        var infoTypesMap = {...subNodeForm.infoTypesMap};
        formData.descItemGroups.forEach(group => {
            group.descItemTypes.forEach(descItemType => {
                delete infoTypesMap[descItemType.id];
            })
        })
        var descItemTypes = [];
        Object.keys(infoTypesMap).forEach(function (key) {
            descItemTypes.push({
                ...subNodeForm.refTypesMap[key],
                ...infoTypesMap[key],
            });
        });

        function typeId(type) {
            switch (type) {
                case "REQUIRED":
                    return 0;
                case "RECOMMENDED":
                    return 1;
                case "POSSIBLE":
                    return 2;
                case "IMPOSSIBLE":
                    return 99;
                default:
                    return 3;
            }
        }

        // Seřazení podle position
        descItemTypes.sort((a, b) => typeId(a.type) - typeId(b.type));
        var submit = (data) => {
            console.log(data);
            this.dispatch(modalDialogHide());
            this.dispatch(faSubNodeFormDescItemTypeAdd(versionId, selectedSubNodeId, nodeKey, data.descItemTypeId));
        };
        // Modální dialog
        var form = <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>;
        this.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), form));
    }

    /**
     * Renderování globálních akcí pro formulář.
     * @return {Object} view
     */
    renderFormActions() {
        let notRoot = !isFaRootId(this.props.nodeId);
        return (
            <div className='node-form-actions-container'>
                <div className='node-form-actions'>
                    <NoFocusButton onClick={this.handleAddDescItemType}><Icon
                        glyph="fa-plus"/>{i18n('subNodeForm.descItemTypeAdd')}</NoFocusButton>
                    <NoFocusButton onClick={this.handleDescItemTypeUnlockAll}><Icon
                        glyph="fa-lock"/>{i18n('subNodeForm.descItemTypeUnlockAll')}</NoFocusButton>
                    {
                        notRoot &&
                        <AddNodeDropdown key="before"
                                         title={i18n('subNodeForm.addNodeBefore')}
                                         glyph="fa-plus"
                                         action={this.handleAddNodeBefore}
                                         node={this.props.selectedSubNode}
                                         version={this.props.versionId}
                                         direction="BEFORE"
                        />
                    }
                    {
                        notRoot &&
                        <AddNodeDropdown key="after"
                                         title={i18n('subNodeForm.addNodeAfter')}
                                         glyph="fa-plus"
                                         action={this.handleAddNodeAfter}
                                         node={this.props.selectedSubNode}
                                         version={this.props.versionId}
                                         direction="AFTER"
                        />
                    }
                    <AddNodeDropdown key="child"
                                     title={i18n('subNodeForm.addSubNode')}
                                     glyph="fa-plus"
                                     action={this.handleAddChildNode}
                                     node={this.props.selectedSubNode}
                                     version={this.props.versionId}
                                     direction="CHILD"
                    />
                    {
                        notRoot &&
                        <NoFocusButton onClick={this.handleDeleteNode}><Icon
                            glyph="fa-trash"/>{i18n('subNodeForm.deleteNode')}</NoFocusButton>
                    }
                </div>
            </div>
        )
    }

    render() {
        const {subNodeForm, closed} = this.props;
        var formData = subNodeForm.formData

        var formActions = closed ? null : this.renderFormActions();
        var descItemGroups = formData.descItemGroups.map((group, groupIndex) => (
            this.renderDescItemGroup(group, groupIndex)
        ));

        return (
            <div className='node-form'>
                {formActions}
                <div ref='nodeForm' className='desc-item-groups'>
                    {descItemGroups}
                </div>
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion, focus} = state
    var fa = null;
    if (arrRegion.activeIndex != null) {
        fa = arrRegion.fas[arrRegion.activeIndex];
    }

    return {
        nodeSettings: arrRegion.nodeSettings,
        fa: fa,
        focus,
    }
}

SubNodeForm.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    parentNode: React.PropTypes.object.isRequired,
    selectedSubNode: React.PropTypes.object.isRequired,
    selectedSubNodeId: React.PropTypes.number.isRequired,
    nodeKey: React.PropTypes.number.isRequired,
    nodeId: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string]),
    nodeSettings: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    subNodeForm: React.PropTypes.object.isRequired,
    closed: React.PropTypes.bool.isRequired,
    conformityInfo: React.PropTypes.object.isRequired,
    descItemCopyFromPrevEnabled: React.PropTypes.bool.isRequired,
}

module.exports = connect(mapStateToProps)(SubNodeForm);
