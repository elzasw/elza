import {
    DataBit,
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
    DataType,
    DataUnitdate,
    DataUnitid,
    DataUriRef,
    NodeItem,
} from 'elza-api';
import { DescItemFromServer } from 'typings/DescItem';
import { ItemClass } from '../../../../constants';
import { DeprecatedNodeTemplate, NodeTemplate, NodeTemplateItem } from './templates';
import { getValue } from './utils';

function isBoolean(value: number | string | boolean): value is boolean {
    if (typeof value != 'boolean') {
        throw 'Value is not boolean.';
    }
    return true;
}

function isString(value: number | string | boolean): value is string {
    if (typeof value != 'string') {
        throw 'Value is not string.';
    }
    return true;
}

function isNumber(_value: number | string | boolean): _value is number {
    const value = parseInt(_value.toString());
    if (typeof value != 'number' && !isNaN(value)) {
        throw 'RecordRef value is not number';
    }
    return true;
}

function isUndefined(value: unknown): value is undefined {
    if (value == undefined) {
        return true;
    }
    return false;
}

function createBitData(bitValue?: number | string | boolean): DataBit {
    if (isUndefined(bitValue) || isBoolean(bitValue)) {
        return {
            dataType: DataType.Bit,
            bitValue,
        };
    }
}

function createCoordinatesData(value?: number | string | boolean): DataCoordinates {
    if (isUndefined(value) || isString(value)) {
        return {
            dataType: DataType.Coordinates,
            value,
        };
    }
}
function createDateData(value?: number | string | boolean): DataDate {
    if (isUndefined(value) || isString(value)) {
        return {
            dataType: DataType.Date,
            value,
        };
    }
}
function createDecimalData(value?: number | string | boolean): DataDecimal {
    if (isUndefined(value) || isNumber(value)) {
        const _value = value != undefined ? parseInt(value.toString()) : undefined;
        return {
            dataType: DataType.Decimal,
            value: _value,
        };
    }
}
function createEnumData(value?: number | string | boolean): DataEnum {
    if (value) {
        console.warn(`There is value set on enum: ${value}`);
    }

    return {
        dataType: DataType.Enum,
    };
}
function createFileRefData(fileId?: number | string | boolean): DataFileRef {
    if (isUndefined(fileId) || isNumber(fileId)) {
        const _fileId = fileId != undefined ? parseInt(fileId.toString()) : undefined;
        return {
            dataType: DataType.FileRef,
            fileId: _fileId,
        };
    }
}
function createFormattedTextData(value?: number | string | boolean): DataFormattedText {
    if (isUndefined(value) || isString(value)) {
        return {
            dataType: DataType.FormattedText,
            value,
        };
    }
}
function createIntData(integerValue?: number | string | boolean): DataInteger {
    if (isUndefined(integerValue) || isNumber(integerValue)) {
        const _integerValue = integerValue != undefined ? parseInt(integerValue.toString()) : undefined;
        return {
            dataType: DataType.Int,
            integerValue: _integerValue,
        };
    }
}
function createJsonTableData(value?: number | string | boolean): DataJsonTable {
    if (isUndefined(value) || isString(value)) {
        return {
            dataType: DataType.JsonTable,
            value,
        };
    }
}
function createRecordRefData(value?: number | string | boolean): DataRecordRef {
    if (isUndefined(value) || isNumber(value)) {
        const _value = value != undefined ? parseInt(value.toString()) : undefined;
        return {
            dataType: DataType.RecordRef,
            value: _value,
        };
    }
}
function createStringData(stringValue?: number | string | boolean): DataString {
    if (isUndefined(stringValue) || isString(stringValue)) {
        return {
            dataType: DataType.String,
            stringValue,
        };
    }
}
function createStructureRefData(structuredObjectId?: number | string | boolean): DataStructureRef {
    if (isUndefined(structuredObjectId) || isNumber(structuredObjectId)) {
        const _structuredObjectId =
            structuredObjectId != undefined ? parseInt(structuredObjectId.toString()) : undefined;
        return {
            dataType: DataType.Structured,
            structuredObjectId: _structuredObjectId,
        };
    }
}
function createTextData(textValue?: number | string | boolean): DataText {
    if (isUndefined(textValue) || isString(textValue)) {
        return {
            dataType: DataType.Text,
            textValue,
        };
    }
}
function createUnitdateData(value?: number | string | boolean): DataUnitdate {
    if (isUndefined(value) || isString(value)) {
        return {
            dataType: DataType.Unitdate,
            value,
        };
    }
}
function createUnitIdData(unitId?: number | string | boolean): DataUnitid {
    if (isUndefined(unitId) || isString(unitId)) {
        return {
            dataType: DataType.Unitid,
            unitId,
        };
    }
}
function createUriRefData(
    value?: number | string | boolean,
    description?: string,
    nodeId?: number,
    refTemplateId?: number
): DataUriRef {
    if (isUndefined(value) || isString(value)) {
        return {
            dataType: DataType.UriRef,
            value,
            description,
            nodeId,
            refTemplateId,
        };
    }
}

const dataTypeToClassMap = {
    [DataType.Bit]: ItemClass.BIT,
    [DataType.Coordinates]: ItemClass.COORDINATES,
    [DataType.Date]: ItemClass.DATE,
    [DataType.Decimal]: ItemClass.DECIMAL,
    [DataType.Enum]: ItemClass.ENUM,
    [DataType.FileRef]: ItemClass.FILE_REF,
    [DataType.FormattedText]: ItemClass.FORMATTED_TEXT,
    [DataType.Int]: ItemClass.INT,
    [DataType.JsonTable]: ItemClass.JSON_TABLE,
    [DataType.RecordRef]: ItemClass.RECORD_REF,
    [DataType.String]: ItemClass.STRING,
    [DataType.Structured]: ItemClass.STRUCTURE,
    [DataType.Text]: ItemClass.TEXT,
    [DataType.Unitdate]: ItemClass.UNITDATE,
    [DataType.Unitid]: ItemClass.UNIT_ID,
    [DataType.UriRef]: ItemClass.URI_REF,
};

const dataCreateMap = {
    [ItemClass.BIT]: createBitData,
    [ItemClass.COORDINATES]: createCoordinatesData,
    [ItemClass.DATE]: createDateData,
    [ItemClass.DECIMAL]: createDecimalData,
    [ItemClass.ENUM]: createEnumData,
    [ItemClass.FILE_REF]: createFileRefData,
    [ItemClass.FORMATTED_TEXT]: createFormattedTextData,
    [ItemClass.INT]: createIntData,
    [ItemClass.JSON_TABLE]: createJsonTableData,
    [ItemClass.RECORD_REF]: createRecordRefData,
    [ItemClass.STRING]: createStringData,
    [ItemClass.STRUCTURE]: createStructureRefData,
    [ItemClass.TEXT]: createTextData,
    [ItemClass.UNITDATE]: createUnitdateData,
    [ItemClass.UNIT_ID]: createUnitIdData,
    [ItemClass.URI_REF]: createUriRefData,
};

export function convertToNewTemplate(template: DeprecatedNodeTemplate | NodeTemplate): NodeTemplate {
    const convertedFormData: NodeTemplateItem[] = [];
    if (Array.isArray(template.formData)) {
        // template is in new format, does not need conversion
        return template as NodeTemplate;
    }

    Object.entries(template.formData).forEach(([typeIdString, descItems]) => {
        const typeId = parseInt(typeIdString);
        if (isNaN(typeId)) {
            throw 'Desc item type id is not an int';
        }

        const _descItems = descItems;
        _descItems.forEach(({ value, position, ...descItem }) => {
            const itemClass = descItem['@class'];
            if (!itemClass) {
                throw 'ItemClass is undefined';
            }

            const createDataFn = dataCreateMap[itemClass];
            if (!createDataFn) {
                throw 'CreateData function is undefined';
            }

            convertedFormData.push({
                itemTypeId: typeId,
                itemSpecId: descItem.descItemSpecId,
                position,
                data:
                    createDataFn &&
                    createDataFn(
                        template.withValues ? value : undefined,
                        descItem.description,
                        descItem.nodeId,
                        descItem.refTemplateId
                    ),
            });
        });
    });

    console.log('#template converted:', convertedFormData, ', original:', template);
    return {
        ...template,
        formData: convertedFormData,
    };
}

export function convertToOldDescItem(descItem: NodeItem) {
    const itemClass = descItem.data?.dataType ? dataTypeToClassMap[descItem.data?.dataType] : undefined;
    const _value = getValue(descItem);

    return {
        '@class': itemClass,
        descItemSpecId: descItem.itemSpecId,
        itemTypeId: descItem.itemTypeId,
        position: descItem.position,
        value: _value.value,
        description: _value.description,
        refTemplateId: _value.refTemplateId,
        nodeId: _value.nodeId,
        descItemObjectId: descItem.itemObjectId,
        id: descItem.id,
        undefined: descItem.undefined,
        readOnly: descItem.readOnly,
        inhibited: descItem.inhibited,
    };
}

export function convertToOldTemplate(template: NodeTemplate) {
    const convertedFormData: DescItemFromServer<string | number | boolean>[] = [];
    if (!Array.isArray(template.formData)) {
        throw 'Old format not supported';
    }

    template.formData.forEach((descItem) => convertedFormData.push(convertToOldDescItem(descItem)));

    console.log('#template converted to old(array):', convertedFormData, ', original:', template);
    return {
        ...template,
        formData: convertedFormData,
    };
}
