import React from 'react';
import ReactDOM from 'react-dom';
import {
    AbstractReactComponent,
    FormInput,
    i18n,
    Icon,
    NoFocusButton,
    TooltipTrigger,
    Utils,
} from '../../../components/shared';
import {connect} from 'react-redux';
import {objectEqualsDiff, propsEquals, valuesEquals} from '../../Utils';
import {nodeFormActions} from '../../../actions/arr/subNodeForm.jsx';
import {hasDescItemTypeValue} from '../../../components/arr/ArrUtils.jsx';
import {indexById} from '../../../stores/app/utils.jsx';
import classNames from 'classnames';
import * as perms from '../../../actions/user/Permission.jsx';
import {Shortcuts} from 'react-shortcuts';
import DescItemTypeSpec from './DescItemTypeSpec';
import {PropTypes} from 'prop-types';
import defaultKeymap from './DescItemTypeKeymap.jsx';
import './AbstractDescItem.scss';
import {convertValue, validate} from '../../../stores/app/arr/subNodeForm.jsx';
import {WebApi} from '../../../actions/index';
import objectById from '../../../shared/utils/objectById';

const placeholder = document.createElement('div');
placeholder.className = 'placeholder';

/**
 * Atribut - desc item type.
 *
 * TODO @randak výcenásobný attr nefunguje kvůli lokálnímu state!!! takže OHACKY které jedou do store a zapisují tu jsou kvůli @randak
 */
class DescItemType extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    static defaultProps = {
        draggable: true,
        hideDelete: false,
        onCreatePacket: () => {},
    };

    static propTypes = {
        onChange: PropTypes.func.isRequired,
        onChangeSpec: PropTypes.func.isRequired,
        onChangePosition: PropTypes.func.isRequired,
        onBlur: PropTypes.func.isRequired,
        onFocus: PropTypes.func.isRequired,
        onCoordinatesDownload: PropTypes.func.isRequired,
        onJsonTableDownload: PropTypes.func.isRequired,
        onCoordinatesUpload: PropTypes.func.isRequired,
        onCreateRecord: PropTypes.func.isRequired,
        onDetailRecord: PropTypes.func.isRequired,
        onCreatePacket: PropTypes.func.isRequired,
        onCreateFile: PropTypes.func.isRequired,
        onFundFiles: PropTypes.func.isRequired,
        onDescItemTypeRemove: PropTypes.func.isRequired,
        onDescItemTypeLock: PropTypes.func.isRequired,
        onDescItemTypeCopy: PropTypes.func.isRequired,
        onDescItemTypeCopyFromPrev: PropTypes.func.isRequired,
        onDescItemRemove: PropTypes.func.isRequired,
        onDescItemAdd: PropTypes.func.isRequired,
        refType: PropTypes.object.isRequired,
        infoType: PropTypes.object.isRequired,
        descItemType: PropTypes.object.isRequired,
        rulDataType: PropTypes.object.isRequired,
        calendarTypes: PropTypes.object.isRequired,
        structureTypes: PropTypes.object.isRequired,
        locked: PropTypes.bool.isRequired,
        hideDelete: PropTypes.bool,
        readMode: PropTypes.bool.isRequired,
        arrPerm: PropTypes.bool.isRequired,
        notIdentified: PropTypes.bool.isRequired,
        onDescItemNotIdentified: PropTypes.func.isRequired,
        closed: PropTypes.bool.isRequired,
        copy: PropTypes.bool.isRequired,
        conformityInfoMissings: PropTypes.array,
        versionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        userDetail: PropTypes.object.isRequired,
        showNodeAddons: PropTypes.bool.isRequired,
        strictMode: PropTypes.bool.isRequired,
        descItemFactory: PropTypes.func.isRequired,
    };

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    /** Object pro dynamické ref */
    refObjects = {};

    /**  **/
    refDragOverContainer = null;

    constructor(props) {
        super(props);

        this.containers = {};
        this.state = {
            descItemType: {...props.descItemType},
        };
        this.bindMethods(
            'focus',
            'getShowDeleteDescItem',
            'getShowDeleteDescItemType',
            'handleBlur',
            'handleChange',
            'handleChangeSpec',
            'handleCoordinatesUpload',
            'handleCoordinatesUploadButtonClick',
            'handleCreateRecord',
            'handleDescItemShortcuts',
            'handleDescItemTypeCopy',
            'handleDescItemTypeCopyFromPrev',
            'handleDescItemTypeLock',
            'handleDescItemTypeShortcuts',
            'handleDetailRecord',
            'handleDragEnd',
            'handleDragLeave',
            'handleDragOver',
            'handleDragStart',
            'handleFocus',
            'handleJsonTableUploadButtonClick',
            'handleJsonTableUploadUpload',
            'handleSwitchCalculating',
            'removePlaceholder',
            'renderDescItem',
            'renderLabel',
        );
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const newDescItemType = {...nextProps.descItemType};
        const {formKey, value} = this.state;
        if (formKey) {
            for (let a = 0; a < newDescItemType.descItems.length; a++) {
                const di = newDescItemType.descItems[a];
                // Nutno nasetovat všechny klíče reálné hodnoty do DescItemu
                for (let key of Object.keys(value)) {
                    di[key] = value[key];
                }
                di.touched = true;
                break;
            }
        }

        this.setState({
            descItemType: newDescItemType,
        });
    }

    shouldComponentUpdate(nextProps, nextState) {
        return true;
        if (this.state !== nextState) {
            console.log('[DescItemType]#[state]##############', nextProps.refType.name);
            return true;
        }
        console.log('[DescItemType]###############', nextProps.refType.name);
        // return !propsEquals(this.props, nextProps, undefined, true);
        return !objectEqualsDiff(this.props, nextProps, undefined, '', true);
    }
    // shouldComponentUpdate(nextProps, nextState) {
    //     return true;
    //     // const eqProps = ['descItemType', 'rulDataType', 'calendarTypes', 'locked', 'copy', 'readMode'];
    //     // return !propsEquals(this.props, nextProps, eqProps);
    // }

    handleDescItemTypeShortcuts(action) {
        console.log('#handleDescItemTypeShortcuts', '[' + action + ']', this);

        const {locked} = this.props;

        switch (action) {
            case 'deleteDescItemType':
                if (!locked && this.getShowDeleteDescItemType()) {
                    this.props.onDescItemTypeRemove();
                }
                break;
            default:
                break;
        }
    }

    handleDescItemShortcuts(descItemIndex, action) {
        //console.log("#handleDescItemShortcuts", '[' + action + ']', this, 'index', descItemIndex);

        const {locked, readMode, descItemType, infoType, onDescItemRemove, onDescItemAdd} = this.props;

        switch (action) {
            case 'addDescItem':
                if (!locked && !readMode) {
                    // přidávat hodnoty lze jen pokud není zamčeno
                    onDescItemAdd();
                }
                break;
            case 'deleteDescItem':
                if (!locked && !readMode && infoType.rep === 1) {
                    // mazat hodnoty lze jen u vícehodnotových atributů a není zamčeno
                    const descItem = descItemType.descItems[descItemIndex];
                    if (this.getShowDeleteDescItem(descItem)) {
                        onDescItemRemove(descItemIndex);
                    }
                }
                break;
            default:
                break;
        }
    }

    focus(item) {
        const {descItemType, refType} = this.props;

        const refPrefix = refType.useSpecification ? 'spec_' : '';

        let ref, descItem;
        if (typeof item.descItemObjectId !== 'undefined' && item.descItemObjectId !== null) {
            // konkrétní hodnota
            descItem =
                descItemType.descItems[indexById(descItemType.descItems, item.descItemObjectId, 'descItemObjectId')];
            ref = this.refObjects[refPrefix + descItem.formKey];
        } else if (typeof item.descItemIndex !== 'undefined' && item.descItemIndex !== null) {
            // konkrétní index
            descItem = descItemType.descItems[item.descItemIndex];
            ref = this.refObjects[refPrefix + descItem.formKey];
        } else {
            // obecně atribut - dáme na první hodnotu
            descItem = descItemType.descItems[0];
            ref = this.refObjects[refPrefix + descItem.formKey];
        }

        if (ref) {
            if (refType.useSpecification) {
                // focus bude na specifikaci
                if (ref.focus) {
                    ref.focus();
                } else {
                    console.error('Cannot find focus method for desc item', ref);
                }
            } else {
                // focus bude na vlastní hodnotu atributu
                descItem = ref;
                if (ref.getWrappedInstance) {
                    descItem = ref.getWrappedInstance();
                }
                if (descItem.focus) {
                    descItem.focus();
                } else {
                    console.error('Cannot find focus method for desc item', descItem);
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
            <DescItemTypeSpec
                key={key}
                ref={ref => (this.refObjects[key] = ref)}
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
    validateUnitdate = (value, descItemIndex) => {
        return () => {
            WebApi.validateUnitdate(value).then(result => {
                const {refType} = this.props;
                let newDescItemType = this.state.descItemType;
                const newDescItem = {...newDescItemType.descItems[descItemIndex]};

                // validation with added error from server
                let valueServerError;
                if (!result.valid) {
                    valueServerError = result.message;
                }
                newDescItem.error = validate(newDescItem, refType, valueServerError);

                newDescItemType.descItems[descItemIndex] = newDescItem;
                this.setState({
                    descItemType: newDescItemType,
                    error: result, //unitdate validation expects different format
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
        const descItem = {...newDescItemType.descItems[descItemIndex]};
        // convert value only if it exists
        const convertedValue = value ? convertValue(value, descItem, rulDataType.code) : {value};
        const touched = convertedValue.touched || !valuesEquals(convertedValue.value, descItem.prevValue);
        const newDescItem = {
            ...descItem,
            ...convertedValue,
            touched,
        };
        // Unitdate server validation
        if (rulDataType.code === 'UNITDATE') {
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
        newDescItemType.descItems[descItemIndex] = newDescItem;

        this.setState({
            descItemType: newDescItemType,
            value: convertedValue,
            formKey: descItem.formKey,
            error,
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
     * Vytvoření nového obalu.
     *
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     */
    handleCreatePacket(descItemIndex) {
        this.props.onCreatePacket(descItemIndex);
    }

    /**
     * Vytvoření nového souboru.
     *
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     */
    handleCreateFile(descItemIndex) {
        this.props.onCreateFile(descItemIndex);
    }

    /**
     * Zobrazení seznamu souborů
     *
     * @param descItemIndex {number} index hodnoty atributu v seznamu
     */
    handleFundFiles(descItemIndex) {
        this.props.onFundFiles(descItemIndex);
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
            specId = Number(value);
        } else {
            specId = '';
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
        value &&
            onChange(
                descItemIndex,
                value && Object.keys(value).length === 1 && value.value ? value.value : value,
                error,
            );

        onBlur(descItemIndex);

        // resets the modified value and error
        this.setState({
            value: null,
            formKey: null,
            error: null,
        });

        const itemElement = this.containers[descItemIndex];
        // returns back the "draggable" attribute
        itemElement.setAttribute('draggable', true);
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
        itemElement.removeAttribute('draggable');
    }

    cancelDragging(e) {
        e.stopPropagation();
        e.preventDefault();
        return false;
    }

    handleDragStart(e) {
        const {fundId, userDetail} = this.props;

        // Pokud nemá právo na pořádání, nelze provádět akci
        if (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            return this.cancelDragging(e);
        }

        // Pokud je AS uzavřené, nelze dělat DND
        if (this.props.closed) {
            return this.cancelDragging(e);
        }

        // Pokud nekliknul na dragger, nelze přesouvat
        const drgs = e.target.getElementsByClassName('dragger');
        if (drgs.length !== 1) {
            return this.cancelDragging(e);
        }

        // Nelze přesouvat neuložené položky
        const index = e.currentTarget.dataset.id;
        if (typeof this.props.descItemType.descItems[index].id === 'undefined') {
            return this.cancelDragging(e);
        }

        const draggerRect = drgs[0].getBoundingClientRect();
        const clickOnDragger =
            e.clientX >= draggerRect.left &&
            e.clientX <= draggerRect.right &&
            e.clientY >= draggerRect.top &&
            e.clientY <= draggerRect.bottom;
        if (!clickOnDragger) {
            return this.cancelDragging(e);
        }

        this.dragged = e.currentTarget;
        this.prevDraggedStyleDisplay = this.dragged.style.display;
        e.dataTransfer.effectAllowed = 'move';

        // Firefox requires dataTransfer data to be set
        e.dataTransfer.setData('text/html', e.currentTarget);
    }

    handleDragEnd(e) {
        //this.dragged.style.display = "block";
        this.dragged.style.display = this.prevDraggedStyleDisplay;

        this.removePlaceholder();

        if (!this.over || !this.dragged) {
            return;
        }

        // Update data
        let from = Number(this.dragged.dataset.id);
        let to = Number(this.over.dataset.id);
        if (from < to) to--;
        if (this.nodePlacement == 'after') to++;
        //console.log(from, to);
        if (from !== to) {
            this.props.onChangePosition(from, to);
        }
    }

    hasClass(elem, klass) {
        return (' ' + elem.className + ' ').indexOf(' ' + klass + ' ') > -1;
    }

    isUnderContainer(el, container) {
        while (el !== null) {
            if (el === container) {
                return true;
            }
            el = el.parentNode;
        }
        return false;
    }

    removePlaceholder() {
        placeholder.parentNode === this.dragged.parentNode.parentNode &&
            this.dragged.parentNode.parentNode.removeChild(placeholder);
    }

    handleDragLeave(e) {
        e.preventDefault();
        this.over = null;
        this.dragged && this.removePlaceholder();
        return;
    }

    handleDragOver(e) {
        e.preventDefault();

        if (!this.dragged) {
            e.dataTransfer.dropEffect = 'none';
            return;
        }

        this.dragged.style.display = 'none';

        const dragOverContainer = ReactDOM.findDOMNode(this.refDragOverContainer);
        if (!this.isUnderContainer(e.target, dragOverContainer)) {
            e.dataTransfer.dropEffect = 'none';
            this.over = null;
            this.removePlaceholder();
            return;
        }

        if (e.target.className == 'placeholder') return;

        let realTarget = e.target;
        let found = false;
        while (realTarget !== null) {
            if (typeof realTarget.dataset.id !== 'undefined') {
                found = true;
                break;
            }
            realTarget = realTarget.parentNode;
        }

        if (!found) {
            return;
        }

        // Over chceme na div s datasetem
        this.over = realTarget;

        // Inside the dragOver method - chceme az na shortcuts
        const useTarget = realTarget.parentNode;
        const parent = useTarget.parentNode;
        const overRect = this.over.getBoundingClientRect();
        const height2 = (overRect.bottom - overRect.top) / 2;

        if (e.clientY < overRect.top + height2) {
            this.nodePlacement = 'before';
            parent.insertBefore(placeholder, useTarget);
        } else if (e.clientY >= overRect.top + height2) {
            this.nodePlacement = 'after';
            parent.insertBefore(placeholder, useTarget.nextElementSibling);
        } else {
        }
    }

    handleCoordinatesUploadButtonClick() {
        ReactDOM.findDOMNode(this.refUploadInput.refs.input).click();
    }

    handleJsonTableUploadButtonClick() {
        ReactDOM.findDOMNode(this.refUploadInput.refs.input).click();
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

    handleJsonTableUploadUpload(e) {
        const fileList = e.target.files;

        if (fileList.length != 1) {
            return;
        }
        const file = fileList[0];

        this.props.onJsonTableUpload(file);
        e.target.value = null;
    }

    /**
     * Renders the value of the descItem
     */
    renderDescItemValue = (descItem, descItemIndex, locked) => {
        const {
            refType,
            singleDescItemTypeEdit,
            structureTypes,
            calendarTypes,
            rulDataType,
            descItemFactory,
            readMode,
            infoType,
            typePrefix,
            versionId,
            fundId,
        } = this.props;

        let specName = null;
        if (descItem.descItemSpecId) {
            specName = refType.descItemSpecsMap[descItem.descItemSpecId].name;
        }

        let structureType = {};
        if (structureTypes) {
            structureType = objectById(structureTypes.data, refType.structureTypeId);
        }

        const additionalProps = {
            RECORD_REF: {
                itemName: refType.shortcut,
                specName: specName,
                singleDescItemTypeEdit: singleDescItemTypeEdit,
                itemTypeId: refType.id,
                onDetail: value => {
                    this.handleDetailRecord(descItemIndex, value);
                },
                onCreateRecord: value => {
                    this.handleCreateRecord(descItemIndex);
                },
            },
            STRUCTURED: {
                singleDescItemTypeEdit: singleDescItemTypeEdit,
                structureTypeCode: structureType ? structureType.code : null,
                structureTypeName: structureType ? structureType.name : null,
                anonymous: structureType ? structureType.anonymous : null,
            },
            FILE_REF: {
                onCreateFile: value => {
                    this.handleCreateFile(descItemIndex);
                },
                onFundFiles: value => {
                    this.handleFundFiles(descItemIndex);
                },
            },
            UNITDATE: {
                calendarTypes: calendarTypes,
            },
            JSON_TABLE: {
                refType,
                onDownload: value => {
                    this.props.onJsonTableDownload(descItem.descItemObjectId, value);
                },
                onUpload: this.handleJsonTableUploadUpload,
            },
            COORDINATES: {
                onUpload: this.handleCoordinatesUpload,
                onDownload: value => {
                    this.props.onCoordinatesDownload(descItem.descItemObjectId, value);
                },
            },
            INT: {
                refType,
            },
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
            ref: ref => (this.refObjects[key] = ref),
            cal: infoType.cal && !infoType.calSt,
            typePrefix,
            readOnly: descItem.saving,
            versionId: versionId,
            fundId: fundId,
            key: itemComponentKey,
            descItemFactory,
            repeatable: infoType.rep == 1,
            ...additionalProps[rulDataType.code],
        };

        return descItemFactory.createDescItem(rulDataType.code, descItemProps);
    };

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

        const parts = [];
        let partsCls = 'desc-item-value-container';

        const key = descItem.formKey;

        if (refType.useSpecification) {
            parts.push(this.renderDescItemSpec('spec_' + key, descItem, descItemIndex, locked));
            if (rulDataType.code != 'ENUM') {
                partsCls += ' desc-item-spec-and-value';
            }
        }
        partsCls += ' dt' + rulDataType.code;

        let dragProps;
        if (Utils.detectIE() || readMode || !draggable) {
            dragProps = {};
        } else {
            dragProps = {
                'data-id': descItemIndex,
                draggable: infoType.rep === 1,
                onDragStart: this.handleDragStart,
                onDragEnd: this.handleDragEnd,
            };
        }

        // render value (if not enum)
        if (rulDataType.code != 'ENUM') {
            parts.push(this.renderDescItemValue(descItem, descItemIndex, locked));
        }

        return (
            <Shortcuts
                key={key}
                name="DescItem"
                handler={this.handleDescItemShortcuts.bind(this, descItemIndex)}
                alwaysFireHandler
                global
            >
                <div
                    key="container"
                    className={cls}
                    {...dragProps}
                    ref={ref => {
                        this.containers[descItemIndex] = ref;
                    }}
                >
                    {!readMode && infoType.rep == 1 && draggable && (
                        <div className="dragger">
                            <Icon className="up" glyph="fa-angle-up" />
                            <Icon className="down" glyph="fa-angle-down" />
                            &nbsp;
                        </div>
                    )}

                    <div key="container" className={partsCls}>
                        {parts}
                    </div>
                    {actions.length > 0 && (
                        <div key="actions" className="desc-item-action-container">
                            {actions}
                        </div>
                    )}
                </div>
            </Shortcuts>
        );
    }

    getShowDeleteDescItem(descItem) {
        const {fundId, userDetail, refType, infoType, descItemType, closed, locked, readMode, arrPerm} = this.props;

        // Pokud nemá právo na pořádání, nelze provádět akci
        if (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId}) && !arrPerm) {
            return false;
        }

        if (closed || locked || readMode) {
            return false;
        }

        // ##
        // # Má se zobrazovat ikona mazání z hlediska hodnoty desc item?
        // ##
        if (nodeFormActions.descItemNeedStore(descItem, refType)) {
            return false;
        }

        if (typeof descItem.id !== 'undefined') {
            // je na serveru, můžeme smazat
            return true;
        }

        if (descItem.touched) {
            // změnil pole, ale není v DB, je možné ji smazat
            return true;
        }

        // Mazat lze jen hodnota, která není povinná z hlediska specifikace
        if (
            refType.useSpecification &&
            hasDescItemTypeValue(refType.dataType) &&
            typeof descItem.descItemSpecId !== 'undefined' &&
            descItem.descItemSpecId !== ''
        ) {
            const infoSpec = infoType.descItemSpecsMap[descItem.descItemSpecId];
            if (infoSpec.type == 'REQUIRED' || infoSpec.type == 'RECOMMENDED') {
                return false;
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

        return true;
    }

    getShowDeleteDescItemType() {
        const {fundId, userDetail, refType, infoType, closed, readMode, hideDelete, arrPerm} = this.props;
        const {descItemType} = this.state;

        // Pokud nemá právo na pořádání, nelze provádět akci
        if (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId}) && !arrPerm) {
            return false;
        }

        if (closed || readMode || hideDelete) {
            return false;
        }

        if (descItemType.descItems.length === 0) {
            return true;
        }

        let descItemsShowDeleteItem = false;
        for (let a = 0; a < descItemType.descItems.length; a++) {
            let descItem = descItemType.descItems[a];

            if (nodeFormActions.descItemNeedStore(descItem, refType)) {
                return false;
            }

            if (this.getShowDeleteDescItem(descItem)) {
                descItemsShowDeleteItem = true;
            }
        }

        return descItemsShowDeleteItem;
    }

    /**
     * Renderování nadpisu atributu - včetně akcí pro atribut.
     * @return {Object} view
     */
    renderLabel() {
        const {
            fundId,
            showNodeAddons,
            userDetail,
            descItemCopyFromPrevEnabled,
            singleDescItemTypeEdit,
            copy,
            locked,
            infoType,
            refType,
            conformityInfoMissings,
            closed,
            readMode,
            notIdentified,
            rulDataType,
            onDescItemNotIdentified,
            customActions,
            arrPerm,
        } = this.props;
        const {descItemType} = this.state;

        const actions = [];

        const hasPermission = userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId}) || arrPerm;
        // Sestavení akcí
        if (hasPermission) {
            if (showNodeAddons && !closed && !readMode && !singleDescItemTypeEdit) {
                actions.push(
                    <NoFocusButton
                        disabled={!descItemCopyFromPrevEnabled}
                        title={i18n('subNodeForm.descItemType.copyFromPrev')}
                        key="book"
                        onClick={this.handleDescItemTypeCopyFromPrev}
                    >
                        <Icon glyph="fa-paste" />
                    </NoFocusButton>,
                    <NoFocusButton
                        title={i18n('subNodeForm.descItemType.copy')}
                        key="copy"
                        onClick={this.handleDescItemTypeCopy}
                    >
                        <Icon className={copy ? 'copy' : 'nocopy'} glyph="fa-files-o" />
                    </NoFocusButton>,
                    <NoFocusButton
                        title={i18n('subNodeForm.descItemType.lock')}
                        key="lock"
                        onClick={this.handleDescItemTypeLock}
                    >
                        <Icon className={locked ? 'locked' : 'unlocked'} glyph="fa-lock" />
                    </NoFocusButton>,
                );
            }
        }

        const showDeleteButton = this.getShowDeleteDescItemType();
        if (showDeleteButton) {
            actions.push(
                <NoFocusButton
                    key="delete"
                    onClick={this.props.onDescItemTypeRemove}
                    title={i18n('subNodeForm.deleteDescItemType')}
                >
                    <Icon glyph="fa-trash" />
                </NoFocusButton>,
            );
        }

        // Zprávy o chybějících položkách
        const missings = conformityInfoMissings;
        if (missings && missings.length > 0) {
            const messages = missings.map(missing => missing.description);
            const tooltip = <div>{messages}</div>;
            actions.push(
                <TooltipTrigger key="state" content={tooltip} holdOnHover placement="auto">
                    <div className="btn btn-default">
                        <Icon className="messages" glyph="fa-exclamation-triangle" />
                    </div>
                </TooltipTrigger>,
            );
        }

        if (infoType.cal === 1 && !readMode) {
            const title = infoType.calSt ? i18n('subNodeForm.calculate-user') : i18n('subNodeForm.calculate-auto');
            actions.push(
                <NoFocusButton
                    onClick={this.handleSwitchCalculating}
                    key="calculate"
                    title={title}
                    className="alwaysVisible"
                >
                    {infoType.calSt ? (
                        <span className="fa-stack">
                            <Icon glyph="fa-calculator fa-stack-1x" />
                            <Icon glyph="fa-ban fa-stack-2x" />
                        </span>
                    ) : (
                        <Icon glyph="fa-calculator" />
                    )}
                </NoFocusButton>,
            );
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
        const tooltip = <div dangerouslySetInnerHTML={{__html: titleText}}></div>;

        if (hasPermission) {
            if (infoType.rep === 1 && !(locked || closed || readMode)) {
                const {onDescItemAdd} = this.props;
                if (this.props.rulDataType.code === 'COORDINATES') {
                    actions.push(
                        <NoFocusButton
                            key="add"
                            onClick={onDescItemAdd}
                            title={i18n('subNodeForm.descItemType.title.add')}
                        >
                            <Icon glyph="fa-plus" />
                        </NoFocusButton>,
                        <NoFocusButton
                            key="upload"
                            onClick={this.handleCoordinatesUploadButtonClick}
                            title={i18n('subNodeForm.descItemType.title.add')}
                        >
                            <Icon glyph="fa-upload" />
                        </NoFocusButton>,
                        <FormInput
                            key="upload-field"
                            className="hidden"
                            accept="application/vnd.google-earth.kml+xml"
                            type="file"
                            ref={ref => (this.refUploadInput = ref)}
                            onChange={this.handleCoordinatesUpload}
                        />,
                    );
                } else if (this.props.rulDataType.code === 'JSON_TABLE') {
                    actions.push(
                        <NoFocusButton
                            key="add"
                            onClick={onDescItemAdd}
                            title={i18n('subNodeForm.descItemType.title.add')}
                        >
                            <Icon glyph="fa-plus" />
                        </NoFocusButton>,
                        <NoFocusButton
                            key="upload"
                            onClick={this.handleJsonTableUploadButtonClick}
                            title={i18n('subNodeForm.descItem.jsonTable.action.upload')}
                        >
                            <Icon glyph="fa-upload" />
                        </NoFocusButton>,
                        <FormInput
                            key="upload-field"
                            className="hidden"
                            accept="text/csv"
                            type="file"
                            ref={ref => (this.refUploadInput = ref)}
                            onChange={this.handleJsonTableUploadUpload}
                        />,
                    );
                } else {
                    actions.push(
                        <NoFocusButton
                            key="add"
                            onClick={onDescItemAdd}
                            title={i18n('subNodeForm.descItemType.title.add')}
                        >
                            <Icon glyph="fa-plus" />
                        </NoFocusButton>,
                    );
                }
            }
        }

        if (!closed && !readMode && !infoType.rep && infoType.ind && rulDataType.code !== 'ENUM') {
            actions.push(
                <NoFocusButton
                    key="notIdentified"
                    onClick={() => {
                        if (descItemType.descItems.length === 1) {
                            onDescItemNotIdentified(0, descItemType.descItems[0]);
                        }
                    }}
                    title={i18n('subNodeForm.descItemType.title.notIdentified')}
                >
                    <Icon glyph="fa-low-vision" className={notIdentified ? 'notIdentified' : 'identified'} />
                </NoFocusButton>,
            );
        }

        // Render
        return (
            <div key="label" className="desc-item-type-label">
                <TooltipTrigger key="title" className="title" content={tooltip} holdOnHover placement="vertical">
                    {refType.shortcut}
                </TooltipTrigger>
                <div key="actions" className="actions">
                    {actions}
                    {customActions}
                </div>
                {descItemType.descItems.filter(i => i.touched).length > 0 && (
                    <span key="edited" className="desc-item-type-edited">
                        {i18n('subNodeForm.descItem.edited')}
                    </span>
                )}
            </div>
        );
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

    /**
     * Akce na přepnutí vyplňování hodnot atributu.
     */
    handleSwitchCalculating() {
        this.props.onSwitchCalculating();
    }

    render() {
        const {
            fundId,
            userDetail,
            onDescItemRemove,
            onDescItemAdd,
            rulDataType,
            infoType,
            locked,
            conformityInfo,
            closed,
            readMode,
            onDescItemNotIdentified,
            arrPerm,
        } = this.props;
        const {descItemType} = this.state;

        if (descItemType.id === 63) {
            console.log('RENDER...', descItemType);
        }

        // console.log('[DescItemType] RENDER', this.props.refType.name);

        const label = this.renderLabel();
        const showDeleteDescItemType = this.getShowDeleteDescItemType();
        const descItems = descItemType.descItems
            .filter(i => i.hasOwnProperty('formKey'))
            .map((descItem, descItemIndex) => {
                const actions = [];

                // pokud prvek popisu neni pocitan automaticky nebo je u nej povoleno zadavat vlastni hodnotu
                const editable = !infoType.cal || infoType.calSt;

                // pokud je zapnuty rezim uprav a prvek popisu je upravitelny
                if (!readMode && editable) {
                    // pokud je opakovatelny, neni ENUM a je mozne ho nastavit jako "nezjisteno"
                    if (infoType.rep === 1 && rulDataType.code !== 'ENUM' && infoType.ind) {
                        actions.push(
                            <NoFocusButton
                                key="notIdentified"
                                className={descItem.undefined ? 'notIdentified' : 'identified'}
                                onClick={() => onDescItemNotIdentified(descItemIndex, descItem)}
                                title={i18n('subNodeForm.descItemType.title.notIdentified')}
                            >
                                <Icon glyph="fa-low-vision" />
                            </NoFocusButton>,
                        );
                    }

                    // pokud je opakovatelny, nebo je jich vice nez 1
                    if (infoType.rep === 1 || descItemType.descItems.length > 1) {
                        actions.push(
                            <NoFocusButton
                                disabled={!this.getShowDeleteDescItem(descItem)}
                                key="delete"
                                onClick={onDescItemRemove.bind(this, descItemIndex)}
                                title={i18n('subNodeForm.deleteDescItem')}
                            >
                                <Icon glyph="fa-times" />
                            </NoFocusButton>,
                        );
                    }
                }
                const errors = conformityInfo.errors[descItem.descItemObjectId];
                if (errors && errors.length > 0) {
                    const messages = errors.map(error => error.description);
                    const tooltip = <div>{messages}</div>;
                    actions.push(
                        <TooltipTrigger key="info" content={tooltip} holdOnHover placement="auto" showDelay={1}>
                            <div className="btn btn-default">
                                <Icon glyph="fa-exclamation-triangle" />
                            </div>
                        </TooltipTrigger>,
                    );
                }

                let canModifyDescItem = !(locked || closed || descItem.readOnly);

                // Pokud nemá právo na pořádání, nelze provádět akci
                if (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId}) && !arrPerm) {
                    canModifyDescItem = false;
                }

                return this.renderDescItem(descItemType, descItem, descItemIndex, actions, !canModifyDescItem);
            });

        const cls = classNames({
            'read-mode': readMode,
            'desc-item-type': true,
            active: descItemType.hasFocus,
            ['el-' + infoType.width]: true,
        });

        return (
            <Shortcuts
                name="DescItemType"
                className={cls}
                handler={this.handleDescItemTypeShortcuts}
                global
                alwaysFireHandler
            >
                {label}
                <div
                    ref={ref => (this.refDragOverContainer = ref)}
                    className="desc-item-type-desc-items"
                    onDragOver={this.handleDragOver}
                    onDragLeave={this.handleDragLeave}
                >
                    {descItems}
                </div>
            </Shortcuts>
        );
    }
}

function mapStateToProps(state) {
    const {userDetail} = state;

    return {
        userDetail,
    };
}

export default connect(mapStateToProps, null, null, {forwardRef: true})(DescItemType);
