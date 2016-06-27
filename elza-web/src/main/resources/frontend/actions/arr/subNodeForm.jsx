/**
 * Akce pro formulář JP.
 */

// Konfigurace velikosti cache dat pro formulář
const CACHE_SIZE = 20
const CACHE_SIZE2 = CACHE_SIZE/2

import {WebApi} from 'actions/index.jsx';
import {getMapFromList, indexById, findByRoutingKeyInGlobalState} from 'stores/app/utils.jsx'
import {getFocusDescItemLocation} from 'stores/app/arr/subNodeFormUtils.jsx'
import {valuesEquals} from 'components/Utils.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import {getRoutingKeyType} from 'stores/app/utils.jsx'
import * as types from 'actions/constants/ActionTypes.js';
import {addToastrSuccess,addToastrDanger} from 'components/shared/toastr/ToastrActions.jsx'
import {i18n} from 'components/index.jsx';

class ItemFormActions {
    constructor(area) {
        this.area = area;
    }

    isSubNodeFormCacheAction(action) {
        switch (action.type) {
            case types.FUND_SUB_NODE_FORM_CACHE_RESPONSE:
            case types.FUND_SUB_NODE_FORM_CACHE_REQUEST:
                return true
            default:
                return false
        }
    }

    isSubNodeFormAction(action) {
        switch (action.type) {
            case types.FUND_SUB_NODE_FORM_REQUEST:
            case types.FUND_SUB_NODE_FORM_RECEIVE:
            case types.FUND_SUB_NODE_FORM_VALUE_CHANGE:
            case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_POSITION:
            case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
            case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_PARTY:
            case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_RECORD:
            case types.FUND_SUB_NODE_FORM_VALUE_VALIDATE_RESULT:
            case types.FUND_SUB_NODE_FORM_VALUE_BLUR:
            case types.FUND_SUB_NODE_FORM_VALUE_FOCUS:
            case types.FUND_SUB_NODE_FORM_VALUE_ADD:
            case types.FUND_SUB_NODE_FORM_VALUE_DELETE:
            case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE:
            case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD:
            case types.FUND_SUB_NODE_FORM_VALUE_RESPONSE:
            case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE:
                return true
            default:
                return false
        }
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
            result
        }
    }

    /**
     * Smazání atributu POUZE z formuláře, nikoli na serveru!
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění atributu
     * @param {bool} onlyDescItems pokud je true, pouze se odeberou hodnoty atributu, ale daný atribut na formuláři zůstane, pokud je false, odebere se i atribut
     */
    _fundSubNodeFormDescItemTypeDeleteInStore(versionId, routingKey, valueLocation, onlyDescItems) {
        return {
            type: types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE,
            area: this.area,
            versionId,
            routingKey,
            valueLocation,
            onlyDescItems
        }
    }

    /**
     * Informační akce o provedené operaci na serveru.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
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
            descItemResult: descItemResult
        }
    }

    /**
     * Načtení server dat pro formulář node pro aktuálně předané parametry s využitím cache - pokud jsou data v cache, použije je, jinak si vyžádá nová data a zajistí i nakešování okolí.
     * Odpovídá volání WebApi.getFundNodeForm, jen dále zajišťuje cache.
     * @param {Object} getState odkaz na funkci pro načtení store
     * @param {Object} dispatch odkaz na funkci dispatch
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @return {Object} promise pro vrácení nových dat
     */
    _getNodeForm(getState, dispatch, versionId, nodeId, routingKey) {
        const type = getRoutingKeyType(routingKey)
        switch (type) {
            case 'NODE':    // podpora kešování
                var state = getState()
                var node = this._getNodeStore(state, versionId, routingKey);
                if (node === null) return   // nemělo by nastat

                const subNodeFormCache = node.subNodeFormCache

                var data = subNodeFormCache.dataCache[nodeId]
                if (!data) {    // není v cache, načteme ji včetně okolí
                    // ##
                    // # Data pro cache, jen pokud již cache nenačítá
                    // ##
                    if (!subNodeFormCache.isFetching) {
                        if (node.isNodeInfoFetching || !node.nodeInfoFetched || node.nodeInfoDirty) {   // nemáme platné okolí (okolní NODE) pro daný NODE, raději je načteme ze serveru; nemáme vlastně okolní NODE pro získání seznamu ID pro načtení formulářů pro cache
                            //console.log('### READ_CACHE', 'around')

                            dispatch(this._fundSubNodeFormCacheRequest(versionId, routingKey))
                            WebApi.getFundNodeFormsWithAround(versionId, nodeId, CACHE_SIZE2)
                                .then(json => {
                                    dispatch(this._fundSubNodeFormCacheResponse(versionId, routingKey, json.forms))
                                })
                        } else {    // pro získání id okolí můžeme použít store
                            // Načtení okolí položky
                            var index = indexById(node.childNodes, nodeId)
                            var left = node.childNodes.slice(Math.max(index - CACHE_SIZE2, 0), index)
                            var right = node.childNodes.slice(index, index + CACHE_SIZE2)

                            var idsForFetch = []
                            left.forEach(n => {
                                if (!subNodeFormCache.dataCache[n.id]) {
                                    idsForFetch.push(n.id)
                                }
                            })
                            right.forEach(n => {
                                if (!subNodeFormCache.dataCache[n.id]) {
                                    idsForFetch.push(n.id)
                                }
                            })

                            //console.log('### READ_CACHE', idsForFetch, node.childNodes, left, right)

                            if (idsForFetch.length > 0) {   // máme něco pro načtení
                                dispatch(this._fundSubNodeFormCacheRequest(versionId, routingKey))
                                WebApi.getFundNodeForms(versionId, idsForFetch)
                                    .then(json => {
                                        dispatch(this._fundSubNodeFormCacheResponse(versionId, routingKey, json.forms))
                                    })
                            }
                        }
                    }

                    // ##
                    // # Data požadovaného formuláře
                    // ##
                    return WebApi.getFundNodeForm(versionId, nodeId)
                } else {    // je v cache, vrátíme ji
                    //console.log('### USE_CACHE')
                    return new Promise(function (resolve, reject) {
                        resolve(data)
                    })
                }
            case 'DATA_GRID':   // není podpora kešování
                return WebApi.getFundNodeForm(versionId, nodeId)
        }
    }

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
        }
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
            formsMap
        }
    }

    /**
     * Načtení subNodeForm store.
     * @param {Object} state globální store
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @return subNodeForm store
     */
    _getSubNodeFormStore(state, versionId, routingKey) {
        const type = getRoutingKeyType(routingKey)
        switch (type) {
            case 'NODE':
                var node = this._getNodeStore(state, versionId, routingKey)
                if (node !== null) {
                    return node.subNodeForm
                } else {
                    return null
                }
            case 'DATA_GRID':
                var fundIndex = indexById(state.arrRegion.funds, versionId, "versionId");
                if (fundIndex !== null) {
                    return state.arrRegion.funds[fundIndex].fundDataGrid.subNodeForm
                } else {
                    return null
                }
        }

        return null;
    }

    /**
     * Načtení node store pro předaná data.
     * @param {Object} state globální store
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @return node store
     */
    _getNodeStore(state, versionId, routingKey) {
        var r = findByRoutingKeyInGlobalState(state, versionId, routingKey);
        if (r != null) {
            return r.node;
        }

        return null;
    }

    /**
     * Nové načtení dat.
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     */
    _fundSubNodeFormFetch(versionId, nodeId, routingKey) {
        return (dispatch, getState) => {
            dispatch(this.fundSubNodeFormRequest(versionId, nodeId, routingKey))
            this._getNodeForm(getState, dispatch, versionId, nodeId, routingKey)
                .then(json => {
                    var state = getState()
                    var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);
                    if (subNodeForm.fetchingId == nodeId) {
                        dispatch(this.fundSubNodeFormReceive(versionId, nodeId, routingKey, json, state.refTables.rulDataTypes, state.refTables.descItemTypes))
                    }
                })
        }
    }

    /**
     * Odeslání hodnoty atributu na server - buď vytvoření nebo aktualizace.
     * @param {Object} dispatch odkaz na funkci dispatch
     * @param {Object} getState odkaz na funkci pro načtení store
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    _formValueStore(dispatch, getState, versionId, routingKey, valueLocation) {
        var state = getState();
        var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);
        var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        var refType = subNodeForm.refTypesMap[loc.descItemType.id]

        if (this.descItemNeedStore(loc.descItem, refType)) {
            if (typeof loc.descItem.id !== 'undefined') {
                WebApi.updateDescItem(versionId, subNodeForm.data.node.version, loc.descItem)
                    .then(json => {
                        dispatch(this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, 'UPDATE'));
                    })
            } else {
                WebApi.createDescItem(versionId, subNodeForm.data.node.id, subNodeForm.data.node.version, loc.descItemType.id, loc.descItem)
                    .then(json => {
                        dispatch(this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, 'CREATE'));
                    })
            }
        }
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
        }
    }

    /**
     * Akce přidání coordinates jako DescItem - Probíhá uploadem - doplnění hodnot pomocí WS
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {int} descItemTypeId Konkrétní typ desc item
     * @param {File} file Soubor k uploadu
     */
    fundSubNodeFormValueUploadCoordinates(versionId, routingKey, descItemTypeId, file) {
        return (dispatch, getState) => {
            var state = getState();
            var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);

            const formData = new FormData();
            formData.append('file', file);
            formData.append('fundVersionId', versionId);
            formData.append('descItemTypeId', descItemTypeId);
            formData.append('nodeId', subNodeForm.data.node.id);
            formData.append('nodeVersion', subNodeForm.data.node.version);
            WebApi.arrCoordinatesImport(formData).then(() => {
                dispatch(addToastrSuccess(i18n('import.toast.success'), i18n('import.toast.successCoordinates')));
            }).catch(() => {
                dispatch(addToastrDanger(i18n('import.toast.error'), i18n('import.toast.errorCoordinates')));
            });
        }
    }

    /**
     * Akce přidání csv jako DescItem - Probíhá uploadem.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {int} descItemTypeId Konkrétní typ desc item
     * @param {File} file Soubor k uploadu
     */
    fundSubNodeFormValueUploadCsv(versionId, routingKey, descItemTypeId, file) {
        return (dispatch, getState) => {
            var state = getState();
            var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);

            WebApi.descItemCsvImport(versionId, subNodeForm.data.node.id, subNodeForm.data.node.version, descItemTypeId, file).then(() => {
                dispatch(addToastrSuccess(i18n('import.toast.success'), i18n('import.toast.successJsonTable')));
            }).catch(() => {
                dispatch(addToastrDanger(i18n('import.toast.error'), i18n('import.toast.errorJsonTable')));
            });
        }
    }

    /**
     * Akce validace hodnoty na serveru - týká se jen hodnot datace.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty pro validaci
     */
    fundSubNodeFormValueValidate(versionId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            var state = getState();
            var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);
            var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            WebApi.validateUnitdate(loc.descItem.value)
                .then(json => {
                    dispatch(this._fundSubNodeFormValueValidateResult(versionId, routingKey, valueLocation, json));
                })
        }
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
                dispatch
            })

            if (forceStore) {
                this._formValueStore(dispatch, getState, versionId, routingKey, valueLocation)
            }
        }
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
            var state = getState();
            var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);
            var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            if (!loc.descItem.error.hasError && typeof loc.descItem.id !== 'undefined') {
                dispatch({
                    type: types.FUND_SUB_NODE_FORM_VALUE_CHANGE_POSITION,
                    area: this.area,
                    versionId,
                    routingKey,
                    valueLocation,
                    index,
                })

                var descItem = {...loc.descItem, position: index + 1}

                WebApi.updateDescItem(versionId, subNodeForm.data.node.version, descItem)
                    .then(json => {
                        let newValueLocation = {...valueLocation, descItemIndex: index}
                        dispatch(this._fundSubNodeFormDescItemResponse(versionId, routingKey, newValueLocation, json, 'UPDATE'));
                    })
            }
        }
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
            WebApi.copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId)
                .then(json => {
                    dispatch(this.fundSubNodeFormDescItemTypeCopyFromPrevResponse(versionId, routingKey, valueLocation, json));
                })
        }
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
                dispatch
            })
        }
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
                dispatch
            })
        }
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
            })

            // Vynucení uložení na server, pokud je validní jako celek
            this._formValueStore(dispatch, getState, versionId, routingKey, valueLocation)
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
                var needUpdate = false;
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
                receivedAt: Date.now()
            });

            this._formValueStore(dispatch, getState, versionId, routingKey, valueLocation)
        }
    }

    /**
     * Smazání hodnoty atributu.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    fundSubNodeFormValueDelete(versionId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            var state = getState();
            var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);
            var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            dispatch({
                type: types.FUND_SUB_NODE_FORM_VALUE_DELETE,
                area: this.area,
                versionId,
                routingKey,
                valueLocation,
            })

            if (typeof loc.descItem.id !== 'undefined') {
                WebApi.deleteDescItem(versionId, subNodeForm.data.node.version, loc.descItem)
                    .then(json => {
                        dispatch(this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, 'DELETE'));
                    })
            }
        }
    }

    fundSubNodeFormHandleClose(versionId, routingKey) {
        return (dispatch, getState) => {
            var state = getState()
            var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);
            const valueLocation = getFocusDescItemLocation(subNodeForm)
            if (valueLocation !== null) {
                dispatch(this.fundSubNodeFormValueBlur(versionId, routingKey, valueLocation))
            }
        }
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
                descItemTypeId
            })

            var state = getState()
            var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);
            dispatch(setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemTypeId, descItemObjectId: null}))
        }
    }

    /**
     * Smazání atributu.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění atributu
     */
    fundSubNodeFormDescItemTypeDelete(versionId, routingKey, valueLocation) {
        return (dispatch, getState) => {
            var state = getState();
            var subNodeForm = this._getSubNodeFormStore(state, versionId, routingKey);
            var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            var hasDescItemsForDelete = false;
            loc.descItemType.descItems.forEach(descItem => {
                if (typeof descItem.id !== 'undefined') {
                    hasDescItemsForDelete = true;
                }
            });

            dispatch(this._fundSubNodeFormDescItemTypeDeleteInStore(versionId, routingKey, valueLocation, false));

            if (hasDescItemsForDelete) {
                WebApi.deleteDescItemType(versionId, subNodeForm.data.node.id, subNodeForm.data.node.version, loc.descItemType.id)
                    .then(json => {
                        dispatch(this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, 'DELETE_DESC_ITEM_TYPE'));
                    })
            }
        }
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
            copySiblingResult
        }
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
        }
    }

    /**
     * Vyžádání dat - aby byla ve store k dispozici.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     */
    fundSubNodeFormFetchIfNeeded(versionId, routingKey) {
        return (dispatch, getState) => {
            var state = getState();

            const type = getRoutingKeyType(routingKey)
            switch (type) {
                case 'NODE':
                    var node = this._getNodeStore(state, versionId, routingKey)
                    if (node !== null) {
                        const subNodeForm = node.subNodeForm
                        if ((!subNodeForm.fetched || subNodeForm.dirty) && !subNodeForm.isFetching) {
                            dispatch(this._fundSubNodeFormFetch(versionId, node.selectedSubNodeId, routingKey));
                        }
                    }
                    break
                case 'DATA_GRID':
                    var fundIndex = indexById(state.arrRegion.funds, versionId, "versionId");
                    if (fundIndex !== null) {
                        const fundDataGrid = state.arrRegion.funds[fundIndex].fundDataGrid
                        const subNodeForm = fundDataGrid.subNodeForm
                        if ((!subNodeForm.fetched || subNodeForm.dirty) && !subNodeForm.isFetching) {
                            dispatch(this._fundSubNodeFormFetch(versionId, fundDataGrid.nodeId, routingKey));
                        }
                    }
                    break
            }

        }
    }

    /**
     * Nová data byla načtena.
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} json objekt s daty
     * @param {Object} rulDataTypes store - datové typy pro atributy
     * @param {Object} descItemTypes store - obecný předpis atributů - ref
     */
    fundSubNodeFormReceive(versionId, nodeId, routingKey, json, rulDataTypes, descItemTypes) {
        return {
            type: types.FUND_SUB_NODE_FORM_RECEIVE,
            area: this.area,
            versionId,
            nodeId,
            routingKey,
            data: json,
            rulDataTypes,
            refDescItemTypes: descItemTypes,
            receivedAt: Date.now()
        }
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
        }
    }
}

class NodeFormActions extends ItemFormActions {
    constructor() {
        super("NODE");
    }

}
class OutputFormActions extends ItemFormActions {
    constructor() {
        super("OUTPUT");
    }

}

module.exports = {
    nodeFormActions: new NodeFormActions(),
    outputFormActions: new OutputFormActions(),
};
