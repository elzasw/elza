/**
 * Obecné akce pro formulář v pořádání.
 */

import { setFocus } from 'actions/global/focus';
import { statusSaved, statusSaving, savingApiWrapper } from 'actions/global/status';
import { WebApi } from 'actions/index';
import { i18n } from 'components/shared';
import { addToastrDanger, addToastrSuccess } from 'components/shared/toastr/ToastrActions';
import { valuesEquals } from 'components/Utils';
import { indexById } from 'stores/app/utils';
import { ActionTypes } from 'actions/constants/ActionTypes';
import { fromDuration } from 'components/validate';
import { DisplayType, FOCUS_KEYS } from '../../../constants';
import { getMapFromList } from '../../../shared/utils';
import { DataTypeCode, ItemAvailabilityNumToEnumMap } from 'stores/app/accesspoint/itemFormUtils';
import { getFocusDescItemLocation } from 'stores/app/arr/subNodeFormUtils';
import { increaseNodeVersion } from '../node';
import { Action } from 'redux';
import { AppState, DescItemTypeRef, RefTablesState } from 'typings/store';
import { DescItem, DescItemFromServer, DescItemType } from 'typings/DescItem';
import { NodeData, ParentInfo, SubNodeForm, ValueLocationIndex } from 'typings/store/SubNodeForm.types';
import { RulDataTypeCodeEnum } from 'api/RulDataTypeCodeEnum';
import { IDescItemStructure } from 'components/arr/nodeForm/DescItemTypes';

export const STRUCTURE_AREA = 'STRUCTURE';
export const OUTPUT_AREA = 'OUTPUT';
export const NODE_AREA = 'NODE';

export interface ActionWithArea extends Action {
    area: AreaType;
}

interface ParentObjectIdInfo {
    parentId: number;
    parentVersion: number;
}

export interface CreateDescItemResult {
    item: DescItemFromServer<any>;
    parent: ParentInfo;
}

export enum AreaType {
    STRUCTURE_AREA = 'STRUCTURE',
    OUTPUT_AREA = 'OUTPUT',
    NODE_AREA = 'NODE',
}

export enum OperationType {
    CREATE = 'CREATE',
    UPDATE = 'UPDATE',
    DELETE = 'DELETE',
    DELETE_DESC_ITEM_TYPE = 'DELETE_DESC_ITEM_TYPE',
}

export class ItemFormActions {
    area = AreaType.NODE_AREA;
    constructor(area: AreaType) {
        this.area = area;
    }

    isSubNodeFormCacheActionOfArea(action: ActionWithArea, area: AreaType) {
        if (action.area === area) {
            switch (action.type) {
                case ActionTypes.FUND_SUB_NODE_FORM_CACHE_RESPONSE:
                case ActionTypes.FUND_SUB_NODE_FORM_CACHE_REQUEST:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    isSubNodeFormCacheAction(action: ActionWithArea) {
        return this.isSubNodeFormCacheActionOfArea(action, this.area);
    }

    isSubNodeFormActionOfArea(action: ActionWithArea, area: AreaType) {
        if (action.area === area) {
            switch (action.type) {
                case ActionTypes.FUND_SUB_NODE_FORM_REQUEST:
                case ActionTypes.FUND_SUB_NODE_FORM_RECEIVE:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_CREATE:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_CHANGE:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_CHANGE_POSITION:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_CHANGE_RECORD:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_VALIDATE_RESULT:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_BLUR:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_FOCUS:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_ADD:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_NOT_IDENTIFIED:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_DELETE:
                case ActionTypes.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE:
                case ActionTypes.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD:
                case ActionTypes.FUND_SUB_NODE_FORM_DESC_ITEM_TYPES_ADD_TEMPLATE:
                case ActionTypes.FUND_SUB_NODE_FORM_TEMPLATE_USE:
                case ActionTypes.FUND_SUB_NODE_FORM_VALUE_RESPONSE:
                case ActionTypes.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE:
                case ActionTypes.FUND_SUB_NODE_FORM_OUTPUT_CALC_SWITCH:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    isSubNodeFormAction(action: ActionWithArea) {
        return this.isSubNodeFormActionOfArea(action, this.area);
    }

    /**
     * Akce propagace výsledku validace hodnoty ze serveru do store.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} result výsledek validace
     */
    _fundSubNodeFormValueValidateResult(versionId: number, routingKey: string, valueLocation: ValueLocationIndex, result) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_VALIDATE_RESULT,
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
    _fundSubNodeFormDescItemTypeDeleteInStore(versionId: number, routingKey: string, valueLocation: ValueLocationIndex, onlyDescItems: boolean) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE,
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
    _fundSubNodeFormDescItemResponse(versionId: number, routingKey: string, valueLocation: ValueLocationIndex, descItemResult, operationType: OperationType) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_RESPONSE,
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
    _fundSubNodeFormDescItemCreate(versionId: number, routingKey: string, valueLocation: ValueLocationIndex) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_CREATE,
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
    _getItemFormData(_getState, _dispatch, _versionId, _nodeId, _routingKey, _showChildren, _showParents): Promise<NodeData> { return; }

    /**
     * Bylo zahájeno nové načítání dat.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     */
    _fundSubNodeFormCacheRequest(versionId: number, routingKey: string) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_CACHE_REQUEST,
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
    _fundSubNodeFormCacheResponse(versionId: number, routingKey: string, formsMap) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_CACHE_RESPONSE,
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
    _getItemFormStore(_state: AppState, _versionId: number, _routingKey: string): SubNodeForm { return; }

    /**
     * Načtení store nadřazeného objektu, např. NODE v pořádání nebo OUTPUT.
     * @param {Object} state globální store
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @return store
     */
    // @Abstract
    _getParentObjStore(_state: AppState, _versionId: number, _routingKey: string): any { return; }

    /**
     * Nové načtení dat.
     * @param {int} versionId verze AS
     * @param {int} nodeId id node záložky, které se to týká
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {bool} needClean má se formulář reinicializovat a vymazat cšechna editace? - jako nové načtení formuláře
     * @param showChildren zobrazovat potomky?
     * @param showParents zobrazovat předky ke kořeni?
     */
    _fundSubNodeFormFetch(versionId: number, nodeId: number, routingKey: string, needClean: boolean, showChildren?: boolean, showParents?: boolean) {
        return (dispatch, getState) => {
            dispatch(this.fundSubNodeFormRequest(versionId, nodeId, routingKey));
            this._getItemFormData(getState, dispatch, versionId, nodeId, routingKey, showChildren, showParents).then(
                (nodeData) => {
                    const state = getState();
                    const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
                    if (subNodeForm && subNodeForm.fetchingId == nodeId) {
                        // Nastavení správných typů u itemTypes - ze serveru chodí čísla místo enumů
                        const formData = this.transformNodeData(nodeData);

                        dispatch(
                            this.fundSubNodeFormReceive(
                                versionId,
                                nodeId,
                                routingKey,
                                formData,
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
    _callUpdateDescItem(_dispatch: (action: any) => any, _formState: any, _versionId: number, _parentVersionId: number, _parentId: number, _descItem: any): Promise<NodeData> { return; }

    /** Metoda pro volání API, vložení hodnoty */
    // @Abstract
    _callCreateDescItem(_versionId: number, _parentId: number, _parentVersionId: number, _descItemTypeId: number, _descItem: DescItem, _refType: DescItemTypeRef): Promise<CreateDescItemResult> { return; }

    /**
     * Aktualizace čísel typů item a specs - na enumy (tedy např. hodnota 0 na IMPOSSIBLE)
     * @param nodeData
     */
    transformNodeData({ itemTypes: _itemTypes, ...otherProps }: NodeData) {
        // Nastavení správných typů u itemTypes - ze serveru chodí čísla místo enumů

        const itemTypes: Partial<DescItemType>[] = _itemTypes?.map(({ type, specs: _specs, ...otherProps }) => {
            // itemType.itemType = ItemAvailabilityNumToEnumMap[itemType.type];
            // itemType.specs.forEach(itemSpec => {
            //     itemSpec.itemType = ItemAvailabilityNumToEnumMap[itemSpec.type];
            // });
            // itemType.descItemSpecsMap = getMapFromList(itemType.specs) as Record<number, typeof itemType.specs[number]>;
            const specs = _specs.map((itemSpec) => {
                return {
                    ...itemSpec,
                    itemType: ItemAvailabilityNumToEnumMap[itemSpec.type]
                }
            })

            return {
                ...otherProps,
                type,
                itemType: ItemAvailabilityNumToEnumMap[type],
                specs,
                descItemSpecsMap: getMapFromList(specs) as Record<number, typeof specs[number]>,
            }
        });

        return {
            ...otherProps,
            itemTypes,
        };
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
    _formValueStore(dispatch, getState: () => AppState, versionId: number, routingKey: string, valueLocation: ValueLocationIndex, overrideDescItem?: DescItem | IDescItemStructure) {
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
        } else if (
            refType.dataType.code === RulDataTypeCodeEnum.RECORD_REF ||
            refType.dataType.code === RulDataTypeCodeEnum.STRUCTURED
        ) {
            if ((descItem as any).structureData) {
                delete (descItem as any).structureData
            }
            // const { structureData, ...otherDescItem } = descItem;
        }

        if (this.descItemNeedStore(descItem, refType) || overrideDescItem) {
            // Reálné provedení operace
            if (typeof descItem.id !== 'undefined') {
                dispatch(statusSaving());
                this._callUpdateDescItem(dispatch, subNodeForm, versionId, parentVersionId, parentId, descItem).then(
                    json => {
                        // Nastavení správných typů u itemTypes - ze serveru chodí čísla místo enumů
                        const nodeData = this.transformNodeData(json);

                        if (this.area === AreaType.OUTPUT_AREA || this.area === AreaType.STRUCTURE_AREA) {
                            dispatch(
                                this._fundSubNodeFormDescItemResponse(
                                    versionId,
                                    routingKey,
                                    valueLocation,
                                    nodeData,
                                    OperationType.UPDATE,
                                ),
                            );
                            if (this.area === AreaType.STRUCTURE_AREA) {
                                dispatch(this._fundSubNodeFormFetch(versionId, parentId, routingKey, true));
                            }
                        } else {
                            dispatch(this._fundSubNodeUpdate(versionId, refTables, nodeData));
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
                        refType,
                    ).then(json => {
                        dispatch(
                            this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, OperationType.CREATE),
                        );
                        dispatch(statusSaved());
                        if (this.area === AreaType.STRUCTURE_AREA) {
                            dispatch(this._fundSubNodeFormFetch(versionId, parentId, routingKey, true));
                        }
                    });
                }
            }
        }
    }

    _fundSubNodeUpdate(versionId: number, refTables: RefTablesState, data) {
        return {
            type: ActionTypes.FUND_SUBNODE_UPDATE,
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
    fundSubNodeFormValueAdd(versionId: number, routingKey: string, valueLocation: ValueLocationIndex) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_ADD,
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
    fundSubNodeFormValueNotIdentified(versionId: number, routingKey: string, valueLocation: ValueLocationIndex, descItem: DescItem) {
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
    _callArrCoordinatesImport(_versionId: number, _parentId: number, _parentVersionId: number, _descItemTypeId: number, _file): Promise<void> { return; }

    /**
     * Akce přidání coordinates jako DescItem - Probíhá uploadem - doplnění hodnot pomocí WS
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {int} descItemTypeId Konkrétní typ desc item
     * @param {File} file Soubor k uploadu
     */
    fundSubNodeFormValueUploadCoordinates(versionId: number, routingKey: string, descItemTypeId: number, file) {
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
    _callDescItemCsvImport(_versionId: number, _parentId: number, _parentVersionId: number, _descItemTypeId: number, _file: any): Promise<void> { return; }

    /**
     * Akce přidání csv jako DescItem - Probíhá uploadem.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {int} descItemTypeId Konkrétní typ desc item
     * @param {File} file Soubor k uploadu
     */
    fundSubNodeFormValueUploadCsv(versionId: number, routingKey: string, descItemTypeId: number, file) {
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
     * Akce změny hodnoty a její promítnutí do store, případné uložení na server, pokud je toto vynuceno parametrem forceStore.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value nová hodnota
     * @param {boolean} forceStore pokud je true, je hodnota i odeslána na server pro uložení
     */
    fundSubNodeFormValueChange(versionId: number, routingKey: string, valueLocation: ValueLocationIndex, value, forceStore) {
        return (dispatch, getState: () => AppState) => {
            dispatch({
                type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_CHANGE,
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
    fundSubNodeFormValueChangePosition(versionId: number, routingKey: string, valueLocation: ValueLocationIndex, index: number) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            if (!loc.descItem.error.hasError && typeof loc.descItem.id !== 'undefined') {
                dispatch({
                    type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_CHANGE_POSITION,
                    area: this.area,
                    versionId,
                    routingKey,
                    valueLocation,
                    index,
                });

                const descItem = { ...loc.descItem, position: index + 1 };

                this._callUpdateDescItem(
                    dispatch,
                    subNodeForm,
                    versionId,
                    subNodeForm.data.parent.version,
                    subNodeForm.data.parent.id,
                    descItem,
                ).then(json => {
                    const newValueLocation = { ...valueLocation, descItemIndex: index };
                    dispatch(
                        this._fundSubNodeFormDescItemResponse(versionId, routingKey, newValueLocation, json, OperationType.UPDATE),
                    );
                    if (this.area === STRUCTURE_AREA) {
                        const parentId = subNodeForm.data.parent.id;
                        dispatch(this._fundSubNodeFormFetch(versionId, parentId, routingKey, false));
                    }
                });
            }
        };
    }

    /**
     * Akce změna hodnoty atributu - odkaz na rejstřík.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     * @param {Object} value hodnota
     */
    fundSubNodeFormValueChangeRecord(versionId: number, routingKey: string, valueLocation: ValueLocationIndex, value) {
        return (dispatch, getState) => {
            dispatch({
                type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_CHANGE_RECORD,
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
    fundSubNodeFormValueChangeSpec(versionId: number, routingKey: string, valueLocation: ValueLocationIndex, value) {
        return (dispatch, getState) => {
            // Dispatch zmněny specifikace
            dispatch({
                type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_CHANGE_SPEC,
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
        if ((!descItem.error || (descItem.error && !descItem.error.hasError)) && descItem.touched) {
            if (typeof descItem.id !== 'undefined') {
                // Jen pokud se hodnota nebo specifikace změnila
                let needUpdate = false;
                if (refType.useSpecification && !valuesEquals(descItem.descItemSpecId, descItem.prevDescItemSpecId)) {
                    needUpdate = true;
                }
                // TODO tato část by chtělat projít protože "valuesEquals" bere null a undefined jako stejné hodnoty
                if (!valuesEquals(descItem.value, descItem.prevValue)) {
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
    fundSubNodeFormValueBlur(versionId: number, routingKey: string, valueLocation: ValueLocationIndex) {
        return (dispatch, getState) => {
            dispatch({
                type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_BLUR,
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
    _callDeleteDescItem(_versionId: number, _parentId: number, _parentVersionId: number, _descItem: DescItem): Promise<any> { return; }

    /**
     * Smazání hodnoty atributu.
     * @param {int} versionId verze AS
     * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param {Object} valueLocation konkrétní umístění hodnoty
     */
    fundSubNodeFormValueDelete(versionId: number, routingKey: string, valueLocation: ValueLocationIndex) {
        return (dispatch, getState) => {
            const state = getState();
            const subNodeForm = this._getItemFormStore(state, versionId, routingKey);
            const loc = subNodeForm.getLoc(subNodeForm, valueLocation);

            dispatch({
                type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_DELETE,
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
                        this._fundSubNodeFormDescItemResponse(versionId, routingKey, valueLocation, json, OperationType.DELETE),
                    );
                    if (this.area === STRUCTURE_AREA) {
                        const parentId = subNodeForm.data.parent.id;
                        dispatch(this._fundSubNodeFormFetch(versionId, parentId, routingKey, true));
                    }
                });
            }
        };
    }

    _callSetInhibitDescItem(_nodeId: number, _itemId: number, _inhibit: boolean) { return; }

    fundSubNodeFormValueSetInhibit(nodeId: number, itemId: number, inhibit: boolean) {
        return (dispatch) => {
            return savingApiWrapper(dispatch, this._callSetInhibitDescItem(nodeId, itemId, inhibit));
        }
    }

    fundSubNodeFormHandleClose(versionId: number, routingKey: string) {
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
    fundSubNodeFormDescItemTypeAdd(versionId: number, routingKey: string, descItemTypeId: number) {
        return (dispatch, getState) => {
            dispatch({
                type: ActionTypes.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD,
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

    fundSubNodeFormTemplateUse(versionId: number, routingKey: string, template, replaceValues, addItemTypeIds) {
        return (dispatch, getState) => {
            const state = getState();
            dispatch({
                type: ActionTypes.FUND_SUB_NODE_FORM_TEMPLATE_USE,
                area: this.area,
                versionId,
                routingKey,
                template,
                replaceValues,
                groups: state.refTables.groups.data,
                addItemTypeIds,
            });
            dispatch({
                type: ActionTypes.FUND_TEMPLATE_USE,
                area: this.area,
                versionId,
                template,
            });
        };
    }

    fundSubNodeFormTemplateUseOnly(versionId: number, template) {
        return dispatch => {
            dispatch({
                type: ActionTypes.FUND_TEMPLATE_USE,
                area: this.area,
                versionId,
                template,
            });
        };
    }

    /** Metoda pro volání API. */
    // @Abstract
    _callDeleteDescItemType(_versionId: number, _parentId: number, _parentVersionId: number, _descItemTypeId: number): Promise<any> { return Promise.resolve() }

    /** Metoda pro volání API. */
    // @Abstract
    // _callSetNotIdentifiedDescItem(versionId, elementId, parentNodeVersion, itemTypeId, itemSpecId, itemObjectId) { }

    /** Metoda pro volání API. */
    // @Abstract
    // _callUnsetNotIdentifiedDescItem(versionId, elementId, parentNodeVersion, itemTypeId, itemSpecId, itemObjectId) { }

    /**
     * Přidání PP (který je počítaný a ještě ve formuláři není) do formuláře
     * @param versionId verze AS
     * @param itemTypeId id typu PP
     * @param strict
     */
    addCalculatedDescItem(versionId: number, itemTypeId: number, strict: boolean = false) {
        return (dispatch, getState: () => AppState) => {
            const state = getState();
            const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
            if (fundIndex !== null) {
                const getOutputId = state.arrRegion.funds[fundIndex].fundOutput.fundOutputDetail.subNodeForm.fetchingId;
                return WebApi.switchOutputCalculating(versionId, getOutputId, itemTypeId, strict).then(data => {
                    if (!data) {
                        dispatch(this.fundSubNodeFormDescItemTypeAdd(versionId, "1", itemTypeId));
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
    switchOutputCalculating(versionId: number, itemTypeId: number, routingKey: string, valueLocation: ValueLocationIndex) {
        return (dispatch, getState: () => AppState) => {
            const state = getState();
            const fundIndex = indexById(state.arrRegion.funds, versionId, 'versionId');
            if (fundIndex !== null) {
                const getOutputId = state.arrRegion.funds[fundIndex].fundOutput.fundOutputDetail.subNodeForm.fetchingId;
                WebApi.switchOutputCalculating(versionId, getOutputId, itemTypeId, undefined).then(() => {
                    dispatch({
                        type: ActionTypes.FUND_SUB_NODE_FORM_OUTPUT_CALC_SWITCH,
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
    fundSubNodeFormDescItemTypeDelete(versionId: number, routingKey: string, valueLocation: ValueLocationIndex) {
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
                            OperationType.DELETE_DESC_ITEM_TYPE,
                        ),
                    );
                    if (this.area === STRUCTURE_AREA) {
                        dispatch(this._fundSubNodeFormFetch(versionId, subNodeForm.data.parent.id, routingKey, true));
                    }
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
    fundSubNodeFormDescItemTypeCopyFromPrevResponse(versionId: number, routingKey: string, valueLocation: ValueLocationIndex, copySiblingResult) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE,
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
    fundSubNodeFormValueFocus(versionId: number, routingKey: string, valueLocation: ValueLocationIndex) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_VALUE_FOCUS,
            area: this.area,
            versionId,
            routingKey,
            valueLocation,
        };
    }

    // @Abstract
    _getParentObjIdInfo(_parentObjStore: any, _routingKey: string): ParentObjectIdInfo { return; }

    /**
     * Vyžádání dat - aby byla ve store k dispozici.
     * @param {int} versionId verze AS
     * @param {int|null} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
     * @param needClean
     */
    fundSubNodeFormFetchIfNeeded(versionId: number, routingKey: string, needClean: boolean = false, showChildren: boolean, showParents: boolean) {
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
                    subNodeForm &&
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
    fundSubNodeFormReceive(versionId: number, nodeId: number, routingKey: string, json, rulDataTypes, descItemTypes, groups, needClean: boolean) {
        // console.log("(((((((((((((((((((((", JSON.parse(JSON.stringify(json)));
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_RECEIVE,
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
    fundSubNodeFormRequest(versionId: number, nodeId: number, routingKey: string) {
        return {
            type: ActionTypes.FUND_SUB_NODE_FORM_REQUEST,
            area: this.area,
            versionId,
            nodeId,
            routingKey,
        };
    }
}
