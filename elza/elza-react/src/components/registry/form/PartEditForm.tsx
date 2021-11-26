import React, {useEffect, useState} from 'react';
import {arrayInsert, change, Field, FieldArray, FieldArrayFieldsProps, WrappedFieldArrayProps} from 'redux-form';
import {connect} from 'react-redux';
import './PartEditForm.scss';
import {Action} from 'redux';
import classNames from 'classnames';
import {ThunkDispatch} from 'redux-thunk';
import {ApPartFormVO} from '../../../api/ApPartFormVO';
import {Icon} from '../../index';
import {Alert, Button, Col, Form, Row} from 'react-bootstrap';
import {ApAccessPointCreateVO} from '../../../api/ApAccessPointCreateVO';
import {ApItemVO} from '../../../api/ApItemVO';
import {ApCreateTypeVO} from '../../../api/ApCreateTypeVO';
import {RulDataTypeCodeEnum} from '../../../api/RulDataTypeCodeEnum';
import {RulDescItemTypeExtVO} from '../../../api/RulDescItemTypeExtVO';
import {RulDataTypeVO} from '../../../api/RulDataTypeVO';
import {RulPartTypeVO} from '../../../api/RulPartTypeVO';
import {RequiredType} from '../../../api/RequiredType';
import {Loading} from '../../shared';
import * as ItemInfo from '../../../utils/ItemInfo';
import {
    compareCreateTypes,
    computeAllowedItemSpecIds,
    findItemPlacePosition,
    hasItemValue,
    sortOwnItems,
} from '../../../utils/ItemInfo';
import {ApItemBitVO} from '../../../api/ApItemBitVO';
import {ApItemAccessPointRefVO} from '../../../api/ApItemAccessPointRefVO';
import {modalDialogHide, modalDialogShow} from '../../../actions/global/modalDialog';
import {WebApi} from '../../../actions/WebApi';
import ReduxFormFieldErrorDecorator from '../../shared/form/ReduxFormFieldErrorDecorator';
import UnitdateField from '../field/UnitdateField';
import SpecificationField from '../field/SpecificationField';
import {useDebouncedEffect} from '../../../utils/hooks';
import FormInput from '../../shared/form/FormInput';
import {Area} from '../../../api/Area';
import RelationPartItemEditModalForm from '../modal/RelationPartItemEditModalForm';
import {objectById} from '../../../shared/utils';
import {ApViewSettingRule, ApViewSettings, ItemType} from '../../../api/ApViewSettings';
import storeFromArea from '../../../shared/utils/storeFromArea';
import {AP_VIEW_SETTINGS} from '../../../constants';
import {DetailStoreState} from '../../../types';
import ImportCoordinateModal from '../Detail/coordinate/ImportCoordinateModal';
import i18n from '../../i18n';

type OwnProps = {
    partTypeId: number;
    apTypeId: number;
    scopeId: number;
    formData?: ApPartFormVO;
    submitting: boolean;
    parentPartId?: number;
    apId?: number;
    partId?: number;
    formInfo: {
        formName: string;
        sectionName: string;
    };
};

type Props = {} & OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps>;

const renderItem = (
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

    let valueField;
    switch (systemCode) {
        case RulDataTypeCodeEnum.UNITDATE:
            valueField = (
                <Field
                    name={`${name}.value`}
                    label={itemType.shortcut}
                    disabled={fieldDisabled}
                    component={ReduxFormFieldErrorDecorator}
                    renderComponent={UnitdateField}
                />
            );
            break;
        case RulDataTypeCodeEnum.TEXT:
            valueField = (
                <Field
                    name={`${name}.value`}
                    label={itemType.shortcut}
                    disabled={fieldDisabled}
                    maxLength={dataType.textLengthLimitUse || undefined}
                    component={ReduxFormFieldErrorDecorator}
                    renderComponent={FormInput}
                    type={'textarea'}
                />
            );
            break;
        case RulDataTypeCodeEnum.STRING:
            valueField = (
                <Field
                    name={`${name}.value`}
                    label={itemType.shortcut}
                    disabled={fieldDisabled}
                    maxLength={dataType.textLengthLimitUse || undefined}
                    component={ReduxFormFieldErrorDecorator}
                    renderComponent={FormInput}
                />
            );
            break;
        case RulDataTypeCodeEnum.RECORD_REF:
            customFieldRender = true;

            let displayValue;
            const apItem = item as ApItemAccessPointRefVO;
            if (itemType.useSpecification) {
                displayValue = apItem.specId
                    ? `${objectById(itemType.descItemSpecs, apItem.specId).shortcut}: ${
                          apItem.accessPoint && apItem.accessPoint.name
                      }`
                    : apItem.accessPoint && apItem.accessPoint.name;
            } else {
                displayValue = apItem.accessPoint && apItem.accessPoint.name;
            }
            valueField = (
                <Row className={'d-flex'}>
                    <Col>
                        <Form.Label>{itemType.shortcut}</Form.Label>
                        <Form.Control value={displayValue} disabled={true} />
                    </Col>
                    <Col xs="auto" className="action-buttons">
                        <Button
                            disabled={disabled}
                            variant={'action' as any}
                            onClick={() => onCustomEditItem(name, systemCode, item)}
                        >
                            <Icon glyph="fa-edit" />
                        </Button>
                    </Col>
                </Row>
            );
            break;
        case RulDataTypeCodeEnum.URI_REF:
            valueField = (
                <Row>
                    <Col xs={6}>
                        <Field
                            name={`${name}.value`}
                            label={itemType.shortcut}
                            disabled={fieldDisabled}
                            maxLength={1000}
                            component={ReduxFormFieldErrorDecorator}
                            renderComponent={FormInput}
                        />
                    </Col>
                    <Col xs={6}>
                        <Field
                            name={`${name}.description`}
                            label="Název odkazu"
                            disabled={fieldDisabled}
                            maxLength={250}
                            component={ReduxFormFieldErrorDecorator}
                            renderComponent={FormInput}
                        />
                    </Col>
                </Row>
            );
            break;
        case RulDataTypeCodeEnum.DECIMAL:
        case RulDataTypeCodeEnum.INT:
            valueField = (
                <Field
                    name={`${name}.value`}
                    label={itemType.shortcut}
                    disabled={fieldDisabled}
                    component={ReduxFormFieldErrorDecorator}
                    renderComponent={FormInput}
                />
            );
            break;
        case RulDataTypeCodeEnum.COORDINATES:
            valueField = (
                <Row>
                    <Col>
                        <Field
                            name={`${name}.value`}
                            label={itemType.shortcut}
                            disabled={fieldDisabled}
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
                                showImportDialog(`${name}.value`);
                            }}
                        >
                            <Icon glyph={'fa-file'} />
                        </Button>
                    </Col>
                </Row>
            );
            break;
        case RulDataTypeCodeEnum.BIT:
            valueField = (
                <Field
                    name={`${name}.value`}
                    label={itemType.shortcut}
                    disabled={fieldDisabled}
                    component={ReduxFormFieldErrorDecorator}
                    renderComponent={FormInput}
                    type={'checkbox'}
                />
            );
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

    let deleteAction;
    if (deleteMode) {
        deleteAction = (
            <Button className={'item-delete-action'} onClick={() => onDeleteItem(index)} variant={'action' as any}>
                <Icon glyph={'fa-trash'} />
            </Button>
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
                    {!valueField && deleteAction}
                </Col>
            )}
            {valueField && (
                <Col xs={valueSpecification && valueField ? 6 : 12} className={valueSpecification ? 'pl-1' : ''}>
                    {valueField}
                    {deleteMode && deleteAction}
                </Col>
            )}
        </Row>
    );
};

const renderItems = (
    props: WrappedFieldArrayProps & {
        disabled: boolean;
        refTables: any;
        partTypeId: number;
        deleteMode: boolean;
        itemTypeAttributeMap: Record<number, ApCreateTypeVO>;
        onCustomEditItem: (name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) => void;
        showImportDialog: (field: string) => void;
        formData?: ApPartFormVO;
        apId?: number;
        itemTypeSettings: ItemType[];
        descItemTypesMap: any;
    },
): any => {
    const {
        refTables,
        partTypeId,
        disabled,
        deleteMode,
        fields,
        onCustomEditItem,
        itemTypeAttributeMap,
        formData,
        apId,
        showImportDialog,
        itemTypeSettings,
        descItemTypesMap,
    } = props;

    const items = fields.getAll() as ApItemVO[];
    if (!items) {
        return <div />;
    }
    let index = 0;

    let result: any = [];

    const handleDeleteItem = (index: number) => {
        fields.remove(index);
    };

    while (index < items.length) {
        let index2 = index + 1;
        while (index2 < items.length && items[index].typeId === items[index2].typeId) {
            index2++;
        }

        const itemTypeExt: RulDescItemTypeExtVO = descItemTypesMap[items[index].typeId];
        let width = 2; // default
        if (itemTypeExt) {
            const itemType: ItemType = objectById(itemTypeSettings, itemTypeExt.code, 'code');
            if (itemType && itemType.width) {
                width = itemType.width;
            }
        }

        let sameItems = items.slice(index, index2);
        // eslint-disable-next-line
        const inputs = sameItems.map((item, i) => {
            let name = `items[${i + index}]`;
            return renderItem(
                name,
                i + index,
                fields,
                refTables,
                disabled,
                deleteMode,
                handleDeleteItem,
                onCustomEditItem,
                itemTypeAttributeMap,
                showImportDialog,
                apId,
                formData,
            );
        });

        result.push(
            <Col key={index} xs={width <= 0 ? 12 : width} className="item-wrapper">
                {inputs}
            </Col>,
        );

        index = index2;
    }

    return result;
};

const renderAddActions = ({
    attributes,
    formData,
    refTables,
    partTypeId,
    fields,
    handleAddItems,
    descItemTypesMap,
    apViewSettings,
}: WrappedFieldArrayProps & {
    attributes: Array<ApCreateTypeVO>;
    formData?: ApPartFormVO;
    refTables: any;
    partTypeId: number;
    descItemTypesMap: Record<number, RulDescItemTypeExtVO>;
    apViewSettings: ApViewSettingRule;
    handleAddItems: (
        attributes: Array<ApCreateTypeVO>,
        refTables: any,
        formItems: Array<ApItemVO>,
        partTypeId: number,
        arrayInsert: (index: number, value: any) => void,
        userAction: boolean,
        descItemTypesMap: Record<number, RulDescItemTypeExtVO>,
        apViewSettings: ApViewSettingRule,
    ) => void;
}): any => {
    const existingItemTypeIds: Record<number, boolean> = {};
    formData &&
        formData.items.forEach(i => {
            existingItemTypeIds[i.typeId] = true;
        });

    return attributes
        .filter(attr => attr.repeatable || !existingItemTypeIds[attr.itemTypeId])
        .map((attr, index) => {
            const itemType = refTables.descItemTypes.itemsMap[attr.itemTypeId] as RulDescItemTypeExtVO;
            return (
                <Button
                    key={index}
                    variant={'link'}
                    title={itemType.name}
                    style={{paddingLeft: 0, color: '#000'}}
                    onClick={() => {
                        handleAddItems(
                            [attr],
                            refTables,
                            formData ? formData.items : [],
                            partTypeId,
                            fields.insert,
                            true,
                            descItemTypesMap,
                            apViewSettings,
                        );
                    }}
                >
                    <Icon className="mr-1" glyph={'fa-plus'} />
                    {itemType.shortcut}
                </Button>
            );
        });
};

const PartEditForm = ({
    refTables,
    partTypeId,
    apTypeId,
    formData,
    submitting,
    handleAddItems,
    onCustomEditItem,
    customArrayInsert,
    parentPartId,
    partId,
    apId,
    scopeId,
    showImportDialog,
    apViewSettings,
    descItemTypesMap,
}: Props) => {
    const [deleteMode, setDeleteMode] = useState<boolean>(false);
    const [lastAttributesFetch, setLastAttributesFetch] = useState({id: 0});
    const [editErrors, setEditErrors] = useState<Array<string> | undefined>(undefined);
    const [availAttributes, setAvailAttributes] = useState<ApCreateTypeVO[] | undefined>();

    const apViewSettingRule = apViewSettings.data!.rules[apViewSettings.data!.typeRuleSetMap[apTypeId]];

    const fetchAttributes = (formData: ApPartFormVO, partId?: number, parentPartId?: number) => {
        const form: ApAccessPointCreateVO = {
            typeId: apTypeId,
            partForm: {
                ...formData,
                parentPartId,
                items: [...formData.items.filter(hasItemValue)],
                partId: partId,
            },
            accessPointId: apId,
            scopeId: scopeId,
        };

        lastAttributesFetch.id++;
        const fetchId = lastAttributesFetch.id;

        WebApi.getAvailableItems(form).then(attributesInfo => {
            if (fetchId === lastAttributesFetch.id) {
                //const itemTypeInfoMap = refTables.partTypes.itemsMap[partType] || {};

                // Seřazení dat
                let attrs = attributesInfo.attributes;
                attrs.sort((a, b) => {
                    return compareCreateTypes(a, b, partTypeId, refTables, descItemTypesMap, apViewSettingRule);
                });

                setAvailAttributes(attrs);
                setEditErrors(attributesInfo.errors);
                setLastAttributesFetch({id: fetchId});

                // Přidání povinných atributů, pokud ještě ve formuláři nejsou
                const existingItemTypeIds = formData.items.map(i => i.typeId);
                const addAtttrs = attrs.filter(attr => {
                    if (attr.requiredType === RequiredType.REQUIRED) {
                        return existingItemTypeIds.indexOf(attr.itemTypeId) < 0;
                    } else {
                        return false;
                    }
                });

                handleAddItems(
                    addAtttrs,
                    refTables,
                    formData.items || [],
                    partTypeId,
                    customArrayInsert(),
                    false,
                    descItemTypesMap,
                    apViewSettings,
                );
            }
        });
    };

    // eslint-disable-next-line
    useDebouncedEffect(
        () => {
            if (formData && !deleteMode) {
                fetchAttributes(formData, partId, parentPartId);
            }
        },
        5000,
        [formData],
        !availAttributes,
    );

    useEffect(() => {
        setAvailAttributes(undefined);
    }, []);

    useEffect(() => {
        if (!deleteMode && formData) {
            fetchAttributes(formData, partId, parentPartId);
        }
    }, [deleteMode]);

    if (!refTables) {
        return <div />;
    }

    const handleDeleteMode = () => {
        deleteMode && setAvailAttributes(undefined);
        setDeleteMode(!deleteMode);
    };

    const disabled = submitting || !availAttributes;

    const itemTypeAttributeMap: Record<number, ApCreateTypeVO> = {};
    if (availAttributes) {
        availAttributes.forEach((attribute: ApCreateTypeVO) => {
            itemTypeAttributeMap[attribute.itemTypeId] = attribute;
        });
    }

    const renderValidationErrors = (errors: Array<string>) => {
        return (
            <>
                <ul>
                    {errors.map(value => (
                        <li>{value}</li>
                    ))}
                </ul>
            </>
        );
    };

    if (!availAttributes) {
        return <Loading />;
    }

    return (
        <div>
            {editErrors && editErrors.length > 0 && (
                <Row key="validationAlert" className="mb-3">
                    <Col className="w-100">
                        <div className="ap-validation-alert">
                            <h3>Chyby validace formuláře.</h3>
                            {renderValidationErrors(editErrors)}
                        </div>
                    </Col>
                </Row>
            )}
            <Row key="actions" className="mb-3 d-flex justify-content-between">
                <Col style={{flex: 1}}>
                    {!deleteMode && availAttributes && formData && (
                        <FieldArray
                            key="action-items"
                            name="items"
                            component={renderAddActions}
                            partTypeId={partTypeId}
                            attributes={availAttributes}
                            formData={formData}
                            refTables={refTables}
                            handleAddItems={handleAddItems}
                            descItemTypesMap={descItemTypesMap}
                            apViewSettings={apViewSettings.data!.rules[apViewSettings.data!.typeRuleSetMap[apTypeId]]}
                        />
                    )}
                </Col>
                <Col xs="auto">
                    <Button disabled={disabled} variant={'outline-dark'} onClick={() => handleDeleteMode()}>
                        {deleteMode ? 'Ukončit režim odstraňování' : 'Odstranit položky formuláře'}
                    </Button>
                </Col>
            </Row>

            {/* todo: gutter={16} */}
            <Row key="inputs" className="part-edit-form d-flex">
                <FieldArray
                    disabled={disabled}
                    name="items"
                    component={renderItems}
                    itemTypeAttributeMap={itemTypeAttributeMap}
                    refTables={refTables}
                    partTypeId={partTypeId}
                    deleteMode={deleteMode}
                    onCustomEditItem={(name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) =>
                        onCustomEditItem(name, systemCode, item, refTables, partTypeId, itemTypeAttributeMap)
                    }
                    formData={formData}
                    apId={apId}
                    showImportDialog={showImportDialog}
                    itemTypeSettings={apViewSettingRule?.itemTypes || []}
                    descItemTypesMap={descItemTypesMap}
                />
            </Row>
        </div>
    );
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>, props: OwnProps) => ({
    customArrayInsert: () => {
        let field = props.formInfo.sectionName ? `${props.formInfo.sectionName}.items` : 'items';
        return (index: number, value: any) => {
            dispatch(arrayInsert(props.formInfo.formName, field, index, value));
        };
    },
    handleAddItems: (
        attributes: Array<ApCreateTypeVO>,
        refTables: any,
        formItems: ApItemVO[],
        partTypeId: number,
        arrayInsert: (index: number, value: any) => void,
        userAction: boolean,
        descItemTypesMap,
        apViewSettings,
    ) => {
        const newItems = attributes.map(attribute => {
            const itemType = refTables.descItemTypes.itemsMap[attribute.itemTypeId] as RulDescItemTypeExtVO;
            const dataType = refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO;

            const item: ApItemVO = {
                typeId: attribute.itemTypeId,
                '@class': ItemInfo.getItemClass(dataType.code),
                position: 1, // TODO: dořešit pozici
            };

            // Implicitní hodnoty
            switch (dataType.code) {
                case RulDataTypeCodeEnum.BIT:
                    ((item as unknown) as ApItemBitVO).value = false;
                    break;
            }

            // Implicitní specifikace - pokud má specifikaci a má právě jednu položku a současně jde o povinnou hodnotu
            // Pokud uživatel přidal ručně i pro nepovinné
            if (itemType.useSpecification && (attribute.requiredType === RequiredType.REQUIRED || userAction)) {
                if (attribute.itemSpecIds && attribute.itemSpecIds.length === 1) {
                    item.specId = attribute.itemSpecIds[0];
                }
            }

            return item;
        });

        // Vložení do formuláře - od konce
        sortOwnItems(partTypeId, newItems, refTables, descItemTypesMap, apViewSettings);
        newItems.reverse().forEach(item => {
            let index = findItemPlacePosition(item, formItems, partTypeId, refTables, descItemTypesMap, apViewSettings);
            arrayInsert(index, item);
        });
    },
    onCustomEditItem: (
        name: string,
        systemCode: RulDataTypeCodeEnum,
        item: ApItemVO,
        refTables: any,
        partTypeId: number,
        itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    ) => {
        const initialValues: any = {
            onlyMainPart: false,
            area: Area.ALLNAMES,
            specId: item.specId,
        };

        if (((item as unknown) as ApItemAccessPointRefVO).value != null) {
            initialValues.codeObj = {
                id: ((item as unknown) as ApItemAccessPointRefVO).value,
                // @ts-ignore
                codeObj: item.accessPoint,
                // @ts-ignore
                name: item.accessPoint && item.accessPoint.name,
                specId: item.specId,
            };
        }

        dispatch(
            modalDialogShow(
                this,
                refTables.descItemTypes.itemsMap[item.typeId].shortcut,
                <RelationPartItemEditModalForm
                    initialValues={initialValues}
                    itemTypeAttributeMap={itemTypeAttributeMap}
                    typeId={item.typeId}
                    apTypeId={props.apTypeId}
                    scopeId={props.scopeId}
                    partTypeId={partTypeId}
                    onSubmit={form => {
                        let field = 'partForm.' + name;
                        const fieldValue: any = {
                            ...item,
                            specId: form.specId ? parseInt(form.specId) : null,
                            accessPoint: {
                                '@class': '.ApAccessPointVO',
                                id: form.codeObj.id,
                                name: form.codeObj.name,
                            },
                            value: form.codeObj ? form.codeObj.id : null,
                        };
                        dispatch(change(props.formInfo.formName, field, fieldValue));
                        dispatch(modalDialogHide());
                    }}
                />,
            ),
        );
    },
    showImportDialog: (field: string) =>
        dispatch(
            modalDialogShow(
                this,
                i18n('ap.coordinate.import.title'),
                <ImportCoordinateModal
                    onSubmit={async formData => {
                        const reader = new FileReader();
                        reader.onload = async () => {
                            const data = reader.result;
                            try {
                                const fieldValue = await WebApi.importApCoordinates(data!, formData.format);
                                let realField = props.formInfo.sectionName
                                    ? `${props.formInfo.sectionName}.${field}`
                                    : field;
                                dispatch(change(props.formInfo.formName, realField, fieldValue));
                            } catch (e) {
                                //notification.error({message: 'Nepodařilo se importovat souřadnice'});
                            }
                        };
                        reader.readAsBinaryString(formData.file);
                    }}
                    onSubmitSuccess={(result, dispatch) => dispatch(modalDialogHide())}
                />,
            ),
        ),
});

const mapStateToProps = (state: any) => {
    return {
        descItemTypesMap: state.refTables.descItemTypes.itemsMap as Record<number, RulDescItemTypeExtVO>,
        apViewSettings: storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
        refTables: state.refTables,
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(PartEditForm);
