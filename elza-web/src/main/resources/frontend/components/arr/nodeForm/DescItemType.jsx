/**
 * Atribut - desc item type.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {Utils, Icon, i18n, AbstractReactComponent, NoFocusButton} from 'components/index.jsx';
import {Tooltip, OverlayTrigger,Input} from 'react-bootstrap';
import {addToastrDanger} from 'components/shared/toastr/ToastrActions.jsx'
import {connect} from 'react-redux'
import {WebApi} from 'actions/index.jsx';
import DescItemString from './DescItemString.jsx'
import DescItemUnitid from './DescItemUnitid.jsx'
import DescItemText from './DescItemText.jsx'
import DescItemInt from './DescItemInt.jsx'
import DescItemDecimal from './DescItemDecimal.jsx'
import DescItemCoordinates from './DescItemCoordinates.jsx'
import DescItemUnitdate from './DescItemUnitdate.jsx'
import DescItemPacketRef from './DescItemPacketRef.jsx'
import DescItemPartyRef from './DescItemPartyRef.jsx'
import DescItemRecordRef from './DescItemRecordRef.jsx'
import {propsEquals} from 'components/Utils.jsx'
import {descItemNeedStore} from 'actions/arr/subNodeForm.jsx'
import {hasDescItemTypeValue} from 'components/arr/ArrUtils.jsx'
import {indexById} from 'stores/app/utils.jsx'
var classNames = require('classnames');
import * as perms from 'actions/user/Permission.jsx';
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component')

require ('./AbstractDescItem.less')

var keyModifier = Utils.getKeyModifier()

var keymap = {
    DescItemType: {
        deleteDescItemType: keyModifier + 'y',
    },
    DescItem: {
        addDescItem: keyModifier + 'i',
        deleteDescItem: keyModifier + 'd',
    },
}
var shortcutManager = new ShortcutsManager(keymap)

var placeholder = document.createElement("div");
placeholder.className = "placeholder";

var DescItemType = class DescItemType extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderDescItemSpec', 'renderDescItem', 'renderLabel',
                'handleChange', 'handleChangeSpec', 'handleCreateParty', 'handleCreateRecord',
                'handleBlur', 'handleFocus', 'handleDescItemTypeLock', 'handleDescItemTypeCopy', 'handleDetailParty',
                'handleDetailRecord', 'handleDescItemTypeCopyFromPrev', 'handleDragStart', 'handleDragEnd', 'handleDragOver',
                'handleDragLeave', 'getShowDeleteDescItemType', 'getShowDeleteDescItem', 'focus', 'handleDescItemTypeShortcuts',
                'handleDescItemShortcuts', 'handleCoordinatesUploadButtonClick', 'handleCoordinatesUpload', 'removePlaceholder');
    }

    componentWillReceiveProps(nextProps) {
    }

    shouldComponentUpdate(nextProps, nextState) {
        return true;
        var eqProps = ['descItemType', 'rulDataType', 'calendarTypes', 'packetTypes', 'packets', 'locked', 'copy']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    handleDescItemTypeShortcuts(action) {
        console.log("#handleDescItemTypeShortcuts", '[' + action + ']', this);

        const {locked} = this.props

        switch (action) {
            case 'deleteDescItemType':
                if (!locked && this.getShowDeleteDescItemType()) {
                    this.props.onDescItemTypeRemove()
                }
                break
        }
    }

    handleDescItemShortcuts(descItemIndex, action) {
        console.log("#handleDescItemShortcuts", '[' + action + ']', this, 'index', descItemIndex);

        const {locked, descItemType, infoType, onDescItemRemove, onDescItemAdd} = this.props

        switch (action) {
            case 'addDescItem':
                if (!locked) {   // přidávat hodnoty lze jen pokud není zamčeno
                    onDescItemAdd()
                }
                break
            case 'deleteDescItem':
                if (!locked && infoType.rep === 1) {   // mazat hodnoty lze jen u vícehodnotových atributů a není zamčeno
                    var descItem = descItemType.descItems[descItemIndex]
                    if (this.getShowDeleteDescItem(descItem)) {
                        onDescItemRemove(descItemIndex)
                    }
                }
                break
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    focus(item) {
        const {descItemType, refType} = this.props

        const refPrefix = refType.useSpecification ? 'spec_' : ''

        var ref, descItem;
        if (typeof item.descItemObjectId !== 'undefined' && item.descItemObjectId !== null) {   // konkrétní hodnota
            descItem = descItemType.descItems[indexById(descItemType.descItems, item.descItemObjectId, 'descItemObjectId')]
            ref = this.refs[refPrefix + descItem.formKey]
        } else if (typeof item.descItemIndex !== 'undefined' && item.descItemIndex !== null) {   // konkrétní index
            descItem = descItemType.descItems[item.descItemIndex]
            ref = this.refs[refPrefix + descItem.formKey]
        } else {    // obecně atribut - dáme na první hodnotu
            descItem = descItemType.descItems[0]
            ref = this.refs[refPrefix + descItem.formKey]
        }

        if (ref) {
            if (refType.useSpecification) { // focus bude na specifikaci
                if (ref.focus) {
                    ref.focus()
                } else {
                    console.error('Cannot find focus method for desc item', ref)
                }
            } else {    // focus bude na vlastní hodnotu atributu
                descItem = ref.getWrappedInstance()
                if (descItem.focus) {
                    descItem.focus()
                } else {
                    console.error('Cannot find focus method for desc item', descItem)
                }
            }
        }
    }

    /**
     * Renderování specifikace atributu.
     * @param key {Object} key pro hodnotu
     * @param descItem {Object} objekt hodnoty atributu
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param locked {Boolean}
     * @return {Object} view
     */
    renderDescItemSpec(key, descItem, descItemIndex, locked) {
        const {infoType, refType} = this.props;

        var options = infoType.specs.map(spec => {
            var fullSpec = {...spec, ...refType.descItemSpecsMap[spec.id]}
            var clsSpec = ['spec-' + spec.type.toLowerCase()];
            return <option className={clsSpec} key={fullSpec.id} value={fullSpec.id}>{fullSpec.name}</option>
        });

        var cls = classNames({
            'form-control': true,
            value: true,
            'desc-item-spec': true,
            error: descItem.error.spec,
            active: descItem.hasFocus,
        });

        var descItemSpecProps = {
            onChange: this.handleChangeSpec.bind(this, descItemIndex),
            onBlur: this.handleBlur.bind(this, descItemIndex),
            onFocus: this.handleFocus.bind(this, descItemIndex),
            disabled: locked
        }

        return (
            <select
                key={key}
                ref={key}
                className={cls}
                {...descItemSpecProps}
                value={descItem.descItemSpecId}
                title={descItem.error.spec}
            >
                <option key="novalue" />
                {options}
            </select>
        )
    }

    /**
     * Změna hodnoty atributu.
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param value {Object} nová hodnota atibutu
     */
    handleChange(descItemIndex, value) {
        this.props.onChange(descItemIndex, value);
    }

    /**
     * Vytvoření nového hesla.
     *
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     */
    handleCreateRecord(descItemIndex) {
        this.props.onCreateRecord(descItemIndex);
    }

    /**
     * Zobrazení detailu rejstříku
     *
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param recordId {Integer} identifikátor typu osoby
     */
    handleDetailRecord(descItemIndex, recordId) {
        this.props.onDetailRecord(descItemIndex, recordId);
    }

    /**
     * Vytvoření nové osoby.
     *
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param partyTypeId {Integer} identifikátor typu osoby
     */
    handleCreateParty(descItemIndex, partyTypeId) {
        this.props.onCreateParty(descItemIndex, partyTypeId);
    }

    /**
     * Zobrazení detailu osoby
     *
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param partyId {Integer} identifikátor osoby
     */
    handleDetailParty(descItemIndex, partyId) {
        this.props.onDetailParty(descItemIndex, partyId);
    }

    /**
     * Změna hodnoty specifikace atributu.
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param e {Object} event od prvku
     */
    handleChangeSpec(descItemIndex, e) {
        var specId
        if (typeof e.target.value !== 'undefined' && e.target.value !== null && e.target.value !== '') {
            specId = Number(e.target.value)
        } else {
            specId = ''
        }

        this.props.onChangeSpec(descItemIndex, specId);
    }

    /**
     * Opuštení focusu hodnoty atributu.
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     */
    handleBlur(descItemIndex) {
        this.props.onBlur(descItemIndex);
    }

    /**
     * Získání focusu na hodnotu atributu.
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     */
    handleFocus(descItemIndex) {
        this.props.onFocus(descItemIndex);
    }

    cancelDragging(e) {
        e.stopPropagation();
        e.preventDefault();
        return false;
    }

    handleDragStart(e) {
        const {fundId, userDetail} = this.props

        // Pokud nemá právo na pořádání, nelze provádět akci
        if (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            return this.cancelDragging(e)
        }

        // Pokud je AS uzavřené, nelze dělat DND
        if (this.props.closed) {
            return this.cancelDragging(e)
        }

        // Pokud nekliknul na dragger, nelze přesouvat
        var drgs = e.target.getElementsByClassName('dragger')
        if (drgs.length !== 1) {
            return this.cancelDragging(e)
        }

        // Nelze přesouvat neuložené položky
        var index = e.currentTarget.dataset.id
        if (typeof this.props.descItemType.descItems[index].id === 'undefined') {
            return this.cancelDragging(e)
        }

        var draggerRect = drgs[0].getBoundingClientRect();
        var clickOnDragger = (e.clientX >= draggerRect.left && e.clientX <= draggerRect.right
            && e.clientY >= draggerRect.top && e.clientY <= draggerRect.bottom)
        if (!clickOnDragger) {
            return this.cancelDragging(e)
        }

        this.dragged = e.currentTarget;
        this.prevDraggedStyleDisplay = this.dragged.style.display
        e.dataTransfer.effectAllowed = 'move';

        // Firefox requires dataTransfer data to be set
        e.dataTransfer.setData("text/html", e.currentTarget);
    }

    handleDragEnd(e) {
        //this.dragged.style.display = "block";
        this.dragged.style.display = this.prevDraggedStyleDisplay

        this.removePlaceholder()

        if (!this.over || !this.dragged) {
            return
        }

        // Update data
        var from = Number(this.dragged.dataset.id);
        var to = Number(this.over.dataset.id);
        if(from < to) to--;
        if(this.nodePlacement == "after") to++;
        //console.log(from, to);
        if (from !== to) {
            this.props.onChangePosition(from, to);
        }
    }

    hasClass(elem, klass) {
         return (" " + elem.className + " ").indexOf(" " + klass + " ") > -1;
    }

    isUnderContainer(el, container) {
        while (el !== null) {
            if (el === container) {
                return true;
            }
            el = el.parentNode;
        }
        return false
    }

    removePlaceholder() {
        placeholder.parentNode === this.dragged.parentNode.parentNode && this.dragged.parentNode.parentNode.removeChild(placeholder)
    }

    handleDragLeave(e) {
        e.preventDefault();
        this.over = null;
        this.dragged && this.removePlaceholder()
        return
    }

    handleDragOver(e) {
        e.preventDefault();

        if (!this.dragged) {
            e.dataTransfer.dropEffect = "none";
            return
        }

        this.dragged.style.display = "none";

        var dragOverContainer = ReactDOM.findDOMNode(this.refs.dragOverContainer);
        if (!this.isUnderContainer(e.target, dragOverContainer)) {
            e.dataTransfer.dropEffect = "none";
            this.over = null;
            this.removePlaceholder()
            return
        }

        if (e.target.className == "placeholder") return;

        var realTarget = e.target;
        var found = false;
        while (realTarget !== null) {
            if (typeof realTarget.dataset.id !== 'undefined') {
                found = true;
                break
            }
            realTarget = realTarget.parentNode
        }

        if (!found) {
            return
        }

        // Over chceme na div s datasetem
        this.over = realTarget;

        // Inside the dragOver method - chceme az na shortcuts
        var useTarget = realTarget.parentNode
        var parent = useTarget.parentNode;
        var overRect = this.over.getBoundingClientRect();
        var height2 = (overRect.bottom - overRect.top) / 2;

        if (e.clientY < overRect.top + height2) {
            this.nodePlacement = "before"
            parent.insertBefore(placeholder, useTarget);
        } else if (e.clientY >= overRect.top + height2) {
            this.nodePlacement = "after";
            parent.insertBefore(placeholder, useTarget.nextElementSibling);
        } else {

        }
    }

    handleCoordinatesUploadButtonClick() {
        this.refs.uploadInput.getInputDOMNode().click();
    }

    handleCoordinatesUpload(e) {
        const fileList = e.target.files;

        if (fileList.length != 1) {
            return;
        }
        const file = fileList[0];

        this.props.onCoordinatesUpload(file);
        e.target.value = null;
    }


    /**
     * Renderování hodnoty atributu.
     * @param descItemType {Object} atribut
     * @param descItem {Object} objekt hodnoty atributu
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param actions {Array} pole akcí
     * @param locked {Boolean} je atribut uzamčen?
     * @return {Object} view
     */
    renderDescItem(descItemType, descItem, descItemIndex, actions, locked) {
        const {refType, fundId, infoType, singleDescItemTypeEdit, rulDataType, calendarTypes, packets, packetTypes, versionId} = this.props;

        var cls = 'desc-item-type-desc-item-container';
        if (actions.length > 0) {
            cls += ' with-action';
        }
        if (infoType.rep === 1) {
            cls += ' draggable-desc-items';
        }

        var parts = [];
        var partsCls = 'desc-item-value-container';

        var key = descItem.formKey;

        if (refType.useSpecification) {
            parts.push(
                this.renderDescItemSpec('spec_' + key, descItem, descItemIndex, locked)
            );
            if (rulDataType.code != 'ENUM') {
                partsCls += ' desc-item-spec-and-value';
            }
        }

        var descItemProps = {
            descItem: descItem,
            onChange: this.handleChange.bind(this, descItemIndex),
            onBlur: this.handleBlur.bind(this, descItemIndex),
            onFocus: this.handleFocus.bind(this, descItemIndex),
            locked: locked,
            ref: key
        }

        var dragProps;
        if (/MSIE 10/i.test(navigator.userAgent) || /MSIE 9/i.test(navigator.userAgent) || /rv:11.0/i.test(navigator.userAgent) || /Edge\/12./i.test(navigator.userAgent)) {
            dragProps = {};
        } else {
            dragProps = {
                'data-id': descItemIndex,
                draggable: infoType.rep === 1,
                onDragStart: this.handleDragStart,
                onDragEnd: this.handleDragEnd,
            }
        }



        var itemComponentKey = 'value_' + key;
        switch (rulDataType.code) {
            case 'PARTY_REF':
                parts.push(<DescItemPartyRef key={itemComponentKey}
                    {...descItemProps}
                    singleDescItemTypeEdit={singleDescItemTypeEdit}
                    onDetail={this.handleDetailParty.bind(this, descItemIndex)}
                    onCreateParty={this.handleCreateParty.bind(this, descItemIndex)}
                    versionId={versionId}
                    />)
                break;
            case 'RECORD_REF':
                parts.push(<DescItemRecordRef key={itemComponentKey}
                    {...descItemProps}
                    singleDescItemTypeEdit={singleDescItemTypeEdit}
                    onDetail={this.handleDetailRecord.bind(this, descItemIndex)}
                    onCreateRecord={this.handleCreateRecord.bind(this, descItemIndex)}
                    versionId={versionId}
                    />)
                break;
            case 'PACKET_REF':
                parts.push(<DescItemPacketRef key={itemComponentKey}
                    {...descItemProps}
                    singleDescItemTypeEdit={singleDescItemTypeEdit}
                    packets={packets}
                    fundId={fundId}
                    packetTypes={packetTypes}
                    />)
                break;
            case 'UNITDATE':
                parts.push(<DescItemUnitdate key={itemComponentKey}
                    {...descItemProps}
                    calendarTypes={calendarTypes}
                    />)
                break;
            case 'UNITID':
                parts.push(<DescItemUnitid key={itemComponentKey}
                    {...descItemProps}
                    />)
                break;

            case 'STRING':
                parts.push(<DescItemString key={itemComponentKey}
                    {...descItemProps}
                    />)
                break;
            case 'FORMATTED_TEXT':
            case 'TEXT':
                parts.push(<DescItemText key={itemComponentKey}
                    {...descItemProps}
                    />)
                break;
            case 'DECIMAL':
                parts.push(<DescItemDecimal key={itemComponentKey}
                    {...descItemProps}
                    />)
                break;
            case 'INT':
                parts.push(<DescItemInt key={itemComponentKey}
                    {...descItemProps}
                    />)
                break;
            case 'COORDINATES':
                parts.push(<DescItemCoordinates key={itemComponentKey}
                        repeatable={infoType.rep == 1}
                        onUpload={this.handleCoordinatesUpload}
                    {...descItemProps}
                    onDownload={this.props.onCoordinatesDownload.bind(this, descItem.descItemObjectId)}
                    />)
                break;
            case 'ENUM':
                break;
            default:
                parts.push(<div key={itemComponentKey}>-unsupported type {rulDataType.code}-</div>)
        }
        return (
            <Shortcuts key={key} name='DescItem' handler={this.handleDescItemShortcuts.bind(this, descItemIndex)}>
                <div className={cls} {...dragProps}>
                    {infoType.rep == 1 && <div className='dragger'><Icon className="up" glyph="fa-angle-up"/><Icon className="down" glyph="fa-angle-down"/>&nbsp;</div>}
                    <div key="container" className={partsCls}>
                        {parts}
                    </div>
                    {actions.length > 0 && <div key="actions" className='desc-item-action-container'>{actions}</div>}
                </div>
            </Shortcuts>
        )
    }

    getShowDeleteDescItem(descItem) {
        const {fundId, userDetail, refType, infoType, descItemType, closed, locked} = this.props;

        // Pokud nemá právo na pořádání, nelze provádět akci
        if (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            return false
        }

        if (closed || locked) {
            return false
        }

        // ##
        // # Má se zobrazovat ikona mazání z hlediska hodnoty desc item?
        // ##
        if (descItemNeedStore(descItem, refType)) {
            return false
        }

        if (typeof descItem.id !== 'undefined') {   // je na serveru, můžeme smazat
            return true
        }

        if (descItem.touched) { // změnil pole, ale není v DB, je možné ji smazat
            return true
        }

        // Mazat lze jen hodnota, která není povinná z hlediska specifikace
        if (refType.useSpecification && hasDescItemTypeValue(refType.dataType) && typeof descItem.descItemSpecId !== 'undefined' && descItem.descItemSpecId !== '') {
            const infoSpec = infoType.descItemSpecsMap[descItem.descItemSpecId]
            if (infoSpec.type == 'REQUIRED' || infoSpec.type == 'RECOMMENDED') {
                return false
            }
        }

        // ##
        // # Má se zobrazovat ikona mazání z hlediska typu atributu?
        // ##
        /*if (infoType.type == 'REQUIRED' || infoType.type == 'RECOMMENDED') {    // můžeme smazat pouze, pokud má nějakou hodnotu, kterou lze smazat
            var haveDBValue = false
            descItemType.descItems.forEach(descItem => {
                if (typeof descItem.id !== 'undefined') {   // je v db
                    haveDBValue = true
                }
            })
            if (!haveDBValue) {
                return false
            }
        }*/

        return true
    }

    getShowDeleteDescItemType() {
        const {fundId, userDetail, refType, infoType, descItemType, closed} = this.props;

        // Pokud nemá právo na pořádání, nelze provádět akci
        if (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            return false
        }

        if (closed) {
            return false
        }

        if (descItemType.descItems.length === 0) {
            return true
        }

        var descItemsShowDeleteItem = false
        for (var a = 0; a < descItemType.descItems.length; a++) {
            let descItem = descItemType.descItems[a]

            if (descItemNeedStore(descItem, refType)) {
                return false
            }

            if (this.getShowDeleteDescItem(descItem)) {
                descItemsShowDeleteItem = true
            }
        }

        return descItemsShowDeleteItem
    }


    /**
     * Renderování nadpisu atributu - včetně akcí pro atribut.
     * @return {Object} view
     */
    renderLabel() {
        const {fundId, userDetail, descItemCopyFromPrevEnabled, singleDescItemTypeEdit, copy, locked, descItemType, infoType, refType, conformityInfo, closed} = this.props;

        var actions = [];

        // Sestavení akcí
        if (userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            if (!closed && !singleDescItemTypeEdit) {
                actions.push(<NoFocusButton title={i18n('subNodeForm.descItemType.copy')} key="copy" onClick={this.handleDescItemTypeCopy}><Icon className={copy ? 'copy' : 'nocopy'} glyph="fa-files-o" /></NoFocusButton>);
                actions.push(<NoFocusButton disabled={!descItemCopyFromPrevEnabled} title={i18n('subNodeForm.descItemType.copyFromPrev')} key="book" onClick={this.handleDescItemTypeCopyFromPrev}><Icon glyph="fa-book" /></NoFocusButton>);
                actions.push(<NoFocusButton title={i18n('subNodeForm.descItemType.lock')} key="lock" onClick={this.handleDescItemTypeLock}><Icon className={locked ? 'locked' : 'unlocked'}  glyph="fa-lock" /></NoFocusButton>);
            }
        }

        // Zprávy o chybějících položkách
        var missings = conformityInfo.missings[descItemType.id];
        if (missings && missings.length > 0) {
            var messages = missings.map(missing => missing.description);
            var tooltip = <Tooltip id="messages">{messages}</Tooltip>
            actions.push(<OverlayTrigger key="state" placement="right" overlay={tooltip}>
                <div className='btn btn-default'><Icon glyph="fa-exclamation-triangle" /></div>
            </OverlayTrigger>);
        }

        if (this.getShowDeleteDescItemType()) {
            actions.push(<NoFocusButton key="delete" onClick={this.props.onDescItemTypeRemove} title={i18n('subNodeForm.deleteDescItemType')}><Icon glyph="fa-trash" /></NoFocusButton>);
        }

        var titleText = descItemType.name;
        if (refType.description && refType.description.length > 0) {
            if (refType.description != titleText) {
                titleText = [titleText, refType.description].join('\n')
            }
        }

        // Render
        return (
            <div className='desc-item-type-label'>
                <div className='title' title={titleText}>
                    {refType.shortcut}
                </div>
                <div className='actions'>
                    {actions}
                </div>
            </div>
        )
    }

    /**
     * Zapnutí/vypnutí zámku na atributu.
     */
    handleDescItemTypeLock() {
        this.props.onDescItemTypeLock(!this.props.locked);
    }

    /**
     * Zapnutí/vypnutí opakovaného kopírování na atributu.
     */
    handleDescItemTypeCopy() {
        this.props.onDescItemTypeCopy(!this.props.copy);
    }

    /**
     * Akce okamžitého kopírování hodnoty atributu z předcházející JP.
     */
    handleDescItemTypeCopyFromPrev() {
        this.props.onDescItemTypeCopyFromPrev();
    }

    render() {
        const {fundId, userDetail, onDescItemRemove, onDescItemAdd, descItemType, refType, infoType, locked, conformityInfo, closed} = this.props;

        var label = this.renderLabel();

        var addAction;
        if (userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            if (infoType.rep === 1 && !(locked || closed)) {
                if (this.props.rulDataType.code === "COORDINATES") {
                    addAction = <div className='desc-item-type-actions'>
                        <NoFocusButton onClick={onDescItemAdd} title={i18n('subNodeForm.descItemType.title.add')}><Icon glyph="fa-plus" /></NoFocusButton>
                        <NoFocusButton onClick={this.handleCoordinatesUploadButtonClick} title={i18n('subNodeForm.descItemType.title.add')}><Icon glyph="fa-upload" /></NoFocusButton>
                        <Input className="hidden" accept="application/vnd.google-earth.kml+xml" type="file" ref='uploadInput' onChange={this.handleCoordinatesUpload} />
                    </div>
                } else {
                    addAction = <div className='desc-item-type-actions'><NoFocusButton onClick={onDescItemAdd} title={i18n('subNodeForm.descItemType.title.add')}><Icon glyph="fa-plus" /></NoFocusButton></div>
                }
            }
        }

        var showDeleteDescItemType = this.getShowDeleteDescItemType();
        var descItems = descItemType.descItems.map((descItem, descItemIndex) => {
            var actions = [];

            if (infoType.rep === 1) {
                actions.push(<NoFocusButton disabled={!this.getShowDeleteDescItem(descItem)} key="delete" onClick={onDescItemRemove.bind(this, descItemIndex)} title={i18n('subNodeForm.deleteDescItem')}><Icon glyph="fa-times" /></NoFocusButton>);
            }

            var errors = conformityInfo.errors[descItem.descItemObjectId];
            if (errors && errors.length > 0) {
                var messages = errors.map(error => error.description);
                var tooltip = <Tooltip id="info">{messages}</Tooltip>
                actions.push(<OverlayTrigger key="info" placement="left" overlay={tooltip}>
                    <div className='btn btn-default'><Icon glyph="fa-exclamation-triangle" /></div>
                </OverlayTrigger>);
            }

            var canModifyDescItem = !(locked || closed)

            // Pokud nemá právo na pořádání, nelze provádět akci
            if (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
                canModifyDescItem = false
            }

            return this.renderDescItem(descItemType, descItem, descItemIndex, actions, !canModifyDescItem)
        });

        var cls = classNames({
            'desc-item-type': true,
            active: descItemType.hasFocus,
            ['el-' + infoType.width]: true
        });

        return (
            <Shortcuts name='DescItemType' className={cls} handler={this.handleDescItemTypeShortcuts}>
                    {label}
                    <div ref='dragOverContainer' className='desc-item-type-desc-items' onDragOver={this.handleDragOver} onDragLeave={this.handleDragLeave}>
                        {descItems}
                    </div>
                    {addAction}
            </Shortcuts>
        )
    }
};

function mapStateToProps(state) {
    const {userDetail} = state

    return {
        userDetail,
    }
}

DescItemType.propTypes = {
    onChange: React.PropTypes.func.isRequired,
    onChangeSpec: React.PropTypes.func.isRequired,
    onChangePosition: React.PropTypes.func.isRequired,
    onBlur: React.PropTypes.func.isRequired,
    onFocus: React.PropTypes.func.isRequired,
    onCreateParty: React.PropTypes.func.isRequired,
    onCoordinatesDownload: React.PropTypes.func.isRequired,
    onCoordinatesUpload: React.PropTypes.func.isRequired,
    onDetailParty: React.PropTypes.func.isRequired,
    onCreateRecord: React.PropTypes.func.isRequired,
    onDetailRecord: React.PropTypes.func.isRequired,
    onDescItemTypeRemove: React.PropTypes.func.isRequired,
    onDescItemTypeLock: React.PropTypes.func.isRequired,
    onDescItemTypeCopy: React.PropTypes.func.isRequired,
    onDescItemTypeCopyFromPrev: React.PropTypes.func.isRequired,
    onDescItemRemove: React.PropTypes.func.isRequired,
    onDescItemAdd: React.PropTypes.func.isRequired,
    refType: React.PropTypes.object.isRequired,
    infoType: React.PropTypes.object.isRequired,
    descItemType: React.PropTypes.object.isRequired,
    rulDataType: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    locked: React.PropTypes.bool.isRequired,
    closed: React.PropTypes.bool.isRequired,
    copy: React.PropTypes.bool.isRequired,
    conformityInfo: React.PropTypes.object.isRequired,
    versionId: React.PropTypes.number.isRequired,
    fundId: React.PropTypes.number.isRequired,
    userDetail: React.PropTypes.object.isRequired,
}

DescItemType.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

module.exports = connect(mapStateToProps, null, null, { withRef: true })(DescItemType);
