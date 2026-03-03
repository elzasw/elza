import { NodeItem } from 'elza-api';
import {
    isDataBit,
    isDataCoordinates,
    isDataDate,
    isDataDecimal,
    isDataFileRef,
    isDataFormattedText,
    isDataInteger,
    isDataJsonTable,
    isDataRecordRef,
    isDataString,
    isDataStructureRef,
    isDataText,
    isDataUnitdate,
    isDataUnitid,
    isDataUriRef,
    isDataEnum,
} from './types';

export function getValue({ data }: NodeItem) {
    if (!data) {
        return { value: undefined };
    }
    if (isDataBit(data)) {
        return { value: data.bitValue };
    } else if (isDataCoordinates(data)) {
        return { value: data.value };
    } else if (isDataDate(data)) {
        return { value: data.value };
    } else if (isDataDecimal(data)) {
        return { value: data.value };
    } else if (isDataFileRef(data)) {
        return { value: data.fileId };
    } else if (isDataFormattedText(data)) {
        return { value: data.value };
    } else if (isDataInteger(data)) {
        return { value: data.integerValue };
    } else if (isDataJsonTable(data)) {
        return { value: data.value };
    } else if (isDataRecordRef(data)) {
        return { value: data.value };
    } else if (isDataString(data)) {
        return { value: data.stringValue };
    } else if (isDataStructureRef(data)) {
        return { value: data.structuredObjectId };
    } else if (isDataText(data)) {
        return { value: data.textValue };
    } else if (isDataUnitdate(data)) {
        return { value: data.value };
    } else if (isDataUnitid(data)) {
        return { value: data.unitId };
    } else if (isDataUriRef(data)) {
        return {
            value: data.value,
            description: data.description,
            refTemplateId: data.refTemplateId,
            nodeId: data.nodeId,
        };
    } else if (isDataEnum(data)) {
        return { value: undefined };
    }

    return { value: undefined };
}

export function hasValue({ data, itemSpecId }: NodeItem) {
    if (!data) {
        return { value: undefined };
    }
    if (isDataBit(data)) {
        return data.bitValue != undefined;
    } else if (isDataCoordinates(data)) {
        return data.value != undefined;
    } else if (isDataDate(data)) {
        return data.value != undefined;
    } else if (isDataDecimal(data)) {
        return data.value != undefined;
    } else if (isDataFileRef(data)) {
        return data.fileId != undefined;
    } else if (isDataFormattedText(data)) {
        return data.value != undefined;
    } else if (isDataInteger(data)) {
        return data.integerValue != undefined;
    } else if (isDataJsonTable(data)) {
        return data.value != undefined;
    } else if (isDataRecordRef(data)) {
        return data.value != undefined;
    } else if (isDataString(data)) {
        return data.stringValue != undefined;
    } else if (isDataStructureRef(data)) {
        return data.structuredObjectId != undefined;
    } else if (isDataText(data)) {
        return data.textValue != undefined;
    } else if (isDataUnitdate(data)) {
        return data.value != undefined;
    } else if (isDataUnitid(data)) {
        return data.unitId != undefined;
    } else if (isDataUriRef(data)) {
        return data.value != undefined;
    } else if (isDataEnum(data)) {
        return itemSpecId != undefined;
    }

    return false;
}

export function isValueEqual(itemA: NodeItem, itemB: NodeItem) {
    const dataA = itemA.data;
    const dataB = itemB.data;

    if (
        !dataA ||
        !dataB ||
        dataA?.dataType !== dataB?.dataType ||
        itemA.itemSpecId !== itemB.itemSpecId // if spec ids are different, the values are not equal
    ) {
        return false;
    }

    if (isDataBit(dataA) && isDataBit(dataB)) {
        return dataA.bitValue === dataB.bitValue;
    } else if (isDataCoordinates(dataA) && isDataCoordinates(dataB)) {
        return dataA.value === dataB.value;
    } else if (isDataDate(dataA) && isDataDate(dataB)) {
        return dataA.value === dataB.value;
    } else if (isDataDecimal(dataA) && isDataDecimal(dataB)) {
        return dataA.value === dataB.value;
    } else if (isDataFileRef(dataA) && isDataFileRef(dataB)) {
        return dataA.fileId === dataB.fileId;
    } else if (isDataFormattedText(dataA) && isDataFormattedText(dataB)) {
        return dataA.value === dataB.value;
    } else if (isDataInteger(dataA) && isDataInteger(dataB)) {
        return dataA.integerValue === dataB.integerValue;
    } else if (isDataJsonTable(dataA) && isDataJsonTable(dataB)) {
        return dataA.value === dataB.value;
    } else if (isDataRecordRef(dataA) && isDataRecordRef(dataB)) {
        return dataA.value === dataB.value;
    } else if (isDataString(dataA) && isDataString(dataB)) {
        return dataA.stringValue === dataB.stringValue;
    } else if (isDataStructureRef(dataA) && isDataStructureRef(dataB)) {
        return dataA.structuredObjectId === dataB.structuredObjectId;
    } else if (isDataText(dataA) && isDataText(dataB)) {
        return dataA.textValue === dataB.textValue;
    } else if (isDataUnitdate(dataA) && isDataUnitdate(dataB)) {
        return dataA.value === dataB.value;
    } else if (isDataUnitid(dataA) && isDataUnitid(dataB)) {
        return dataA.unitId === dataB.unitId;
    } else if (isDataUriRef(dataA) && isDataUriRef(dataB)) {
        return (
            dataA.value === dataB.value &&
            dataA.description === dataB.description &&
            dataA.refTemplateId === dataB.refTemplateId &&
            dataA.nodeId === dataB.nodeId
        );
    } else if (isDataEnum(dataA) && isDataEnum(dataB)) {
        return true; // true by default, because all desc items with different specIds are treated as different values
    }

    return false;
}
