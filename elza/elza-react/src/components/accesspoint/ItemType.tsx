import classNames from 'classnames';
import * as PropTypes from 'prop-types';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

import { connect } from 'react-redux';
import { Shortcuts } from 'react-shortcuts';
import { nodeFormActions } from '../../actions/arr/subNodeForm';
import { WebApi } from '../../actions/index.jsx';
import { i18n, Icon, NoFocusButton, TooltipTrigger, Utils } from '../../components/shared';
import {
    convertValue,
    DataType,
    FragmentTypeVO,
    ItemTypeExt,
    RefType,
    RefTypeExt,
    validate,
} from '../../stores/app/accesspoint/itemForm';
import { DataTypeCode } from '../../stores/app/accesspoint/itemFormInterfaces';
import { indexById } from '../../stores/app/utils';
import { objectById } from '../../stores/app/utils2';
import { Dispatch } from '../../typings/globals';
import { hasDescItemTypeValue } from '../arr/ArrUtils';
import '../arr/nodeForm/AbstractDescItem.scss';
import defaultKeymap from '../arr/nodeForm/DescItemTypeKeymap.jsx';
import { propsEquals, valuesEquals } from '../Utils';
import { ItemFactoryInterface } from './ItemFactoryInterface';
import DescItemTypeSpec from './ItemTypeSpec.jsx';

const placeholder = document.createElement("div");
placeholder.className = "placeholder";

interface FromState {
}

interface DispatchProps {
    dispatch: Dispatch<FromState>;
    userDetail: any;
    fragmentTypes: FragmentTypeVO[];
}

export interface Props {
    typePrefix: string;
    descItemType: ItemTypeExt;
    refType: RefType;
    infoType: RefTypeExt;
    rulDataType: DataType;
    calendarTypes: any;
    structureTypes: {data: {id:number, code: string, name: string}[]};
    onCreateParty: Function;
    onDetailParty: Function;
    onCreateRecord: Function;
    onDetailRecord: Function;
    onDescItemAdd: Function;
    onDescItemRemove: Function;
    onChange: Function;
    onChangePosition: Function;
    onChangeSpec: Function;
    onBlur: Function;
    onFocus: Function;
    onDescItemTypeRemove: Function;
    onDescItemTypeLock: Function;
    onDescItemTypeCopy: Function;
    onDescItemTypeCopyFromPrev: Function;
    showNodeAddons: boolean;
    locked: boolean;
    closed: boolean;
    copy: boolean;
    conformityInfo: {missings: any[], errors: any[]};
    descItemCopyFromPrevEnabled: boolean;
    readMode: boolean;
    strictMode: boolean;
    notIdentified: boolean;
    onDescItemNotIdentified: Function;
    customActions?: React.ReactNode;
    descItemFactory: ItemFactoryInterface;
    hideDelete?: boolean,
    draggable?: boolean
}

interface ItemFormClassState {
}

/**
 * Atribut - desc item type.
 */
class ItemTypeClass extends React.Component<DispatchProps & Props, ItemFormClassState>  {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };

    static defaultProps = {
        draggable: true,
        hideDelete: false,
        onCreatePacket: ()=>{}
    };
    shortcutManager: any;
    containers: {};

    readonly state: {
        descItemType: any;
        value?: any;
        error?: any;
    };
    dragged: any;
    private prevDraggedStyleDisplay: any;
    over: any;
    private nodePlacement: string;

    UNSAFE_componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }
    constructor(props) {
        super(props);

        this.containers = {};
        this.state = {
            descItemType: {...props.descItemType}
        };

    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.setState({
            descItemType: {...nextProps.descItemType}
        });
    }

    shouldComponentUpdate(nextProps, nextState) {
        return true;
        const eqProps = ['descItemType', 'rulDataType', 'calendarTypes', 'locked', 'copy', 'readMode'];
        return !propsEquals(this.props, nextProps, eqProps);
    }

    handleDescItemTypeShortcuts(action) {
        console.log("#handleDescItemTypeShortcuts", '[' + action + ']', this);

        const {locked} = this.props;

        switch (action) {
            case 'deleteDescItemType':
                if (!locked && this.getShowDeleteDescItemType()) {
                    this.props.onDescItemTypeRemove()
                }
                break
        }
    }

    handleDescItemShortcuts(descItemIndex, action) {
        //console.log("#handleDescItemShortcuts", '[' + action + ']', this, 'index', descItemIndex);

        const {locked, readMode, descItemType, infoType, onDescItemRemove, onDescItemAdd} = this.props;

        switch (action) {
            case 'addDescItem':
                if (!locked && !readMode) {   // přidávat hodnoty lze jen pokud není zamčeno
                    onDescItemAdd()
                }
                break;
            case 'deleteDescItem':
                if (!locked && !readMode && infoType.rep === 1) {   // mazat hodnoty lze jen u vícehodnotových atributů a není zamčeno
                    const descItem = descItemType.items[descItemIndex];
                    if (this.getShowDeleteDescItem(descItem)) {
                        onDescItemRemove(descItemIndex)
                    }
                }
                break
        }
    }

    focus(item) {
        const {descItemType, refType} = this.props;

        const refPrefix = refType.useSpecification ? 'spec_' : '';

        let ref, descItem;
        if (typeof item.descItemObjectId !== 'undefined' && item.descItemObjectId !== null) {   // konkrétní hodnota
            const indexDesIt = indexById(descItemType.items, item.descItemObjectId, 'descItemObjectId');
            if (indexDesIt === null) {
                return;
            }
            descItem = descItemType.items[indexDesIt];
            ref = this.refs[refPrefix + descItem.formKey]
        } else if (typeof item.descItemIndex !== 'undefined' && item.descItemIndex !== null) {   // konkrétní index
            descItem = descItemType.items[item.descItemIndex];
            ref = this.refs[refPrefix + descItem.formKey]
        } else {    // obecně atribut - dáme na první hodnotu
            descItem = descItemType.items[0];
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
                descItem = ref.getWrappedInstance();
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
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     * @param locked {Boolean}
     * @return {Object} view
     */
    renderDescItemSpec(key, descItem, descItemIndex, locked) {
        const {infoType, refType, readMode, strictMode} = this.props;

        return (
            // @ts-ignore
            <DescItemTypeSpec
                key={key}
                ref={key}
                descItem={descItem}
                locked={locked || descItem.undefined}
                infoType={infoType}
                refType={refType}
                readMode={readMode}
                onChange={this.handleChangeSpec.bind(this, descItemIndex)}
                onBlur={this.handleBlur.bind(this, descItemIndex)}
                onFocus={this.handleFocus.bind(this, descItemIndex)}
                strictMode={strictMode}
                />
        );
    }

    /*
     * Unitdate server validation
     */
    validateUnitdate = (value, descItemIndex)=>{
        return () => {
            WebApi.validateUnitdate(value).then((result)=>{
                const {refType} = this.props;
                let newDescItemType = this.state.descItemType;
                const newDescItem = {...newDescItemType.items[descItemIndex]};

                // validation with added error from server
                let valueServerError;
                if (!result.valid) {
                    valueServerError = result.message;
                }
                newDescItem.error = validate(newDescItem, refType, valueServerError);

                newDescItemType.items[descItemIndex] = newDescItem;
                this.setState({
                    descItemType: newDescItemType,
                    error: result //unitdate validation expects different format
                });
            });
        };
    };

    /**
     * Změna hodnoty atributu.
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     * @param value {Object} nová hodnota atibutu
     */
    handleChange(descItemIndex, value) {
        //this.props.onChange(descItemIndex, value);
        // Switched to local value change. Value update in "handleBlur"

        const {rulDataType, refType} = this.props;
        let newDescItemType = this.state.descItemType;

        // Convert value to a value compatible with specified data type
        const descItem = {...newDescItemType.items[descItemIndex]};
        // convert value only if it exists
        const convertedValue = value ? convertValue(value, descItem, rulDataType.code) : {value};
        const touched = convertedValue.touched || !valuesEquals(convertedValue.value, descItem.prevValue);
        const newDescItem = {
            ...descItem,
            ...convertedValue,
            touched
        };
        // Unitdate server validation
        if (rulDataType.code === "UNITDATE") {
            // debouncing validation request
            if (newDescItem.validateTimer) {
                clearTimeout(descItem.validateTimer);
            }
            newDescItem.validateTimer = setTimeout(this.validateUnitdate(newDescItem.value, descItemIndex), 250);
        }
        // newDescItem validation
        const error = validate(newDescItem, refType);
        newDescItem.error = error;

        // Replace the original descItem
        newDescItemType.items[descItemIndex] = newDescItem;

        this.setState({
            descItemType: newDescItemType,
            value,
            error
        });
    }

    /**
     * Vytvoření nového hesla.
     *
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     */
    handleCreateRecord(descItemIndex) {
        this.props.onCreateRecord(descItemIndex);
    }

    /**
     * Zobrazení detailu rejstříku
     *
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     * @param recordId {number} identifikátor typu osoby
     */
    handleDetailRecord(descItemIndex, recordId) {
        this.props.onDetailRecord(descItemIndex, recordId);
    }

    /**
     * Vytvoření nové osoby.
     *
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     * @param partyTypeId {number} identifikátor typu osoby
     */
    handleCreateParty(descItemIndex, partyTypeId) {
        this.props.onCreateParty(descItemIndex, partyTypeId);
    }

    /**
     * Zobrazení detailu osoby
     *
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     * @param partyId {number} identifikátor osoby
     */
    handleDetailParty(descItemIndex, partyId) {
        this.props.onDetailParty(descItemIndex, partyId);
    }

    /**
     * Změna hodnoty specifikace atributu.
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     * @param eventOrValue {Object} event nebo hodnota od prvku
     */
    handleChangeSpec(descItemIndex, eventOrValue) {
        const isEvent = !!(eventOrValue && eventOrValue.stopPropagation && eventOrValue.preventDefault);
        const value = isEvent ? eventOrValue.target.value : eventOrValue;

        let specId;
        if (typeof value !== 'undefined' && value !== null && value !== '') {
            specId = Number(value)
        } else {
            specId = ''
        }

        this.props.onChangeSpec(descItemIndex, specId);
    }

    /**
     * Opuštení focusu hodnoty atributu.
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     */
    handleBlur(descItemIndex) {
        const {onBlur, onChange} = this.props;
        const {value, error} = this.state;

        // Calls the onChange in handleBlur to prevent too frequent re-renders
        value && onChange(descItemIndex, value, error);

        onBlur(descItemIndex);

        // resets the modified value and error
        this.setState({
            value:null,
            error:null
        });

        const itemElement = this.containers[descItemIndex];
        // returns back the "draggable" attribute
        itemElement.setAttribute("draggable", true);
    }

    /**
     * Získání focusu na hodnotu atributu.
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     */
    handleFocus(descItemIndex) {
        this.props.onFocus(descItemIndex);
        const itemElement = this.containers[descItemIndex];
        // removes attribute "draggable" due to a bug in Mozilla Firefox,
        // see https://bugzilla.mozilla.org/show_bug.cgi?id=1189486
        // or https://bugzilla.mozilla.org/show_bug.cgi?id=800050
        itemElement.removeAttribute("draggable");
    }

    cancelDragging(e) {
        e.stopPropagation();
        e.preventDefault();
        return false;
    }

    handleDragStart(e): boolean {
        // Pokud je AS uzavřené, nelze dělat DND
        if (this.props.closed) {
            return this.cancelDragging(e)
        }

        // Pokud nekliknul na dragger, nelze přesouvat
        const drgs = e.target.getElementsByClassName('dragger');
        if (drgs.length !== 1) {
            return this.cancelDragging(e)
        }

        // Nelze přesouvat neuložené položky
        const index = e.currentTarget.dataset.id;
        if (typeof this.props.descItemType.items[index].id === 'undefined') {
            return this.cancelDragging(e)
        }

        const draggerRect = drgs[0].getBoundingClientRect();
        const clickOnDragger = (e.clientX >= draggerRect.left && e.clientX <= draggerRect.right
        && e.clientY >= draggerRect.top && e.clientY <= draggerRect.bottom);
        if (!clickOnDragger) {
            return this.cancelDragging(e)
        }

        this.dragged = e.currentTarget;
        this.prevDraggedStyleDisplay = this.dragged.style.display;
        e.dataTransfer.effectAllowed = 'move';

        // Firefox requires dataTransfer data to be set
        e.dataTransfer.setData("text/html", e.currentTarget);
        return true;
    }

    handleDragEnd(e) {
        //this.dragged.style.display = "block";
        this.dragged.style.display = this.prevDraggedStyleDisplay;

        this.removePlaceholder();

        if (!this.over || !this.dragged) {
            return
        }

        // Update data
        let from = Number(this.dragged.dataset.id);
        let to = Number(this.over.dataset.id);
        if (from < to) to--;
        if (this.nodePlacement == "after") to++;
        //console.log(from, to);
        if (from !== to) {
            this.props.onChangePosition(from, to);
        }
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
        this.dragged && this.removePlaceholder();
        return
    }

    handleDragOver(e) {
        e.preventDefault();

        if (!this.dragged) {
            e.dataTransfer.dropEffect = "none";
            return
        }

        this.dragged.style.display = "none";

        const dragOverContainer = ReactDOM.findDOMNode(this.refs.dragOverContainer);
        if (!this.isUnderContainer(e.target, dragOverContainer)) {
            e.dataTransfer.dropEffect = "none";
            this.over = null;
            this.removePlaceholder();
            return
        }

        if (e.target.className == "placeholder") return;

        let realTarget = e.target;
        let found = false;
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
        const useTarget = realTarget.parentNode;
        const parent = useTarget.parentNode;
        const overRect = this.over.getBoundingClientRect();
        const height2 = (overRect.bottom - overRect.top) / 2;

        if (e.clientY < overRect.top + height2) {
            this.nodePlacement = "before";
            parent.insertBefore(placeholder, useTarget);
        } else if (e.clientY >= overRect.top + height2) {
            this.nodePlacement = "after";
            parent.insertBefore(placeholder, useTarget.nextElementSibling);
        } else {

        }
    }

    /**
     * Renders the value of the descItem
     */
    renderDescItemValue(descItem, descItemIndex, locked) {
        const {refType, structureTypes, calendarTypes, rulDataType, descItemFactory, readMode, infoType, typePrefix} = this.props;

        let specName = null;
        if (descItem.descItemSpecId) {
            specName = refType.descItemSpecsMap[descItem.descItemSpecId].name;
        }

        let structureType;
        if(structureTypes){
            structureType = objectById(structureTypes.data, refType.structureTypeId);
        }

        let fragmentType;
        if (refType.fragmentTypeId) {
            fragmentType = objectById(structureTypes.data, refType.fragmentTypeId);
        }

        const additionalProps = {
            [DataTypeCode.PARTY_REF]:{
                itemName: refType.shortcut,
                specName: specName,
                onDetail: (value)=>{this.handleDetailParty(descItemIndex, value);},
                onCreateParty: (value)=>{this.handleCreateParty(descItemIndex, value);}
            },
            [DataTypeCode.RECORD_REF]: {
                itemName: refType.shortcut,
                specName: specName,
                onDetail: (value)=>{this.handleDetailRecord(descItemIndex, value);},
                onCreateRecord: (value)=>{this.handleCreateRecord(descItemIndex);},
            },
            [DataTypeCode.STRUCTURED]:{
                structureTypeCode: structureType ? structureType.code : null,
                structureTypeName: structureType ? structureType.name : null,
            },
            [DataTypeCode.UNITDATE]:{
                calendarTypes: calendarTypes
            },
            [DataTypeCode.INT]:{
                refType
            },
            [DataTypeCode.APFRAG_REF]:{
                fragmentType
            },
            // Neimplmentované typy
            // [DataTypeCode.FILE_REF]: {},
            // [DataTypeCode.JSON_TABLE]:{refType,},
            // [DataTypeCode.COORDINATES]:{}
        };

        const key = descItem.formKey;
        const itemComponentKey = 'value_' + key;

        let descItemProps = {
            hasSpecification: refType.useSpecification,
            descItem: descItem,
            onChange: this.handleChange.bind(this, descItemIndex),
            onBlur: this.handleBlur.bind(this, descItemIndex),
            onFocus: this.handleFocus.bind(this, descItemIndex),
            locked: locked,
            readMode: readMode,
            ref: key,
            cal: infoType.cal && !infoType.calSt,
            typePrefix,
            readOnly: descItem.saving,
            key: itemComponentKey,
            descItemFactory,
            repeatable: infoType.rep == 1,
            ...additionalProps[rulDataType.code]
        };

        return descItemFactory.createItem(rulDataType.code, descItemProps);
    }

    /**
     * Renderování hodnoty atributu.
     * @param descItemType {Object} atribut
     * @param descItem {Object} objekt hodnoty atributu
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     * @param actions {Array} pole akcí
     * @param locked {Boolean} je atribut uzamčen?
     * @return {Object} view
     */
    renderDescItem(descItemType, descItem, descItemIndex, actions, locked) {
        const {refType, readMode, infoType, rulDataType, draggable} = this.props;

        let cls = 'desc-item-type-desc-item-container';
        if (actions.length > 0) {
            cls += ' with-action';
        }
        if (infoType.rep === 1) {
            cls += ' draggable-desc-items';
        }

        const parts : React.ReactNode[] = [];
        let partsCls = 'desc-item-value-container';

        const key = descItem.formKey;

        if (refType.useSpecification) {
            parts.push(
                this.renderDescItemSpec('spec_' + key, descItem, descItemIndex, locked)
            );
            if (rulDataType.code != 'ENUM') {
                partsCls += ' desc-item-spec-and-value';
            }
        }
        partsCls += " dt" + rulDataType.code;



        let dragProps;
        if (Utils.detectIE() || readMode || !draggable) {
            dragProps = {};
        } else {
            dragProps = {
                'data-id': descItemIndex,
                draggable: infoType.rep === 1,
                onDragStart: this.handleDragStart,
                onDragEnd: this.handleDragEnd,
            }
        }

        // render value (if not enum)
        if(rulDataType.code != "ENUM"){
            parts.push(this.renderDescItemValue(descItem, descItemIndex, locked));
        }

        return (
            <Shortcuts key={key} name='DescItem' handler={this.handleDescItemShortcuts.bind(this, descItemIndex)} alwaysFireHandler global>
                <div key="container" className={cls} {...dragProps} ref={(ref)=>{this.containers[descItemIndex] = ref;}}>
                    {!readMode && infoType.rep == 1 && draggable &&
                      <div className='dragger'>
                            <Icon className="up" glyph="fa-angle-up"/>
                            <Icon className="down" glyph="fa-angle-down"/>&nbsp;
                    </div>}

                    <div key="container" className={partsCls}>
                      {parts}
                    </div>
                    {actions.length > 0 && <div key="actions" className='desc-item-action-container'>{actions}</div>}
                </div>
            </Shortcuts>
        )
    }

    getShowDeleteDescItem(descItem) {
        const {refType, infoType, closed, locked, readMode} = this.props;

        if (closed || locked || readMode) {
            return false
        }

        // ##
        // # Má se zobrazovat ikona mazání z hlediska hodnoty desc item?
        // ##
        if (nodeFormActions.descItemNeedStore(descItem, refType)) {
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

        return true
    }

    getShowDeleteDescItemType() {
        const {refType, closed, readMode, hideDelete} = this.props;
        const {descItemType} = this.state;

        if (closed || readMode || hideDelete) {
            return false
        }

        if (descItemType.items.length === 0) {
            return true
        }

        let descItemsShowDeleteItem = false
        for (let a = 0; a < descItemType.items.length; a++) {
            let descItem = descItemType.items[a]

            if (nodeFormActions.descItemNeedStore(descItem, refType)) {
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
        const {
            showNodeAddons, descItemCopyFromPrevEnabled,
            copy, locked, infoType, refType, conformityInfo, closed, readMode, notIdentified,
            rulDataType, onDescItemNotIdentified, customActions
        } = this.props;
        const {descItemType} = this.state;

        const actions : React.ReactNode[] = [];

        // Sestavení akcí
        if (showNodeAddons && !closed && !readMode) {
            actions.push(
                <NoFocusButton disabled={!descItemCopyFromPrevEnabled}
                               title={i18n('subNodeForm.descItemType.copyFromPrev')} key="book"
                               onClick={this.handleDescItemTypeCopyFromPrev}><Icon
                    glyph="fa-paste"/></NoFocusButton>,
                <NoFocusButton title={i18n('subNodeForm.descItemType.copy')} key="copy"
                               onClick={this.handleDescItemTypeCopy}><Icon className={copy ? 'copy' : 'nocopy'}
                                                                           glyph="fa-files-o"/></NoFocusButton>,
                <NoFocusButton title={i18n('subNodeForm.descItemType.lock')} key="lock"
                               onClick={this.handleDescItemTypeLock}><Icon
                    className={locked ? 'locked' : 'unlocked'} glyph="fa-lock"/></NoFocusButton>
            );
        }

        const showDeleteButton = this.getShowDeleteDescItemType();
        if (showDeleteButton) {
            actions.push(<NoFocusButton key="delete" onClick={this.props.onDescItemTypeRemove}
                                        title={i18n('subNodeForm.deleteDescItemType')}><Icon
                glyph="fa-trash"/></NoFocusButton>);
        }

        // Zprávy o chybějících položkách
        const missings = conformityInfo.missings[descItemType.id];
        if (missings && missings.length > 0) {
            const messages = missings.map(missing => missing.description);
            const tooltip = <div>{messages}</div>
            actions.push(<TooltipTrigger
                key="state"
                content={tooltip}
                holdOnHover
                placement="auto"
            >
                <div className='btn btn-default'><Icon className="messages" glyph="fa-exclamation-triangle"/></div>
            </TooltipTrigger>);
        }

        let titleText = descItemType.name;
        if (refType.description && refType.description.length > 0) {
            if (refType.description != titleText) {
                if (titleText && titleText.length > 0) {
                    titleText = [titleText, refType.description].join('<br/>');
                } else {
                    titleText = refType.description;
                }
            }
        }
        const tooltip = <div dangerouslySetInnerHTML={{__html: titleText}}></div>

        if (infoType.rep === 1 && !(locked || closed || readMode)) {
            const {onDescItemAdd} = this.props;
            actions.push(<NoFocusButton key="add" onClick={onDescItemAdd} title={i18n('subNodeForm.descItemType.title.add')}>
                <Icon glyph="fa-plus"/>
            </NoFocusButton>);
        }

        if (!closed && !readMode && !infoType.rep && infoType.ind && rulDataType.code !== 'ENUM') {
            actions.push(<NoFocusButton key="notIdentified" onClick={() => {
                if (descItemType.items.length === 1) {
                    onDescItemNotIdentified(0, descItemType.items[0]);
                }
            }}
                                        title={i18n('subNodeForm.descItemType.title.notIdentified')}><Icon
                glyph="fa-low-vision" className={notIdentified ? 'notIdentified' : 'identified'} /></NoFocusButton>);
        }

        // Render
        return (
            <div key="label" className='desc-item-type-label'>
                <TooltipTrigger
                    key="title"
                    className='title'
                    content={tooltip}
                    holdOnHover
                    placement="vertical"
                >
                    {refType.shortcut}
                </TooltipTrigger>
                <div key="actions" className='actions'>
                    {actions}
                    {customActions}
                </div>
                {descItemType.items.filter(i => i.touched).length > 0 &&
                <span key="edited" className="desc-item-type-edited">{i18n('subNodeForm.descItem.edited')}</span>}
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
        const {onDescItemRemove, rulDataType, infoType,
            locked, conformityInfo, closed, readMode, onDescItemNotIdentified} = this.props;
        const {descItemType} = this.state;

        const label = this.renderLabel();
        const items = descItemType.items.map((descItem, descItemIndex) => {
            const actions : React.ReactNode[] = [];

            if (!readMode && infoType.rep === 1 && !(infoType.cal && !infoType.calSt)) {
                if (rulDataType.code !== 'ENUM' && infoType.ind) {
                    actions.push(<NoFocusButton key="notIdentified"
                                                className={descItem.undefined ? 'notIdentified' : 'identified'}
                                                onClick={() => onDescItemNotIdentified(descItemIndex, descItem)}
                                                title={i18n('subNodeForm.descItemType.title.notIdentified')}><Icon
                        glyph="fa-low-vision"/></NoFocusButton>);
                }
                actions.push(<NoFocusButton disabled={!this.getShowDeleteDescItem(descItem)} key="delete"
                                            onClick={onDescItemRemove.bind(this, descItemIndex)}
                                            title={i18n('subNodeForm.deleteDescItem')}><Icon
                    glyph="fa-times"/></NoFocusButton>);
            }

            const errors = conformityInfo.errors[descItem.descItemObjectId];
            if (errors && errors.length > 0) {
                const messages = errors.map(error => error.description);
                const tooltip = <div>{messages}</div>;
                actions.push(<TooltipTrigger
                    key="info"
                    content={tooltip}
                    holdOnHover
                    placement="auto"
                    showDelay={1}
                >
                    <div className='btn btn-default'><Icon glyph="fa-exclamation-triangle"/></div>
                </TooltipTrigger>);
            }

            let canModifyDescItem = !(locked || closed);

            return this.renderDescItem(descItemType, descItem, descItemIndex, actions, !canModifyDescItem)
        });

        const cls = classNames({
            'read-mode': readMode,
            'desc-item-type': true,
            active: descItemType.hasFocus,
            ['el-' + infoType.width]: true
        });

        return (
            <Shortcuts name='DescItemType' className={cls} handler={this.handleDescItemTypeShortcuts} global alwaysFireHandler>
                {label}
                <div ref='dragOverContainer' className='desc-item-type-desc-items' onDragOver={this.handleDragOver}
                     onDragLeave={this.handleDragLeave}>
                    {items}
                </div>
            </Shortcuts>
        )
    }
}

function mapStateToProps(state, ownProps: Props) {
    const {userDetail} = state;

    return {
        userDetail
    }
}

export default connect(mapStateToProps)(ItemTypeClass as any);
