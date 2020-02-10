import {ApItemVO, IItemFormState} from "../../stores/app/accesspoint/itemForm";
import {valuesEquals} from '../Utils';
import {i18n} from '../shared';
import {fromDuration} from "../../components/validate";
import {statusSaved, statusSaving} from '../../actions/global/status';
import {DisplayType, FOCUS_KEYS} from "../../constants";
import {increaseNodeVersion} from '../../actions/arr/node'
import {WebApi} from '../../actions/index.jsx';
import {setFocus} from '../../actions/global/focus'
import {getFocusDescItemLocation} from '../../stores/app/arr/subNodeFormUtils'
import {findByRoutingKeyInGlobalState, getMapFromList, getRoutingKeyType, indexById} from '../../stores/app/utils'
import {ThunkAction} from "redux-thunk";
import {AnyAction} from "redux";
import * as types from '../../actions/constants/ActionTypes.js'
type ThunkResult<R> = ThunkAction<R, {
    refTables: {
        rulDataTypes: {},
        descItemTypes: {},
    }
}, AnyAction>;



interface BaseAction {
    area: string;
    type: string;
}

export abstract class ItemFormActions {

    readonly area : string;

    constructor(area : string) {
        this.area = area;
    }

    isSubNodeFormActionOfArea(action: BaseAction, area : string) {
        if (action.area === area) {
            switch (action.type) {
                case types.ITEM_FORM_REQUEST:
                case types.ITEM_FORM_RECEIVE:
                case types.ITEM_FORM_VALUE_CREATE:
                case types.ITEM_FORM_VALUE_CHANGE:
                case types.ITEM_FORM_FORM_VALUE_CHANGE_POSITION:
                case types.ITEM_FORM_VALUE_CHANGE_SPEC:
                case types.ITEM_FORM_VALUE_CHANGE_PARTY:
                case types.ITEM_FORM_VALUE_CHANGE_RECORD:
                case types.ITEM_FORM_VALUE_VALIDATE_RESULT:
                case types.ITEM_FORM_VALUE_BLUR:
                case types.ITEM_FORM_VALUE_FOCUS:
                case types.ITEM_FORM_VALUE_ADD:
                case types.ITEM_FORM_VALUE_DELETE:
                case types.ITEM_FORM_DESC_ITEM_TYPE_DELETE:
                case types.ITEM_FORM_DESC_ITEM_TYPE_ADD:
                case types.ITEM_FORM_TEMPLATE_USE:
                case types.ITEM_FORM_VALUE_RESPONSE:
                case types.ITEM_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE:
                case types.ITEM_FORM_OUTPUT_CALC_SWITCH:
                case types.CHANGE_ACCESS_POINT:
                    return true;
                default:
                    return false
            }
        } else {
            return false;
        }
    }

    isSubNodeFormAction(action : BaseAction) {
        return this.isSubNodeFormActionOfArea(action, this.area);
    }

    /**
     * Akce propagace výsledku validace hodnoty ze serveru do store.
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} result výsledek validace
     */
    _fundSubNodeFormValueValidateResult(valueLocation, result): AnyAction {
        return {
            type: types.ITEM_FORM_VALUE_VALIDATE_RESULT,
            area: this.area,
            valueLocation,
            result
        }
    }

    /**
     * Smazání atributu POUZE z formuláře, nikoli na serveru!
     * @param {Object} valueLocation konkrétní umístění atributu
     * @param {boolean} onlyDescItems pokud je true, pouze se odeberou hodnoty atributu, ale daný atribut na formuláři zůstane, pokud je false, odebere se i atribut
     */
    _fundSubNodeFormDescItemTypeDeleteInStore(valueLocation, onlyDescItems): AnyAction {
        return {
            type: types.ITEM_FORM_DESC_ITEM_TYPE_DELETE,
            area: this.area,
            valueLocation,
            onlyDescItems
        }
    }

    /**
     * Informační akce o provedené operaci na serveru.
     * @param {Object} descItemResult --
     * @param {Object} valueLocation konkrétní umístění atributu nebo hodnoty
     * @param {string} operationType typ operace, jedna z hodnot: hodnota atributu['UPDATE', 'CREATE', 'DELETE'], atribut['DELETE_DESC_ITEM_TYPE']
     */
    _fundSubNodeFormDescItemResponse(valueLocation, descItemResult, operationType): AnyAction {
        return {
            type: types.ITEM_FORM_VALUE_RESPONSE,
            area: this.area,
            valueLocation,
            operationType,
            descItemResult: descItemResult
        }
    }

    /**
     * Nastavení stavu o vytváření elementu
     * @param {Object} valueLocation konkrétní umístění atributu nebo hodnoty
     */
    _fundSubNodeFormDescItemCreate(valueLocation): AnyAction {
        return {
            type: types.ITEM_FORM_VALUE_CREATE,
            area: this.area,
            valueLocation
        }
    }

    /**
     * Načtení server dat pro formulář pro aktuálně předané parametry.
     * @param {Object} getState odkaz na funkci pro načtení store
     * @param {Object} dispatch odkaz na funkci dispatch
     * @param showChildren
     * @param showParents
     * @return {Object} promise pro vrácení nových dat
     */
    abstract _getItemFormData(getState, dispatch, showChildren: boolean, showParents: boolean): Promise<Object>;

    /**
     * Načtení itemForm store podle předaných parametrů.
     * @param {Object} state globální store
     * @return subNodeForm store
     */
    abstract _getItemFormStore(state) : IItemFormState;

    /**
     * Načtení store nadřazeného objektu, např. NODE v pořádání nebo OUTPUT.
     * @param {Object} state globální store
     * @return store
     */
    abstract _getParentObjStore(state): {id: number};

    /**
     * Nové načtení dat.
     * @param {object} parent
     * @param {bool} needClean má se formulář reinicializovat a vymazat cšechna editace? - jako nové načtení formuláře
     * @param showChildren zobrazovat potomky?
     * @param showParents zobrazovat předky ke kořeni?
     */
    _fundSubNodeFormFetch(parent, needClean, showChildren, showParents) {
        return (dispatch, getState) => {
            dispatch(this.fundSubNodeFormRequest(parent));
            this._getItemFormData(getState, dispatch, showChildren, showParents)
                .then(json => {
                    const state = getState();
                    dispatch(this.fundSubNodeFormReceive(parent, json, state.refTables.rulDataTypes, state.refTables.descItemTypes, state.refTables.groups.data, needClean))
                })
        }
    }

    /** Metoda pro volání API. */
    abstract _callUpdateDescItem(parent, descItem) : Promise<any>;

    /** Metoda pro volání API. */
    abstract _callCreateDescItem(parent, descItemTypeId, descItem) : Promise<any>;

    /**
     * Odeslání hodnoty atributu na server - buď vytvoření nebo aktualizace.
     * @param {Function} dispatch odkaz na funkci dispatch
     * @param {Function} getState odkaz na funkci pro načtení store
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object|undefined} overrideDescItem forcing to save
     */
    _formValueStore(dispatch: Function, getState: Function, valueLocation, overrideDescItem? : ApItemVO<any>) {
        const state = getState();
        const subNodeForm = this._getItemFormStore(state);
        const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        const refType = subNodeForm.refTypesMap!!.get(loc!!.itemType.id)!!;
        const refTables = state.refTables;

        let item = overrideDescItem || loc!!.item!!;
        const parent = subNodeForm.data!!.parent;

        // pokud se jedná o číslo a zároveň se zobrazuje v HH:mm:ss je třeba ho převést
        if (refType.dataType.code === 'INT' && refType.viewDefinition === DisplayType.DURATION) {
            item = {
                ...item,
                value: fromDuration(item.value)
            };
        }

        if (this.descItemNeedStore(item, refType) || overrideDescItem) {
            dispatch(statusSaving());

            // Reálné provedení operace
            if (typeof item.id !== 'undefined') {
                this._callUpdateDescItem(parent, item)
                    .then(json => {
                        let descItemResult = {};
                        if (json && Array.isArray(json) && json.length > 0) {
                            descItemResult = {item: json[0]};
                        }
                        // if(this.area === OutputFormActions.AREA || this.area === StructureFormActions.AREA || this.area === AccessPointFormActions.AREA){
                            dispatch(this._fundSubNodeFormDescItemResponse(valueLocation, descItemResult, 'UPDATE'));
                        // }
                        dispatch(this._fundSubNodeUpdate(refTables, json));
                        dispatch(statusSaved());
                    })
            } else {
                if (!loc!!.item!!.saving) {
                    dispatch(this._fundSubNodeFormDescItemCreate(valueLocation));
                    this._callCreateDescItem(subNodeForm.data!!.parent, loc!!.itemType.id, item)
                        .then(json => {
                            let descItemResult = {};
                            if (json && Array.isArray(json) && json.length > 0) {
                                descItemResult = {item: json[0]};
                            }
                            console.log("formValueStore - id undefined",json);
                            dispatch(this._fundSubNodeFormDescItemResponse(valueLocation, descItemResult, 'CREATE'));
                            dispatch(statusSaved());
                        })
                }
            }
        }
    }

    _fundSubNodeUpdate(refTables, data): AnyAction {
        return {
            type: types.FUND_SUBNODE_UPDATE,
            data,
            refTables
        }
    }

    /**
     * Akce přidání nové prázdné hodnoty descItem vícehodnotového atributu descItemType.
     * @param {Object} valueLocation konkrétní umístění nové hodnoty
     */
    fundSubNodeFormValueAdd(valueLocation): AnyAction {
        return {
            type: types.ITEM_FORM_VALUE_ADD,
            area: this.area,
            valueLocation,
        }
    }

    /**
     * Akce na nastavení hodnoty atributu jako Nezjištěno.
     *
     * @param {Object} valueLocation konkrétní umístění nové hodnoty
     * @param descItem
     */
    fundSubNodeFormValueNotIdentified(valueLocation, descItem): ThunkResult<void> {
        return (dispatch, getState) => {
            let undef = descItem.undefined || false;
            descItem.undefined = !undef;

            const state = getState();
            const subNodeForm = this._getItemFormStore(state);

            if (!undef) {
                this._formValueStore(dispatch, getState, valueLocation, descItem);
            } else {
                this._callDeleteDescItem(subNodeForm.data!!.parent, descItem,);
            }
        };
    }

    /**
     * Akce validace hodnoty na serveru - týká se jen hodnot datace.
     * @param {Object} valueLocation konkrétní umístění hodnoty pro validaci
     */
    fundSubNodeFormValueValidate(valueLocation): ThunkResult<void> {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            // only when loc exists
            if (loc) {
                WebApi.validateUnitdate(loc!!.item!!.value)
                    .then(json => {
                        dispatch(this._fundSubNodeFormValueValidateResult(valueLocation, json));
                    })
            }
        }
    }

    /**
     * Akce změny hodnoty a její promítnutí do store, případné uložení na server, pokud je toto vynuceno parametrem forceStore.
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value nová hodnota
     * @param {boolean} forceStore pokud je true, je hodnota i odeslána na server pro uložení
     */
    fundSubNodeFormValueChange(valueLocation, value, forceStore): ThunkResult<void> {
        return (dispatch, getState) => {
            dispatch({
                type: types.ITEM_FORM_VALUE_CHANGE,
                area: this.area,
                        valueLocation,
                value,
                dispatch,
                formActions: this,
            });

            if (forceStore) {
                this._formValueStore(dispatch, getState, valueLocation)
            }
        }
    }

    /**
     * Akce změna pozice hodnoty vícehodnotového atributu - změna pořadí hodnot.
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {boolean} index nový index hodnoty v rámci atributu
     */
    fundSubNodeFormValueChangePosition(valueLocation, index): ThunkResult<void> {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            if (!loc!!.item!!.error.hasError && typeof loc!!.item!!.id !== 'undefined') {
                dispatch({
                    type: types.ITEM_FORM_FORM_VALUE_CHANGE_POSITION,
                    area: this.area,
                    valueLocation,
                    index,
                });

                const descItem = {...loc!!.item, position: index + 1};

                this._callUpdateDescItem(subNodeForm.data!!.parent, descItem)
                    .then(json => {
                        let descItemResult = {};
                        if (json && Array.isArray(json) && json.length > 0) {
                            descItemResult = {item: json[0]};
                        }
                        const newValueLocation = {...valueLocation, descItemIndex: index};
                        dispatch(this._fundSubNodeFormDescItemResponse(newValueLocation, descItemResult, 'UPDATE'));
                    })
            }
        }
    }

    /**
     * Akce změna hodnoty atributu - odkaz na osoby.
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value hodnota
     */
    fundSubNodeFormValueChangeParty(valueLocation, value): ThunkResult<void> {
        return (dispatch, getState) => {
            dispatch({
                type: types.ITEM_FORM_VALUE_CHANGE_PARTY,
                area: this.area,
                        valueLocation,
                value,
                dispatch
            })
        }
    }

    /**
     * Akce změna hodnoty atributu - odkaz na rejstřík.
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value hodnota
     */
    fundSubNodeFormValueChangeRecord(valueLocation, value): ThunkResult<void> {
        return (dispatch, getState) => {
            dispatch({
                type: types.ITEM_FORM_VALUE_CHANGE_RECORD,
                area: this.area,
                        valueLocation,
                value,
                dispatch
            })
        }
    }

    /**
     * Akce změna hodnoty specifikace atributu.
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value hodnota
     */
    fundSubNodeFormValueChangeSpec(valueLocation, value): ThunkResult<void> {
        return (dispatch, getState) => {
            // Dispatch zmněny specifikace
            dispatch({
                type: types.ITEM_FORM_VALUE_CHANGE_SPEC,
                area: this.area,
                        valueLocation,
                value,
            });

            // Vynucení uložení na server, pokud je validní jako celek
            this._formValueStore(dispatch, getState, valueLocation)
        }
    }

    /**
     * Test, zda je nutné hodnotu atributu uložit, např. pokud byla změněna nebo pokud ještě nebyla založena na serveru.
     * @param {Object} descItem hodnota atributu
     * @param {Object} refType ref typ atributu
     */
    descItemNeedStore(descItem, refType) {
        if (!descItem.error.hasError && descItem.touched) {
            if (typeof descItem.id !== 'undefined') {
                // Jen pokud se hodnota nebo specifikace změnila
                let needUpdate = false;
                if (refType.useSpecification && !valuesEquals(descItem.descItemSpecId, descItem.prevDescItemSpecId)) {
                    needUpdate = true;
                }
                if (!valuesEquals(descItem.value, descItem.prevValue)) {
                    needUpdate = true;
                }
                if (!valuesEquals(descItem.calendarTypeId, descItem.prevCalendarTypeId)) {
                    needUpdate = true;
                }

                return needUpdate
            } else {
                return true
            }
        }
        return false
    }

    /**
     * Blur na hodnotě atributu.
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    fundSubNodeFormValueBlur(valueLocation): ThunkResult<void> {
        return (dispatch, getState) => {
            dispatch({
                type: types.ITEM_FORM_VALUE_BLUR,
                area: this.area,
                        valueLocation,
                receivedAt: Date.now()
            });

            this._formValueStore(dispatch, getState, valueLocation)
        }
    }

    /** Metoda pro volání API. */
    abstract _callDeleteDescItem(parent, descItem);

    /**
     * Smazání hodnoty atributu.
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    fundSubNodeFormValueDelete(valueLocation): ThunkResult<void> {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            dispatch({
                type: types.ITEM_FORM_VALUE_DELETE,
                area: this.area,
                        valueLocation,
            });

            if (typeof loc!!.item!!.id !== 'undefined') {
                this._callDeleteDescItem(subNodeForm.data!!.parent, loc!!.item)
                    .then(json => {
                        dispatch(this._fundSubNodeFormDescItemResponse(valueLocation, json, 'DELETE'));
                    })
            }
        }
    }

    fundSubNodeFormHandleClose(routingKey): ThunkResult<void> {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state);
            const valueLocation = getFocusDescItemLocation(subNodeForm);
            if (valueLocation !== null) {
                dispatch(this.fundSubNodeFormValueBlur(valueLocation))
            }
        }
    }

    /**
     * Přidání atributu na formulář.
     * @param {int} descItemTypeId id atributu
     */
    fundSubNodeFormDescItemTypeAdd(descItemTypeId): ThunkResult<void> {
        return (dispatch, getState) => {
            dispatch({
                type: types.ITEM_FORM_DESC_ITEM_TYPE_ADD,
                area: this.area,
                        descItemTypeId
            });

            //const state = getState();
            //const subNodeForm = this._getItemFormStore(state);
            dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {descItemTypeId: descItemTypeId, descItemObjectId: null}))
        }
    }

    /** Metoda pro volání API. */
    abstract _callDeleteDescItemType(parent, descItemTypeId);

    /** Metoda pro volání API. */
    abstract _callSetNotIdentifiedDescItem(elementId, itemTypeId, itemSpecId, itemObjectId);

    /** Metoda pro volání API. */
    abstract _callUnsetNotIdentifiedDescItem(elementId, itemTypeId, itemSpecId, itemObjectId);

    /**
     * Přidání PP (který je počítaný a ještě ve formuláři není) do formuláře
     * @param itemTypeId id typu PP
     * @param strict
     */
    addCalculatedDescItem(itemTypeId, strict = false) {
        // Not implemented
        console.warn("Not implemented addCalculatedDescItem");
    }

    /**
     * Přepnutí počítání hodnot atributu uživatelské/automatické.
     *
     * @param {int} itemTypeId identifikátor typu atributu
     * @param {Object} valueLocation konkrétní umístění atributu
     */
    switchOutputCalculating(itemTypeId, valueLocation) {
        return (dispatch, getState) => {
            const state = getState();
            const fundIndex = indexById(state.arrRegion.funds, "versionId");
            if (fundIndex !== null) {
                const getOutputId = state.arrRegion.funds[fundIndex].fundOutput.fundOutputDetail.subNodeForm.fetchingId;
                WebApi.switchOutputCalculating(getOutputId, itemTypeId).then(() => {
                    dispatch({
                        type: types.ITEM_FORM_OUTPUT_CALC_SWITCH,
                        area: this.area,
                                                valueLocation
                    })
                });
            }
        }
    }

    /**
     * Smazání atributu.
     * @param {Object} valueLocation konkrétní umístění atributu
     */
    fundSubNodeFormDescItemTypeDelete(valueLocation) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            let hasDescItemsForDelete = false;
            loc!!.itemType.items.forEach(descItem => {
                if (typeof descItem.id !== 'undefined') {
                    hasDescItemsForDelete = true;
                }
            });

            dispatch(this._fundSubNodeFormDescItemTypeDeleteInStore(valueLocation, false));

            if (hasDescItemsForDelete) {
                this._callDeleteDescItemType(subNodeForm.data!!.parent, loc!!.itemType.id)
                    .then(json => {
                        dispatch(this._fundSubNodeFormDescItemResponse(valueLocation, json, 'DELETE_DESC_ITEM_TYPE'));
                    })
            }
        }
    }

    /**
     * Informační akce o provedené operaci kopírování hodnot atributu z předchozí JP.
     * @param {Object} valueLocation konkrétní umístění atributu
     * @param {Object} copySiblingResult nová nakopírovaná data - objekt ze serveru
     */
    fundSubNodeFormDescItemTypeCopyFromPrevResponse(valueLocation, copySiblingResult): AnyAction {
        return {
            type: types.ITEM_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE,
            area: this.area,
            valueLocation,
            copySiblingResult
        }
    }

    /**
     * Focus na hodnotě atributu.
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    fundSubNodeFormValueFocus(valueLocation): AnyAction {
        return {
            type: types.ITEM_FORM_VALUE_FOCUS,
            area: this.area,
            valueLocation,
        }
    }

    /**
     * Vyžádání dat - aby byla ve store k dispozici.
     * @param {object} parent
     * @param {boolean} needClean
     * @param {boolean} showChildren
     * @param {boolean} showParents
     */
    fundSubNodeFormFetchIfNeeded(parent: any, needClean : boolean = false, showChildren : boolean = false, showParents: boolean = false) {
        return (dispatch, getState) => {
            const state = getState();

            // Fetch může být pouze v případě, že už jsou načteny číselníkové hodnoty na typy položek v ref
            if (
                !state.refTables.rulDataTypes.isFetching && state.refTables.rulDataTypes.fetched
                &&
                !state.refTables.descItemTypes.isFetching && state.refTables.descItemTypes.fetched
            ) {
                const subNodeForm = this._getItemFormStore(state);
                if (JSON.stringify(parent) !== JSON.stringify(subNodeForm.parent) || (!subNodeForm.fetched || subNodeForm.dirty || subNodeForm.needClean || needClean) && !subNodeForm.isFetching) {
                    dispatch(this._fundSubNodeFormFetch(parent, subNodeForm.needClean || needClean, showChildren, showParents));
                }
            }
        }
    }

    /**
     * Nová data byla načtena.
     * @param {any} parent parentu
     * @param {Object} json objekt s daty
     * @param {Object} rulDataTypes store - datové typy pro atributy
     * @param {Object} descItemTypes store - obecný předpis atributů - ref
     * @param {Object} groups store - skupiny pro typy atributů
     * @param {bool} needClean má se formulář reinicializovat a vymazat cšechna editace? - jako nové načtení formuláře
     */
    fundSubNodeFormReceive(parent, json, rulDataTypes, descItemTypes, groups, needClean): AnyAction {
        return {
            type: types.ITEM_FORM_RECEIVE,
            area: this.area,
            data: json,
            parent,
            rulDataTypes,
            refDescItemTypes: descItemTypes,
            groups,
            receivedAt: Date.now(),
            needClean
        }
    }

    /**
     * Bylo zahájeno nové načítání dat.
     * @param {object} parent parentu
     */
    fundSubNodeFormRequest(parent): AnyAction {
        return {
            type: types.ITEM_FORM_REQUEST,
            area: this.area,
            parent
        }
    }
}
