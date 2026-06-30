import { Api } from "api/api";
import { DataType, FormItemType, StructuredObjectItem } from "elza-api";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { EditItem } from "components/arr/node-edit/types";

export interface MultiFormItem {
    item: EditItem;
    localId: string;
    forcedDisplayString?: string;
}

export type TypeMode = "locked" | "modified" | "deleted";

export interface SdoBatchPayload {
    ids: number[];
    autoincrementItemTypeIds: number[];
    deleteItemTypeIds: number[];
    items: StructuredObjectItem[];
}

let counter = 0;
function nextKey() {
    const key = `multi-struct-item-${counter}`;
    counter++;
    return key;
}

function createEmptyItem(
    itemTypeId: number,
    position: number,
    dataTypeCode: DataType,
    readOnly: boolean,
): EditItem {
    return {
        itemTypeId,
        position,
        readOnly,
        data: { dataType: dataTypeCode },
    };
}

export interface UseMultiStructureFormDataResult {
    itemTypes: FormItemType[];
    allItemTypes: FormItemType[];
    visibleTypeIds: number[];
    isLoading: boolean;
    formItemsByType: Record<number, MultiFormItem[]>;
    getTypeMode: (itemTypeId: number) => TypeMode;
    isAutoincremented: (itemTypeId: number) => boolean;
    setModified: (itemTypeId: number) => void;
    addType: (itemTypeId: number) => void;
    setLocked: (itemTypeId: number) => void;
    toggleDeleted: (itemTypeId: number) => void;
    toggleAutoincrement: (itemTypeId: number) => void;
    addEmptyItem: (itemTypeId: number) => void;
    updateItem: (itemTypeId: number, localId: string, item: EditItem) => void;
    deleteItem: (itemTypeId: number, localId: string) => void;
    buildPayload: () => SdoBatchPayload;
}

export function useMultiStructureFormData(
    fundId: number,
    fundVersionId: number,
    structureObjectId: number,
    structureObjectIds: number[],
): UseMultiStructureFormDataResult {
    const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
    const dataTypeRefs = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap);

    const [allItemTypes, setAllItemTypes] = useState<FormItemType[]>([]);
    const [presentTypeIds, setPresentTypeIds] = useState<number[]>([]);
    const [addedTypeIds, setAddedTypeIds] = useState<number[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [modeByType, setModeByType] = useState<Record<number, TypeMode>>({});
    const [autoincrementTypeIds, setAutoincrementTypeIds] = useState<number[]>([]);
    const [formItemsByType, setFormItemsByType] = useState<Record<number, MultiFormItem[]>>({});

    const dataTypeCodeByType = useRef<Record<number, DataType>>({});

    useEffect(() => {
        setIsLoading(true);
        (async () => {
            const { data } = await Api.structure.sdoGetFormStructureItems(fundId, structureObjectId, fundVersionId);
            data.itemTypes.forEach(({ itemTypeId }) => {
                const itemTypeRef = itemTypeRefs[itemTypeId];
                const dataType = itemTypeRef ? dataTypeRefs[itemTypeRef.dataTypeId] : undefined;
                if (dataType) {
                    dataTypeCodeByType.current[itemTypeId] = dataType.code as DataType;
                }
            });
            setAllItemTypes(data.itemTypes);
            setPresentTypeIds([...new Set(data.items.map(({ itemTypeId }) => itemTypeId))]);
            setIsLoading(false);
        })();
    }, [fundId, fundVersionId, structureObjectId, itemTypeRefs, dataTypeRefs]);

    const visibleTypeIds = useMemo(
        () => [...new Set([...presentTypeIds, ...addedTypeIds])],
        [presentTypeIds, addedTypeIds],
    );

    const itemTypes = useMemo(
        () => allItemTypes.filter(({ itemTypeId }) => visibleTypeIds.includes(itemTypeId)),
        [allItemTypes, visibleTypeIds],
    );

    const getTypeMode = useCallback(
        (itemTypeId: number): TypeMode => modeByType[itemTypeId] ?? "locked",
        [modeByType],
    );

    const isAutoincremented = useCallback(
        (itemTypeId: number) => autoincrementTypeIds.includes(itemTypeId),
        [autoincrementTypeIds],
    );

    function emptyItemFor(itemTypeId: number, position: number, readOnly: boolean): MultiFormItem {
        return {
            localId: nextKey(),
            item: createEmptyItem(
                itemTypeId,
                position,
                dataTypeCodeByType.current[itemTypeId],
                readOnly,
            ),
        };
    }

    function setModified(itemTypeId: number) {
        setModeByType((prev) => ({ ...prev, [itemTypeId]: "modified" }));
        setFormItemsByType((prev) =>
            prev[itemTypeId]?.length ? prev : { ...prev, [itemTypeId]: [emptyItemFor(itemTypeId, 0, false)] },
        );
    }

    function addType(itemTypeId: number) {
        setAddedTypeIds((prev) => (prev.includes(itemTypeId) ? prev : [...prev, itemTypeId]));
    }

    function setLocked(itemTypeId: number) {
        setModeByType((prev) => ({ ...prev, [itemTypeId]: "locked" }));
        setAutoincrementTypeIds((prev) => prev.filter((id) => id !== itemTypeId));
        setFormItemsByType((prev) => {
            const next = { ...prev };
            delete next[itemTypeId];
            return next;
        });
    }

    function toggleDeleted(itemTypeId: number) {
        setModeByType((prev) => {
            const isDeleted = prev[itemTypeId] === "deleted";
            return { ...prev, [itemTypeId]: isDeleted ? "locked" : "deleted" };
        });
        setAutoincrementTypeIds((prev) => prev.filter((id) => id !== itemTypeId));
        setFormItemsByType((prev) => {
            const next = { ...prev };
            delete next[itemTypeId];
            return next;
        });
    }

    function toggleAutoincrement(itemTypeId: number) {
        setAutoincrementTypeIds((prev) =>
            prev.includes(itemTypeId)
                ? prev.filter((id) => id !== itemTypeId)
                : [...prev, itemTypeId],
        );
    }

    function addEmptyItem(itemTypeId: number) {
        setFormItemsByType((prev) => {
            const existing = prev[itemTypeId] ?? [];
            return { ...prev, [itemTypeId]: [...existing, emptyItemFor(itemTypeId, existing.length, false)] };
        });
    }

    function updateItem(itemTypeId: number, localId: string, item: EditItem) {
        setFormItemsByType((prev) => ({
            ...prev,
            [itemTypeId]: (prev[itemTypeId] ?? []).map((formItem) =>
                formItem.localId === localId ? { ...formItem, item } : formItem,
            ),
        }));
    }

    function deleteItem(itemTypeId: number, localId: string) {
        setFormItemsByType((prev) => ({
            ...prev,
            [itemTypeId]: (prev[itemTypeId] ?? []).filter((formItem) => formItem.localId !== localId),
        }));
    }

    function buildPayload(): SdoBatchPayload {
        const items: StructuredObjectItem[] = [];
        Object.entries(formItemsByType).forEach(([typeId, formItems]) => {
            if (modeByType[Number(typeId)] !== "modified") {
                return;
            }
            formItems.forEach(({ item }) => {
                items.push(item as StructuredObjectItem);
            });
        });

        const deleteItemTypeIds = Object.entries(modeByType)
            .filter(([, mode]) => mode === "deleted")
            .map(([typeId]) => Number(typeId));

        return {
            ids: structureObjectIds,
            autoincrementItemTypeIds: autoincrementTypeIds,
            deleteItemTypeIds,
            items,
        };
    }

    return {
        itemTypes,
        allItemTypes,
        visibleTypeIds,
        isLoading,
        formItemsByType,
        getTypeMode,
        isAutoincremented,
        setModified,
        addType,
        setLocked,
        toggleDeleted,
        toggleAutoincrement,
        addEmptyItem,
        updateItem,
        deleteItem,
        buildPayload,
    };
}
