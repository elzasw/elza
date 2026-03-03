import {
    ItemData,
    DataBit,
    DataType,
    DataCoordinates,
    DataDate,
    DataDecimal,
    DataEnum,
    DataFileRef,
    DataFormattedText,
    DataInteger,
    DataJsonTable,
    DataRecordRef,
    DataString,
    DataStructureRef,
    DataText,
    DataUnitdate,
    DataUnitid,
    DataUriRef,
} from 'elza-api';

export function isDataBit(data: ItemData): data is DataBit {
    return data.dataType === DataType.Bit;
}

export function isDataCoordinates(data: ItemData): data is DataCoordinates {
    return data.dataType === DataType.Coordinates;
}

export function isDataDate(data: ItemData): data is DataDate {
    return data.dataType === DataType.Date;
}

export function isDataDecimal(data: ItemData): data is DataDecimal {
    return data.dataType === DataType.Decimal;
}

export function isDataEnum(data: ItemData): data is DataEnum {
    return data.dataType === DataType.Enum;
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

export function isDataUnitid(data: ItemData): data is DataUnitid {
    return data.dataType === DataType.Unitid;
}

export function isDataUriRef(data: ItemData): data is DataUriRef {
    return data.dataType === DataType.UriRef;
}
