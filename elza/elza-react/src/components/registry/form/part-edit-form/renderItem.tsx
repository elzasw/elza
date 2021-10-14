import React, { FC, ReactNode } from 'react';
// import { Field, FieldArrayFieldsProps} from 'redux-form';
import { Field } from 'react-final-form';
import { useSelector } from 'react-redux';
import classNames from 'classnames';
import {Icon} from '../../../index';
import {Button, Col, Row} from 'react-bootstrap';
import {ApItemVO} from '../../../../api/ApItemVO';
import {ApCreateTypeVO} from '../../../../api/ApCreateTypeVO';
import {RulDataTypeCodeEnum} from '../../../../api/RulDataTypeCodeEnum';
import {RulDescItemTypeExtVO} from '../../../../api/RulDescItemTypeExtVO';
import {RulDataTypeVO} from '../../../../api/RulDataTypeVO';
import { AppState } from '../../../../typings/store';
import { computeAllowedItemSpecIds } from '../../../../utils/ItemInfo';
import {ApItemAccessPointRefVO} from '../../../../api/ApItemAccessPointRefVO';
import ReduxFormFieldErrorDecorator from '../../../shared/form/ReduxFormFieldErrorDecorator';
import SpecificationField from '../../field/SpecificationField';
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
    disabled: boolean,
    deleteMode: boolean,
    name: string,
    index: number,
    item: ApItemVO,
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    onDeleteItem: (index: number) => void,
    onEdit: (name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) => void,
    onImport: (field: string) => void,
}> = ({
    name,
    index,
    item,
    disabled,
    deleteMode,
    itemTypeAttributeMap,
    onDeleteItem,
    onEdit,
    onImport,
}) => {
    const refTables = useSelector<AppState>(({refTables})=> refTables)
    return renderItem(name, index, item, refTables, disabled, deleteMode, onDeleteItem, onEdit, itemTypeAttributeMap, onImport)
}

export const renderItem = (
    name: string,
    index: number,
    item: ApItemVO,
    refTables: any,
    disabled: boolean,
    deleteMode: boolean,
    onDeleteItem: (index: number) => void,
    onEdit: (name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) => void,
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    onImport: (field: string) => void,
) => {
    // const item = fields.fields.value[index];
    const itemType = refTables.descItemTypes.itemsMap[item.typeId] as RulDescItemTypeExtVO;
    const dataType = refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO;
    const dataTypeCode = dataType.code;
    const fieldDisabled = disabled || deleteMode;

    // pro ty, co chtějí jinak renderovat skupinu...,  pokud je true, task se nerenderuje specifikace, ale pouze valueField a v tom musí být již vše...
    let customFieldRender = false;

    const commonFieldProps = {
        name,
        disabled: fieldDisabled,
        label: itemType.shortcut,
        onChange: (a: any) => { console.log("field on change", a);}
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
                dataType={dataType}
                onEdit={onEdit}
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
            valueField = <FormCoordinates {...commonFieldProps} onImport={onImport} />
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
        const useItemSpecIds = computeAllowedItemSpecIds(itemTypeAttributeMap, itemType, item.specId);

        valueSpecification = (
            <FormSpecification
                name={name}
                label={valueField ? 'Specifikace' : itemType.shortcut}
                itemType={itemType}
                itemSpecIds={useItemSpecIds}
                disabled={fieldDisabled}
                />
        );
    }

    const deleteAction = deleteMode ?
        <Button 
            className={'item-delete-action'} 
            onClick={() => onDeleteItem(index)} 
            variant={'action' as any}
        >
            <Icon glyph={'fa-trash'} />
        </Button> 
        : undefined;

    const cls = classNames('item-value-wrapper', {
        'has-specification': !!valueSpecification,
        'has-value': !!valueField,
    });

    return (
        <Row key={index} className={cls}>
            {valueSpecification && (
                <Col xs={valueSpecification && valueField ? 6 : 12} className={'spcification-wrapper'}>
                    {valueSpecification}
                    {!valueField && deleteAction}
                </Col>
            )}
            {valueField && (
                <Col xs={valueSpecification && valueField ? 6 : 12} className={valueSpecification ? 'pl-1' : ''}>
                    { valueField }
                    { deleteAction }
                </Col>
            )}
        </Row>
    );
};
