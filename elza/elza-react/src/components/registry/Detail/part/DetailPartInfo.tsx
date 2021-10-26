import React, { FC } from 'react';
import { useSelector } from 'react-redux';
import { ItemType } from '../../../../api/ApViewSettings';
import { objectById } from '../../../../shared/utils';
import { Bindings } from '../../../../types';
import { AppState } from '../../../../typings/store';
import { DetailMultipleItem } from '../item';
import './DetailPartInfo.scss';
import { RevisionItem } from '../../revision';

interface Props {
    items: RevisionItem[];
    globalEntity: boolean;
    bindings: Bindings;
    itemTypeSettings: ItemType[];
}

export const DetailPartInfo: FC<Props> = ({
    items = [],
    globalEntity,
    bindings,
    itemTypeSettings,
}) => {
    const descItemTypesMap = useSelector((state: AppState) => state.refTables.descItemTypes.itemsMap || {})

    const renderItems = (items: RevisionItem[]) => {
        if (items.length === 0) {
            return (
                <i>Nejsou definovány žádné hodnoty atributů</i>
            );
        }

        const result: React.ReactNode[] = [];

        let itemGroup: RevisionItem[] = [];
        let itemGroupId: number | undefined;
        let groupStartIndex: number | undefined;

        items.forEach(({item, updatedItem}, index, array)=>{
            const typeId = item?.typeId || updatedItem?.typeId;
            // create item group for current typeId, if it doesn't exist
            if(typeId !== itemGroupId){
                itemGroupId = typeId;
                itemGroup = [];
                groupStartIndex = index;
            }

            // add item in item group
            if(typeId === itemGroupId){
                itemGroup.push({item, updatedItem});
            }

            const nextItem = array.length > index ? array[index + 1] : undefined;
            const nextTypeId = nextItem ? nextItem.item?.typeId || nextItem.updatedItem?.typeId : undefined;

            // add itemGroup to 'rendered' items only when the next item is not in the same
            // typeId group.

            if( typeId !== nextTypeId){
                result.push(
                    <DetailMultipleItem
                        key={groupStartIndex}
                        items={itemGroup}
                        globalEntity={globalEntity}
                        bindings={bindings}
                        typeId={typeId}
                        />
                )
            }
        })

        return result;
    };

    const sortedItems = items.sort((a, b) => {
        const aTypeId = a.item?.typeId || a.updatedItem?.typeId;
        const bTypeId = b.updatedItem?.typeId || b.updatedItem?.typeId;
        const aItemType: ItemType = aTypeId ? objectById(itemTypeSettings, descItemTypesMap[aTypeId].code, 'code') : null;
        const bItemType: ItemType = bTypeId ? objectById(itemTypeSettings, descItemTypesMap[bTypeId].code, 'code') : null;
        if (aItemType == null && bItemType == null) {
            return 0;
        } else if (aItemType == null) {
            return -1;
        } else if (bItemType == null) {
            return 1;
        } else {
            const aPos = aItemType.position || 9999;
            const bPos = bItemType.position || 9999;
            return aPos - bPos;
        }
    });

    return <div className="detail-part-info">
        {renderItems(sortedItems)}
    </div>
}
