import React, {FC} from 'react';
import {connect} from 'react-redux';
import DetailItemContent from "./DetailItemContent";
import "./DetailItem.scss";
import {ApItemVO} from "../../../api/ApItemVO";
import {RulDescItemTypeExtVO} from "../../../api/RulDescItemTypeExtVO";

interface Props extends ReturnType<typeof mapStateToProps> {
    item: ApItemVO;
    globalEntity: boolean;
}

const DetailItem: FC<Props> = ({item, globalEntity, descItemTypesMap}) => {
    const itemType = descItemTypesMap[item.typeId];

    return (
        <div className="detail-item">
            <div className="detail-item-header mt-1">
                {itemType ? itemType.name : `UNKNOWN_AE_TYPE: ${item.typeId}`}
            </div>
            <div className="detail-item-content">
                <DetailItemContent item={item} globalEntity={globalEntity}/>
            </div>
        </div>
    );
};

const mapStateToProps = (state) => ({
    descItemTypesMap: state.refTables.descItemTypes.itemsMap as RulDescItemTypeExtVO[],
});

export default connect(mapStateToProps)(DetailItem);
