import React, {FC} from 'react';
import {SystemCode} from '../../../api/generated/model';
//import {CodelistState} from '../../shared/reducers/codelist/CodelistReducer';
import {connect} from 'react-redux';
import "./DetailItem.scss";
import {NavLink} from "react-router-dom";
import DetailCoordinateItem from "./coordinate/DetailCoordinateItem";
import {MOCK_CODE_DATA} from './mock';
import {ApItemVO} from "../../../api/ApItemVO";
import {ApItemCoordinatesVO} from "../../../api/ApItemCoordinatesVO";
import {ApItemAccessPointRefVO} from "../../../api/ApItemAccessPointRefVO";
import {ApItemUriRefVO} from "../../../api/ApItemUriRefVO";

interface Props extends ReturnType<typeof mapStateToProps> {
    item: ApItemVO;
    globalEntity: boolean;
}

const DetailItemContent: FC<Props> = ({item, globalEntity, rulDataTypes, descItemTypes}) => {
    const itemType = descItemTypes.itemsMap[item.typeId];
    const dataType = rulDataTypes.itemsMap[itemType.dataTypeId];

    let customFieldRender = false;  // pro ty, co chtějí jinak renderovat skupinu...,  pokud je true, task se nerenderuje specifikace, ale pouze valueField a v tom musí být již vše...

    let valueField;
    let textValue;
    let displayValue;
    switch (dataType.systemCode) {
        case SystemCode.NULL:
            break;
        case SystemCode.LINK:
            let ii = item as ApItemUriRefVO;
            valueField = <a href={ii.value} title={ii.value} target={"_blank"}
                            rel={"noopener noreferrer"}>{ii.description || ii.value}</a>
            break;
        case SystemCode.RECORDREF:
            customFieldRender = true;
            let recordRefItem = item as ApItemAccessPointRefVO;

            textValue = typeof recordRefItem.value !== 'undefined' && recordRefItem.value ? recordRefItem.value : '?';
            if (itemType.useSpecification) {
                displayValue = recordRefItem.specId ? `${descItemTypes.itemsMap[recordRefItem.specId].name}: ${textValue}` : textValue;
            } else {
                displayValue = recordRefItem.value;
            }

            valueField =
                <NavLink target={"_blank"} to={`/global/${recordRefItem.value}`}>{displayValue}</NavLink>;
            break;
        case SystemCode.COORDINATES:
            customFieldRender = true;
            valueField = <DetailCoordinateItem item={item as ApItemCoordinatesVO} globalEntity={globalEntity}/>;
            break;
        default:
            //todo: pridat dalsi case dle typu. Vychozi ApItemVO nema value
            //valueField = 'value' in item && typeof item.value !== 'undefined' && item.value ? item.value : '?';
            break;
    }

    let valueSpecification;
    if (!customFieldRender && itemType.useSpecification) {
        if (!!item.specId && descItemTypes.itemsMap[item.specId]) {
            valueSpecification = descItemTypes.itemsMap[item.specId].name;
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

const mapStateToProps = (state) => ({
    rulDataTypes: state.refTables.rulDataTypes,
    descItemTypes: state.refTables.descItemTypes,

});

export default connect(mapStateToProps)(DetailItemContent);
