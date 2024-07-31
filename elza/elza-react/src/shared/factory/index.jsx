// -
import { Item } from './Item';
import { ItemUnitdate } from './ItemUnitdate';
import { ItemRecordRef } from './ItemRecordRef';
import { ItemFileRef } from './ItemFileRef';
import { ItemCoordinates } from './ItemCoordinates';
import { ItemDecimal } from './ItemDecimal';
import { ItemEnum } from './ItemEnum';
import { ItemFormattedText } from './ItemFormattedText';
import { ItemInt } from './ItemInt';
import { ItemJsonTable } from './ItemJsonTable';
import { ItemString } from './ItemString';
import { ItemStructure } from './ItemStructure';
import { ItemText } from './ItemText';
import { ItemUnitid } from './ItemUnitid';
import { ItemBit } from './ItemBit';
import { ItemUriRef } from "./ItemUriRef";
import { DataTypeCode } from 'stores/app/accesspoint/itemFormUtils';

const itemsMap = {
    [DataTypeCode.UNITDATE]: ItemUnitdate,
    [DataTypeCode.RECORD_REF]: ItemRecordRef,
    [DataTypeCode.FILE_REF]: ItemFileRef,
    [DataTypeCode.COORDINATES]: ItemCoordinates,
    [DataTypeCode.DECIMAL]: ItemDecimal,
    [DataTypeCode.ENUM]: ItemEnum,
    [DataTypeCode.FORMATTED_TEXT]: ItemFormattedText,
    [DataTypeCode.INT]: ItemInt,
    [DataTypeCode.JSON_TABLE]: ItemJsonTable,
    [DataTypeCode.STRING]: ItemString,
    [DataTypeCode.STRUCTURED]: ItemStructure,
    [DataTypeCode.TEXT]: ItemText,
    [DataTypeCode.UNITID]: ItemUnitid,
    [DataTypeCode.URI_REF]: ItemUriRef,
    [DataTypeCode.TEXT]: ItemText,
    [DataTypeCode.BIT]: ItemBit,
}

export function createClass(item, refType) {
    if (!refType?.dataType?.code) { throw Error('Invalid refType', refType); }

    const ItemClass = itemsMap[refType.dataType.code];

    if (!ItemClass) {
        console.error('Invalid item class', item);
        return new Item(item, refType);
    }

    return new ItemClass(item, refType);
}
