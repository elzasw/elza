import React, { FC } from 'react';
import { useSelector } from 'react-redux';
import { ApItemVO } from '../../../api/ApItemVO';
import { ItemType } from '../../../api/ApViewSettings';
import { objectById } from '../../../shared/utils';
import { Bindings } from '../../../types';
import { AppState } from '../../../typings/store';
import DetailMultipleItem from './DetailMultipleItem';
import './DetailPart.scss';

interface Props {
    items: ApItemVO[];
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

    const renderItems = (items: ApItemVO[]) => {
        if (items.length === 0) {
            return (
                <div className={''}>
                    <i>Nejsou definovány žádné hodnoty atributů</i>
                </div>
            );
        }

        const result: React.ReactNode[] = [];

        let itemGroup: ApItemVO[] = [];
        let itemGroupId: number | undefined;
        let groupStartIndex: number | undefined;

        items.forEach((item, index, array)=>{
            // create item group for current typeId, if it doesn't exist
            if(item.typeId !== itemGroupId){
                itemGroupId = item.typeId;
                itemGroup = [];
                groupStartIndex = index;
            }

            // add item in item group
            if(item.typeId === itemGroupId){
                itemGroup.push(item);
            }

            const nextItem = array.length > index ? array[index + 1] : undefined;

            // add itemGroup to 'rendered' items only when the next item is not in the same
            // typeId group.
            if(item.typeId !== nextItem?.typeId){
                result.push(
                    <div key={groupStartIndex}>
                        <DetailMultipleItem
                            items={itemGroup}
                            globalEntity={globalEntity}
                            bindings={bindings}
                            />
                    </div>
                )
            }
        })

        return result;
    };

    const sortedItems = items.sort((a, b) => {
        const aItemType: ItemType = objectById(itemTypeSettings, descItemTypesMap[a.typeId].code, 'code');
        const bItemType: ItemType = objectById(itemTypeSettings, descItemTypesMap[b.typeId].code, 'code');
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

    return <div>
        {renderItems(sortedItems)}
    </div>
}
