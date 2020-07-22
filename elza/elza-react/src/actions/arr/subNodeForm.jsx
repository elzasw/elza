/**
 * Akce pro formulář JP.
 */

import {DisplayType, FOCUS_KEYS} from '../../constants.tsx';
import {WebApi} from 'actions/index.jsx';
import {findByRoutingKeyInGlobalState, getRoutingKeyType, indexById} from 'stores/app/utils.jsx';
import {getFocusDescItemLocation} from 'stores/app/arr/subNodeFormUtils.jsx';
import {valuesEquals} from 'components/Utils.jsx';
import {setFocus} from 'actions/global/focus.jsx';
import {increaseNodeVersion} from './node.jsx';
import {outputIncreaseNodeVersion} from './outputActions.jsx';
import * as types from '../../actions/constants/ActionTypes.js';
import {addToastrDanger, addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {i18n} from 'components/shared';
import {statusSaved, statusSaving} from 'actions/global/status.jsx';
import {fundNodeInfoReceive} from './nodeInfo.jsx';
import NodeRequestController from 'websocketController.jsx';
import {fundSubNodeInfoReceive} from './subNodeInfo';
import {fromDuration} from '../../components/validate';
import {ItemAvailabilityNumToEnumMap} from '../../stores/app/accesspoint/itemFormUtils';
import {getMapFromList} from '../../shared/utils';

// Konfigurace velikosti cache dat pro formulář
const CACHE_SIZE = 20;
const CACHE_SIZE2 = CACHE_SIZE / 2;

export class ItemFormActions {
    constructor(area) {
        this.area = area;
    }

    isSubNodeFormCacheActionOfArea(action, area) {
        if (action.area === area) {
            switch (action.type) {
                case types.FUND_SUB_NODE_FORM_CACHE_RESPONSE:
                case types.FUND_SUB_NODE_FORM_CACHE_REQUEST:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    isSubNodeFormCacheAction(action) {
        return this.isSubNodeFormCacheActionOfArea(action, this.area);
    }

    isSubNodeFormActionOfArea(action, area) {
        if (action.area === area) {
            switch (action.type) {
                case types.FUND_SUB_NODE_FORM_REQUEST:
                case types.FUND_SUB_NODE_FORM_RECEIVE:
                case types.FUND_SUB_NODE_FORM_VALUE_CREATE:
                case types.FUND_SUB_NODE_FORM_VALUE_CHANGE:
                case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_POSITION:
                case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
                case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_PARTY:
                case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_RECORD:
                case types.FUND_SUB_NODE_FORM_VALUE_VALIDATE_RESULT:
                case types.FUND_SUB_NODE_FORM_VALUE_BLUR:
                case types.FUND_SUB_NODE_FORM_VALUE_FOCUS:
                case types.FUND_SUB_NODE_FORM_VALUE_ADD:
                case types.FUND_SUB_NODE_FORM_VALUE_NOT_IDENTIFIED:
                case types.FUND_SUB_NODE_FORM_VALUE_DELETE:
                case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE:
                case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD:
                case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPES_ADD_TEMPLATE:
                case types.FUND_SUB_NODE_FORM_TEMPLATE_USE:
                case types.FUND_SUB_NODE_FORM_VALUE_RESPONSE:
                case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE:
                case types.FUND_SUB_NODE_FORM_OUTPUT_CALC_SWITCH:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    isSubNodeFormAction(action) {
        return this.isSubNodeFormActionOfArea(action, this.area);
    }

    /**
     * Akce propagace výsledku validace hodnoty ze serveru do store.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} result výsledek validace
     */
    _fundSubNodeFormValueValidateResult(versionId, routingKey, valueLocation, result) {
        return {
            type: types.FUND_SUB_NODE_FORM_VALUE_VALIDATE_RESULT,
            area: this.area,
            versionId,
            routingKey,
            valueLocation,
            result,
        };
    }

    /**
     * Smazání atributu POUZE z formuláře, nikoli na serveru!
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění atributu
     * @param {boolean} onlyDescItems pokud je true, pouze se odeberou hodnoty atributu, ale daný atribut na formuláři zůstane, pokud je false, odebere se i atribut
     */
    _fundSubNodeFormDescItemTypeDeleteInStore(versionId, routingKey, valueLocation, onlyDescItems) {
        return {
            type: types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE,
            area: this.area,
            versionId,
            routingKey,
            valueLocation,
            onlyDescItems,
        };
    }

    /**
     * Informační akce o provedené operaci na serveru.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} descItemResult --
     * @param {Object} valueLocation konkrétní umístění atributu nebo hodnoty
     * @param {string} operationType typ operace, jedna z hodnot: hodnota atributu['UPDATE', 'CREATE', 'DELETE'], atribut['DELETE_DESC_ITEM_TYPE']
     */
    _fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, descItemResult, operationType) {
        return {
            type: types.FUND_SUB_NODE_FORM_VALUE_RESPONSE,
            area: this.area,
            versionId,
            routingKey,
            valueLocation,
            operationType,
            descItemResult: descItemResult,
        };
    }

    /**
     * Nastavení stavu o vytváření elementu
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění atributu nebo hodnoty
     */
    _fundSubNodeFormDescItemCreate(versionId, routingKey, valueLocation) {
        return {
            type: types.FUND_SUB_NODE_FORM_VALUE_CREATE,
            area: this.area,
            versionId,
            routingKey,
            valueLocation,
        };
    }

    /**
     * Načtení server dat pro formulář pro aktuálně předané parametry.
     * @param {Object} getState odkaz na funkci pro načtení store
     * @param {Object} dispatch odkaz na funkci dispatch
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @return {Object} promise pro vrácení nových dat
     */
    // @Abstract
    _getItemFormData(getState, dispatch, versionId, nodeId, routingKey, showChildren, showParents) {}

    /**
     * Bylo zahájeno nové načítání dat.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     */
    _fundSubNodeFormCacheRequest(versionId, routingKey) {
        return {
            type: types.FUND_SUB_NODE_FORM_CACHE_REQUEST,
            area: this.area,
            versionId,
            routingKey,
        };
    }

    /**
     * Nová data byla načtena.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} formsMap objekt s daty
     */
    _fundSubNodeFormCacheResponse(versionId, routingKey, formsMap) {
        return {
            type: types.FUND_SUB_NODE_FORM_CACHE_RESPONSE,
            area: this.area,
            versionId,
            routingKey,
            formsMap,
        };
    }

    /**
     * Načtení itemForm store podle předaných parametrů.
     * @param {Object} state globální store
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @return subNodeForm store
     */
    // @Abstract
    _getItemFormStore(state, versionId, routingKey) {}

    /**
     * Načtení store nadřazeného objektu, např. NODE v pořádání nebo OUTPUT.
     * @param {Object} state globální store
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @return store
     */
    // @Abstract
    _getParentObjStore(state, versionId, routingKey) {}

    /**
     * Nové načtení dat.
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {bool} needClean má se formulář reinicializovat a vymazat cšechna editace? - jako nové načtení formuláře
     * @param showChildren zobrazovat potomky?
     * @param showParents zobrazovat předky ke kořeni?
     */
    _fundSubNodeFormFetch(versionId, nodeId, routingKey, needClean, showChildren, showParents) {
        return (dispatch, getState) => {
            dispatch(this.fundSubNodeFormRequest(versionId, nodeId, routingKey));
            this._getItemFormData(getState, dispatch, versionId, nodeId, routingKey, showChildren, showParents).then(
                json => {
                    const state = getState();
                    const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
                    if (subNodeForm && subNodeForm.fetchingId == nodeId) {
                        // Nastavení správných typů u itemTypes - ze serveru chodí čísla místo enumů
                        this.updateItemTypesTypes(json);

                        dispatch(
                            this.fundSubNodeFormReceive(
                                versionId,
                                nodeId,
                                routingKey,
                                json,
                                state.refTables.rulDataTypes,
                                state.refTables.descItemTypes,
                                state.refTables.groups.data,
                                needClean,
                            ),
                        );
                    }
                },
            );
        };
    }

    /** Metoda pro volání API, aktualizace hodnoty
     * @param {Function} dispatch dispatch pro store
     * @param {Object} formState stav formuláře
     */
    // @Abstract
    _callUpdateDescItem(dispatch, formState, versionId, parentVersionId, parentId, descItem) {}

    /** Metoda pro volání API, vložení hodnoty */
    // @Abstract
    _callCreateDescItem(versionId, parentId, parentVersionId, descItemTypeId, descItem) {}

    /**
     * Aktualizace čísel typů item a specs - na enumy (tedy např. hodnota 0 na IMPOSSIBLE)
     * @param json
     */
    updateItemTypesTypes(json) {
        // Nastavení správných typů u itemTypes - ze serveru chodí čísla místo enumů
        json.itemTypes &&
            json.itemTypes.forEach(itemType => {
                itemType.itemType = ItemAvailabilityNumToEnumMap[itemType.type];
                itemType.specs.forEach(itemSpec => {
                    itemSpec.itemType = ItemAvailabilityNumToEnumMap[itemSpec.type];
                });
                itemType.descItemSpecsMap = getMapFromList(itemType.specs);
            });
    }

    /**
     * Odeslání hodnoty atributu na server - buď vytvoření nebo aktualizace.
     * @param {Function} dispatch odkaz na funkci dispatch
     * @param {Function} getState odkaz na funkci pro načtení store
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE,
     *                         ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    _formValueStore(dispatch, getState, versionId, routingKey, valueLocation, overrideDescItem = false) {
        const state = getState();
        const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
        const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        const refType = subNodeForm.refTypesMap[loc.descItemType.id];
        const refTables = state.refTables;

        let descItem = overrideDescItem || loc.descItem;
        const parentVersionId = subNodeForm.data.parent.version;
        const parentId = subNodeForm.data.parent.id;

        // pokud se jedná o číslo a zároveň se zobrazuje v HH:mm:ss je třeba ho převést
        if (refType.dataType.code === 'INT' && refType.viewDefinition === DisplayType.DURATION) {
            descItem = {
                ...descItem,
                value: fromDuration(descItem.value),
            };
        }

        if (this.descItemNeedStore(descItem, refType) || overrideDescItem) {
            // Reálné provedení operace
            if (typeof descItem.id !== 'undefined') {
                dispatch(statusSaving());
                this._callUpdateDescItem(dispatch, subNodeForm, versionId, parentVersionId, parentId, descItem).then(
                    json => {
                        // Nastavení správných typů u itemTypes - ze serveru chodí čísla místo enumů
                        this.updateItemTypesTypes(json);

                        if (this.area === OutputFormActions.AREA || this.area === StructureFormActions.AREA) {
                            dispatch(
                                this._fundSubNodeFormDescItemResponse(
                                    versionId,
                                    routingKey,
                                    valueLocation,
                                    json,
                                    'UPDATE',
                                ),
                            );
                        } else {
                            dispatch(this._fundSubNodeUpdate(versionId, refTables, json));
                        }
                        dispatch(statusSaved());
                    },
                );
            } else {
                // check if not already saving
                if (!loc.descItem.saving) {
                    dispatch(statusSaving());
                    // Umělé navýšení verze o 1 - aby mohla pozitivně projít případná další update operace
                    dispatch(increaseNodeVersion(versionId, parentId, parentVersionId));
                    dispatch(this._fundSubNodeFormDescItemCreate(versionId, routingKey, valueLocation));
                    this._callCreateDescItem(
                        versionId,
                        subNodeForm.data.parent.id,
                        subNodeForm.data.parent.version,
                        loc.descItemType.id,
                        descItem,
                    ).then(json => {
                        console.log('formValueStore - id undefined', json);
                        dispatch(
                            this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, 'CREATE'),
                        );
                        dispatch(statusSaved());
                    });
                }
            }
        }
    }

    _fundSubNodeUpdate(versionId, refTables, data) {
        return {
            type: types.FUND_SUBNODE_UPDATE,
            data,
            versionId,
            refTables,
        };
    }

    /**
     * Akce přidání nové prázdné hodnoty descItem vícehodnotového atributu descItemType.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění nové hodnoty
     */
    fundSubNodeFormValueAdd(versionId, routingKey, valueLocation) {
        return {
            type: types.FUND_SUB_NODE_FORM_VALUE_ADD,
            area: this.area,
            versionId,
            routingKey,
            valueLocation,
        };
    }

    /**
     * Akce na nastavení hodnoty atributu jako Nezjištěno.
     *
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění nové hodnoty
     * @param descItem
     */
    fundSubNodeFormValueNotIdentified(versionId, routingKey, valueLocation, descItem) {
        return (dispatch, getState) => {
            let undef = descItem.undefined || false;
            descItem.undefined = !undef;

            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            const parentId = subNodeForm.data.parent.id;
            const parentVersionId = subNodeForm.data.parent.version;

            if (!undef) {
                this._formValueStore(dispatch, getState, versionId, routingKey, valueLocation, descItem);
            } else {
                this._callDeleteDescItem(versionId, parentId, parentVersionId, descItem);
            }
            /*
            let loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            if (descItem && descItem.undefined) {
                this._callUnsetNotIdentifiedDescItem(versionId, subNodeForm.nodeId, subNodeForm.data.parent.version, loc.descItemType.id, descItem.descItemSpecId, descItem.descItemObjectId)
                    .then(json => {
                        dispatch(this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, 'UPDATE'));
                        dispatch(this.fundSubNodeFormValueAdd(versionId, routingKey, valueLocation));
                        if (descItem.descItemSpecId) {
                            state = getState();
                            subNodeForm = this._getItemFormStore(state, versionId, routingKey);
                            loc = subNodeForm.getLoc(subNodeForm, valueLocation);
                            const valueLocationNew = {...valueLocation, descItemIndex: loc.descItemType.descItems.length - 1};
                            dispatch(this.fundSubNodeFormValueChangeSpec(versionId, routingKey, valueLocationNew, descItem.descItemSpecId));
                        }
                    });
            } else {
                this._callSetNotIdentifiedDescItem(versionId, subNodeForm.nodeId, subNodeForm.data.parent.version, loc.descItemType.id, descItem.descItemSpecId, descItem.descItemObjectId)
                    .then(json => {
                        dispatch(this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, 'CREATE'));
                    });
            }*/
        };
    }

    /** Metoda pro volání API. */
    // @Abstract
    _callArrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file) {}

    /**
     * Akce přidání coordinates jako DescItem - Probíhá uploadem - doplnění hodnot pomocí WS
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {int} descItemTypeId Konkrétní typ desc item
     * @param {File} file Soubor k uploadu
     */
    fundSubNodeFormValueUploadCoordinates(versionId, routingKey, descItemTypeId, file) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            this._callArrCoordinatesImport(
                versionId,
                subNodeForm.data.parent.id,
                subNodeForm.data.parent.version,
                descItemTypeId,
                file,
            )
                .then(() => {
                    dispatch(addToastrSuccess(i18n('import.toast.success'), i18n('import.toast.successCoordinates')));
                })
                .catch(() => {
                    dispatch(addToastrDanger(i18n('import.toast.error'), i18n('import.toast.errorCoordinates')));
                });
        };
    }

    /** Metoda pro volání API. */
    // @Abstract
    _callDescItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file) {}

    /**
     * Akce přidání csv jako DescItem - Probíhá uploadem.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {int} descItemTypeId Konkrétní typ desc item
     * @param {File} file Soubor k uploadu
     */
    fundSubNodeFormValueUploadCsv(versionId, routingKey, descItemTypeId, file) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);

            this._callDescItemCsvImport(
                versionId,
                subNodeForm.data.parent.id,
                subNodeForm.data.parent.version,
                descItemTypeId,
                file,
            )
                .then(() => {
                    dispatch(addToastrSuccess(i18n('import.toast.success'), i18n('import.toast.successJsonTable')));
                })
                .catch(() => {
                    dispatch(addToastrDanger(i18n('import.toast.error'), i18n('import.toast.errorJsonTable')));
                });
        };
    }

    /**
     * Akce validace hodnoty na serveru - týká se jen hodnot datace.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty pro validaci
     */
    fundSubNodeFormValueValidate(versionId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            // only when loc exists
            if (loc) {
                WebApi.validateUnitdate(loc.descItem.value).then(json => {
                    dispatch(this._fundSubNodeFormValueValidateResult(versionId, routingKey, valueLocation, json));
                });
            }
        };
    }

    /**
     * Akce změny hodnoty a její promítnutí do store, případné uložení na server, pokud je toto vynuceno parametrem forceStore.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value nová hodnota
     * @param {boolean} forceStore pokud je true, je hodnota i odeslána na server pro uložení
     */
    fundSubNodeFormValueChange(versionId, routingKey, valueLocation, value, forceStore) {
        return (dispatch, getState) => {
            dispatch({
                type: types.FUND_SUB_NODE_FORM_VALUE_CHANGE,
                area: this.area,
                versionId,
                routingKey,
                valueLocation,
                value,
                dispatch,
                formActions: this,
            });

            if (forceStore) {
                this._formValueStore(dispatch, getState, versionId, routingKey, valueLocation);
            }
        };
    }

    /**
     * Akce změna pozice hodnoty vícehodnotového atributu - změna pořadí hodnot.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {boolean} index nový index hodnoty v rámci atributu
     */
    fundSubNodeFormValueChangePosition(versionId, routingKey, valueLocation, index) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            if (!loc.descItem.error.hasError && typeof loc.descItem.id !== 'undefined') {
                dispatch({
                    type: types.FUND_SUB_NODE_FORM_VALUE_CHANGE_POSITION,
                    area: this.area,
                    versionId,
                    routingKey,
                    valueLocation,
                    index,
                });

                const descItem = {...loc.descItem, position: index + 1};

                this._callUpdateDescItem(
                    dispatch,
                    subNodeForm,
                    versionId,
                    subNodeForm.data.parent.version,
                    descItem,
                ).then(json => {
                    const newValueLocation = {...valueLocation, descItemIndex: index};
                    dispatch(
                        this._fundSubNodeFormDescItemResponse(versionId, routingKey, newValueLocation, json, 'UPDATE'),
                    );
                });
            }
        };
    }

    /**
     * Akce změna hodnoty atributu - odkaz na osoby.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value hodnota
     */
    fundSubNodeFormValueChangeParty(versionId, routingKey, valueLocation, value) {
        return (dispatch, getState) => {
            dispatch({
                type: types.FUND_SUB_NODE_FORM_VALUE_CHANGE_PARTY,
                area: this.area,
                versionId,
                routingKey,
                valueLocation,
                value,
                dispatch,
            });
        };
    }

    /**
     * Akce změna hodnoty atributu - odkaz na rejstřík.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value hodnota
     */
    fundSubNodeFormValueChangeRecord(versionId, routingKey, valueLocation, value) {
        return (dispatch, getState) => {
            dispatch({
                type: types.FUND_SUB_NODE_FORM_VALUE_CHANGE_RECORD,
                area: this.area,
                versionId,
                routingKey,
                valueLocation,
                value,
                dispatch,
            });
        };
    }

    /**
     * Akce změna hodnoty specifikace atributu.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value hodnota
     */
    fundSubNodeFormValueChangeSpec(versionId, routingKey, valueLocation, value) {
        return (dispatch, getState) => {
            // Dispatch zmněny specifikace
            dispatch({
                type: types.FUND_SUB_NODE_FORM_VALUE_CHANGE_SPEC,
                area: this.area,
                versionId,
                routingKey,
                valueLocation,
                value,
            });

            // Vynucení uložení na server, pokud je validní jako celek
            this._formValueStore(dispatch, getState, versionId, routingKey, valueLocation);
        };
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
                // Nelze použít valuesEquals (prázdný string není !== undefined)
                if (descItem.description !== descItem.prevDescription) {
                    needUpdate = true;
                }
                if (descItem.refTemplateId !== descItem.prevRefTemplateId) {
                    needUpdate = true;
                }

                return needUpdate;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Blur na hodnotě atributu.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    fundSubNodeFormValueBlur(versionId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            dispatch({
                type: types.FUND_SUB_NODE_FORM_VALUE_BLUR,
                area: this.area,
                versionId,
                routingKey,
                valueLocation,
                receivedAt: Date.now(),
            });

            this._formValueStore(dispatch, getState, versionId, routingKey, valueLocation);
        };
    }

    /** Metoda pro volání API. */
    // @Abstract
    _callDeleteDescItem(versionId, parentId, parentVersionId, descItem) {}

    /**
     * Smazání hodnoty atributu.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    fundSubNodeFormValueDelete(versionId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            dispatch({
                type: types.FUND_SUB_NODE_FORM_VALUE_DELETE,
                area: this.area,
                versionId,
                routingKey,
                valueLocation,
            });

            if (typeof loc.descItem.id !== 'undefined') {
                this._callDeleteDescItem(
                    versionId,
                    subNodeForm.data.parent.id,
                    subNodeForm.data.parent.version,
                    loc.descItem,
                ).then(json => {
                    dispatch(
                        this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, 'DELETE'),
                    );
                });
            }
        };
    }

    fundSubNodeFormHandleClose(versionId, routingKey) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            const valueLocation = getFocusDescItemLocation(subNodeForm);
            if (valueLocation !== null) {
                dispatch(this.fundSubNodeFormValueBlur(versionId, routingKey, valueLocation));
            }
        };
    }

    /**
     * Přidání atributu na formulář.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {int} descItemTypeId id atributu
     */
    fundSubNodeFormDescItemTypeAdd(versionId, routingKey, descItemTypeId) {
        return (dispatch, getState) => {
            dispatch({
                type: types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD,
                area: this.area,
                versionId,
                routingKey,
                descItemTypeId,
            });

            //const state = getState();
            //const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            dispatch(
                setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {
                    descItemTypeId: descItemTypeId,
                    descItemObjectId: null,
                }),
            );
        };
    }

    fundSubNodeFormTemplateUse(versionId, routingKey, template, replaceValues, addItemTypeIds) {
        return (dispatch, getState) => {
            const state = getState();
            dispatch({
                type: types.FUND_SUB_NODE_FORM_TEMPLATE_USE,
                area: this.area,
                versionId,
                routingKey,
                template,
                replaceValues,
                groups: state.refTables.groups.data,
                addItemTypeIds,
            });
            dispatch({
                type: types.FUND_TEMPLATE_USE,
                area: this.area,
                versionId,
                template,
            });
        };
    }

    fundSubNodeFormTemplateUseOnly(versionId, template) {
        return dispatch => {
            dispatch({
                type: types.FUND_TEMPLATE_USE,
                area: this.area,
                versionId,
                template,
            });
        };
    }

    /** Metoda pro volání API. */
    // @Abstract
    _callDeleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId) {}

    /** Metoda pro volání API. */
    // @Abstract
    _callSetNotIdentifiedDescItem(versionId, elementId, parentNodeVersion, itemTypeId, itemSpecId, itemObjectId) {}

    /** Metoda pro volání API. */
    // @Abstract
    _callUnsetNotIdentifiedDescItem(versionId, elementId, parentNodeVersion, itemTypeId, itemSpecId, itemObjectId) {}

    /**
     * Přidání PP (který je počítaný a ještě ve formuláři není) do formuláře
     * @param versionId verze AS
     * @param itemTypeId id typu PP
     * @param strict
     */
    addCalculatedDescItem(versionId, itemTypeId, strict = false) {
        return (dispatch, getState) => {
            const state = getState();
            const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
            if (fundIndex !== null) {
                const getOutputId = state.arrRegion.funds[fundIndex].fundOutput.fundOutputDetail.subNodeForm.fetchingId;
                return WebApi.switchOutputCalculating(versionId, getOutputId, itemTypeId, strict).then(data => {
                    if (!data) {
                        dispatch(this.fundSubNodeFormDescItemTypeAdd(versionId, 1, itemTypeId));
                    }
                });
            }
        };
    }

    /**
     * Přepnutí počítání hodnot atributu uživatelské/automatické.
     *
     * @param {int} versionId identifikátor verze AS
     * @param {int} itemTypeId identifikátor typu atributu
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění atributu
     */
    switchOutputCalculating(versionId, itemTypeId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            const state = getState();
            const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
            if (fundIndex !== null) {
                const getOutputId = state.arrRegion.funds[fundIndex].fundOutput.fundOutputDetail.subNodeForm.fetchingId;
                WebApi.switchOutputCalculating(versionId, getOutputId, itemTypeId).then(() => {
                    dispatch({
                        type: types.FUND_SUB_NODE_FORM_OUTPUT_CALC_SWITCH,
                        area: this.area,
                        versionId,
                        routingKey,
                        valueLocation,
                    });
                });
            }
        };
    }

    /**
     * Smazání atributu.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění atributu
     */
    fundSubNodeFormDescItemTypeDelete(versionId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            let hasDescItemsForDelete = false;
            loc.descItemType.descItems.forEach(descItem => {
                if (typeof descItem.id !== 'undefined') {
                    hasDescItemsForDelete = true;
                }
            });

            dispatch(this._fundSubNodeFormDescItemTypeDeleteInStore(versionId, routingKey, valueLocation, false));

            if (hasDescItemsForDelete) {
                this._callDeleteDescItemType(
                    versionId,
                    subNodeForm.data.parent.id,
                    subNodeForm.data.parent.version,
                    loc.descItemType.id,
                ).then(json => {
                    dispatch(
                        this._fundSubNodeFormDescItemResponse(
                            versionId,
                            routingKey,
                            valueLocation,
                            json,
                            'DELETE_DESC_ITEM_TYPE',
                        ),
                    );
                });
            }
        };
    }

    /**
     * Informační akce o provedené operaci kopírování hodnot atributu z předchozí JP.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění atributu
     * @param {Object} copySiblingResult nová nakopírovaná data - objekt ze serveru
     */
    fundSubNodeFormDescItemTypeCopyFromPrevResponse(versionId, routingKey, valueLocation, copySiblingResult) {
        return {
            type: types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE,
            area: this.area,
            versionId,
            routingKey,
            valueLocation,
            copySiblingResult,
        };
    }

    /**
     * Focus na hodnotě atributu.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    fundSubNodeFormValueFocus(versionId, routingKey, valueLocation) {
        return {
            type: types.FUND_SUB_NODE_FORM_VALUE_FOCUS,
            area: this.area,
            versionId,
            routingKey,
            valueLocation,
        };
    }

    // @Abstract
    _getParentObjIdInfo(parentObjStore, routingKey) {}

    /**
     * Vyžádání dat - aby byla ve store k dispozici.
     * @param {int} versionId verze AS
     * @param {int|null} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param needClean
     */
    fundSubNodeFormFetchIfNeeded(versionId, routingKey, needClean = false, showChildren, showParents) {
        return (dispatch, getState) => {
            const state = getState();

            // Fetch může být pouze v případě, že už jsou načteny číselníkové hodnoty na typy položek v ref
            if (
                !state.refTables.rulDataTypes.isFetching &&
                state.refTables.rulDataTypes.fetched &&
                !state.refTables.descItemTypes.isFetching &&
                state.refTables.descItemTypes.fetched
            ) {
                const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
                const parentObjStore = this._getParentObjStore(state, versionId, routingKey);
                if (
                    (!subNodeForm.fetched || subNodeForm.dirty || subNodeForm.needClean || needClean) &&
                    !subNodeForm.isFetching
                ) {
                    const parentObjIdInfo = this._getParentObjIdInfo(parentObjStore, routingKey);
                    dispatch(
                        this._fundSubNodeFormFetch(
                            versionId,
                            parentObjIdInfo.parentId,
                            routingKey,
                            subNodeForm.needClean || needClean,
                            showChildren,
                            showParents,
                        ),
                    );
                }
            }
        };
    }

    /**
     * Nová data byla načtena.
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} json objekt s daty
     * @param {Object} rulDataTypes store - datové typy pro atributy
     * @param {Object} descItemTypes store - obecný předpis atributů - ref
     * @param {Object} groups store - skupiny pro typy atributů
     * @param {bool} needClean má se formulář reinicializovat a vymazat cšechna editace? - jako nové načtení formuláře
     */
    fundSubNodeFormReceive(versionId, nodeId, routingKey, json, rulDataTypes, descItemTypes, groups, needClean) {
        return {
            type: types.FUND_SUB_NODE_FORM_RECEIVE,
            area: this.area,
            versionId,
            nodeId,
            routingKey,
            data: json,
            rulDataTypes,
            refDescItemTypes: descItemTypes,
            groups,
            receivedAt: Date.now(),
            needClean,
        };
    }

    /**
     * Bylo zahájeno nové načítání dat.
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     */
    fundSubNodeFormRequest(versionId, nodeId, routingKey) {
        return {
            type: types.FUND_SUB_NODE_FORM_REQUEST,
            area: this.area,
            versionId,
            nodeId,
            routingKey,
        };
    }
}

//var debouncedGetFundNodeForm = debounce(WebApi.getFundNodeForm,200);
class NodeFormActions extends ItemFormActions {
    static AREA = 'NODE';

    constructor() {
        super(NodeFormActions.AREA);
    }

    /**
     * Akce kopírování hodnot konkrétního atributu z předcházející JP.
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} nodeVersionId verze node
     * @param {int} descItemTypeId id atribtu
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění
     */
    fundSubNodeFormValuesCopyFromPrev(versionId, nodeId, nodeVersionId, descItemTypeId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            dispatch(this._fundSubNodeFormDescItemTypeDeleteInStore(versionId, routingKey, valueLocation, true));
            WebApi.copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId).then(json => {
                dispatch(
                    this.fundSubNodeFormDescItemTypeCopyFromPrevResponse(versionId, routingKey, valueLocation, json),
                );
            });
        };
    }

    /**
     * Načtení server dat pro formulář pro aktuálně předané parametry s využitím cache - pokud jsou data v cache, použije je, jinak si vyžádá nová data a zajistí i nakešování okolí.
     * Odpovídá volání WebApi.getFundNodeForm, jen dále zajišťuje cache.
     */
    //@Override
    _getItemFormData(getState, dispatch, versionId, nodeId, routingKey, showChildren, showParents) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE': // podpora kešování
                const state = getState();
                const node = this._getParentObjStore(state, versionId, routingKey);
                if (node === null) {
                    console.error('Node not found, versionId=' + versionId);
                    return; // nemělo by nastat
                }

                const subNodeFormCache = node.subNodeFormCache;

                const data = subNodeFormCache.dataCache[nodeId];
                if (!data) {
                    // není v cache, načteme ji včetně okolí
                    // ##
                    // # Data pro cache, jen pokud již cache nenačítá
                    // ##
                    if (false) {
                        if (node.isNodeInfoFetching || !node.nodeInfoFetched || node.nodeInfoDirty) {
                            // nemáme platné okolí (okolní NODE) pro daný NODE, raději je načteme ze serveru; nemáme vlastně okolní NODE pro získání seznamu ID pro načtení formulářů pro cache
                            //console.log('### READ_CACHE', 'around')

                            dispatch(this._fundSubNodeFormCacheRequest(versionId, routingKey));
                            WebApi.getFundNodeFormsWithAround(versionId, nodeId, CACHE_SIZE2).then(json => {
                                dispatch(this._fundSubNodeFormCacheResponse(versionId, routingKey, json.forms));
                            });
                        } else {
                            // pro získání id okolí můžeme použít store
                            // Načtení okolí položky
                            const index = indexById(node.childNodes, nodeId);
                            const left = node.childNodes.slice(Math.max(index - CACHE_SIZE2, 0), index);
                            const right = node.childNodes.slice(index, index + CACHE_SIZE2);

                            const idsForFetch = [];
                            left.forEach(n => {
                                if (!subNodeFormCache.dataCache[n.id]) {
                                    idsForFetch.push(n.id);
                                }
                            });
                            right.forEach(n => {
                                if (!subNodeFormCache.dataCache[n.id]) {
                                    idsForFetch.push(n.id);
                                }
                            });

                            //console.log('### READ_CACHE', idsForFetch, node.childNodes, left, right)

                            if (idsForFetch.length > 0) {
                                // máme něco pro načtení
                                dispatch(this._fundSubNodeFormCacheRequest(versionId, routingKey));
                                WebApi.getFundNodeForms(versionId, idsForFetch).then(json => {
                                    dispatch(this._fundSubNodeFormCacheResponse(versionId, routingKey, json.forms));
                                });
                            }
                        }
                    }

                    // ##
                    // # Data požadovaného formuláře
                    // ##

                    const nodeParam = {nodeId};
                    const resultParam = {
                        formData: true,
                        parents: showParents && node.changeParent,
                        children: showChildren,
                        siblingsFrom: node.viewStartIndex,
                        siblingsMaxCount: node.pageSize,
                        siblingsFilter: node.filterText,
                    };
                    return WebApi.getNodeData(versionId, nodeParam, resultParam).then(json => {
                        dispatch(
                            fundNodeInfoReceive(versionId, nodeId, routingKey, {
                                childNodes: json.siblings ? json.siblings : null,
                                nodeCount: json.nodeCount,
                                nodeIndex: json.nodeIndex,
                                parentNodes: json.parents ? json.parents : null,
                            }),
                        );

                        dispatch(fundSubNodeInfoReceive(versionId, nodeId, routingKey, {nodes: json.children}));

                        return json.formData;
                    });
                } else {
                    // je v cache, vrátíme ji
                    //console.log('### USE_CACHE')
                    return new Promise(function(resolve, reject) {
                        resolve(data);
                    });
                }
            case 'DATA_GRID': // není podpora kešování
                const nodeParam = {nodeId};
                const resultParam = {
                    formData: true,
                };
                return WebApi.getNodeData(versionId, nodeParam, resultParam).then(json => json.formData);
            default:
                break;
        }
    }

    // @Override
    _getItemFormStore(state, versionId, routingKey) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE':
                const node = this._getParentObjStore(state, versionId, routingKey);
                if (node !== null) {
                    return node.subNodeForm;
                } else {
                    return null;
                }
            case 'DATA_GRID':
                const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
                if (fundIndex !== null) {
                    return state.arrRegion.funds[fundIndex].fundDataGrid.subNodeForm;
                } else {
                    return null;
                }
            default:
                break;
        }

        return null;
    }

    // @Override
    _getParentObjStore(state, versionId, routingKey) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE':
                const r = findByRoutingKeyInGlobalState(state, versionId, routingKey);
                if (r !== null) {
                    return r.node;
                }
                break;
            case 'DATA_GRID':
                const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
                if (fundIndex !== null) {
                    return state.arrRegion.funds[fundIndex].fundDataGrid;
                }
                break;
            default:
                break;
        }

        return null;
    }

    // @Override
    _callUpdateDescItem(dispatch, formState, versionId, parentVersionId, parentId, descItem) {
        // Umělé navýšení verze o 1 - aby mohla pozitivně projít případná další update operace
        console.log('Before update, parentVersionId: ', parentVersionId);
        dispatch(increaseNodeVersion(versionId, parentId, parentVersionId));

        console.log('update desc Item');
        return new Promise((resolve, reject) => {
            NodeRequestController.updateRequest(versionId, parentVersionId, parentId, descItem, json => {
                resolve(json);
            });
        });
    }

    // @Override
    _callDeleteDescItem(versionId, parentId, parentVersionId, descItem) {
        return WebApi.deleteDescItem(versionId, parentId, parentVersionId, descItem);
    }

    // @Override
    _callCreateDescItem(versionId, parentId, parentVersionId, descItemTypeId, descItem) {
        console.log('create desc Item');
        return WebApi.createDescItem(versionId, parentId, parentVersionId, descItemTypeId, descItem);
    }

    // @Override
    _callArrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        return WebApi.arrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDescItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        return WebApi.descItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDeleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId) {
        return WebApi.deleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId);
    }

    // @Override
    _callSetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        descItemTypeId,
        descItemSpecId,
        descItemObjectId,
    ) {
        return WebApi.setNotIdentifiedDescItem(
            versionId,
            nodeId,
            parentNodeVersion,
            descItemTypeId,
            descItemSpecId,
            descItemObjectId,
        );
    }

    // @Override
    _callUnsetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        descItemTypeId,
        descItemSpecId,
        descItemObjectId,
    ) {
        return WebApi.unsetNotIdentifiedDescItem(
            versionId,
            nodeId,
            parentNodeVersion,
            descItemTypeId,
            descItemSpecId,
            descItemObjectId,
        );
    }

    // @Override
    _getParentObjIdInfo(parentObjStore, routingKey) {
        const type = getRoutingKeyType(routingKey);
        switch (type) {
            case 'NODE':
                return {
                    parentId: parentObjStore.selectedSubNodeId,
                    parentVersion: parentObjStore.subNodeForm.versionId,
                };
            case 'DATA_GRID':
                return {parentId: parentObjStore.nodeId, parentVersion: parentObjStore.subNodeForm.versionId};
            default:
                break;
        }
    }
}

class OutputFormActions extends ItemFormActions {
    static AREA = 'OUTPUT';

    constructor() {
        super(OutputFormActions.AREA);
    }

    /** Načtení server dat pro formulář pro aktuálně předané parametry BEZ využití cache. */
    //@Override
    _getItemFormData(getState, dispatch, versionId, nodeId, routingKey) {
        // není podpora kešování
        return WebApi.getOutputNodeForm(versionId, nodeId);
    }

    // @Override
    _getItemFormStore(state, versionId, routingKey) {
        const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
        if (fundIndex !== null) {
            return state.arrRegion.funds[fundIndex].fundOutput.fundOutputDetail.subNodeForm;
        } else {
            return null;
        }
    }

    // @Override
    _getParentObjStore(state, versionId, routingKey) {
        const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
        if (fundIndex !== null) {
            const fund = state.arrRegion.funds[fundIndex];
            return fund.fundOutput.fundOutputDetail;
        } else {
            return null;
        }
    }

    // @Override
    _callUpdateDescItem(dispatch, formState, fundVersionId, outputVersionId, outputId, descItem) {
        // increase output version
        dispatch(outputIncreaseNodeVersion(fundVersionId, outputId, outputVersionId));

        return WebApi.updateOutputItem(fundVersionId, outputVersionId, descItem);
    }

    // @Override
    _callDeleteDescItem(versionId, parentId, parentVersionId, descItem) {
        return WebApi.deleteOutputItem(versionId, parentId, parentVersionId, descItem);
    }

    // @Override
    _callCreateDescItem(versionId, parentId, nodeVersionId, descItemTypeId, descItem) {
        return WebApi.createOutputItem(versionId, parentId, nodeVersionId, descItemTypeId, descItem);
    }

    // @Override
    _callArrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        return WebApi.arrOutputCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDescItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        return WebApi.descOutputItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file);
    }

    // @Override
    _callDeleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId) {
        return WebApi.deleteOutputItemType(versionId, parentId, parentVersionId, descItemTypeId);
    }

    // @Override
    _callSetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        descItemTypeId,
        descItemSpecId,
        descItemObjectId,
    ) {
        return WebApi.setNotIdentifiedOutputItem(
            versionId,
            nodeId,
            parentNodeVersion,
            descItemTypeId,
            descItemSpecId,
            descItemObjectId,
        );
    }

    // @Override
    _callUnsetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        outputItemTypeId,
        outputItemSpecId,
        outputItemObjectId,
    ) {
        return WebApi.unsetNotIdentifiedOutputItem(
            versionId,
            nodeId,
            parentNodeVersion,
            outputItemTypeId,
            outputItemSpecId,
            outputItemObjectId,
        );
    }

    // @Override
    _getParentObjIdInfo(parentObjStore, routingKey) {
        return {parentId: parentObjStore.id, parentVersion: parentObjStore.version};
    }
}

class StructureFormActions extends ItemFormActions {
    static AREA = 'STRUCTURE';

    constructor() {
        super(StructureFormActions.AREA);
    }

    /** Načtení server dat pro formulář pro aktuálně předané parametry BEZ využití cache. */
    //@Override
    _getItemFormData(getState, dispatch, versionId, nodeId, routingKey) {
        // není podpora kešování
        return WebApi.getFormStructureItems(versionId, nodeId).then(i => ({...i, parent: {...i.parent, version: 0}}));
    }

    // @Override
    _getItemFormStore(state, versionId, routingKey) {
        const subStore = state.structures.stores[String(routingKey)];
        if (!!subStore) {
            return subStore.subNodeForm;
        } else {
            return null;
        }
    }

    // @Override
    _getParentObjStore(state, versionId, routingKey) {
        const subStore = state.structures.stores[String(routingKey)];
        if (!!subStore) {
            return subStore;
        } else {
            return null;
        }
    }

    // @Override
    _callCreateDescItem(versionId, parentId, nodeVersionId, descItemTypeId, descItem) {
        return WebApi.createStructureItem(versionId, parentId, descItemTypeId, descItem);
    }

    // @Override
    _callUpdateDescItem(dispatch, formState, versionId, parentVersionId, parentId, descItem) {
        return WebApi.updateStructureItem(versionId, descItem);
    }

    // @Override
    _callDeleteDescItem(versionId, parentId, parentVersionId, descItem) {
        return WebApi.deleteStructureItem(versionId, descItem);
    }

    // @Override
    _callArrCoordinatesImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        // Not implemented
    }

    // @Override
    _callDescItemCsvImport(versionId, parentId, parentVersionId, descItemTypeId, file) {
        // Not implemented
    }

    // @Override
    _callDeleteDescItemType(versionId, parentId, parentVersionId, descItemTypeId) {
        return WebApi.deleteStructureItemsByType(versionId, parentId, descItemTypeId);
    }

    // @Override
    _callSetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        descItemTypeId,
        descItemSpecId,
        descItemObjectId,
    ) {
        // Not implemented
    }

    // @Override
    _callUnsetNotIdentifiedDescItem(
        versionId,
        nodeId,
        parentNodeVersion,
        outputItemTypeId,
        outputItemSpecId,
        outputItemObjectId,
    ) {
        // Not implemented
    }

    // @Override
    _getParentObjIdInfo(parentObjStore, routingKey) {
        return {parentId: parentObjStore.id, parentVersion: parentObjStore.version};
    }
}

export const nodeFormActions = new NodeFormActions();
export const outputFormActions = new OutputFormActions();
export const structureFormActions = new StructureFormActions();
