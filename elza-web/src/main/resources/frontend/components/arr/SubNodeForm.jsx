/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

require('./SubNodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, AddPacketForm, AddPartyPersonForm, AddRegistryForm,
        AddPartyEventForm, AddPartyGroupForm, AddPartyDynastyForm, AddPartyOtherForm} from 'components';
import {connect} from 'react-redux'
import {indexById} from 'stores/app/utils.jsx'
import {faSubNodeFormDescItemTypeAdd, faSubNodeFormValueChange, faSubNodeFormDescItemTypeDelete,
        faSubNodeFormValueChangeSpec,faSubNodeFormValueBlur, faSubNodeFormValueFocus, faSubNodeFormValueAdd,
        faSubNodeFormValueDelete} from 'actions/arr/subNodeForm'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
var classNames = require('classnames');
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import DescItemString from './nodeForm/DescItemString'
import DescItemType from './nodeForm/DescItemType'
import AddDescItemTypeForm from './nodeForm/AddDescItemTypeForm'
import {lockDescItemType, unlockDescItemType, unlockAllDescItemType,
        copyDescItemType, nocopyDescItemType} from 'actions/arr/nodeSetting'
import {addNode} from 'actions/arr/node'
import {createPacket} from 'actions/arr/packets'
import faSelectSubNode from 'actions/arr/nodes'
import {isFaRootId} from './ArrUtils.jsx'
import {insertPartyArr} from 'actions/party/party'

var SubNodeForm = class SubNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderDescItemGroup', 'handleAddDescItemType', 'renderDescItemType', 'handleChange',
            'handleChangeSpec', 'handleDescItemTypeRemove', 'handleBlur', 'handleFocus', 'renderFormActions',
            'getDescItemTypeInfo', 'handleDescItemAdd', 'handleDescItemRemove', 'handleDescItemTypeLock',
            'handleDescItemTypeUnlockAll', 'handleDescItemTypeCopy', 'handleAddNodeBefore', 'handleAddNodeAfter',
            'handleCreatePacket', 'handleCreatePacketSubmit', 'handleAddChildNode', 'handleCreateParty',
            'handleCreatePartySubmit', 'handleCreateRecord',  'handleCreateRecordSubmit'
        );

//console.log("@@@@@-SubNodeForm-@@@@@", props);
    }

    componentDidMount() {
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
//console.log("@@@@@-SubNodeForm-@@@@@", props);
        this.dispatch(calendarTypesFetchIfNeeded());
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
            <div key={'type-' + descItemGroup + '-' + descItemGroupIndex} className={cls}>
                <div className='desc-item-types'>
                    {descItemTypes}
                </div>
            </div>
        )
    }

    /**
     * Dohledání předpisu typu atributu pro daný typ.
     * @param descItemType {Object} typ atributu
     * @return {Object} předpis typu atributu
     */
    getDescItemTypeInfo(descItemType) {
        return this.props.descItemTypeInfos[indexById(this.props.descItemTypeInfos, descItemType.id)];
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
     * Přidání node před aktuální node a následovné vybrání
     */
    handleAddNodeBefore() {
        this.dispatch(addNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId, "BEFORE", this.getDescItemTypeCopyIds()));
    }

    /**
     * Přidání node za aktuální node a následovné vybrání
     */
    handleAddNodeAfter() {
        this.dispatch(addNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId, "AFTER", this.getDescItemTypeCopyIds()))
    }

    /**
     * Přidání podřízeného záznamu
     */
    handleAddChildNode() {
        this.dispatch(addNode(this.props.selectedSubNode, this.props.selectedSubNode, this.props.versionId, "CHILD", this.getDescItemTypeCopyIds()));
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

        this.dispatch(modalDialogShow(this, i18n('arr.packet.title.add'), <AddPacketForm initData={initData} create findingAidId={findingAidId} onSubmit={this.handleCreatePacketSubmit.bind(this, valueLocation)} />));
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
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(modalDialogShow(this, i18n('registry.addRegistry'),
                <AddRegistryForm create onSubmit={this.handleCreateRecordSubmit.bind(this, valueLocation)}/>));
    }

    /**
     * Vytvoření hesla po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreateRecordSubmit(valueLocation, data) {
        const {versionId, selectedSubNodeId, nodeKey} = this.props;

        // TODO slapa: Až se dodělají rejstříky, napojit
        console.warn(valueLocation, data);
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
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        var data = {
            partyTypeId: partyTypeId
        }
        switch(partyTypeId){
            case 1:
                // zobrazení formuláře fyzicke osoby
                this.dispatch(modalDialogShow(this, i18n('party.addParty') , <AddPartyPersonForm initData={data} onSubmit={this.handleCreatePartySubmit.bind(this, valueLocation)} />));
                break;
            case 2:
                // zobrazení formuláře rodu
                this.dispatch(modalDialogShow(this, i18n('party.addPartyDynasty') , <AddPartyDynastyForm initData={data} onSubmit={this.handleCreatePartySubmit.bind(this, valueLocation)} />));
                break;
            case 3:
                // zobrazení formuláře korporace
                this.dispatch(modalDialogShow(this, i18n('party.addPartyGroup') , <AddPartyGroupForm initData={data} onSubmit={this.handleCreatePartySubmit.bind(this, valueLocation)} />));
                break;
            case 4:
                // zobrazení formuláře dočasné korporace
                this.dispatch(modalDialogShow(this, i18n('party.addPartyEvent') , <AddPartyEventForm initData={data} onSubmit={this.handleCreatePartySubmit.bind(this, valueLocation)} />));
                break;
            default:
                // zobrazení formuláře jine osoby - ostatni
                this.dispatch(modalDialogShow(this, i18n('party.addPartyOther') , <AddPartyOtherForm initData={data} onSubmit={this.handleCreatePartySubmit.bind(this, valueLocation)} />));
                break;
        }

    }

    /**
     * Vytvoření obalu po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreatePartySubmit(valueLocation, data) {
        const {versionId, selectedSubNodeId, nodeKey} = this.props;

        var partyType = '';                                     // typ osoby - je potreba uvest i jako specialni klivcove slovo
        switch(data.partyTypeId){
            case 1: partyType = '.ParPersonVO'; break;          // typ osoby osoba
            case 2: partyType = '.ParDynastyVO'; break;         // typ osoby rod
            case 3: partyType = '.ParPartyGroupVO'; break;      // typ osoby korporace
            case 4: partyType = '.ParEventVO'; break;           // typ osoby docasna korporace - udalost
        }
        var party = {
            '@type': partyType,
            partyType: {
                partyTypeId: data.partyTypeId
            },
            genealogy: data.nameMain,
            scope: '',
            record: {
                registerTypeId: data.recordTypeId,              // typ záznamu
                scopeId:1                                       //trida rejstriku
            },
            from: {
                textDate: data.validFrom,
                calendarTypeId: data.calendarTypeIdFrom
            },
            to: {
                textDate: data.validTo,
                calendarTypeId: data.calendarTypeIdTo
            },
            partyNames : [{
                nameFormType: {
                    nameFormTypeId: data.nameFormTypeId
                },
                displayName: data.nameMain,
                mainPart: data.nameMain,
                otherPart: data.nameOther,
                degreeBefore: data.degreeBefore,
                degreeAfter: data.degreeAfter,
                prefferedName: true,
                validFrom: {
                    textDate: data.validFrom,
                    calendarTypeId: data.calendarTypeIdFrom
                },
                validTo: {
                    textDate: data.validTo,
                    calendarTypeId: data.calendarTypeIdTo
                }
            }]
        }
        this.dispatch(insertPartyArr(party, valueLocation, versionId, selectedSubNodeId, nodeKey));
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

        this.dispatch(faSubNodeFormValueChange(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation, value));
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
     * Renderování atributu.
     * @param descItemType {Object} atribut
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @return {Object} view
     */
    renderDescItemType(descItemType, descItemTypeIndex, descItemGroupIndex) {
        const {rulDataTypes, calendarTypes, nodeSettings, nodeId, packetTypes, packets, conformityInfo} = this.props;

        var rulDataType = rulDataTypes.items[indexById(rulDataTypes.items, descItemType.dataTypeId)];
        var descItemTypeInfo = this.getDescItemTypeInfo(descItemType);

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
            <DescItemType key={descItemType.code}
                descItemType={descItemType}
                descItemTypeInfo={descItemTypeInfo}
                rulDataType={rulDataType}
                calendarTypes={calendarTypes}
                packetTypes={packetTypes}
                packets={packets}
                onCreatePacket={this.handleCreatePacket.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onCreateParty={this.handleCreateParty.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onCreateRecord={this.handleCreateRecord.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemAdd={this.handleDescItemAdd.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemRemove={this.handleDescItemRemove.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onChange={this.handleChange.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onChangeSpec={this.handleChangeSpec.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onBlur={this.handleBlur.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onFocus={this.handleFocus.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemTypeRemove={this.handleDescItemTypeRemove.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemTypeLock={this.handleDescItemTypeLock.bind(this, descItemType.id)}
                onDescItemTypeCopy={this.handleDescItemTypeCopy.bind(this, descItemType.id)}
                locked={locked}
                copy={copy}
                conformityInfo={conformityInfo}
            />
        )
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType() {
        const {descItemTypeInfos, formData, versionId, selectedSubNodeId, nodeKey} = this.props;

        // Pro přidání chceme jen ty, které zatím ještě nemáme
        var descItemTypesMap = {};
        descItemTypeInfos.forEach(descItemType => {
            descItemTypesMap[descItemType.id] = descItemType;
        })
        formData.descItemGroups.forEach(group => {
            group.descItemTypes.forEach(descItemType => {
                delete descItemTypesMap[descItemType.id];
            })
        })
        var descItemTypes = [];
        Object.keys(descItemTypesMap).forEach(function (key) {
            descItemTypes.push(descItemTypesMap[key]);
        });

        // Seřazení podle position
        descItemTypes.sort((a, b) => a.position - b.position);

        // Modální dialog
        var form = <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmit={(data) => {
            this.dispatch(modalDialogHide());
            this.dispatch(faSubNodeFormDescItemTypeAdd(versionId, selectedSubNodeId, nodeKey, data.descItemTypeId));
        }}/>
        this.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), form));
    }

    /**
     * Renderování globálních akcí pro formulář.
     * @return {Object} view
     */
    renderFormActions() {
        let notRoot = !isFaRootId(this.props.nodeId);
        return (
            <div className='node-form-actions'>
                <NoFocusButton onClick={this.handleAddDescItemType}><Icon glyph="fa-plus"/>Přidat prvek</NoFocusButton>
                <NoFocusButton onClick={this.handleDescItemTypeUnlockAll}><Icon glyph="fa-lock"/>Odemknout
                    vše</NoFocusButton>
                {
                    notRoot &&
                    <NoFocusButton onClick={this.handleAddNodeBefore}><Icon glyph="fa-plus"/>Přidat JP
                        před</NoFocusButton>
                }
                {
                    notRoot &&
                    <NoFocusButton onClick={this.handleAddNodeAfter}><Icon glyph="fa-plus"/>Přidat JP za</NoFocusButton>
                }
                <NoFocusButton onClick={this.handleAddChildNode}><Icon glyph="fa-plus"/>Přidat podřízený
                    JP</NoFocusButton>
                {
                    notRoot &&
                    <NoFocusButton><Icon glyph="fa-trash"/>Zrušit JP</NoFocusButton>
                }
            </div>
        )
    }

    render() {
        const {calendarTypes, formData} = this.props;

        if (calendarTypes.isFetching && !calendarTypes.fetched) {
            return <div className='node-form'></div>
        }

        var formActions = this.renderFormActions();
        var descItemGroups = formData.descItemGroups.map((group, groupIndex) => (
            this.renderDescItemGroup(group, groupIndex)
        ));

        return (
            <div className='node-form'>
                {formActions}
                <div className='desc-item-groups'>
                    {descItemGroups}
                </div>
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state
    return {
        nodeSettings: arrRegion.nodeSettings
    }
}

SubNodeForm.propTypes = {
    descItemTypeInfos: React.PropTypes.array.isRequired,
    versionId: React.PropTypes.number.isRequired,
    parentNode: React.PropTypes.object.isRequired,
    selectedSubNode: React.PropTypes.object.isRequired,
    selectedSubNodeId: React.PropTypes.number.isRequired,
    nodeKey: React.PropTypes.number.isRequired,
    nodeId: React.PropTypes.oneOfType(React.PropTypes.number, React.PropTypes.string),
    nodeSettings: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    formData: React.PropTypes.object.isRequired,
    conformityInfo: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(SubNodeForm);
