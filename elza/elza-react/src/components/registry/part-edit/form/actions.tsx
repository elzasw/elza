import './PartEditForm.scss';
import {ApItemVO} from 'api/ApItemVO';
import {ApCreateTypeVO} from 'api/ApCreateTypeVO';
import {RulDataTypeCodeEnum} from 'api/RulDataTypeCodeEnum';
import {RulDescItemTypeExtVO} from 'api/RulDescItemTypeExtVO';
import {RulDataTypeVO} from 'api/RulDataTypeVO';
import {RequiredType} from 'api/RequiredType';
import * as ItemInfo from 'utils/ItemInfo';
import { findItemPlacePosition } from 'utils/partEdit';
import {ApItemBitVO} from 'api/ApItemBitVO';
import {WebApi} from 'actions/WebApi';
import { RefTablesState } from 'typings/store'
import { ApViewSettings} from 'api/ApViewSettings';
import {ApAccessPointCreateVO} from 'api/ApAccessPointCreateVO';
// import { ApPartFormVO } from "api/ApPartFormVO";
import { compareCreateTypes, hasItemValue } from 'utils/ItemInfo';
import { DetailStoreState } from 'types';
import { RevisionItem } from '../../revision';
import { RevisionApPartForm } from '../form';
import { AutoValue } from 'elza-api';

export const addEmptyItems = (
    attributes: Array<ApCreateTypeVO>,
    refTables: RefTablesState,
    formItems: RevisionItem[],
    partTypeId: number,
    arrayInsert: (index: number, value: RevisionItem) => void,
    userAction: boolean,
) => {
    let newItems = getNewItems(attributes, refTables, userAction);

    // Vložení do formuláře - od konce
    // sortOwnItems(partTypeId, newItems, refTables, apViewSettings);

    newItems.reverse().forEach(item => {
        let index = findItemPlacePosition(item, formItems, partTypeId, refTables);
        arrayInsert(index, item);
    });
}

export const addItemsWithValues= (
    attributes: Array<ApCreateTypeVO>,
    values: AutoValue[],
    refTables: RefTablesState,
    formItems: RevisionItem[],
    partTypeId: number,
    arrayInsert: (index: number, value: RevisionItem) => void,
) => {
    const newItems = getItemsWithValues(attributes, values, refTables);

    newItems.reverse().forEach((item) => {
        const index = findItemPlacePosition(item, formItems, partTypeId, refTables);
        arrayInsert(index, item);
    });
}

const getItemsWithValues = (
    attributes: Array<ApCreateTypeVO>,
    values: AutoValue[],
    refTables: RefTablesState,
) => {
    return attributes.map((attribute)=>{
        const itemType = refTables.descItemTypes.itemsMap[attribute.itemTypeId] as RulDescItemTypeExtVO;
        const dataType = refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO;
        const value = values.find((value) => value.itemTypeId === attribute.itemTypeId);
        const item: any = {
            typeId: attribute.itemTypeId,
            '@class': ItemInfo.getItemClass(dataType.code),
            position: 1, // TODO: dořešit pozici
            value: value?.value,
            specId: value?.itemSpecId,
        }

        return {
            updatedItem: item,
            typeId: attribute.itemTypeId,
            '@class': ItemInfo.getItemClass(dataType.code),
        };
    })
}

const getNewItems = (attributes: Array<ApCreateTypeVO>, refTables: RefTablesState, userAction: boolean) => {
    return attributes.map(attribute => {
        const itemType = refTables.descItemTypes.itemsMap[attribute.itemTypeId] as RulDescItemTypeExtVO;
        const dataType = refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO;

        const item: ApItemVO = {
            typeId: attribute.itemTypeId,
            '@class': ItemInfo.getItemClass(dataType.code),
            position: 1, // TODO: dořešit pozici
        };

        // Implicitní hodnoty
        switch (dataType.code) {
            case RulDataTypeCodeEnum.BIT:
                ((item as unknown) as ApItemBitVO).value = false;
                break;
        }

        // Implicitní specifikace - pokud má specifikaci a má právě jednu položku a současně jde o povinnou hodnotu
        // Pokud uživatel přidal ručně i pro nepovinné
        if (itemType.useSpecification && (attribute.requiredType === RequiredType.REQUIRED || userAction)) {
            if (attribute.itemSpecIds && attribute.itemSpecIds.length === 1) {
                item.specId = attribute.itemSpecIds[0];
            }
        }

        return {
            updatedItem: item,
            typeId: attribute.itemTypeId,
            '@class': ItemInfo.getItemClass(dataType.code),
        };
    });

}

export const getUpdatedForm = async (
    data: RevisionApPartForm, 
    typeId: number, 
    scopeId: number, 
    apViewSettings: DetailStoreState<ApViewSettings>,
    refTables: RefTablesState,
    partTypeId: number,
    partId?: number, 
    parentPartId?: number, 
    accessPointId?: number,
) => {
    const apViewSettingRule = apViewSettings.data!.rules[apViewSettings.data!.typeRuleSetMap[typeId]];
    const items = data.items.filter((item) => 
        item.updatedItem?.changeType !== "DELETED"
    ).map((item) => 
        item.updatedItem || item.item as any
    );
    console.log("get updated form items", items, data);
    const form: ApAccessPointCreateVO = {
        typeId,
        partForm: {
            ...data,
            parentPartId,
            items: [...items.filter(hasItemValue)],
            partId: partId,
        },
        accessPointId,
        scopeId,
    };

    const { attributes, errors } = await WebApi.getAvailableItems(form);

    attributes.sort((a, b) => {
        return compareCreateTypes(a, b, partTypeId, refTables, apViewSettingRule);
    });

    return {
        attributes,
        errors,
        data: {
            ...data,
            items: getItemsWithRequired(data.items, attributes, partTypeId, refTables),
        } as RevisionApPartForm
    }
};

export const getItemsWithRequired = ( 
    items: RevisionItem[], 
    attributes: ApCreateTypeVO[], 
    partTypeId: number,
    refTables: RefTablesState,
) => {
    const newItems: RevisionItem[] = [];
    addEmptyItems(
        getRequiredAttributes(items, attributes), 
        refTables,
        items,
        partTypeId,
        (_index, item) => {newItems.push(item)},
        false,
    )
    return sortApItems([...items, ...newItems], refTables.descItemTypes.itemsMap);
}

const sortApItems = (items: RevisionItem[], descItemTypesMap: Record<number, RulDescItemTypeExtVO>) => {
    return [...items].sort((a, b) => {
        return descItemTypesMap[a.typeId].viewOrder - descItemTypesMap[b.typeId].viewOrder;
    })
}

const getRequiredAttributes = (items: RevisionItem[], attributes: ApCreateTypeVO[]) => {
    const existingItemTypeIds = items.map(i => i.typeId);
    const requiredAttributes = attributes.filter(attributes => {
        if (attributes.requiredType === RequiredType.REQUIRED) {
            return existingItemTypeIds.indexOf(attributes.itemTypeId) < 0;
        } else {
            return false;
        }
    });
    return requiredAttributes;
}
