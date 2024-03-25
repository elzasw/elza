import React, { ReactNode, FC } from 'react';
// import { WrappedFieldArrayProps} from 'redux-form';
import { FieldArrayRenderProps } from 'react-final-form-arrays';
import './PartEditForm.scss';
import { Col } from 'react-bootstrap';
import { ApItemVO } from 'api/ApItemVO';
import { ApCreateTypeVO } from 'api/ApCreateTypeVO';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import { objectById, storeFromArea } from 'shared/utils';
import { ItemType } from 'api/ApViewSettings';
import { ApDescItem } from './renderItem'
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store'
import { RevisionItem } from "../../revision";
import { DetailStoreState } from 'types';
import { ApAccessPointVO } from 'api';
import { AREA_REGISTRY_DETAIL } from 'actions/registry/registry';

interface RenderItemsProps extends FieldArrayRenderProps<RevisionItem, any> {
    disabled: boolean;
    deleteMode: boolean;
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>;
    itemTypeSettings: ItemType[];
    onDeleteItem?: (index: number) => void;
    itemPrefix?: string;
    partTypeId: number;
    scopeId: number;
    apTypeId: number;
    revision?: boolean;
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
    revision = false,
}) => {
    const detail = useSelector((appState: AppState) => {
        const detail = storeFromArea(appState, AREA_REGISTRY_DETAIL) as DetailStoreState<ApAccessPointVO>;
        return detail.data;
    })
    if (!fields.value) { return <></>; }

    const handleDeleteItem = (index: number) => {
        fields.remove(index);
        onDeleteItem?.(index);
    };

    const revisionItems = fields.value // getRevisionItems(partItems || undefined, fields.value);
    const itemGroups = groupItemsByType(revisionItems);
    let absoluteIndex = 0;

    return <>
        {itemGroups.map((items, groupIndex) => {
            return <ApDescItemGroup
                key={groupIndex}
                items={items}
                itemTypeSettings={itemTypeSettings}
                revisionActive={revision}
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
                        disableRevision={!revision}
                        onDeleteItem={handleDeleteItem}
                        itemTypeAttributeMap={itemTypeAttributeMap}
                        partTypeId={partTypeId}
                        scopeId={scopeId}
                        apTypeId={apTypeId}
                        entityName={detail?.name}
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
    revisionActive?: boolean;
}> = ({
    items,
    itemTypeSettings,
    children,
    revisionActive = false,
}) => {
    const descItemTypesMap = useSelector((state:AppState) => state.refTables.descItemTypes.itemsMap)

    return <Col
        xs={getItemWidth(items.length > 0 ? items[0].updatedItem : undefined, descItemTypesMap, itemTypeSettings, revisionActive)}
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
    itemTypeSettings: ItemType[],
    revisionActive: boolean = false,
) => {
    const groupTypeId = item?.typeId;
    const itemTypeExt = groupTypeId && descItemTypesMap ? descItemTypesMap[groupTypeId] : undefined;

    let width = 2; // default
    const max = 12;

    if (itemTypeExt) {
        const itemType: ItemType = objectById(itemTypeSettings, itemTypeExt.code, 'code');
        if (itemType && itemType.width) {
            width = itemType.width;
        }
        if(revisionActive){
            const increase = 3;
            width = width + increase <= max ? width + increase : max;
        }
    }

    return width <= 0 ? max : width;
}

