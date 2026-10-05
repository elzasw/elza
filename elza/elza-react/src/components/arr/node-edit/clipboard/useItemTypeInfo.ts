import { useAppSelector } from 'utils/hooks/useAppSelector';
import { GetItemTypeInfo } from './remap';

/** Looks up an item type's data type and specification usage in the reference tables. */
export function useItemTypeInfo(): GetItemTypeInfo {
    const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
    const dataTypeRefs = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap);

    return (itemTypeId) => {
        const typeRef = itemTypeRefs[itemTypeId];
        if (!typeRef) {
            return undefined;
        }
        return {
            dataType: dataTypeRefs[typeRef.dataTypeId]?.code,
            useSpecification: typeRef.useSpecification,
        };
    };
}
