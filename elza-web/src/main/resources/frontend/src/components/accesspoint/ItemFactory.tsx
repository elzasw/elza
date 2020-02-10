import * as React from 'react';
import DescItemString from '../arr/nodeForm/DescItemString.jsx';
import DescItemUnitid from '../arr/nodeForm/DescItemUnitid.jsx';
import DescItemText from '../arr/nodeForm/DescItemText.jsx';
import DescItemInt from '../arr/nodeForm/DescItemInt.jsx';
import DescItemDecimal from '../arr/nodeForm/DescItemDecimal.jsx';
import DescItemCoordinates from '../arr/nodeForm/DescItemCoordinates.jsx';
import DescItemUnitdate from '../arr/nodeForm/DescItemUnitdate.jsx';
// import DescItemStructureRef from '../arr/nodeForm/DescItemStructureRef.jsx';
import DescItemFileRef from '../arr/nodeForm/DescItemFileRef.jsx';
// import DescItemPartyRef from '../arr/nodeForm/DescItemPartyRef.jsx';
import DescItemRecordRef from '../arr/nodeForm/DescItemRecordRef.jsx';
import DescItemJsonTable from '../arr/nodeForm/DescItemJsonTable.jsx';
import DescItemDate from '../arr/nodeForm/DescItemDate.jsx';
import {DataTypeCode} from "../../stores/app/accesspoint/itemFormInterfaces";
import {ItemFactoryInterface} from "./ItemFactoryInterface";
// import ItemFragmentRef from "./ItemFragmentRef";
import ItemFragmentRef from '../arr/nodeForm/DescItemFragmentRef.jsx';

import('../arr/nodeForm/DescItemPartyRef.jsx').then(x => ItemFactory.typeComponentMap[DataTypeCode.PARTY_REF] = x);
import('../arr/nodeForm/DescItemStructureRef.jsx').then(x => ItemFactory.typeComponentMap[DataTypeCode.STRUCTURED] = x);

export class ItemFactory implements ItemFactoryInterface {
    static typeComponentMap = {
        [DataTypeCode.PARTY_REF]: null,//DescItemPartyRef,
        [DataTypeCode.RECORD_REF]: DescItemRecordRef,
        [DataTypeCode.STRUCTURED]: null,//DescItemStructureRef,
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
    };

    static createItem(type: DataTypeCode, props) {
        const componentClass = ItemFactory.typeComponentMap[type];
        if(!componentClass){
            throw "Unknown desc item data type code: " + type;
        }
        return React.createElement(componentClass, props);
    }

    createItem(type: DataTypeCode, props: any) {
        return ItemFactory.createItem(type, props)
    }
}
