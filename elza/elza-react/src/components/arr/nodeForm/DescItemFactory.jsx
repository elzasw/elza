import React from 'react';
import DescItemString from './DescItemString';
import DescItemUnitid from './DescItemUnitid';
import DescItemText from './DescItemText';
import DescItemInt from './DescItemInt';
import DescItemDecimal from './DescItemDecimal';
import DescItemCoordinates from './DescItemCoordinates';
import DescItemUnitdate from './DescItemUnitdate';
import DescItemStructureRef from './DescItemStructureRef';
import DescItemFileRef from './DescItemFileRef';
import DescItemRecordRef from './DescItemRecordRef';
import DescItemJsonTable from './DescItemJsonTable';
import DescItemDate from './DescItemDate';
import DescItemLink from './DescItemLink';

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
        URI_REF: DescItemLink,
    };

    static createDescItem = (type, props) => {
        props.ref && typeof props.ref === 'string' && console.log('ref', props.ref, props);
        const DescItem = DescItemFactory.typeComponentMap[type];
        if (!DescItem) {
            throw new Error(`Unknown desc item data type code: ${type}`);
        }
        return <DescItem {...props} />;
    };
}
