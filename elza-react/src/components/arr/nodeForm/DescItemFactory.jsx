import React from 'react';
import DescItemString from './DescItemString.jsx';
import DescItemUnitid from './DescItemUnitid.jsx';
import DescItemText from './DescItemText.jsx';
import DescItemInt from './DescItemInt.jsx';
import DescItemDecimal from './DescItemDecimal.jsx';
import DescItemCoordinates from './DescItemCoordinates.jsx';
import DescItemUnitdate from './DescItemUnitdate.jsx';
import DescItemStructureRef from './DescItemStructureRef.jsx';
import DescItemFileRef from './DescItemFileRef.jsx';
import DescItemRecordRef from './DescItemRecordRef.jsx';
import DescItemJsonTable from './DescItemJsonTable.jsx';
import DescItemDate from './DescItemDate.jsx';
import {DataTypeCode} from 'stores/app/accesspoint/itemFormInterfaces';

export default class DescItemFactory {
    static typeComponentMap = {
        RECORD_REF: DescItemRecordRef,
        STRUCTURED: DescItemStructureRef,
        FILE_REF: DescItemFileRef,
        UNITDATE: DescItemUnitdate,
        UNITID: DescItemUnitid,
        JSON_TABLE: DescItemJsonTable,
        STRING: DescItemString,
        FORMATTED_TEXT: DescItemText,
        TEXT: DescItemText,
        DECIMAL: DescItemDecimal,
        INT: DescItemInt,
        COORDINATES: DescItemCoordinates,
        DATE: DescItemDate,
    };

    static createDescItem = (type, props) => {
        const DescItem = DescItemFactory.typeComponentMap[type];
        if (!DescItem) {
            if (type === DataTypeCode.PARTY_REF) {
                alert('PARTY_REF');
                console.warn('%c ::party ', 'background: black; color: yellow;');
                return null;
            } else {
                throw new Error(`Unknown desc item data type code: ${type}`);
            }
        }
        return <DescItem {...props} />;
    };
}
