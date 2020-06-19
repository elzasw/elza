import React, {useEffect, useState} from 'react';
import {arrayInsert, Field, FieldArray, FieldArrayFieldsProps, WrappedFieldArrayProps} from 'redux-form';
import {connect} from "react-redux";
import "./PartEditForm.scss";
import {Action} from "redux";
import classNames from "classnames";
import {ThunkDispatch} from "redux-thunk";
import {PartType} from "../../../api/generated/model";
import {CodelistData} from "../../../types";
import {ApPartFormVO} from "../../../api/ApPartFormVO";
import {Icon} from "../../index";
import {Alert, Button, Col, Form, Row} from "react-bootstrap";
import {ApAccessPointCreateVO} from "../../../api/ApAccessPointCreateVO";
import {ApItemVO} from "../../../api/ApItemVO";
import {ApCreateTypeVO} from "../../../api/ApCreateTypeVO";
import {RulDataTypeCodeEnum} from "../../../api/RulDataTypeCodeEnum";
import {RulDescItemTypeExtVO} from "../../../api/RulDescItemTypeExtVO";
import {RulDataTypeVO} from "../../../api/RulDataTypeVO";
import {RulPartTypeVO} from "../../../api/RulPartTypeVO";
import {RequiredType} from "../../../api/RequiredType";
import {Loading} from "../../shared";
import * as ItemInfo from "../../../utils/ItemInfo";
import {
    compareCreateTypes,
    computeAllowedItemSpecIds,
    findItemPlacePosition,
    hasItemValue,
    sortOwnItems
} from "../../../utils/ItemInfo";
import {ApItemBitVO} from "../../../api/ApItemBitVO";
import {ApItemAccessPointRefVO} from "../../../api/ApItemAccessPointRefVO";
import {modalDialogShow} from "../../../actions/global/modalDialog";
import {WebApi} from "../../../actions/WebApi";
import ReduxFormFieldErrorDecorator from "../../shared/form/ReduxFormFieldErrorDecorator";
import UnitdateField from "../field/UnitdateField";
import SpecificationField from "../field/SpecificationField";
import {useDebouncedEffect} from "../../../utils/hooks";

type OwnProps = {
    partType: PartType;
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
    }
}

type Props = {} & OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps>;

const renderItem = (name: string,
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
            valueField = <Field
                name={`${name}.value`}
                label={itemType.shortcut}
                disabled={fieldDisabled}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={UnitdateField}
            />;
            break;
        case RulDataTypeCodeEnum.TEXT:
            valueField = <Field
                name={`${name}.value`}
                label={itemType.shortcut}
                disabled={fieldDisabled}
                maxLength={dataType.textLengthLimitUse || undefined}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={Form.Control}
            />;
            break;
        case RulDataTypeCodeEnum.STRING:
            valueField = <Field
                name={`${name}.value`}
                label={itemType.shortcut}
                disabled={fieldDisabled}
                maxLength={dataType.textLengthLimitUse || undefined}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={Form.Control}
            />;
            break;
        case RulDataTypeCodeEnum.RECORD_REF:
            customFieldRender = true;

            let displayValue;
            if (itemType.useSpecification) {
                //@ts-ignore
                displayValue = item.specId ? `${itemType.descItemSpecs[item.specId].shortcut}: ${item.value}` : item.value;
            } else {
                //@ts-ignore
                displayValue = item.value;
            }
            valueField = <Row className={'d-flex'}>
                <Col style={{flex: 1}}>
                    <Form.Label>
                        {itemType.shortcut}
                    </Form.Label>
                    <Form.Control
                        value={displayValue}
                        disabled={true}
                    />
                </Col>
                <Col>
                    <Button
                        disabled={disabled}
                        onClick={() => onCustomEditItem(name, systemCode, item)}
                    >
                        <Icon glyph={'fa-pen'}/>
                    </Button>
                </Col>
            </Row>;
            break;
        case RulDataTypeCodeEnum.URI_REF:
            valueField = <Row>
                <Col xs={12}>
                    <Field
                        name={`${name}.value`}
                        label={itemType.shortcut}
                        disabled={fieldDisabled}
                        maxLength={1000}
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={Form.Control}
                    />
                </Col>
                <Col xs={12}>
                    <Field
                        name={`${name}.name`}
                        label="Název odkazu"
                        disabled={fieldDisabled}
                        maxLength={250}
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={Form.Control}
                    />
                </Col>
            </Row>;
            break;
        case RulDataTypeCodeEnum.INT:
            valueField =
                <Field
                    name={`${name}.value`}
                    label={itemType.shortcut}
                    disabled={fieldDisabled}
                    component={ReduxFormFieldErrorDecorator}
                    renderComponent={Form.Control}
                    type={'number'}
                />;
            break;
        case RulDataTypeCodeEnum.COORDINATES:
            valueField = <Row>
                <Col xs={23}>
                    <Field
                        name={`${name}.value`}
                        label={itemType.shortcut}
                        disabled={fieldDisabled}
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={Form.Control}
                    />
                </Col>
                <Col xs={1}>
                    {/*TODO: az bude na serveru */}
                    <Button className={classNames("side-container-button", "m-1")} title={"Importovat"}
                            onClick={() => {
                                alert('Neni implementovano');
                                //showImportDialog(`${name}.value`);
                            }}>
                        <Icon glyph={'fa-file-import'}/>
                    </Button>
                </Col>
            </Row>;
            break;
        case RulDataTypeCodeEnum.BIT:
            valueField =
                <Field
                    name={`${name}.value`}
                    label={itemType.shortcut}
                    disabled={fieldDisabled}
                    component={ReduxFormFieldErrorDecorator}
                    renderComponent={Form.Check}
                    type={'switch'}
                />;
            break;
        default:
            console.warn("Nepodporovaný typ", dataType.code);
            break;
    }

    let valueSpecification;
    if (!customFieldRender && itemType.useSpecification) {
        const useItemSpecIds = computeAllowedItemSpecIds(itemTypeAttributeMap, itemType, item.specId);

        valueSpecification = <Field
            name={`${name}.itemSpecId`}
            label={valueField ? "Specifikace" : itemType.shortcut}
            itemTypeId={itemType.id}
            itemSpecIds={useItemSpecIds}
            disabled={fieldDisabled}
            component={ReduxFormFieldErrorDecorator}
            renderComponent={SpecificationField}
        />;
    }

    let deleteAction;
    if (deleteMode) {
        deleteAction = <Button
            className="item-delete-action absolute-right"
            onClick={() => onDeleteItem(index)}
        >
            <Icon glyph={'fa-trash'}/>
        </Button>;
    }

    const cls = classNames("item-value-wrapper", {
        "has-specification": !!valueSpecification,
        "has-value": !!valueField,
    });

    return <Row
        key={index}
        className={cls}
    >
        {valueSpecification && <Col xs={valueSpecification && valueField ? 6 : 12} className={"spcification-wrapper"}>
            {valueSpecification}
            {!valueField && deleteMode && <div className="deleted-mode-item-gradient">
                <div>&nbsp;</div>
            </div>}
            {!valueField && deleteAction}
        </Col>}
        {valueField &&
        <Col xs={valueSpecification && valueField ? 6 : 12} className={valueSpecification ? "pl-1" : ""}>
            {valueField}
            {deleteMode && <div className="deleted-mode-item-gradient">
                <div>&nbsp;</div>
            </div>}
            {deleteMode && deleteAction}
        </Col>}
    </Row>
};

const renderItems = (props: WrappedFieldArrayProps & {
    disabled: boolean,
    refTables: any,
    partType: PartType,
    deleteMode: boolean,
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    onCustomEditItem: (name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) => void,
    showImportDialog: (field: string) => void,
    formData?: ApPartFormVO,
    apId?: number,
}): any => {
    const {refTables, partType, disabled, deleteMode, fields, onCustomEditItem, itemTypeAttributeMap, formData, apId, showImportDialog} = props;

    const items = fields.getAll();
    if (!items) {
        return <div/>;
    }
    let index = 0;

    let result: any = [];

    const handleDeleteItem = (index: number) => {
        fields.remove(index);
    };

    while (index < items.length) {
        let index2 = index + 1;
        while (index2 < items.length && items[index].itemTypeId === items[index2].itemTypeId) {
            index2++;
        }

        let itemInfo;
        if (refTables.partTypes.itemsMap[partType]) {
            itemInfo = refTables.partTypes.itemsMap[partType][items[index].itemTypeId] as RulPartTypeVO;
        }
        let width = itemInfo ? itemInfo.width : 2;

        let sameItems = items.slice(index, index2);
        // eslint-disable-next-line
        const inputs = sameItems.map((item, i) => {
            let name = `items[${i + index}]`;
            return renderItem(name, i + index, fields, refTables, disabled, deleteMode, handleDeleteItem, onCustomEditItem, itemTypeAttributeMap, showImportDialog, apId, formData);
        });

        result.push(<Col key={index} xs={width <= 0 ? 12 : width} className="item-wrapper">
            {inputs}
        </Col>);

        index = index2;
    }

    return result;
};

const renderAddActions = ({attributes, formData, refTables, partType, fields, handleAddItems}: WrappedFieldArrayProps & {
    attributes: Array<ApCreateTypeVO>,
    formData?: ApPartFormVO,
    refTables: any,
    partType: PartType,
    handleAddItems: (
        attributes: Array<ApCreateTypeVO>,
        codelist: CodelistData,
        formItems: Array<ApItemVO>,
        partType: PartType,
        arrayInsert: (index: number, value: any) => void,
        userAction: boolean
    ) => void
}): any => {
    const existingItemTypeIds: Record<number, boolean> = {};
    formData && formData.items.forEach(i => {
        existingItemTypeIds[i.typeId] = true;
    });

    return attributes
        .filter(attr => attr.repeatable || !existingItemTypeIds[attr.itemTypeId])
        .map((attr, index) => {
            const itemType = refTables.descItemTypes.itemsMap[attr.itemTypeId] as RulDescItemTypeExtVO;
            return <Button
                key={index}
                variant={'link'}
                title={itemType.name}
                style={{paddingLeft: 0}}
                onClick={() => {
                    handleAddItems([attr], refTables, formData ? formData.items : [], partType, fields.insert, true);
                }}
            >
                <Icon className="mr-1" glyph={'fa-plus'}/>
                {itemType.shortcut}
            </Button>
        });
};

const PartEditForm = ({
                          refTables,
                          partType,
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
                      }: Props) => {

    const [deleteMode, setDeleteMode] = useState(false);
    const [lastAttributesFetch, setLastAttributesFetch] = useState({id: 0});
    const [editErrors, setEditErrors] = useState<Array<string> | undefined>(undefined);
    const [availAttributes, setAvailAttributes] = useState<ApCreateTypeVO[] | undefined>();

    const fetchAttributes = (formData: ApPartFormVO, partId?: number, parentPartId?: number) => {
        const form: ApAccessPointCreateVO = {
            typeId: apTypeId,
            partForm: {
                ...formData,
                parentPartId,
                items: [
                    ...formData.items.filter(hasItemValue),
                ],
                partId: partId,
            },
            accessPointId: apId,
            scopeId: scopeId,
        };

        lastAttributesFetch.id++;
        const fetchId = lastAttributesFetch.id;

        WebApi.getAvailableItems(form)
            .then(attributesInfo => {
                if (fetchId === lastAttributesFetch.id) {
                    //const itemTypeInfoMap = refTables.partTypes.itemsMap[partType] || {};

                    // Seřazení dat
                    let attrs = attributesInfo.attributes;
                    attrs.sort((a, b) => {
                        return compareCreateTypes(a, b, partType, refTables);
                    });

                    setAvailAttributes(attrs);
                    setEditErrors(attributesInfo.errors);

                    // Přidání povinných atributů, pokud ještě ve formuláři nejsou
                    const existingItemTypeIds = formData.items
                        .map(i => i.typeId);
                    const addAtttrs = attrs
                        .filter(attr => {
                            if (attr.requiredType === RequiredType.REQUIRED) {
                                return existingItemTypeIds.indexOf(attr.itemTypeId) < 0;
                            } else {
                                return false;
                            }
                        });

                    handleAddItems(addAtttrs, refTables, formData.items || [], partType, customArrayInsert(), false);
                }
            });
    };

    // eslint-disable-next-line
    useDebouncedEffect(() => {
        if (formData && !deleteMode) {
            fetchAttributes(formData, partId, parentPartId);
        }
    }, 5000, [formData], !availAttributes);

    useEffect(() => {
        setAvailAttributes(undefined);
    }, []);

    useEffect(() => {
        if (!deleteMode && formData) {
            fetchAttributes(formData, partId, parentPartId);
        }
    }, [deleteMode]);

    if (!refTables) {
        return <div/>;
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
        return <>
            <ul className="m-0">{errors.map((value) => <li>{value}</li>)}</ul>
        </>
    };

    if (!availAttributes) {
        return <Loading/>
    }

    return <div>
        {editErrors && editErrors.length > 0 &&
        <Row key="validationAlert" className="mb-3">
            <Alert variant={"warning"}>
                <h3>Chyby validace formuláře.</h3>
                {renderValidationErrors(editErrors)}
            </Alert>
        </Row>
        }
        <Row key="actions" className="mb-3 d-flex justify-content-between">
            <Col style={{flex: 1}}>
                {!deleteMode && availAttributes && formData && <FieldArray
                    key="action-items"
                    name="items"
                    component={renderAddActions}
                    partType={partType}
                    attributes={availAttributes}
                    formData={formData}
                    refTables={refTables}
                    handleAddItems={handleAddItems}
                />}
            </Col>
            <Col>
                <Button disabled={disabled} onClick={() => handleDeleteMode()}>
                    {deleteMode ? "Ukončit režim odstraňování položek formuláře" : "Odstranit položky formuláře"}
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
                partType={partType}
                deleteMode={deleteMode}
                onCustomEditItem={(name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) => onCustomEditItem(name, systemCode, item, refTables, itemTypeAttributeMap)}
                formData={formData}
                apId={apId}
                showImportDialog={showImportDialog}
            />
        </Row>
    </div>
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>, props: OwnProps) => ({
    customArrayInsert: () => {
        let field = props.formInfo.sectionName ? `${props.formInfo.sectionName}.items` : "items";
        return (index: number, value: any) => {
            dispatch(arrayInsert(props.formInfo.formName, field, index, value));
        }
    },
    handleAddItems: (
        attributes: Array<ApCreateTypeVO>,
        refTables: any,
        formItems: ApItemVO[],
        partType: PartType,
        arrayInsert: (index: number, value: any) => void,
        userAction: boolean
    ) => {
        const newItems = attributes.map(attribute => {
            const itemType = refTables.descItemTypes.itemsMap[attribute.itemTypeId] as RulDescItemTypeExtVO;
            const dataType = refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO;

            const item: ApItemVO = {
                typeId: attribute.itemTypeId,
                "@class": ItemInfo.getItemClass(dataType.code),
                position: itemType.viewOrder,
            };

            // Implicitní hodnoty
            switch (dataType.code) {
                case RulDataTypeCodeEnum.BIT:
                    (item as unknown as ApItemBitVO).value = false;
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
        sortOwnItems(partType, newItems, refTables);
        newItems
            .reverse()
            .forEach(item => {
                let index = findItemPlacePosition(item, formItems, partType, refTables);
                arrayInsert(index, item);
            });
    },
    onCustomEditItem: (
        name: string,
        systemCode: RulDataTypeCodeEnum,
        item: ApItemVO,
        refTables: any,
        itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    ) => {
        const initialValues: any = {
            onlyMainPart: false,
            //area: Area.ALLNAMES,
            itemSpecId: item.specId,
        };

        if ((item as unknown as ApItemAccessPointRefVO).value != null) {
            initialValues.codeObj = {
                id: (item as unknown as ApItemAccessPointRefVO).value,
                //@ts-ignore
                name: item.value,
            }
        }

        dispatch(
            modalDialogShow(
                this,
                refTables.descItemTypes.itemsMap[item.typeId].shortcut,
                <div/>
                // <RelationPartItemEditModalForm
                //     initialValues={initialValues}
                //     itemTypeAttributeMap={itemTypeAttributeMap}
                //     itemTypeId={item.typeId}
                //     // onSubmit={(form: RelationPartItemClientVO) => {
                //     //     let field = props.formInfo.sectionName ? `${props.formInfo.sectionName}.${name}` : name;
                //     //     const fieldValue: any = {
                //     //         ...item,
                //     //         textValue: form.codeObj ? form.codeObj.name : "",
                //     //         itemSpecId: form.itemSpecId,
                //     //         value: form.codeObj ? form.codeObj.id : null
                //     //     };
                //     //     dispatch(change(props.formInfo.formName, field, fieldValue));
                //     // }}
                // />
            )
            // ModalActions.showForm(RelationPartItemEditModalForm, {
            //     initialValues,
            //     itemTypeAttributeMap,
            //     itemTypeId: item.itemTypeId,
            //     onSubmit: (form: RelationPartItemClientVO) => {
            //         let field = props.formInfo.sectionName ? `${props.formInfo.sectionName}.${name}` : name;
            //         const fieldValue: any = {
            //             ...item,
            //             textValue: form.codeObj ? form.codeObj.name : "",
            //             itemSpecId: form.itemSpecId,
            //             value: form.codeObj ? form.codeObj.id : null
            //         };
            //         dispatch(change(props.formInfo.formName, field, fieldValue));
            //     }
            // }, {
            //     title: codelist.itemTypesMap[item.itemTypeId].shortcut,
            //     width: "800px"
            // })
        );
    },
    showImportDialog: (field: string) => {
    }
    //todo: az bude na serveru

    // dispatch(
    //     modalDialogShow(
    //         this,
    //         'Importovat souřadnice',
    //         <ImportCoordinateModal
    //             onSubmit={async (formData: any) => {
    //                 const reader = new FileReader();
    //                 reader.onload = async () => {
    //                     const data = reader.result;
    //                     try {
    //                         const fieldValue = await EntitiesClientApiCall.standardApi.importCoordinates(formData.type, data).then(x => x.data);
    //                         dispatch(change(props.formInfo.formName, field, fieldValue));
    //                     } catch (e) {
    //                         notification.error({message: "Nepodařilo se importovat souřadnice"});
    //                     }
    //                 };
    //                 reader.readAsBinaryString(formData.fileList[0].originFileObj);
    //             }}
    //         />
    //     )
    // ),
});

const mapStateToProps = (state: any) => {
    return {
        refTables: state.refTables,
    }
};

export default connect(mapStateToProps, mapDispatchToProps)(PartEditForm);
