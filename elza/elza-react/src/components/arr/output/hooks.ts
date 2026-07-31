import { Api } from "api/api";
import { RulDataTypeVO } from "api/RulDataTypeVO";
import { DataType, FormItemType, MandatoryType, OutputFormData, OutputItem } from "elza-api";
import { useCallback, useEffect, useRef, useState } from "react";
import { DescItemTypeRef } from "typings/store";
import { EventType } from "typings/websocket";
import { AnyMessage } from "typings/websocket/Message";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import { EditItem } from "components/arr/item-form/types";
import { FormItem } from "components/arr/item-form/formItems";

function useWSOutputChanges(outputId: number, callback: () => void) {
    const { addListener, removeListener } = useWebsocket();

    const handleMessage = (message: AnyMessage) => {
        if (
            message.eventType === EventType.OUTPUT_ITEM_CHANGE &&
            message.outputId === outputId
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

let counter = 0;

function useKeyGen(outputId: number) {
    useEffect(() => {
        counter = 0;
    }, [outputId]);

    function getKey() {
        const key = `output-item-${counter}`;
        counter++;
        return key;
    }

    return { getKey };
}

function createEmptyOutputItem(
    itemTypeId: number,
    position: number = 1,
    dataTypeCode: DataType,
): OutputItem {
    return {
        itemTypeId,
        position,
        data: {
            dataType: dataTypeCode,
        },
    };
}

function getForcedItems(
    items: OutputItem[],
    itemTypes: FormItemType[],
    itemTypeRefs: Record<number, DescItemTypeRef>,
    dataTypeRefs: Record<number, RulDataTypeVO>,
): OutputItem[] {
    const forced: OutputItem[] = [];

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
                        ...createEmptyOutputItem(itemTypeId, existingItemCount, dataType.code as DataType),
                        itemSpecId,
                    });
                }
            });

            if (isRequiredOrRecommended && specsToProcess.length === 0 && existingItemCount === 0) {
                forced.push(createEmptyOutputItem(itemTypeId, existingItemCount, dataType.code as DataType));
            }
        } else {
            const typeHasValue = existingItemCount > 0;
            const shouldAdd = isRequiredOrRecommended && !typeHasValue;

            if (shouldAdd) {
                forced.push(createEmptyOutputItem(itemTypeId, existingItemCount, dataType.code as DataType));
            }
        }
    });

    return forced;
}

function convertToFormItems(
    items: OutputItem[],
    oldItems: FormItem[],
    generateLocalId: (item: OutputItem) => string,
): FormItem[] {
    return items.map((item) => {
        const oldItem = oldItems.find(
            ({ item: oldItem }) => oldItem.itemObjectId != undefined && oldItem.itemObjectId === item.itemObjectId,
        );
        return oldItem ? { ...oldItem, item } : { localId: generateLocalId(item), item };
    });
}

export interface UseOutputFormDataResult {
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
    switchCalculating: (itemTypeId: number, manual: boolean) => Promise<void>;
}

export function useOutputFormData(
    outputId: number,
    options?: {
        skipForcedItems?: boolean;
    },
): UseOutputFormDataResult {
    const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
    const dataTypeRefs = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap);

    const { getKey } = useKeyGen(outputId);

    const [itemTypes, setItemTypes] = useState<FormItemType[]>([]);
    const [formItems, setFormItems] = useState<FormItem[]>([]);
    const [forcedFormItems, setForcedFormItems] = useState<FormItem[]>([]);
    const [addedFormItems, setAddedFormItems] = useState<FormItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [storedData, setStoredData] = useState<OutputFormData>();

    const itemsRef = useRef<OutputItem[]>([]);
    // Output version, required by every mutation. Bumped from the `parent` returned by each
    // mutation and by the initial/refetched form data.
    const outputVersionRef = useRef<number>();

    const applyData = useCallback(
        (items: OutputItem[], types: FormItemType[]) => {
            itemsRef.current = items;
            setItemTypes(types);
            setFormItems((prev) => convertToFormItems(items, prev, getKey));
            const forced = options?.skipForcedItems
                ? []
                : getForcedItems(
                    [...items, ...addedFormItems.map(({ item }) => item as OutputItem)],
                    types,
                    itemTypeRefs,
                    dataTypeRefs,
                );
            setForcedFormItems(forced.map((item) => ({ localId: getKey(), item })));
        },
        [itemTypeRefs, dataTypeRefs, addedFormItems, options?.skipForcedItems],
    );

    const fetchAndStoreData = useCallback(async () => {
        const { data } = await Api.output.outputGetOutputFormData(outputId);
        outputVersionRef.current = data.parent.version;
        setStoredData(data);
    }, [outputId]);

    useEffect(() => {
        if (storedData) {
            applyData(storedData.items, storedData.itemTypes);
            setStoredData(undefined);
        }
    }, [storedData, applyData]);

    useEffect(() => {
        setIsLoading(true);
        (async () => {
            await fetchAndStoreData();
            setIsLoading(false);
        })();
    }, [outputId]);

    useWSOutputChanges(outputId, () => {
        fetchAndStoreData();
    });

    function addEmptyItem(typeId: number, specId?: number) {
        const itemTypeRef = itemTypeRefs[typeId];
        if (!itemTypeRef) { return; }
        const dataType = dataTypeRefs[itemTypeRef.dataTypeId];
        if (!dataType) { return; }
        const position = itemsRef.current.filter(({ itemTypeId }) => itemTypeId === typeId).length;
        const newItem: OutputItem = {
            ...createEmptyOutputItem(typeId, position, dataType.code as DataType),
            itemSpecId: specId,
        };
        setAddedFormItems((prev) => [...prev, { localId: getKey(), item: newItem }]);
    }

    async function createItem(item: EditItem, localId: string): Promise<EditItem | undefined> {
        const { data } = await Api.output.outputCreateOutputItem(
            outputId,
            outputVersionRef.current,
            item as OutputItem,
        );
        outputVersionRef.current = data.parent.version;
        const created: EditItem = data.item;
        const removeLocalId = (prev: FormItem[]) => prev.filter(({ localId: id }) => id !== localId);
        setAddedFormItems(removeLocalId);
        setForcedFormItems(removeLocalId);
        setFormItems((prev) => [...prev, { localId, item: created }]);
        itemsRef.current = [...itemsRef.current, data.item];
        await fetchAndStoreData();
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
        const { data } = await Api.output.outputUpdateOutputItem(
            outputId,
            outputVersionRef.current,
            item as OutputItem,
        );
        outputVersionRef.current = data.parent.version;
        await fetchAndStoreData();
    }

    async function deleteItem(item: EditItem, localId: string): Promise<void> {
        if (item.data?.dataId != undefined || item.undefined) {
            const { data } = await Api.output.outputDeleteOutputItem(
                outputId,
                outputVersionRef.current,
                item.itemObjectId!,
            );
            outputVersionRef.current = data.parent.version;
            await fetchAndStoreData();
            return;
        }

        const inAdded = addedFormItems.findIndex(({ localId: id }) => id === localId);
        if (inAdded >= 0) {
            setAddedFormItems((prev) => prev.filter(({ localId: id }) => id !== localId));
            return;
        }

        const emptyItem = createEmptyOutputItem(
            item.itemTypeId!,
            item.position ?? 1,
            item.data?.dataType as DataType,
        );
        setForcedFormItems((prev) =>
            prev.map((formItem) => formItem.localId === localId ? { ...formItem, item: emptyItem } : formItem),
        );
    }

    async function deleteItemsByType(itemTypeId: number): Promise<void> {
        const { data } = await Api.output.outputDeleteOutputItemsByType(
            outputId,
            outputVersionRef.current,
            itemTypeId,
        );
        outputVersionRef.current = data.parent.version;
        await fetchAndStoreData();
    }

    async function switchCalculating(itemTypeId: number, manual: boolean): Promise<void> {
        await Api.output.outputSetOutputItemMode(outputId, itemTypeId, manual);
        await fetchAndStoreData();
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
        switchCalculating,
    };
}
