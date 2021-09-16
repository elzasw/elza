import React, {FC} from 'react';
import { Field, FieldArrayFieldsProps} from 'redux-form';
import './PartEditForm.scss';
import classNames from 'classnames';
import {ApPartFormVO} from '../../../../api/ApPartFormVO';
import {Icon} from '../../../index';
import {Button, Col, Form, Row} from 'react-bootstrap';
import {ApItemVO} from '../../../../api/ApItemVO';
import {ApCreateTypeVO} from '../../../../api/ApCreateTypeVO';
import {RulDataTypeCodeEnum} from '../../../../api/RulDataTypeCodeEnum';
import {RulDescItemTypeExtVO} from '../../../../api/RulDescItemTypeExtVO';
import {RulDataTypeVO} from '../../../../api/RulDataTypeVO';
import {
    computeAllowedItemSpecIds,
} from '../../../../utils/ItemInfo';
import {ApItemAccessPointRefVO} from '../../../../api/ApItemAccessPointRefVO';
import ReduxFormFieldErrorDecorator from '../../../shared/form/ReduxFormFieldErrorDecorator';
import UnitdateField from '../../field/UnitdateField';
import SpecificationField from '../../field/SpecificationField';
import FormInput from '../../../shared/form/FormInput';
import {objectById} from '../../../../shared/utils';
import { RulDescItemSpecExtVO } from 'api/RulDescItemSpecExtVO';

const FormUnitdate:FC<{
    name: string;
    label: string;
    disabled?: boolean;
}> = ({
    name,
    label,
    disabled = false,
}) => {
    return <Field
        name={`${name}.value`}
        label={label}
        disabled={disabled}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={UnitdateField}
        />
}

const FormTextarea:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    limitLength?: boolean;
}> = ({
    name,
    label,
    disabled = false,
    limitLength,
}) => {
    return <Field
        name={`${name}.value`}
        label={label}
        disabled={disabled}
        maxLength={limitLength}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
        type={'textarea'}
        />
}

const FormText:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    limitLength?: boolean;
}> = ({
    name,
    label,
    disabled = false,
    limitLength,
}) => {
    return <Field
        name={`${name}.value`}
        label={label}
        disabled={disabled}
        maxLength={limitLength}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
        />
}

const getDisplayValue = (apItem: ApItemAccessPointRefVO, itemType: RulDescItemTypeExtVO) => {
    const apItemName = apItem.accessPoint?.name;

    if (itemType.useSpecification && apItem.specId) {
        const spec:RulDescItemSpecExtVO | null = objectById(itemType.descItemSpecs, apItem.specId) || null;
        return `${spec?.shortcut}: ${ apItemName }`
    } 

    return apItemName;
}

const FormRecordRef:FC<{
    name: string;
    label: string;
    disabled: boolean;
    item: ApItemAccessPointRefVO;
    itemType: RulDescItemTypeExtVO;
    dataType: RulDataTypeVO;
    onEdit: (name: string, dataTypeCode: string, item: ApItemAccessPointRefVO) => void;
}> = ({
    name,
    label,
    disabled,
    item,
    itemType,
    dataType,
    onEdit,
}) => {
    return (
        <Row className={'d-flex'}>
            <Col>
                <Form.Label>{label}</Form.Label>
                <Form.Control value={getDisplayValue(item, itemType)} disabled={true} />
            </Col>
            <Col xs="auto" className="action-buttons">
                <Button
                    disabled={disabled}
                    variant={'action' as any}
                    onClick={() => onEdit(name, dataType.code, item)}
                >
                    <Icon glyph="fa-edit" />
                </Button>
            </Col>
        </Row>
    );
}

const FormUriRef:FC<{
    name: string;
    label: string;
    disabled?: boolean;
}> = ({
    name,
    label,
    disabled = false,
}) => {
    return <Row>
        <Col xs={6}>
            <Field
                name={`${name}.value`}
                label={label}
                disabled={disabled}
                maxLength={1000}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={FormInput}
                />
        </Col>
        <Col xs={6}>
            <Field
                name={`${name}.description`}
                label="Název odkazu"
                disabled={disabled}
                maxLength={250}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={FormInput}
                />
        </Col>
    </Row>
}

const FormNumber:FC<{
    name: string;
    label: string;
    disabled?: boolean;
}> = ({
    name,
    label,
    disabled = false,
}) => {
    return <Field
        name={`${name}.value`}
        label={label}
        disabled={disabled}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
    />
}

const FormCoordinates:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    onImport?: (name: string) => void;
}> = ({
    name,
    label,
    disabled = false,
    onImport = () => console.warn("'onImport' undefined.")
}) => {
    return <Row>
        <Col>
            <Field
                name={`${name}.value`}
                label={label}
                disabled={disabled}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={FormInput}
                as={'textarea'}
                />
        </Col>
        <Col xs="auto" className="action-buttons">
            {/*TODO: az bude na serveru */}
            <Button
                variant={'action' as any}
                className={classNames('side-container-button', 'm-1')}
                title={'Importovat'}
                onClick={() => {
                    onImport(`${name}.value`);
                }}
            >
                <Icon glyph={'fa-download'} />
            </Button>
        </Col>
    </Row>
}

const FormCheckbox:FC<{
    name: string;
    label: string;
    disabled?: boolean;
}> = ({
    name,
    label,
    disabled = false,
}) => {
    return <Field
        name={`${name}.value`}
        label={label}
        disabled={disabled}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
        type={'checkbox'}
    />
}

export const renderItem = (
    name: string,
    index: number,
    fields: FieldArrayFieldsProps<any>,
    refTables: any,
    disabled: boolean,
    deleteMode: boolean,
    onDeleteItem: (index: number) => void,
    onCustomEditItem: (name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) => void,
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    showImportDialog: (field: string) => void,
    apId?: number,
    formData?: ApPartFormVO,
) => {
    const item = fields.get(index) as ApItemVO;
    let itemType = refTables.descItemTypes.itemsMap[item.typeId] as RulDescItemTypeExtVO;
    let dataType = refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO;
    let systemCode = dataType.code;

    const fieldDisabled = disabled || deleteMode;
    // pro ty, co chtějí jinak renderovat skupinu...,  pokud je true, task se nerenderuje specifikace, ale pouze valueField a v tom musí být již vše...
    let customFieldRender = false;

    const commonFieldProps = {
        name,
        disabled: fieldDisabled,
        label: itemType.shortcut,
    }

    let valueField: React.ReactNode;
    switch (systemCode) {
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
            customFieldRender = false;

            valueField = <FormRecordRef
                {...commonFieldProps}
                item={item as ApItemAccessPointRefVO}
                itemType={itemType}
                dataType={dataType}
                onEdit={onCustomEditItem}
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
            valueField = <FormCoordinates {...commonFieldProps} onImport={showImportDialog} />
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

    let valueSpecification;
    if (!customFieldRender && itemType.useSpecification) {
        const useItemSpecIds = computeAllowedItemSpecIds(itemTypeAttributeMap, itemType, item.specId);

        valueSpecification = (
            <Field
                name={`${name}.specId`}
                label={valueField ? 'Specifikace' : itemType.shortcut}
                itemTypeId={itemType.id}
                itemSpecIds={useItemSpecIds}
                disabled={fieldDisabled}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={SpecificationField}
            />
        );
    }

    const deleteAction = true ?
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
