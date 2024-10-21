import { ItemClass } from '../../../constants';
import { DataBit, DataCoordinates, DataDate, DataDecimal, DataFileRef, DataFormattedText, DataInteger, DataJsonTable, DataRecordRef, DataString, DataStructureRef, DataText, DataType, DataUnitdate, DataUnitid, DataUriRef, ItemData, NodeItem } from 'elza-api';
import { DescItem, DescItemFromServer } from 'typings/DescItem';
import { DescItemTypeRef } from 'typings/store';

export function isDataBit(data: ItemData): data is DataBit {
    return data.dataType === DataType.Bit;
}

export function isDataCoordinates(data: ItemData): data is DataCoordinates {
    return data.dataType === DataType.Coordinates;
}

export function isDataDate(data: ItemData): data is DataDate {
    return data.dataType === DataType.Date;
}

export function isDataFileRef(data: ItemData): data is DataFileRef {
    return data.dataType === DataType.FileRef;
}

export function isDataFormattedText(data: ItemData): data is DataFormattedText {
    return data.dataType === DataType.FormattedText;
}

export function isDataInteger(data: ItemData): data is DataInteger {
    return data.dataType === DataType.Int;
}

export function isDataJsonTable(data: ItemData): data is DataJsonTable {
    return data.dataType === DataType.JsonTable;
}

// export function isDataNull(data: ItemData): data is DataNull {
//     return data.type === DataTypeCode.NULL;
// }

export function isDataRecordRef(data: ItemData): data is DataRecordRef {
    return data.dataType === DataType.RecordRef;
}

export function isDataString(data: ItemData): data is DataString {
    return data.dataType === DataType.String;
}

export function isDataStructureRef(data: ItemData): data is DataStructureRef {
    return data.dataType === DataType.Structured;
}

export function isDataText(data: ItemData): data is DataText {
    return data.dataType === DataType.Text;
}

export function isDataUnitdate(data: ItemData): data is DataUnitdate {
    return data.dataType === DataType.Unitdate;
}

export function isDataUnitId(data: ItemData): data is DataUnitid {
    return data.dataType === DataType.Unitid;
}

export function isDataUriRef(data: ItemData): data is DataUriRef {
    return data.dataType === DataType.UriRef;
}

export function transformToDescItem({ data, ...item }: NodeItem) {
    const descItem: DescItemFromServer<any> = {
        "@class": "class",
        id: item.id,
        descItemObjectId: item.itemObjectId,
        position: item.position,
        undefined: item.undefined,
        itemTypeId: item.itemTypeId,
        descItemSpecId: item.itemSpecId,
        readOnly: item.readOnly,
        inhibited: item.inhibited,
    }

    if (isDataBit(data)) {
        descItem["@class"] = ItemClass.BIT;
        descItem.value = data.bitValue;
    }
    if (isDataCoordinates(data)) {
        descItem["@class"] = ItemClass.COORDINATES;
        descItem.value = data.value;
    }
    if (isDataDate(data)) {
        descItem["@class"] = ItemClass.DATE;
        descItem.value = data.value;
    }
    if (isDataFileRef(data)) {
        descItem["@class"] = ItemClass.FILE_REF;
        descItem.value = data.fileId;
    }
    if (isDataFormattedText(data)) {
        descItem["@class"] = ItemClass.FORMATTED_TEXT;
        descItem.value = data.value;
    }
    if (isDataInteger(data)) {
        descItem["@class"] = ItemClass.INT;
        descItem.value = data.integerValue;
    }
    if (isDataJsonTable(data)) {
        descItem["@class"] = ItemClass.JSON_TABLE;
        descItem.value = data.value;
    }
    if (isDataRecordRef(data)) {
        descItem["@class"] = ItemClass.RECORD_REF;
        descItem.value = data.value;
    }
    if (isDataString(data)) {
        descItem["@class"] = ItemClass.STRING;
        descItem.value = data.stringValue;
    }
    if (isDataStructureRef(data)) {
        descItem["@class"] = ItemClass.STRUCTURE;
        descItem.value = data.structuredObjectId;
    }
    if (isDataText(data)) {
        descItem["@class"] = ItemClass.TEXT;
        descItem.value = data.textValue;
    }
    if (isDataUnitdate(data)) {
        descItem["@class"] = ItemClass.UNITDATE;
        descItem.value = data.value;
    }
    if (isDataUnitId(data)) {
        descItem["@class"] = ItemClass.UNIT_ID;
        descItem.value = data.unitId;
    }
    if (isDataUriRef(data)) {
        descItem["@class"] = ItemClass.URI_REF;
        descItem.value = data.value;
        descItem.description = data.description;
        descItem.refTemplateId = data.refTemplateId;
    }
    return descItem;
}

export function transformToNodeItem({
    id,
    descItemObjectId,
    position,
    undefined,
    itemTypeId,
    descItemSpecId,
    readOnly,
    inhibited,
    ...descItem
}: DescItem, nodeId: number, nodeVersion: number, { dataType }: DescItemTypeRef) {
    let data: ItemData = { dataType: DataType.Enum }
    switch (dataType.code) {
        case DataType.Bit:
            data = {
                bitValue: descItem.value as boolean,
                dataType: DataType.Bit,
            } as DataBit;
            break;
        case DataType.Coordinates:
            data = {
                value: descItem.value as string,
                dataType: DataType.Coordinates,
            } as DataCoordinates;
            break;
        case DataType.Date:
            data = {
                value: descItem.value as string,
                dataType: DataType.Date,
            } as DataDate;
            break;
        case DataType.FileRef:
            data = {
                fileId: descItem.value as number,
                dataType: DataType.FileRef,
            } as DataFileRef;
            break;
        case DataType.FormattedText:
            data = {
                value: descItem.value as string,
                dataType: DataType.FormattedText,
            } as DataFormattedText;
            break;
        case DataType.Int:
            data = {
                integerValue: descItem.value as number,
                dataType: DataType.Int,
            } as DataInteger;
            break;
        case DataType.Decimal:
            data = {
                value: descItem.value as number,
                dataType: DataType.Decimal,
            } as DataDecimal;
            break;
        case DataType.JsonTable:
            data = {
                value: JSON.stringify(descItem.value) as string,
                dataType: DataType.JsonTable,
            } as DataJsonTable;
            break;
        case DataType.RecordRef:
            data = {
                value: descItem.value as number,
                dataType: DataType.RecordRef,
            } as DataRecordRef;
            break;
        case DataType.String:
            data = {
                stringValue: descItem.value as string,
                dataType: DataType.String,
            } as DataString;
            break;
        case DataType.Structured:
            data = {
                structuredObjectId: descItem.value as number,
                dataType: DataType.Structured,
            } as DataStructureRef;
            break;
        case DataType.Text:
            data = {
                textValue: descItem.value as string,
                dataType: DataType.Text,
            } as DataText;
            break;
        case DataType.Unitdate:
            data = {
                value: descItem.value as string,
                dataType: DataType.Unitdate,
            } as DataUnitdate;
            break;
        case DataType.Unitid:
            data = {
                unitId: descItem.value as string,
                dataType: DataType.Unitid,
            } as DataUnitid;
            break;
        case DataType.UriRef:
            data = {
                value: descItem.value as number,
                description: descItem.description,
                refTemplateId: descItem.refTemplateId,
                dataType: DataType.UriRef,
            } as DataRecordRef;
            break;
        case DataType.Enum:
            data = {
                dataType: DataType.Enum,
            };
            break;
        default:
            throw Error(`Unknown data type: ${dataType.code}`);
    }

    const nodeItem: NodeItem = {
        id,
        itemObjectId: descItemObjectId,
        position,
        undefined,
        itemTypeId,
        itemSpecId: descItemSpecId,
        readOnly,
        nodeId,
        nodeVersion,
        inhibited,
        data,
    }
    return nodeItem;
}
