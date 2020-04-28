import React, {FC} from 'react';
import {AeItemVO} from '../../../api/generated/model';
// import {CodelistState} from '../../shared/reducers/codelist/CodelistReducer';
import {connect} from 'react-redux';
import DetailItemContent from "./DetailItemContent";
import "./DetailItem.scss";
import {MOCK_CODE_DATA} from './mock';

interface Props extends ReturnType<typeof mapStateToProps> {
  items: Array<AeItemVO>;
  globalEntity: boolean;
}

const DetailMultipleItem: FC<Props> = ({items,globalEntity, codelist}) => {
  let firstItem = items[0];
  const itemType = codelist.itemTypesMap[firstItem.itemTypeId];

  return (
    <div className="detail-item detail-item-multiple">
      <div className="detail-item-header mt-2">
        {itemType ? itemType.name : `UNKNOWN_AE_TYPE: ${firstItem.itemTypeId}`}
      </div>
      <div className="detail-item-content">
        {items.map((item, index) => (
            <div key={index} className="detail-item-partvalue">
              <DetailItemContent item={item} globalEntity={globalEntity}/>
            </div>
          )
        )}
      </div>
    </div>
  );
};

const mapStateToProps = (state: { codelist: any }) => ({
    codelist: MOCK_CODE_DATA
});

export default connect(mapStateToProps)(DetailMultipleItem);
