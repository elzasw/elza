import React, { FC, ReactNode } from 'react';
// import { Field, FieldArrayFieldsProps} from 'redux-form';
import { useSelector } from 'react-redux';
import classNames from 'classnames';
import {Icon} from '../../../index';
import {Button, Col, Row} from 'react-bootstrap';
import {ApItemVO} from 'api/ApItemVO';
import {ApCreateTypeVO} from 'api/ApCreateTypeVO';
import {RulDataTypeCodeEnum} from 'api/RulDataTypeCodeEnum';
import {RulDescItemTypeExtVO} from 'api/RulDescItemTypeExtVO';
import {RulDataTypeVO} from 'api/RulDataTypeVO';
import { AppState, RefTablesState } from 'typings/store';
import {computeAllowedItemSpecIds, itemValue} from '../../../../utils/ItemInfo';
import {ApItemAccessPointRefVO} from 'api/ApItemAccessPointRefVO';
import {
    FormUnitdate,
    FormTextarea,
    FormText,
    FormRecordRef,
    FormUriRef,
    FormNumber,
    FormCoordinates,
    FormCheckbox,
    FormSpecification,
} from './fields';
import './PartEditForm.scss';

export const ApDescItem:FC<{
    disabled: boolean;
    deleteMode: boolean;
    name: string;
    index: number;
    item?: ApItemVO;
    prevItem?: ApItemVO;
    disableRevision?: boolean;
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>;
    onDeleteItem: (index: number) => void;
    partTypeId: number;
    scopeId: number;
    apTypeId: number;
}> = ({
    name,
    index,
    item,
    disabled,
    deleteMode,
    itemTypeAttributeMap,
    onDeleteItem,
    partTypeId,
    scopeId,
    apTypeId,
    prevItem,
    disableRevision,
}) => {
    const refTables = useSelector(({refTables}:AppState) => refTables);
    return renderItem(name, index, refTables, disabled, deleteMode, onDeleteItem, itemTypeAttributeMap, partTypeId, scopeId, apTypeId, item, prevItem, disableRevision);
}

export const renderItem = (
    name: string,
    index: number,
    refTables: RefTablesState,
    disabled: boolean,
    deleteMode: boolean,
    onDeleteItem: (index: number) => void,
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    partTypeId: number,
    scopeId: number,
    apTypeId: number,
    item?: ApItemVO,
    prevItem?: ApItemVO,
    disableRevision?: boolean,
) => {
    const typeId = item ? item.typeId : prevItem?.typeId as number;
    const itemType = refTables.descItemTypes.itemsMap[typeId] as RulDescItemTypeExtVO;
    if(!itemType){return <>{typeId}</>}
    const dataType = refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO;
    const dataTypeCode = dataType.code;
    const fieldDisabled = disabled || deleteMode;
    // const prevValue = prevItem ? itemValue(prevItem) : '';

    // pro ty, co chtějí jinak renderovat skupinu...,  pokud je true, task se nerenderuje specifikace, ale pouze valueField a v tom musí být již vše...
    let customFieldRender = false;

    const commonFieldProps = {
        name,
        disabled: fieldDisabled,
        label: itemType.shortcut,
        onChange: (a: any) => { console.log("field on change", a);},
        onDelete: () => {onDeleteItem(index);},
        prevItem: prevItem as any,
        disableRevision,
    }

    let valueField: ReactNode;
    switch (dataTypeCode) {
        case RulDataTypeCodeEnum.UNITDATE:
            valueField = <FormUnitdate {...commonFieldProps} />
            break;
        case RulDataTypeCodeEnum.TEXT:
            valueField = <FormTextarea
                {...commonFieldProps}
                limitLength={dataType.textLengthLimitUse || undefined}
            />
            break;
        case RulDataTypeCodeEnum.STRING:
            valueField = <FormText
                {...commonFieldProps}
                limitLength={dataType.textLengthLimitUse || undefined}
            />
            break;
        case RulDataTypeCodeEnum.RECORD_REF:
            customFieldRender = true;

            valueField = <FormRecordRef
                {...commonFieldProps}
                item={item as ApItemAccessPointRefVO}
                itemType={itemType}
                partTypeId={partTypeId}
                scopeId={scopeId}
                itemTypeAttributeMap={itemTypeAttributeMap}
                apTypeId={apTypeId}
                prevItem={prevItem as ApItemAccessPointRefVO}
            />
            break;
        case RulDataTypeCodeEnum.URI_REF:
            valueField = <FormUriRef {...commonFieldProps} />
            break;
        case RulDataTypeCodeEnum.DECIMAL:
        case RulDataTypeCodeEnum.INT:
            valueField = <FormNumber {...commonFieldProps} />
            break;
        case RulDataTypeCodeEnum.COORDINATES:
            valueField = <FormCoordinates {...commonFieldProps} />
            break;
        case RulDataTypeCodeEnum.BIT:
            valueField = <FormCheckbox {...commonFieldProps} />;
            break;
        case RulDataTypeCodeEnum.ENUM:
            //Resime nize
            break;
        default:
            console.warn('Nepodporovaný typ', dataType.code);
            break;
    }

    let valueSpecification: ReactNode;
    if (!customFieldRender && itemType.useSpecification) {
        const useItemSpecIds = computeAllowedItemSpecIds(itemTypeAttributeMap, itemType, item?.specId);
        const getSpecName = (id: number) => {
            return itemType.descItemSpecs.find((spec) => spec.id === id)?.name || "-";
        }

        valueSpecification = (
            <FormSpecification
                {...commonFieldProps}
                label={valueField ? 'Specifikace' : commonFieldProps.label}
                itemType={itemType}
                itemSpecIds={useItemSpecIds}
                getSpecName={getSpecName}
                />
        );
    }

    const cls = classNames('item-value-wrapper', {
        'has-specification': !!valueSpecification,
        'has-value': !!valueField,
    });

    return (
        <Row key={index} className={cls}>
            {valueSpecification && (
                <Col xs={valueSpecification && valueField ? 6 : 12} className={'spcification-wrapper'}>
                    {valueSpecification}
                </Col>
            )}
            {valueField && (
                <Col xs={valueSpecification && valueField ? 6 : 12} className={valueSpecification ? 'pl-1' : ''}>
                    { valueField }
                </Col>
            )}
        </Row>
    );
};
