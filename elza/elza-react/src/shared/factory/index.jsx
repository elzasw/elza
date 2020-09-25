// -
import {CLS} from './factoryConsts';
import {Item} from './Item';
import {ItemUnitdate} from './ItemUnitdate';
import {ItemRecordRef} from './ItemRecordRef';
import {ItemFileRef} from './ItemFileRef';
import {ItemCoordinates} from './ItemCoordinates';
import {ItemDecimal} from './ItemDecimal';
import {ItemEnum} from './ItemEnum';
import {ItemFormattedText} from './ItemFormattedText';
import {ItemInt} from './ItemInt';
import {ItemJsonTable} from './ItemJsonTable';
import {ItemString} from './ItemString';
import {ItemStructure} from './ItemStructure';
import {ItemText} from './ItemText';
import {ItemUnitid} from './ItemUnitid';
import {ItemBit} from './ItemBit';

const itemsMap = {
    '.ArrItemUnitdateVO': ItemUnitdate,
    '.ArrItemRecordRefVO': ItemRecordRef,
    '.ArrItemFileRefVO': ItemFileRef,
    '.ArrItemCoordinatesVO': ItemCoordinates,
    '.ArrItemDecimalVO': ItemDecimal,
    '.ArrItemEnumVO': ItemEnum,
    '.ArrItemFormattedTextVO': ItemFormattedText,
    '.ArrItemIntVO': ItemInt,
    '.ArrItemJsonTableVO': ItemJsonTable,
    '.ArrItemStringVO': ItemString,
    '.ArrItemStructureVO': ItemStructure,
    '.ArrItemTextVO': ItemText,
    '.ArrItemUnitidVO': ItemUnitid,
    '.ArrItemBitVO': ItemBit,
};

export function createClass(item) {
    const itemCls = item[CLS];
    const cls = itemsMap[itemCls];
    if (cls) {
        return new cls(item);
    } else {
        console.error('Invalid item class', item);
        return new Item(item);
    }
}
