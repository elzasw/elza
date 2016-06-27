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
import {fundSubNodeFormDescItemTypeAdd, fundSubNodeFormValueChange, fundSubNodeFormDescItemTypeDelete,
        fundSubNodeFormValueChangeSpec,fundSubNodeFormValueBlur, fundSubNodeFormValueFocus, fundSubNodeFormValueAdd,
        fundSubNodeFormValueDelete, fundSubNodeFormValuesCopyFromPrev, fundSubNodeFormValueChangePosition,
        fundSubNodeFormValueUploadCoordinates, fundSubNodeFormValueUploadCsv} from 'actions/arr/subNodeForm.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import DescItemString from './nodeForm/DescItemString.jsx'
import DescItemType from './nodeForm/DescItemType.jsx'
import AddDescItemTypeForm from './nodeForm/AddDescItemTypeForm.jsx'
import {lockDescItemType, unlockDescItemType, unlockAllDescItemType,
        copyDescItemType, nocopyDescItemType} from 'actions/arr/nodeSetting.jsx'
import {addNode,deleteNode} from '../../actions/arr/node.jsx'
import {isFundRootId} from './ArrUtils.jsx'
import {partySelect, partyAdd} from 'actions/party/party.jsx'
import {registrySelect, registryAdd} from 'actions/registry/registryRegionList.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {setInputFocus} from 'components/Utils.jsx'
import {setFocus, canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {UrlFactory} from 'actions/index.jsx';
var classNames = require('classnames');
var Shortcuts = require('react-shortcuts/component')

var SubNodeForm = class SubNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderDescItemGroup', 'renderDescItemType', 'handleChange', 'handleChangePosition',
            'handleChangeSpec', 'handleDescItemTypeRemove', 'handleBlur', 'handleFocus', 'renderFormActions',
            'handleDescItemAdd', 'handleDescItemRemove', 'handleDescItemTypeLock',
            'handleDescItemTypeUnlockAll', 'handleDescItemTypeCopy', 'handleAddNodeBefore', 'handleAddNodeAfter',
            'handleAddChildNode', 'handleCreateParty',
            'handleCreatedParty', 'handleCreateRecord', 'handleCreatedRecord', 'handleDeleteNode',
            'handleDescItemTypeCopyFromPrev', 'trySetFocus', 'initFocus', 'getFlatDescItemTypes', 'getNodeSetting',
            'addNodeAfterClick', 'addNodeBeforeClick', 'addNodeChildClick', 'handleJsonTableDownload'
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

    initFocus() {
        if (this.refs.nodeForm) {
            var el = ReactDOM.findDOMNode(this.refs.nodeForm);
            if (el) {
                setInputFocus(el, false);
            }
        }        
    }

    trySetFocus(props) {
        var {focus, node} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'arr', 2, 'subNodeForm')) {
                if (focus.item) {   // položka
                    this.setState({}, () => {
                        var ref = this.refs['descItemType' + focus.item.descItemTypeId]
                        if (ref) {
                            var descItemType = ref.getWrappedInstance()
                            descItemType.focus(focus.item)
                        }
                        focusWasSet()
                    })
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
    renderDescItemGroup(descItemGroup, descItemGroupIndex, nodeSetting) {
        const {singleDescItemTypeEdit, singleDescItemTypeId} = this.props

        var descItemTypes = []
        descItemGroup.descItemTypes.forEach((descItemType, descItemTypeIndex) => {
            const render = !singleDescItemTypeEdit || (singleDescItemTypeEdit && singleDescItemTypeId == descItemType.id)

            if (render) {
                const i = this.renderDescItemType(descItemType, descItemTypeIndex, descItemGroupIndex, nodeSetting)
                descItemTypes.push(i)
            }
        });
        var cls = classNames({
            'desc-item-group': true,
            active: descItemGroup.hasFocus
        });

        if (singleDescItemTypeEdit && descItemTypes.length === 0) {
            return null
        }

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

        // Focus na následující hodnotu, pokud existuje, jinak na předchozí hodnotu, pokud existuje, jinak obecně na descItemType (reálně se nastaví na první hodnotu daného atributu)
        // Focus musíme zjišťovat před DISPATCH fundSubNodeFormValueDelete, jinak bychom neměli ve formData správná data, protože ty nejsou immutable!
        var setFocusFunc
        const {subNodeForm: {formData}} = this.props
        var descItemType = formData.descItemGroups[descItemGroupIndex].descItemTypes[descItemTypeIndex]
        var descItem = descItemType.descItems[descItemIndex]
        if (descItemIndex + 1 < descItemType.descItems.length) {    // následující hodnota
            var focusDescItem = descItemType.descItems[descItemIndex + 1]
            setFocusFunc = () => setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: focusDescItem.descItemObjectId, descItemIndex: descItemIndex})
        } else if (descItemIndex > 0) { // předchozí hodnota
            var focusDescItem = descItemType.descItems[descItemIndex - 1]
            setFocusFunc = () => setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: focusDescItem.descItemObjectId, descItemIndex: descItemIndex - 1})
        } else {    // obecně descItemType
            setFocusFunc = () => setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: null, descItemIndex: null})
        }

        // Smazání hodnoty
        this.dispatch(fundSubNodeFormValueDelete(this.props.versionId, this.props.nodeKey, valueLocation));

        // Nyní pošleme focus
        this.dispatch(setFocusFunc())
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

        // Focus na následující prvek, pokud existuje, jinak na předchozí, pokud existuje, jinak na accordion
        // Focus musíme zjišťovat před DISPATCH fundSubNodeFormDescItemTypeDelete, jinak bychom neměli ve formData správná data, protože ty nejsou immutable!
        var setFocusFunc
        const {subNodeForm: {formData}} = this.props
        var descItemType = formData.descItemGroups[descItemGroupIndex].descItemTypes[descItemTypeIndex]
        var types = this.getFlatDescItemTypes(true)
        var index = indexById(types, descItemType.id)
        if (index + 1 < types.length) {
            var focusDescItemType = types[index + 1]
            setFocusFunc = () => setFocus('arr', 2, 'subNodeForm', {descItemTypeId: focusDescItemType.id, descItemObjectId: null})
        } else if (index > 0) {
            var focusDescItemType = types[index - 1]
            setFocusFunc = () => setFocus('arr', 2, 'subNodeForm', {descItemTypeId: focusDescItemType.id, descItemObjectId: null})
        } else {    // nemůžeme žádný prvek najít, focus dostane accordion
            setFocusFunc = () => setFocus('arr', 2, 'accordion')
        }

        // Smazání hodnoty
        this.dispatch(fundSubNodeFormDescItemTypeDelete(this.props.versionId, this.props.nodeKey, valueLocation));

        // Nyní pošleme focus
        this.dispatch(setFocusFunc())
    }

    getFlatDescItemTypes(onlyNotLocked) {
        const {subNodeForm: {formData}} = this.props

        var nodeSetting
        if (onlyNotLocked) {
            nodeSetting = this.getNodeSetting()
        }

        var result = []

        formData.descItemGroups.forEach(group => {
            group.descItemTypes.forEach(type => {
                if (onlyNotLocked) {
                    if (!this.isDescItemLocked(nodeSetting, type.id)) {
                        result.push(type)
                    }
                } else {
                    result.push(type)
                }
            })
        })

        return result
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

        // Focus na novou hodnotu
        const {subNodeForm: {formData}} = this.props
        var descItemType = formData.descItemGroups[descItemGroupIndex].descItemTypes[descItemTypeIndex]
        var index = descItemType.descItems.length
        var setFocusFunc = () => setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: null, descItemIndex: index})

        // Smazání hodnoty
        this.dispatch(fundSubNodeFormValueAdd(this.props.versionId, this.props.nodeKey, valueLocation));

        // Nyní pošleme focus
        this.dispatch(setFocusFunc())
    }

    /**
     * Přidání nové hodnoty coordinates pomocí uploadu
     * @param descItemTypeId {Integer} Id descItemTypeId
     * @param file {File} soubor
     */
    handleCoordinatesUpload(descItemTypeId, file) {
        this.dispatch(fundSubNodeFormValueUploadCoordinates(this.props.versionId, this.props.nodeKey, descItemTypeId, file));
    }
    
    /**
     * Přidání nové hodnoty jsonTable pomocí uploadu.
     * @param file {File} soubor
     */
    handleJsonTableUpload(descItemTypeId, file) {
        this.dispatch(fundSubNodeFormValueUploadCsv(this.props.versionId, this.props.nodeKey, descItemTypeId, file));
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
        this.dispatch(registryAdd(null, versionId, this.handleCreatedRecord.bind(this, valueLocation), '', true));
    }

    /**
     * Vytvoření hesla po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreatedRecord(valueLocation, data, submitType) {
        const {versionId, selectedSubNodeId, nodeKey, fund, subNodeForm} = this.props;

        // Uložení hodnoty
        this.dispatch(fundSubNodeFormValueChange(versionId, nodeKey, valueLocation, data, true));

        // Akce po vytvoření
        if (submitType === 'storeAndViewDetail') {  // přesměrování na detail
            this.dispatch(registrySelect(data.recordId, fund));
            this.dispatch(routerNavigate('registry'));
        } else {    // nastavení focus zpět na prvek
            var formData = subNodeForm.formData
            var descItemType = formData.descItemGroups[valueLocation.descItemGroupIndex].descItemTypes[valueLocation.descItemTypeIndex]
            this.dispatch(setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: null, descItemIndex: valueLocation.descItemIndex}))
        }
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
        const {fund, singleDescItemTypeEdit} = this.props;
        singleDescItemTypeEdit && this.dispatch(modalDialogHide())
        this.dispatch(registrySelect(recordId, fund));
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
        this.dispatch(partyAdd(partyTypeId, versionId, this.handleCreatedParty.bind(this, valueLocation), true));
    }

    /**
     * Vytvoření obalu po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreatedParty(valueLocation, data, submitType) {
        const {versionId, selectedSubNodeId, nodeKey, fund, subNodeForm} = this.props;

        // Uložení hodnoty
        this.dispatch(fundSubNodeFormValueChange(versionId, nodeKey, valueLocation, data, true));

        // Akce po vytvoření
        if (submitType === 'storeAndViewDetail') {  // přesměrování na detail
            this.dispatch(partySelect(data.partyId, fund));
            this.dispatch(routerNavigate('party'));
        } else {    // nastavení focus zpět na prvek
            var formData = subNodeForm.formData
            var descItemType = formData.descItemGroups[valueLocation.descItemGroupIndex].descItemTypes[valueLocation.descItemTypeIndex]
            this.dispatch(setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: null, descItemIndex: valueLocation.descItemIndex}))
        }
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
        const {fund, singleDescItemTypeEdit} = this.props;
        singleDescItemTypeEdit && this.dispatch(modalDialogHide())
        this.dispatch(partySelect(partyId, fund));
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

        this.dispatch(fundSubNodeFormValueBlur(this.props.versionId, this.props.nodeKey, valueLocation));
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

        this.dispatch(fundSubNodeFormValueFocus(this.props.versionId, this.props.nodeKey, valueLocation));
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

        this.dispatch(fundSubNodeFormValueChange(this.props.versionId, this.props.nodeKey, valueLocation, value, false));
    }

    /**
     * Změna pozice hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param newDescItemIndex {Integer} nová pozice - nový index atributu
     */
    handleChangePosition(descItemGroupIndex, descItemTypeIndex, descItemIndex, newDescItemIndex) {
        console.log(222222, descItemGroupIndex, descItemTypeIndex, descItemIndex, newDescItemIndex)
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(fundSubNodeFormValueChangePosition(this.props.versionId, this.props.nodeKey, valueLocation, newDescItemIndex));
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

        this.dispatch(fundSubNodeFormValueChangeSpec(this.props.versionId, this.props.nodeKey, valueLocation, value));
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
        this.dispatch(fundSubNodeFormValuesCopyFromPrev(this.props.versionId, this.props.selectedSubNode.id, this.props.selectedSubNode.version, descItemTypeId, nodeKey, valueLocation));
    }

    handleDeleteNode() {
        if (window.confirm('Opravdu chcete smazat tento JP?')) {
            this.dispatch(deleteNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId));
        }
    }

    isDescItemLocked(nodeSetting, descItemTypeId) {
        // existuje nastavení o JP - zamykání
        if (nodeSetting && nodeSetting.descItemTypeLockIds) {
            var index = nodeSetting.descItemTypeLockIds.indexOf(descItemTypeId);

            // existuje type mezi zamknutými
            if (index >= 0) {
                return true
            }
        }
        return false
    }

    handleCoordinatesDownload(objectId) {
        const {versionId} = this.props;

        window.open(UrlFactory.exportArrCoordinate(objectId, versionId));
    }

    handleJsonTableDownload(objectId) {
        const {versionId} = this.props;
        
        window.open(UrlFactory.exportArrDescItemCsvExport(objectId, versionId));
    }

    /**
     * Renderování atributu.
     * @param descItemType {Object} atribut
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param nodeSetting {object}
     * @return {Object} view
     */
    renderDescItemType(descItemType, descItemTypeIndex, descItemGroupIndex, nodeSetting) {
        const {fundId, subNodeForm, descItemCopyFromPrevEnabled, singleDescItemTypeEdit, rulDataTypes, calendarTypes, closed,
                nodeSettings, nodeId, packetTypes, packets, conformityInfo, versionId} = this.props;

        var refType = subNodeForm.refTypesMap[descItemType.id]
        var infoType = subNodeForm.infoTypesMap[descItemType.id]
        var rulDataType = refType.dataType

        var locked = this.isDescItemLocked(nodeSetting, descItemType.id);
        var copy = false;

        // existují nějaké nastavení pro konkrétní node
        if (nodeSetting) {
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
                ref={'descItemType' + descItemType.id}
                descItemType={descItemType}
                singleDescItemTypeEdit={singleDescItemTypeEdit}
                refType={refType}
                infoType={infoType}
                rulDataType={rulDataType}
                calendarTypes={calendarTypes}
                packetTypes={packetTypes}
                packets={packets}
                onCreateParty={this.handleCreateParty.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDetailParty={this.handleDetailParty.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onCreateRecord={this.handleCreateRecord.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDetailRecord={this.handleDetailRecord.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemAdd={this.handleDescItemAdd.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onCoordinatesUpload={this.handleCoordinatesUpload.bind(this, descItemType.id)}
                onJsonTableUpload={this.handleJsonTableUpload.bind(this, descItemType.id)}
                onDescItemRemove={this.handleDescItemRemove.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onCoordinatesDownload={this.handleCoordinatesDownload.bind(this)}
                onJsonTableDownload={this.handleJsonTableDownload.bind(this)}
                onChange={this.handleChange.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onChangePosition={this.handleChangePosition.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onChangeSpec={this.handleChangeSpec.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onBlur={this.handleBlur.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onFocus={this.handleFocus.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemTypeRemove={this.handleDescItemTypeRemove.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemTypeLock={this.handleDescItemTypeLock.bind(this, descItemType.id)}
                onDescItemTypeCopy={this.handleDescItemTypeCopy.bind(this, descItemType.id)}
                onDescItemTypeCopyFromPrev={this.handleDescItemTypeCopyFromPrev.bind(this, descItemGroupIndex, descItemTypeIndex, descItemType.id)}
                locked={singleDescItemTypeEdit ? false : locked}
                closed={closed}
                copy={copy}
                conformityInfo={conformityInfo}
                descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                versionId={versionId}
                fundId={fundId}
            />
        )
    }

    addNodeAfterClick() {
        this.refs.addNodeAfter.handleToggle(true, false)
    }

    addNodeBeforeClick() {
        this.refs.addNodeBefore.handleToggle(true, false)
    }

    addNodeChildClick() {
        this.refs.addNodeChild.handleToggle(true, false)
    }

    /**
     * Renderování globálních akcí pro formulář.
     * @return {Object} view
     */
    renderFormActions() {
        let notRoot = !isFundRootId(this.props.nodeId);
        return (
            <div className='node-form-actions-container'>
                <div className='node-form-actions'>
                    <NoFocusButton onClick={this.props.onAddDescItemType}><Icon
                        glyph="fa-plus"/>{i18n('subNodeForm.descItemTypeAdd')}</NoFocusButton>
                    <NoFocusButton onClick={this.handleDescItemTypeUnlockAll}><Icon
                        glyph="fa-lock"/>{i18n('subNodeForm.descItemTypeUnlockAll')}</NoFocusButton>
                    {
                        notRoot &&
                        <AddNodeDropdown key="before"
                                        ref='addNodeBefore'
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
                                        ref='addNodeAfter'
                                         title={i18n('subNodeForm.addNodeAfter')}
                                         glyph="fa-plus"
                                         action={this.handleAddNodeAfter}
                                         node={this.props.selectedSubNode}
                                         version={this.props.versionId}
                                         direction="AFTER"
                        />
                    }
                    <AddNodeDropdown key="child"
                                        ref='addNodeChild'
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
                    <NoFocusButton onClick={this.props.onVisiblePolicy}><Icon
                        glyph="fa-eye"/>{i18n('subNodeForm.visiblePolicy')}</NoFocusButton>
                </div>
            </div>
        )
    }

    getNodeSetting() {
        const {nodeSettings, nodeId} = this.props;
       
        var nodeSetting
        if (nodeSettings) {
            nodeSetting = nodeSettings.nodes[nodeSettings.nodes.map(function (node) {
                return node.id;
            }).indexOf(nodeId)];
        }

        return nodeSetting
    }

    render() {
        const {fundId, subNodeForm, closed, nodeSettings, nodeId, singleDescItemTypeEdit, userDetail} = this.props;
        var formData = subNodeForm.formData

        var nodeSetting = this.getNodeSetting()

        var formActions
        if (userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            if (!closed && !singleDescItemTypeEdit) {
                formActions = this.renderFormActions()
            }
        }

        var descItemGroups = []
        formData.descItemGroups.forEach((group, groupIndex) => {
            const i = this.renderDescItemGroup(group, groupIndex, nodeSetting)
            if (i !== null) {
                descItemGroups.push(i)
            }
        });

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
    const {arrRegion, focus, userDetail} = state
    var fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    return {
        nodeSettings: arrRegion.nodeSettings,
        fund,
        focus,
        userDetail,
    }
}

SubNodeForm.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fundId: React.PropTypes.number.isRequired,
    parentNode: React.PropTypes.object.isRequired,
    selectedSubNode: React.PropTypes.object.isRequired,
    selectedSubNodeId: React.PropTypes.number.isRequired,
    nodeKey: React.PropTypes.string.isRequired,
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

module.exports = connect(mapStateToProps, null, null, { withRef: true })(SubNodeForm);
