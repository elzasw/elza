import React, { FC } from 'react';
import { useSelector } from 'react-redux';
import { ApItemVO } from "../../../../api/ApItemVO";
import { Bindings } from "../../../../types";
import { AppState } from "../../../../typings/store";
import "./DetailItem.scss";
import DetailItemContent from "./DetailItemContent";

interface Props {
    bindings?: Bindings;
    item: ApItemVO;
    globalEntity: boolean;
    select: boolean;
}

const DetailItem: FC<Props> = ({
    item, 
    globalEntity, 
    bindings,
    select
}) => {
    const descItemTypesMap = useSelector((state: AppState)=> state.refTables.descItemTypes.itemsMap || {})
    const typeId = item.typeId;
    const itemType = descItemTypesMap[typeId];
    const itemTypeName = itemType ? itemType.name : `UNKNOWN_AE_TYPE: ${typeId}`;

    return (
        <div className="detail-item">
            <div className="detail-item-header">
                {itemTypeName}
            </div>
            <div className="detail-item-content">
                <DetailItemContent select={select} item={item} bindings={bindings} globalEntity={globalEntity} revision={false}/>
            </div>
        </div>
    );
};

export default DetailItem;
