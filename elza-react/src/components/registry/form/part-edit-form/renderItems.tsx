import React from 'react';
import { WrappedFieldArrayProps} from 'redux-form';
import './PartEditForm.scss';
import {ApPartFormVO} from '../../../../api/ApPartFormVO';
import {Col} from 'react-bootstrap';
import {ApItemVO} from '../../../../api/ApItemVO';
import {ApCreateTypeVO} from '../../../../api/ApCreateTypeVO';
import {RulDataTypeCodeEnum} from '../../../../api/RulDataTypeCodeEnum';
import {RulDescItemTypeExtVO} from '../../../../api/RulDescItemTypeExtVO';
import {objectById} from '../../../../shared/utils';
import { ItemType} from '../../../../api/ApViewSettings';
import { renderItem } from './renderItem'

export const renderItems = (
    props: WrappedFieldArrayProps & {
        disabled: boolean;
        refTables: any;
        partTypeId: number;
        deleteMode: boolean;
        itemTypeAttributeMap: Record<number, ApCreateTypeVO>;
        onCustomEditItem: (name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) => void;
        showImportDialog: (field: string) => void;
        formData?: ApPartFormVO;
        apId?: number;
        itemTypeSettings: ItemType[];
        descItemTypesMap: any;
    },
): any => {
    const {
        refTables,
        partTypeId,
        disabled,
        deleteMode,
        fields,
        onCustomEditItem,
        itemTypeAttributeMap,
        formData,
        apId,
        showImportDialog,
        itemTypeSettings,
        descItemTypesMap,
    } = props;

    const items = fields.getAll() as ApItemVO[];
    if (!items) {
        return <div />;
    }
    let index = 0;

    let result: any = [];

    const handleDeleteItem = (index: number) => {
        fields.remove(index);
    };

    while (index < items.length) {
        let index2 = index + 1;
        while (index2 < items.length && items[index].typeId === items[index2].typeId) {
            index2++;
        }

        const itemTypeExt: RulDescItemTypeExtVO = descItemTypesMap[items[index].typeId];
        let width = 2; // default
        if (itemTypeExt) {
            const itemType: ItemType = objectById(itemTypeSettings, itemTypeExt.code, 'code');
            if (itemType && itemType.width) {
                width = itemType.width;
            }
        }

        let sameItems = items.slice(index, index2);
        // eslint-disable-next-line
        const inputs = sameItems.map((item, i) => {
            let name = `items[${i + index}]`;
            return renderItem(
                name,
                i + index,
                fields,
                refTables,
                disabled,
                deleteMode,
                handleDeleteItem,
                onCustomEditItem,
                itemTypeAttributeMap,
                showImportDialog,
                apId,
                formData,
            );
        });

        result.push(
            <Col key={index} xs={width <= 0 ? 12 : width} className="item-wrapper">
                {inputs}
            </Col>,
        );

        index = index2;
    }

    return result;
};

