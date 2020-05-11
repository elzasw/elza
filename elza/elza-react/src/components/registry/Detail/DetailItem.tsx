import React, { FC } from 'react';
// import { CodelistState } from '../../../shared/reducers/codelist/CodelistReducer';
import { connect } from 'react-redux';
import DetailItemContent from "./DetailItemContent";
import "./DetailItem.scss";
import {MOCK_CODE_DATA} from './mock';
import {ApItemVO} from "../../../api/ApItemVO";

interface Props extends ReturnType<typeof mapStateToProps> {
  item: ApItemVO;
  globalEntity: boolean;
}
const DetailItem: FC<Props> = ({ item, globalEntity, codelist }) => {
  const itemType = codelist.itemTypesMap[item.typeId];

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

const mapStateToProps = (state: { codelist: any }) => ({
    codelist: MOCK_CODE_DATA
});

export default connect(mapStateToProps)(DetailItem);
