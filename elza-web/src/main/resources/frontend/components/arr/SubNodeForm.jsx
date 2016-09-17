/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

require('./SubNodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, AddPacketForm, AddPartyForm, AddRegistryForm,
    AddPartyEventForm, AddPartyGroupForm, AddPartyDynastyForm, AddPartyOtherForm,
    AddFileForm} from 'components';
import {connect} from 'react-redux'
import {indexById} from 'stores/app/utils.jsx'
import DescItemType from './nodeForm/DescItemType.jsx'
import {partySelect, partyAdd} from 'actions/party/party.jsx'
import {registrySelect, registryAdd} from 'actions/registry/registryRegionList.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {setInputFocus} from 'components/Utils.jsx'
import {setFocus, canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {UrlFactory} from 'actions/index.jsx';
import {selectTab} from 'actions/global/tab.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {fundPacketsCreate} from 'actions/arr/fundPackets.jsx'
import {fundFilesCreate} from 'actions/arr/fundFiles.jsx'
var classNames = require('classnames');
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx';
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'

var SubNodeForm = class SubNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'renderDescItemGroup',
            'renderDescItemType',
            'handleChange',
            'handleChangePosition',
            'handleChangeSpec',
            'handleDescItemTypeRemove',
            'handleSwitchCalculating',
            'handleBlur',
            'handleFocus',
            'handleDescItemAdd',
            'handleDescItemRemove',
            'handleCreateParty',
            'handleCreatePacket',
            'handleFundPackets',
            'handleCreatePacketFormSubmit',
            'handleDescItemTypeCopyFromPrev',
            'handleDescItemTypeLock',
            'handleDescItemTypeCopy',
            'handleCreatedParty',
            'handleCreateRecord',
            'handleCreatedRecord',
            'trySetFocus',
            'initFocus',
            'getFlatDescItemTypes',
            'handleJsonTableDownload'
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
        const cls = classNames({
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
        // Focus musíme zjišťovat před DISPATCH formActions.fundSubNodeFormValueDelete, jinak bychom neměli ve formData správná data, protože ty nejsou immutable!
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
        this.dispatch(this.props.formActions.fundSubNodeFormValueDelete(this.props.versionId, this.props.routingKey, valueLocation));

        // Nyní pošleme focus
        this.dispatch(setFocusFunc())
    }

    handleSwitchCalculating(descItemGroupIndex, descItemTypeIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        }

        const {subNodeForm: {formData}} = this.props
        var descItemType = formData.descItemGroups[descItemGroupIndex].descItemTypes[descItemTypeIndex];

        var msgI18n = descItemType.calSt === 1 ? 'subNodeForm.calculate-auto.confirm' : 'subNodeForm.calculate-user.confirm';

        if(confirm(i18n(msgI18n))) {
            this.dispatch(this.props.formActions.switchOutputCalculating(this.props.versionId, descItemType.id, this.props.routingKey, valueLocation));
        }
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
        // Focus musíme zjišťovat před DISPATCH formActions.fundSubNodeFormDescItemTypeDelete, jinak bychom neměli ve formData správná data, protože ty nejsou immutable!
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
        this.dispatch(this.props.formActions.fundSubNodeFormDescItemTypeDelete(this.props.versionId, this.props.routingKey, valueLocation));

        // Nyní pošleme focus
        this.dispatch(setFocusFunc())
    }

    getFlatDescItemTypes(onlyNotLocked) {
        const {subNodeForm: {formData}} = this.props

        var nodeSetting
        if (onlyNotLocked) {
            nodeSetting = this.props.nodeSetting;
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

        // pokud není opakovatelný, nelze přidat další položku
        if (descItemType.rep !== 1) {
            return;
        }

        var setFocusFunc = () => setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: null, descItemIndex: index})

        // Přidání hodnoty
        this.dispatch(this.props.formActions.fundSubNodeFormValueAdd(this.props.versionId, this.props.routingKey, valueLocation));

        // Nyní pošleme focus
        this.dispatch(setFocusFunc())
    }

    /**
     * Přidání nové hodnoty coordinates pomocí uploadu
     * @param descItemTypeId {Integer} Id descItemTypeId
     * @param file {File} soubor
     */
    handleCoordinatesUpload(descItemTypeId, file) {
        this.dispatch(this.props.formActions.fundSubNodeFormValueUploadCoordinates(this.props.versionId, this.props.routingKey, descItemTypeId, file));
    }

    /**
     * Přidání nové hodnoty jsonTable pomocí uploadu.
     * @param file {File} soubor
     */
    handleJsonTableUpload(descItemTypeId, file) {
        this.dispatch(this.props.formActions.fundSubNodeFormValueUploadCsv(this.props.versionId, this.props.routingKey, descItemTypeId, file));
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
        const {versionId, routingKey, fund, subNodeForm} = this.props;

        // Uložení hodnoty
        this.dispatch(this.props.formActions.fundSubNodeFormValueChange(versionId, routingKey, valueLocation, data, true));

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
     * Vytvoření obalu.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleCreatePacket(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const {fundId} = this.props

        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        const form = <AddPacketForm
            initData={{}}
            createSingle
            fundId={fundId}
            onSubmitForm={this.handleCreatePacketFormSubmit.bind(this, valueLocation, "SINGLE")}
        />
        this.dispatch(modalDialogShow(this, i18n('arr.packet.title.add'), form));

    }

    /**
     * Odeslání formuláře na vytvoření obalu.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param type          typ obalu
     * @param data          obal
     */
    handleCreatePacketFormSubmit(valueLocation, type, data) {
        const {fundId} = this.props;
        this.dispatch(fundPacketsCreate(fundId, type, data, this.handleCreatedPacket.bind(this, valueLocation)));
    }

    /**
     * Vytvoření obalu.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param data          obal
     */
    handleCreatedPacket(valueLocation, data) {
        const {versionId, routingKey} = this.props;

        // Uložení hodnoty
        this.dispatch(this.props.formActions.fundSubNodeFormValueChange(versionId, routingKey, valueLocation, data, true));
    }

    /**
     * Zobrazení seznamu obalů.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleFundPackets(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const {fundId, userDetail} = this.props;
        var settings = getOneSettings(userDetail.settings, 'FUND_RIGHT_PANEL', 'FUND', fundId);
        console.warn(0, userDetail.settings);
        var dataRight = settings.value ? JSON.parse(settings.value) : null;

        var tabExists = dataRight && dataRight[2] !== undefined ? dataRight[2] : true;

        if (!tabExists) {
            if (confirm(i18n('subNodeForm.packets.confirm'))) {
                var value = {};
                if (dataRight) {
                    dataRight[2] = true;
                    value = dataRight;
                } else {
                    value[2] = true;
                }
                settings.value = JSON.stringify(value);
                settings = setSettings(userDetail.settings, settings.id, settings);
                console.warn(1, settings);
                this.dispatch(userDetailsSaveSettings(settings));
            } else {
                return;
            }
        }

        this.dispatch(routerNavigate('/arr'));
        this.dispatch(selectTab('arr-as', 2));
        this.dispatch(setFocus('arr', 3, null, null));

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
     * Vytvoření souboru.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleCreateFile(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const {fundId} = this.props

        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(modalDialogShow(this, i18n('dms.file.title.add'), <AddFileForm onSubmitForm={this.handleCreateFileFormSubmit.bind(this, valueLocation)} />));
    }

    /**
     * Odeslání formuláře na vytvoření souboru.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param data          soubor
     */
    handleCreateFileFormSubmit(valueLocation, data) {
        const {fundId} = this.props;
        this.dispatch(fundFilesCreate(fundId, data, this.handleCreatedFile.bind(this, valueLocation)))
    }

    /**
     * Vytvoření souboru.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param data          soubor
     */
    handleCreatedFile(valueLocation, data) {
        const {versionId, routingKey} = this.props;

        // Uložení hodnoty
        this.dispatch(this.props.formActions.fundSubNodeFormValueChange(versionId, routingKey, valueLocation, data, true));
    }

    /**
     * Zobrazení seznamu obalů.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleFundFiles(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        this.dispatch(routerNavigate('arr'));
        this.dispatch(selectTab('arr-as', 3));
        this.dispatch(setFocus('arr', 3, null, null));
    }

    /**
     * Vytvoření obalu po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param form {Object} data z formuláře
     */
    handleCreatedParty(valueLocation, data, submitType) {
        const {versionId, routingKey, fund, subNodeForm} = this.props;

        // Uložení hodnoty
        this.dispatch(this.props.formActions.fundSubNodeFormValueChange(versionId, routingKey, valueLocation, data, true));

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

        this.dispatch(this.props.formActions.fundSubNodeFormValueBlur(this.props.versionId, this.props.routingKey, valueLocation));
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

        this.dispatch(this.props.formActions.fundSubNodeFormValueFocus(this.props.versionId, this.props.routingKey, valueLocation));
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

        this.dispatch(this.props.formActions.fundSubNodeFormValueChange(this.props.versionId, this.props.routingKey, valueLocation, value, false));
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

        this.dispatch(this.props.formActions.fundSubNodeFormValueChangePosition(this.props.versionId, this.props.routingKey, valueLocation, newDescItemIndex));
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

        this.dispatch(this.props.formActions.fundSubNodeFormValueChangeSpec(this.props.versionId, this.props.routingKey, valueLocation, value));
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
     * Akce okamžitého kopírování hodnot atributu z předcházející JP.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemTypeId {Integer} id desc item type
     */
    handleDescItemTypeCopyFromPrev(descItemGroupIndex, descItemTypeIndex, descItemTypeId) {
        this.props.onDescItemTypeCopyFromPrev(descItemGroupIndex, descItemTypeIndex, descItemTypeId)
    }

    /**
     * Přidání/odebrání zámku pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param locked {Boolean} true, pokud se má zámek povolit
     */
    handleDescItemTypeLock(descItemTypeId, locked) {
        this.props.onDescItemTypeLock(descItemTypeId, locked);
    }

    /**
     * Přidání/odebrání opakovaného pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param copy {Boolean} true, pokud se má opakované kopírování povolit
     */
    handleDescItemTypeCopy(descItemTypeId, copy) {
        this.props.onDescItemTypeCopy(descItemTypeId, copy);
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
                packetTypes, packets, showNodeAddons, conformityInfo, versionId, readMode} = this.props;

        var refType = subNodeForm.refTypesMap[descItemType.id]
        var infoType = subNodeForm.infoTypesMap[descItemType.id]
        var rulDataType = refType.dataType

        var locked = this.isDescItemLocked(nodeSetting, descItemType.id);

        if (infoType.cal === 1) {
            locked = locked || !infoType.calSt;
        }

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
                onCreatePacket={this.handleCreatePacket.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onFundPackets={this.handleFundPackets.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onCreateFile={this.handleCreateFile.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onFundFiles={this.handleFundFiles.bind(this, descItemGroupIndex, descItemTypeIndex)}
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
                onSwitchCalculating={this.handleSwitchCalculating.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemTypeLock={this.handleDescItemTypeLock.bind(this, descItemType.id)}
                onDescItemTypeCopy={this.handleDescItemTypeCopy.bind(this, descItemType.id)}
                onDescItemTypeCopyFromPrev={this.handleDescItemTypeCopyFromPrev.bind(this, descItemGroupIndex, descItemTypeIndex, descItemType.id)}
                showNodeAddons={showNodeAddons}
                locked={singleDescItemTypeEdit ? false : locked}
                closed={closed}
                copy={copy}
                conformityInfo={conformityInfo}
                descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                versionId={versionId}
                fundId={fundId}
                readMode={readMode}
            />
        )
    }

    render() {
        const {nodeSetting, fundId, subNodeForm, closed, singleDescItemTypeEdit} = this.props;
        var formData = subNodeForm.formData

        var descItemGroups = []
        formData.descItemGroups.forEach((group, groupIndex) => {
            const i = this.renderDescItemGroup(group, groupIndex, nodeSetting)
            if (i !== null) {
                descItemGroups.push(i)
            }
        });

        return (
            <div className='node-form'>
                <div ref='nodeForm' className='desc-item-groups'>
                    {descItemGroups}
                </div>
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {userDetail} = state

    return {
        userDetail
    }
}

SubNodeForm.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fundId: React.PropTypes.number.isRequired,
    routingKey: React.PropTypes.string,
    nodeSetting: React.PropTypes.object,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    subNodeForm: React.PropTypes.object.isRequired,
    closed: React.PropTypes.bool.isRequired,
    readMode: React.PropTypes.bool.isRequired,
    conformityInfo: React.PropTypes.object.isRequired,
    descItemCopyFromPrevEnabled: React.PropTypes.bool.isRequired,
    focus: React.PropTypes.object,
    formActions: React.PropTypes.object.isRequired,
    showNodeAddons: React.PropTypes.bool.isRequired,
}

module.exports = connect(mapStateToProps, null, null, { withRef: true })(SubNodeForm);
