import React, { ReactNode, FC } from 'react';
// import { WrappedFieldArrayProps} from 'redux-form';
import { FieldArrayRenderProps } from 'react-final-form-arrays';
import './PartEditForm.scss';
import { Col } from 'react-bootstrap';
import { ApItemVO } from '../../../../api/ApItemVO';
import { ApCreateTypeVO } from '../../../../api/ApCreateTypeVO';
import { RulDataTypeCodeEnum } from '../../../../api/RulDataTypeCodeEnum';
import { RulDescItemTypeExtVO } from '../../../../api/RulDescItemTypeExtVO';
import { objectById } from '../../../../shared/utils';
import { ItemType } from '../../../../api/ApViewSettings';
import { ApDescItem } from './renderItem'
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store'

interface RenderItemsProps extends FieldArrayRenderProps<ApItemVO, any> {
    disabled: boolean;
    deleteMode: boolean;
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>;
    onCustomEditItem: (name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) => void;
    showImportDialog: (field: string) => void;
    itemTypeSettings: ItemType[];
    onDeleteItem?: (index: number) => void
}

export const ItemsWrapper:FC<RenderItemsProps> = ({
    disabled,
    deleteMode,
    fields,
    onCustomEditItem,
    itemTypeAttributeMap,
    showImportDialog,
    itemTypeSettings,
    onDeleteItem,
}) => {
    if (!fields.value) { return <></>; }

    const handleDeleteItem = (index: number) => {
        fields.remove(index);
        onDeleteItem?.(index);
    };

    const itemGroups = groupItemsByType(fields.value);

    return <>
        {itemGroups.map((items, groupIndex) => {
            return <ApDescItemGroup
                key={groupIndex}
                items={items}
                itemTypeSettings={itemTypeSettings}
            >
                {(item, index) => {
                    const absoluteIndex = index + groupIndex;
                    return <ApDescItem
                        key={index}
                        deleteMode={deleteMode}
                        disabled={disabled}
                        name={`items[${absoluteIndex}]`}
                        index={absoluteIndex}
                        item={item}
                        onDeleteItem={handleDeleteItem}
                        onEdit={onCustomEditItem}
                        onImport={showImportDialog}
                        itemTypeAttributeMap={itemTypeAttributeMap}
                    />
                }}
            </ApDescItemGroup>
        })}
    </>;
};

const ApDescItemGroup: FC<{
    items: ApItemVO[];
    itemTypeSettings: ItemType[];
    children: (item: ApItemVO, index: number) => ReactNode;
}> = ({
    items,
    itemTypeSettings,
    children,
}) => {
    const descItemTypesMap = useSelector((state:AppState) => state.refTables.descItemTypes.itemsMap)

    return <Col
        xs={getItemWidth(items.length > 0 ? items[0] : undefined, descItemTypesMap, itemTypeSettings)}
        className="item-wrapper"
    >
        {items.map(children)}
    </Col>
}

const groupItemsByType = (items: ApItemVO[]) => {
    const groups: ApItemVO[][] = [];
    items.forEach((item)=>{
        const existingGroupIndex = groups.findIndex((group) => item.typeId === group[0]?.typeId)
        if(existingGroupIndex >= 0){
            groups[existingGroupIndex].push(item);
        } else {
            groups.push([item]);
        }
    })
    return groups;
}

const getItemWidth = (
    item: ApItemVO | undefined,
    descItemTypesMap: Record<number, RulDescItemTypeExtVO>,
    itemTypeSettings: ItemType[]
) => {
    const groupTypeId = item?.typeId;
    const itemTypeExt = groupTypeId && descItemTypesMap ? descItemTypesMap[groupTypeId] : undefined;

    let width = 2; // default

    if (itemTypeExt) {
        const itemType: ItemType = objectById(itemTypeSettings, itemTypeExt.code, 'code');
        if (itemType && itemType.width) {
            width = itemType.width;
        }
    }

    return width <= 0 ? 12 : width;
}

