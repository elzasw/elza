import { Api } from "api/api";
import { RulDataTypeVO } from "api/RulDataTypeVO";
import { DataType, FormItemType, MandatoryType, StructuredObjectItem } from "elza-api";
import { useCallback, useEffect, useRef, useState } from "react";
import { DescItemTypeRef } from "typings/store";
import { EventType } from "typings/websocket";
import { AnyMessage } from "typings/websocket/Message";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import { EditItem } from "components/arr/node-edit/types";

function useWSStructureChanges(structureObjectId: number, callback: () => void) {
    const { addListener, removeListener } = useWebsocket();

    const handleMessage = (message: AnyMessage) => {
        if (
            message.eventType === EventType.STRUCTURE_DATA_CHANGE &&
            message.updateIds.includes(structureObjectId)
        ) {
            callback();
        }
    };

    useEffect(() => {
        const listener = addListener(handleMessage);
        return () => {
            removeListener(listener);
        };
    }, []);
}

export interface FormItem {
    item: EditItem;
    localId: string;
}

let counter = 0;

function useKeyGen(structureObjectId: number) {
    useEffect(() => {
        counter = 0;
    }, [structureObjectId]);

    function getKey() {
        const key = `struct-item-${counter}`;
        counter++;
        return key;
    }

    return { getKey };
}

function createEmptyStructureItem(
    itemTypeId: number,
    position: number = 1,
    dataTypeCode: DataType,
): StructuredObjectItem {
    return {
        itemTypeId,
        position,
        data: {
            dataType: dataTypeCode,
        },
    };
}

function getForcedItems(
    items: StructuredObjectItem[],
    itemTypes: FormItemType[],
    itemTypeRefs: Record<number, DescItemTypeRef>,
    dataTypeRefs: Record<number, RulDataTypeVO>,
): StructuredObjectItem[] {
    const forced: StructuredObjectItem[] = [];

    itemTypes.forEach(({ itemTypeId, type, repeatable, specs = [] }) => {
        const itemTypeRef = itemTypeRefs[itemTypeId];
        if (!itemTypeRef) { return; }
        const dataType = dataTypeRefs[itemTypeRef.dataTypeId];
        if (!dataType) { return; }

        const isRequiredOrRecommended =
            type === MandatoryType.Required || type === MandatoryType.Recommended;
        const isPossible = type === MandatoryType.Possible;

        if (!isRequiredOrRecommended && !isPossible) { return; }
        if (isPossible && repeatable) { return; }

        const existingItemsOfType = items.filter(
            ({ itemTypeId: id }) => id === itemTypeId,
        );
        const existingItemCount = existingItemsOfType.length;

        const useSpecification = itemTypeRef.useSpecification && dataType.code !== DataType.Enum;

        if (useSpecification) {
            const specsToProcess = isRequiredOrRecommended
                ? specs.filter(
                    ({ type: specType }) =>
                        specType === MandatoryType.Required ||
                        specType === MandatoryType.Recommended ||
                        (!repeatable && specType === MandatoryType.Possible),
                )
                : specs;

            specsToProcess.forEach(({ itemSpecId }) => {
                const specHasValue = existingItemsOfType.some(
                    ({ itemSpecId: existingSpecId }) => existingSpecId === itemSpecId,
                );
                const shouldAdd = isRequiredOrRecommended ? !specHasValue : false;

                if (shouldAdd) {
                    forced.push({
                        ...createEmptyStructureItem(itemTypeId, existingItemCount, dataType.code as DataType),
                        itemSpecId,
                    });
                }
            });

            if (isRequiredOrRecommended && specsToProcess.length === 0 && existingItemCount === 0) {
                forced.push(createEmptyStructureItem(itemTypeId, existingItemCount, dataType.code as DataType));
            }
        } else {
            const typeHasValue = existingItemCount > 0;
            const shouldAdd = isRequiredOrRecommended && !typeHasValue;

            if (shouldAdd) {
                forced.push(createEmptyStructureItem(itemTypeId, existingItemCount, dataType.code as DataType));
            }
        }
    });

    return forced;
}

function convertToFormItems(
    items: StructuredObjectItem[],
    oldItems: FormItem[],
    generateLocalId: (item: StructuredObjectItem) => string,
): FormItem[] {
    return items.map((item) => {
        const oldItem = oldItems.find(
            ({ item: oldItem }) => oldItem.itemObjectId != undefined && oldItem.itemObjectId === item.itemObjectId,
        );
        return oldItem ? { ...oldItem, item } : { localId: generateLocalId(item), item };
    });
}

export interface UseStructureFormDataResult {
    formItems: FormItem[];
    forcedFormItems: FormItem[];
    addedFormItems: FormItem[];
    itemTypes: FormItemType[];
    isLoading: boolean;
    addEmptyItem: (typeId: number, specId?: number) => void;
    createItem: (item: EditItem, localId: string) => Promise<EditItem | undefined>;
    updateItem: (item: EditItem, localId?: string) => Promise<void>;
    deleteItem: (item: EditItem, localId: string) => Promise<void>;
    deleteItemsByType: (itemTypeId: number) => Promise<void>;
}

export function useStructureFormData(
    fundId: number,
    fundVersionId: number,
    structureObjectId: number,
    options?: {
        skipForcedItems?: boolean;
    },
): UseStructureFormDataResult {
    const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
    const dataTypeRefs = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap);

    const { getKey } = useKeyGen(structureObjectId);

    const [itemTypes, setItemTypes] = useState<FormItemType[]>([]);
    const [formItems, setFormItems] = useState<FormItem[]>([]);
    const [forcedFormItems, setForcedFormItems] = useState<FormItem[]>([]);
    const [addedFormItems, setAddedFormItems] = useState<FormItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    const itemsRef = useRef<StructuredObjectItem[]>([]);

    const applyData = useCallback(
        (items: StructuredObjectItem[], types: FormItemType[]) => {
            itemsRef.current = items;
            setItemTypes(types);
            setFormItems((prev) => convertToFormItems(items, prev, getKey));
            const forced = options?.skipForcedItems
                ? []
                : getForcedItems(
                    [...items, ...addedFormItems.map(({ item }) => item)],
                    types,
                    itemTypeRefs,
                    dataTypeRefs,
                );
            setForcedFormItems(forced.map((item) => ({ localId: getKey(), item })));
        },
        [itemTypeRefs, dataTypeRefs, addedFormItems, options?.skipForcedItems],
    );

    const fetchAndApply = useCallback(async () => {
        const { data } = await Api.structure.sdoGetFormStructureItems(fundId, structureObjectId, fundVersionId);
        applyData(data.items, data.itemTypes);
    }, [fundId, structureObjectId, fundVersionId, applyData]);

    useEffect(() => {
        setIsLoading(true);
        (async () => {
            await fetchAndApply();
            setIsLoading(false);
        })();
    }, [fundId, fundVersionId, structureObjectId]);

    useWSStructureChanges(structureObjectId, () => {
        fetchAndApply();
    });

    function addEmptyItem(typeId: number, specId?: number) {
        const itemTypeRef = itemTypeRefs[typeId];
        if (!itemTypeRef) { return; }
        const dataType = dataTypeRefs[itemTypeRef.dataTypeId];
        if (!dataType) { return; }
        const position = itemsRef.current.filter(({ itemTypeId }) => itemTypeId === typeId).length;
        const newItem: StructuredObjectItem = {
            ...createEmptyStructureItem(typeId, position, dataType.code as DataType),
            itemSpecId: specId,
        };
        setAddedFormItems((prev) => [...prev, { localId: getKey(), item: newItem }]);
    }

    async function createItem(item: EditItem, localId: string): Promise<EditItem | undefined> {
        const { data } = await Api.structure.sdoCreateItem(fundId, structureObjectId, item as StructuredObjectItem);
        const created: EditItem = data.item;
        const removeLocalId = (prev: FormItem[]) => prev.filter(({ localId: id }) => id !== localId);
        setAddedFormItems(removeLocalId);
        setForcedFormItems(removeLocalId);
        setFormItems((prev) => [...prev, { localId, item: created }]);
        itemsRef.current = [...itemsRef.current, data.item];
        return created;
    }

    async function updateItem(item: EditItem, localId?: string): Promise<void> {
        if (!item.data?.dataId && localId) {
            const updateList = (prev: FormItem[]) =>
                prev.map((formItem) => formItem.localId === localId ? { ...formItem, item } : formItem);
            setFormItems(updateList);
            setForcedFormItems(updateList);
            setAddedFormItems(updateList);
            return;
        }
        await Api.structure.sdoUpdateItem(fundId, structureObjectId, true, item as StructuredObjectItem);
    }

    async function deleteItem(item: EditItem, localId: string): Promise<void> {
        if (item.data?.dataId != undefined || item.undefined) {
            await Api.structure.sdoDeleteItem(fundId, structureObjectId, item.itemObjectId!);
            await fetchAndApply();
            return;
        }

        const inAdded = addedFormItems.findIndex(({ localId: id }) => id === localId);
        if (inAdded >= 0) {
            setAddedFormItems((prev) => prev.filter(({ localId: id }) => id !== localId));
            return;
        }

        const emptyItem = createEmptyStructureItem(
            item.itemTypeId!,
            item.position ?? 1,
            item.data?.dataType as DataType,
        );
        setForcedFormItems((prev) =>
            prev.map((formItem) => formItem.localId === localId ? { ...formItem, item: emptyItem } : formItem),
        );
    }

    async function deleteItemsByType(itemTypeId: number): Promise<void> {
        await Api.structure.sdoDeleteItemsByType(fundId, structureObjectId, itemTypeId);
        await fetchAndApply();
    }

    return {
        formItems,
        forcedFormItems,
        addedFormItems,
        itemTypes,
        isLoading,
        addEmptyItem,
        createItem,
        updateItem,
        deleteItem,
        deleteItemsByType,
    };
}
