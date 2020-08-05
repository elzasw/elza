import * as React from 'react';
import {DataTypeCode} from '../../stores/app/accesspoint/itemFormInterfaces';
import DescItemCoordinates from '../arr/nodeForm/DescItemCoordinates';
import DescItemDate from '../arr/nodeForm/DescItemDate';
import DescItemDecimal from '../arr/nodeForm/DescItemDecimal';
import DescItemFileRef from '../arr/nodeForm/DescItemFileRef';
import ItemFragmentRef from '../arr/nodeForm/DescItemFragmentRef';
import DescItemInt from '../arr/nodeForm/DescItemInt';
import DescItemJsonTable from '../arr/nodeForm/DescItemJsonTable';
import DescItemRecordRef from '../arr/nodeForm/DescItemRecordRef';
import DescItemString from '../arr/nodeForm/DescItemString';
import DescItemText from '../arr/nodeForm/DescItemText';
import DescItemUnitdate from '../arr/nodeForm/DescItemUnitdate';
import DescItemUnitid from '../arr/nodeForm/DescItemUnitid';
import {ItemFactoryInterface} from './ItemFactoryInterface';

import('components/arr/nodeForm/DescItemStructureRef').then(
    x => (ItemFactory.typeComponentMap[DataTypeCode.STRUCTURED] = x),
);

export class ItemFactory implements ItemFactoryInterface {
    static typeComponentMap: any = {
        [DataTypeCode.RECORD_REF]: DescItemRecordRef,
        [DataTypeCode.STRUCTURED]: null, //DescItemStructureRef,
        [DataTypeCode.FILE_REF]: DescItemFileRef,
        [DataTypeCode.UNITDATE]: DescItemUnitdate,
        [DataTypeCode.UNITID]: DescItemUnitid,
        [DataTypeCode.JSON_TABLE]: DescItemJsonTable,
        [DataTypeCode.STRING]: DescItemString,
        [DataTypeCode.FORMATTED_TEXT]: DescItemText,
        [DataTypeCode.TEXT]: DescItemText,
        [DataTypeCode.DECIMAL]: DescItemDecimal,
        [DataTypeCode.INT]: DescItemInt,
        [DataTypeCode.COORDINATES]: DescItemCoordinates,
        [DataTypeCode.DATE]: DescItemDate,
        [DataTypeCode.APFRAG_REF]: ItemFragmentRef,
        [DataTypeCode.URI_REF]: ItemFragmentRef,
    };

    static createItem(type: DataTypeCode, props) {
        const componentClass = ItemFactory.typeComponentMap[type];
        if (!componentClass) {
            throw new Error(`Unknown desc item data type code: ${type}`);
        }
        return React.createElement(componentClass, props);
    }

    createItem(type: DataTypeCode, props: any) {
        return ItemFactory.createItem(type, props);
    }
}
