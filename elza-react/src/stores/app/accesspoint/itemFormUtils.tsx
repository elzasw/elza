export enum DataTypeCode {
    FILE_REF = 'FILE_REF',
    RECORD_REF = 'RECORD_REF',
    STRUCTURED = 'STRUCTURED',
    JSON_TABLE = 'JSON_TABLE',
    ENUM = 'ENUM',
    UNITDATE = 'UNITDATE',
    UNITID = 'UNITID',
    FORMATTED_TEXT = 'FORMATTED_TEXT',
    TEXT = 'TEXT',
    STRING = 'STRING',
    INT = 'INT',
    COORDINATES = 'COORDINATES',
    DECIMAL = 'DECIMAL',
    DATE = 'DATE',
    URI_REF = 'URI_REF',
}

export enum ItemAvailability {
    REQUIRED = 'REQUIRED',
    RECOMMENDED = 'RECOMMENDED',
    POSSIBLE = 'POSSIBLE',
    IMPOSSIBLE = 'IMPOSSIBLE',
}

export const ItemAvailabilityNumToEnumMap = {
    [3]: ItemAvailability.REQUIRED,
    [2]: ItemAvailability.RECOMMENDED,
    [1]: ItemAvailability.POSSIBLE,
    [0]: ItemAvailability.IMPOSSIBLE,
};

export const getInfoSpecType = (type: ItemAvailability | number) => {
    if (typeof type === 'number') {
        return ItemAvailabilityNumToEnumMap[String(type)];
    }
    return type;
};

export function getItemClass(dataType) {
    switch (dataType.code) {
        case DataTypeCode.TEXT:
            return '.ApItemTextVO';
        case DataTypeCode.STRING:
            return '.ApItemStringVO';
        case DataTypeCode.INT:
            return '.ApItemIntVO';
        case DataTypeCode.COORDINATES:
            return '.ApItemCoordinatesVO';
        case DataTypeCode.DECIMAL:
            return '.ApItemDecimalVO';
        case DataTypeCode.FILE_REF:
            return '.ApItemFileRefVO';
        case DataTypeCode.RECORD_REF:
            return '.ApItemAccessPointRefVO';
        case DataTypeCode.STRUCTURED:
            return '.ApItemStructureVO';
        case DataTypeCode.JSON_TABLE:
            return '.ApItemJsonTableVO';
        case DataTypeCode.ENUM:
            return '.ApItemEnumVO';
        case DataTypeCode.FORMATTED_TEXT:
            return '.ApItemFormattedTextVO';
        case DataTypeCode.UNITDATE:
            return '.ApItemUnitdateVO';
        case DataTypeCode.UNITID:
            return '.ApItemUnitidVO';
        case DataTypeCode.DATE:
            return '.ApItemDateVO';
        default:
            console.error('Unsupported data type', dataType);
            return null;
    }
}
