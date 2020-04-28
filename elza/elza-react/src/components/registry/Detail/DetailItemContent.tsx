import React, {FC} from 'react';
import {AeItemCoordinatesVO, AeItemLinkVO, AeItemRecordRefVO, AeItemVO, SystemCode} from '../../../api/generated/model';
//import {CodelistState} from '../../shared/reducers/codelist/CodelistReducer';
import {connect} from 'react-redux';
import "./DetailItem.scss";
import {NavLink} from "react-router-dom";
import DetailCoordinateItem from "./coordinate/DetailCoordinateItem";
import {MOCK_CODE_DATA} from './mock';

interface Props extends ReturnType<typeof mapStateToProps> {
  item: AeItemVO;
  globalEntity: boolean;
}

const DetailItemContent: FC<Props> = ({item, globalEntity, codelist}) => {
  const itemType = codelist.itemTypesMap[item.itemTypeId];
  const dataType = codelist.dataTypesMap[itemType.dataTypeId];

  let customFieldRender = false;  // pro ty, co chtějí jinak renderovat skupinu...,  pokud je true, task se nerenderuje specifikace, ale pouze valueField a v tom musí být již vše...

  let valueField;
  let textValue;
  let displayValue;
  switch (dataType.systemCode) {
    case SystemCode.NULL:
      break;
    case SystemCode.LINK:
      let ii = item as AeItemLinkVO;
      valueField = <a href={ii.value} title={ii.value} target={"_blank"}>{ii.name || ii.value}</a>
      break;
    case SystemCode.RECORDREF:
      customFieldRender = true;

      textValue = typeof item.textValue === "undefined" || item.textValue == "" ? "?" : item.textValue;
      if (itemType.useSpecification) {
        displayValue = item.itemSpecId ? `${codelist.itemSpecsMap[item.itemSpecId].name}: ${textValue}` : textValue;
      } else {
        displayValue = item.textValue;
      }

      valueField =
        <NavLink target={"_blank"} to={`/global/${(item as AeItemRecordRefVO).value}`}>{displayValue}</NavLink>;
      break;
    case SystemCode.COORDINATES:
      customFieldRender = true;
      valueField = <DetailCoordinateItem item={item as AeItemCoordinatesVO} globalEntity={globalEntity}/>;
      break;
    default:
      valueField = typeof item.textValue === "undefined" || item.textValue == "" ? "?" : item.textValue;
      break;
  }

  let valueSpecification;
  if (!customFieldRender && itemType.useSpecification) {
    if (!!item.itemSpecId) {
      valueSpecification = codelist.itemSpecsMap[item.itemSpecId].name;
    } else {
      valueSpecification = <i>Bez specifikace</i>
    }
  }

  return (
    <div className="detail-item-content-value">
      {valueSpecification}{valueSpecification && valueField && ": "}{valueField}
    </div>
  );
};

const mapStateToProps = (state: { codelist }) => ({
    codelist: MOCK_CODE_DATA
});

export default connect(mapStateToProps)(DetailItemContent);
