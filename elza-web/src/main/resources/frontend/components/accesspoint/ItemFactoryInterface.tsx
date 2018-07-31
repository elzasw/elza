import * as React from 'react';
import {DataTypeCode} from "../../stores/app/accesspoint/itemForm";

export interface ItemFactoryInterface {
    createItem(type: DataTypeCode, props: any): React.ReactNode;
}
