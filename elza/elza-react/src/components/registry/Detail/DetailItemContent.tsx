import React, {FC} from 'react';
import {connect} from 'react-redux';
import "./DetailItem.scss";
import {NavLink} from "react-router-dom";
import DetailCoordinateItem from "./coordinate/DetailCoordinateItem";
import {ApItemVO} from "../../../api/ApItemVO";
import {ApItemCoordinatesVO} from "../../../api/ApItemCoordinatesVO";
import {ApItemAccessPointRefVO} from "../../../api/ApItemAccessPointRefVO";
import {ApItemUriRefVO} from "../../../api/ApItemUriRefVO";
import {RulDataTypeCodeEnum} from "../../../api/RulDataTypeCodeEnum";
import {RulDataTypeVO} from "../../../api/RulDataTypeVO";
import {ApItemBitVO} from "../../../api/ApItemBitVO";
import {formatDate} from "../../validate";
import {ApItemStringVO} from "../../../api/ApItemStringVO";
import {ApItemDateVO} from "../../../api/ApItemDateVO";
import {ApItemUnitdateVO} from "../../../api/ApItemUnitdateVO";
import {RulDescItemTypeExtVO} from "../../../api/RulDescItemTypeExtVO";
import {getMapFromList} from "../../../shared/utils";
import {RulDescItemSpecExtVO} from "../../../api/RulDescItemSpecExtVO";

interface Props extends ReturnType<typeof mapStateToProps> {
    item: ApItemVO;
    globalEntity: boolean;
}

const DetailItemContent: FC<Props> = ({item, globalEntity, rulDataTypes, descItemTypes}) => {
    const itemType = descItemTypes.itemsMap[item.typeId];
    const dataType: RulDataTypeVO = rulDataTypes.itemsMap[itemType.dataTypeId];

    // pro ty, co chtějí jinak renderovat skupinu...,  pokud je true, task se nerenderuje specifikace, ale pouze valueField a v tom musí být již vše...
    let customFieldRender = false;

    let valueField;
    let textValue;
    let displayValue;

    switch (dataType.code) {
        case RulDataTypeCodeEnum.INT:
        case RulDataTypeCodeEnum.STRING:
        case RulDataTypeCodeEnum.TEXT:
        case RulDataTypeCodeEnum.FORMATTED_TEXT:
        case RulDataTypeCodeEnum.DECIMAL:
            let textItem = item as ApItemStringVO;
            valueField = textItem.value;
            break;

        case RulDataTypeCodeEnum.BIT:
            let bitItem = item as ApItemBitVO;
            valueField = bitItem.value ? 'Ano' : 'Ne';
            break;

        case RulDataTypeCodeEnum.COORDINATES:
            customFieldRender = true;
            valueField = <DetailCoordinateItem item={item as ApItemCoordinatesVO} globalEntity={globalEntity}/>;
            break;

        case RulDataTypeCodeEnum.RECORD_REF:
            customFieldRender = true;
            let recordRefItem = item as ApItemAccessPointRefVO;

            textValue = typeof recordRefItem.value !== 'undefined' && recordRefItem.value ? recordRefItem.value : '?';
            if (itemType.useSpecification) {
                displayValue = recordRefItem.specId ? `${descItemTypes.itemsMap[recordRefItem.specId].name}: ${textValue}` : textValue;
            } else {
                displayValue = recordRefItem.value;
            }

            valueField = <NavLink target={"_blank"} to={`/global/${recordRefItem.value}`}>{displayValue}</NavLink>;
            break;

        case RulDataTypeCodeEnum.ENUM:
            //Resime az nize
            break;

        case RulDataTypeCodeEnum.UNITDATE:
            let unitdateItem = item as ApItemUnitdateVO;
            valueField = unitdateItem.value;
            break;

        case RulDataTypeCodeEnum.DATE:
            let dateItem = item as ApItemDateVO;
            valueField = formatDate(dateItem.value);
            break;

        case RulDataTypeCodeEnum.URI_REF:
            let ii = item as ApItemUriRefVO;
            valueField = <a href={ii.value} title={ii.value} target={"_blank"}
                            rel={"noopener noreferrer"}>{ii.description || ii.value}</a>
            break;

        //todo: Dodelat zobrazeni pro tyto typy
        case RulDataTypeCodeEnum.JSON_TABLE:
        case RulDataTypeCodeEnum.FILE_REF:
        case RulDataTypeCodeEnum.APFRAG_REF:
        case RulDataTypeCodeEnum.UNITID:
        case RulDataTypeCodeEnum.STRUCTURED:
        default:
            let defItem = item as ApItemStringVO;
            valueField = 'value' in defItem && typeof defItem.value !== 'undefined' && defItem.value ? defItem.value : '?';
            break;
    }

    let valueSpecification;
    if (!customFieldRender && itemType.useSpecification ) {
        valueSpecification = <i>Bez specifikace</i>
        if (item.specId) {
            const itemSpec = getMapFromList(itemType.descItemSpecs) as Record<number, RulDescItemSpecExtVO>;
            if (itemSpec[item.specId]) {
                valueSpecification = itemSpec[item.specId].name;
            }
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
    descItemTypes: state.refTables.descItemTypes as { itemsMap: Record<number, RulDescItemTypeExtVO>, items: RulDescItemTypeExtVO[] },
});

export default connect(mapStateToProps)(DetailItemContent);
