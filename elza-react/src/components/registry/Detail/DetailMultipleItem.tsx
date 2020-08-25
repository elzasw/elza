import React, {FC} from 'react';
// import {CodelistState} from '../../shared/reducers/codelist/CodelistReducer';
import {connect} from 'react-redux';
import DetailItemContent from './DetailItemContent';
import './DetailItem.scss';
import {ApItemVO} from '../../../api/ApItemVO';
import {Bindings} from '../../../types';
import {RulDescItemTypeExtVO} from '../../../api/RulDescItemTypeExtVO';

interface Props extends ReturnType<typeof mapStateToProps> {
    bindings?: Bindings;
    items: ApItemVO[];
    globalEntity: boolean;
}

const DetailMultipleItem: FC<Props> = ({items, globalEntity, descItemTypesMap, bindings}) => {
    let firstItem = items[0];
    const itemType = descItemTypesMap[firstItem.typeId];

    return (
        <div className="detail-item detail-item-multiple">
            <div className="detail-item-header mt-2">
                {itemType ? itemType.name : `UNKNOWN_AE_TYPE: ${firstItem.typeId}`}
            </div>
            <div className="detail-item-content">
                {items.map((item, index) => (
                    <div key={index} className="detail-item-partvalue">
                        <DetailItemContent item={item} bindings={bindings} globalEntity={globalEntity} />
                    </div>
                ))}
            </div>
        </div>
    );
};

const mapStateToProps = state => ({
    descItemTypesMap: state.refTables.descItemTypes.itemsMap as RulDescItemTypeExtVO[],
});

export default connect(mapStateToProps)(DetailMultipleItem);
