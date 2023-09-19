import React, { FC } from 'react';
import { connect } from 'react-redux';
import { Action } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import {goToAe} from '../../../../actions/registry/registry';
import { ApItemAccessPointRefVO } from '../../../../api/ApItemAccessPointRefVO';
import { ApItemBitVO } from '../../../../api/ApItemBitVO';
import { ApItemCoordinatesVO } from '../../../../api/ApItemCoordinatesVO';
import { ApItemDateVO } from '../../../../api/ApItemDateVO';
import { ApItemStringVO } from '../../../../api/ApItemStringVO';
import { ApItemUnitdateVO } from '../../../../api/ApItemUnitdateVO';
import { ApItemUriRefVO } from '../../../../api/ApItemUriRefVO';
import { ApItemVO } from '../../../../api/ApItemVO';
import { RulDataTypeCodeEnum } from '../../../../api/RulDataTypeCodeEnum';
import { RulDataTypeVO } from '../../../../api/RulDataTypeVO';
import { RulDescItemSpecExtVO } from '../../../../api/RulDescItemSpecExtVO';
import { RulDescItemTypeExtVO } from '../../../../api/RulDescItemTypeExtVO';
import { getMapFromList, objectById } from '../../../../shared/utils';
import { Bindings } from '../../../../types';
import i18n from '../../../i18n';
import Icon from '../../../shared/icon/Icon';
import { Button } from '../../../ui';
import { formatDate } from '../../../validate';
import { DetailCoordinateItem } from '../coordinate/DetailCoordinateItem';
import './DetailItem.scss';
import { SyncIcon } from '../sync-icon';
import { SyncState } from '../../../../api/SyncState';
import {RouteComponentProps, withRouter} from "react-router";
import {Link} from "react-router-dom";
import {diffChars, diffWords} from "diff";
import { urlEntity } from '../../../../constants';

interface OwnProps extends ReturnType<typeof mapStateToProps> {
    item: ApItemVO;
    prevItem?: ApItemVO;
    updatedItem?: ApItemVO;
    globalEntity: boolean;
    bindings?: Bindings;
    revision?: boolean;
    select: boolean;
}

type Props = OwnProps & ReturnType<typeof mapDispatchToProps> & RouteComponentProps;
const SHORT_TEXT_LENGTH = 40;

const DetailItemContent: FC<Props> = ({
    item, 
    prevItem,
    updatedItem,
    globalEntity, 
    rulDataTypes,
    descItemTypes, 
    bindings, 
    selectAp,
    revision
}) => {
    const itemType = descItemTypes.itemsMap[item.typeId];
    const dataType: RulDataTypeVO = rulDataTypes.itemsMap[itemType.dataTypeId];

    const itemBinding = bindings && bindings.itemsMap[item.id!];

    // pro ty, co chtějí jinak renderovat skupinu...,  pokud je true, task se nerenderuje specifikace, ale pouze valueField a v tom musí být již vše...
    let customFieldRender = false;

    let valueField: React.ReactNode;
    let textValue:string;

    switch (dataType.code) {
        case RulDataTypeCodeEnum.INT:
        case RulDataTypeCodeEnum.DECIMAL:
            valueField = (item as ApItemStringVO).value;
            break;

        case RulDataTypeCodeEnum.STRING:
        case RulDataTypeCodeEnum.TEXT:
        case RulDataTypeCodeEnum.FORMATTED_TEXT:
            textValue = (item as ApItemStringVO).value;
            const prevTextValue = (prevItem as ApItemStringVO | undefined)?.value;
            const updatedTextValue = (updatedItem as ApItemStringVO | undefined)?.value;

            valueField = textValue;
            if(textValue){
                const diffFn = textValue.includes(" ") && textValue.length > SHORT_TEXT_LENGTH ? diffWords : diffChars;

                if(updatedTextValue){
                    const diff = diffFn(textValue, updatedTextValue)
                    valueField = <div>
                        {diff.map(({removed, value, added})=>{
                            return !added 
                                ? <span className={removed ? "removed" : undefined}>
                                    {value}
                                </span> 
                                : <></>
                        })}
                    </div>
                }
                if(prevTextValue){
                    const diff = diffFn(prevTextValue, textValue)
                    valueField = <div>
                        {diff.map(({added, value, removed})=>{
                            return !removed 
                                ? <span className={added ? "added" : undefined}>
                                    {value}
                                </span> 
                                : <></>
                        })}
                    </div>
                }
            }
            break;

        case RulDataTypeCodeEnum.BIT:
            let bitItem = item as ApItemBitVO;
            valueField = bitItem.value ? i18n('global.title.yes') : i18n('global.title.no');
            break;

        case RulDataTypeCodeEnum.COORDINATES:
            customFieldRender = true;
            valueField = <DetailCoordinateItem item={item as ApItemCoordinatesVO} />;
            break;

        case RulDataTypeCodeEnum.RECORD_REF:
            customFieldRender = true;
            let recordRefItem = item as ApItemAccessPointRefVO;
            let displayValue: string;

            textValue = '?';
            if (recordRefItem.value && recordRefItem.accessPoint) {
                textValue = recordRefItem.accessPoint.name;
            } else if (recordRefItem.externalName) {
                textValue = i18n('ap.form.ref.value', recordRefItem.externalName);
            }
            if (itemType.useSpecification) {
                const specs = descItemTypes.itemsMap[recordRefItem.typeId].descItemSpecs;
                const specId = recordRefItem.specId;
                const spec = specId ? objectById(specs, specId) : null;
                displayValue = spec ? `${spec.name}: ${textValue}` : textValue;
            } else {
                displayValue = textValue;
            }

            if (recordRefItem.value) {
                valueField = (
                    <Link 
                        to={urlEntity(recordRefItem.value)} 
                    >
                        {displayValue}
                    </Link>
                );
            } else if (recordRefItem.externalUrl) {
                valueField = (
                    <a target="_blank" href={recordRefItem.externalUrl} rel="noopener noreferrer">
                        {displayValue}
                    </a>
                );
            } else {
                valueField = <span>{displayValue}</span>;
            }

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
            valueField = (
                <a href={ii.value} title={ii.value} target={'_blank'} rel={'noopener noreferrer'}>
                    {ii.description || ii.value}
                </a>
            );
            break;

        //todo: Dodelat zobrazeni pro tyto typy
        case RulDataTypeCodeEnum.JSON_TABLE:
        case RulDataTypeCodeEnum.FILE_REF:
        case RulDataTypeCodeEnum.UNITID:
        case RulDataTypeCodeEnum.STRUCTURED:
        default:
            let defItem = item as ApItemStringVO;
            valueField =
                'value' in defItem && typeof defItem.value !== 'undefined' && defItem.value ? defItem.value : '?';
            break;
    }

    let valueSpecification: React.ReactNode;
    if (!customFieldRender && itemType.useSpecification) {
        valueSpecification = <i>Bez specifikace</i>;
        if (item.specId) {
            const itemSpec = getMapFromList(itemType.descItemSpecs) as Record<number, RulDescItemSpecExtVO>;
            if (itemSpec[item.specId]) {
                valueSpecification = itemSpec[item.specId].name;
            }
        }
    }

    return (
        <div className="detail-item-content-value">
            {valueSpecification}
            {valueSpecification && valueField && ': '}
            {valueField}
            {(itemBinding != null) && (
                <span className="sync-wrapper">
                    <SyncIcon syncState={ itemBinding ? SyncState.SYNC_OK : SyncState.LOCAL_CHANGE}/>
                </span>
            )}
        </div>
    );
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>, {history, select}: RouteComponentProps & {select: boolean}) => ({
    selectAp: (apId: number) => {
        dispatch(goToAe(history, apId, true, !select));
    },
});

const mapStateToProps = state => ({
    rulDataTypes: state.refTables.rulDataTypes,
    descItemTypes: state.refTables.descItemTypes as {
        itemsMap: Record<number, RulDescItemTypeExtVO>;
        items: RulDescItemTypeExtVO[];
    },
});

export default withRouter(connect<any, any, RouteComponentProps>(mapStateToProps, mapDispatchToProps)(DetailItemContent)) as any;
