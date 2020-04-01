import * as React from 'react';
import {DataTypeCode} from '../../stores/app/accesspoint/itemFormInterfaces';

export interface ItemFactoryInterface {
    createItem(type: DataTypeCode, props: any): React.ReactNode;
}
