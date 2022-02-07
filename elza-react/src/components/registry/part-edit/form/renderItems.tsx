import React, { ReactNode, FC } from 'react';
// import { WrappedFieldArrayProps} from 'redux-form';
import { FieldArrayRenderProps } from 'react-final-form-arrays';
import './PartEditForm.scss';
import { Col } from 'react-bootstrap';
import { ApItemVO } from 'api/ApItemVO';
import { ApCreateTypeVO } from 'api/ApCreateTypeVO';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import { objectById } from 'shared/utils';
import { ItemType } from 'api/ApViewSettings';
import { ApDescItem } from './renderItem'
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store'
import {getRevisionItems, RevisionItem} from "../../revision";

interface RenderItemsProps extends FieldArrayRenderProps<ApItemVO, any> {
    disabled: boolean;
    deleteMode: boolean;
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>;
    itemTypeSettings: ItemType[];
    onDeleteItem?: (index: number) => void;
    itemPrefix?: string;
    partTypeId: number;
    scopeId: number;
    apTypeId: number;
    partItems: ApItemVO[] | null | undefined;
}

export const ItemsWrapper:FC<RenderItemsProps> = ({
    disabled,
    deleteMode,
    fields,
    itemTypeAttributeMap,
    itemTypeSettings,
    onDeleteItem,
    itemPrefix = "items",
    partTypeId,
    scopeId,
    apTypeId,
    partItems,
}) => {
    if (!fields.value) { return <></>; }

    const handleDeleteItem = (index: number) => {
        fields.remove(index);
        onDeleteItem?.(index);
    };

    const revisionItems = getRevisionItems(partItems || undefined, fields.value);
    const itemGroups = groupItemsByType(revisionItems);
    let absoluteIndex = 0;

    return <>
        {itemGroups.map((items, groupIndex) => {
            return <ApDescItemGroup
                key={groupIndex}
                items={items}
                itemTypeSettings={itemTypeSettings}
            >
                {(item) => {
                    const index = absoluteIndex;
                    absoluteIndex++;
                    return <ApDescItem
                        key={index}
                        deleteMode={deleteMode}
                        disabled={disabled}
                        name={`${itemPrefix}[${index}]`}
                        index={index}
                        item={item.updatedItem}
                        prevItem={item.item}
                        disableRevision={partItems === null}
                        onDeleteItem={handleDeleteItem}
                        itemTypeAttributeMap={itemTypeAttributeMap}
                        partTypeId={partTypeId}
                        scopeId={scopeId}
                        apTypeId={apTypeId}
                    />
                }}
            </ApDescItemGroup>
        })}
    </>;
};

const ApDescItemGroup: FC<{
    items: RevisionItem[];
    itemTypeSettings: ItemType[];
    children: (item: RevisionItem, index: number) => ReactNode;
}> = ({
    items,
    itemTypeSettings,
    children,
}) => {
    const descItemTypesMap = useSelector((state:AppState) => state.refTables.descItemTypes.itemsMap)

    return <Col
        xs={getItemWidth(items.length > 0 ? items[0].updatedItem : undefined, descItemTypesMap, itemTypeSettings)}
        className="item-wrapper"
    >
        {items.map(children)}
    </Col>
}

const groupItemsByType = (items: RevisionItem[]) => {
    const groups: RevisionItem[][] = [];
    items.forEach((item)=>{
        const existingGroupIndex = groups.findIndex((group) => item.updatedItem?.typeId === group[0]?.updatedItem?.typeId)
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

