import React, {useEffect, useState} from 'react';
import {arrayInsert, change, FieldArray, FieldArrayFieldsProps, WrappedFieldArrayProps} from 'redux-form';
import {connect} from "react-redux";
import "./PartEditForm.scss";
import {Action} from "redux";
import classNames from "classnames";
import {ThunkDispatch} from "redux-thunk";
import RelationPartItemEditModalForm from "../modal/RelationPartItemEditModalForm";
import InterpiItemEditModalForm from '../modal/InterpiItemEditModalForm';
import {PartType, RequiredType, SystemCode} from "../../../api/generated/model";
import {ApPartFormVO} from "../../../api/generated/model/ap-part-form-vo";
import {ApCreateTypeVO} from "../../../api/generated/model/ap-create-type-vo";
import {ApItemVO} from "../../../api/generated/model/ap-item-vo";
import { Field, reduxForm } from 'redux-form'
import {EDIT_AE_DETAIL_AREA, PART_EDIT_FORM_ATTRIBUTES} from "../../../constants";
import {Col, Row} from "react-bootstrap";
import {Button} from "../../ui";
import {Icon} from "../../index";

type OwnProps = {
    partType: PartType;
    aeTypeId: number;
    formData?: ApPartFormVO;
    submitting: boolean;
    parentPartId?: number;
    aeId?: number;
    partId?: number;
    formInfo: {
        formName: string;
        sectionName: string;
    }
}

type Props = {
//  codelist: CodelistData;
} & OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps>;

const renderItem = (name: string,
                    index: number,
                    fields: FieldArrayFieldsProps<any>,
                    //codelist: CodelistData,
                    disabled: boolean,
                    deleteMode: boolean,
                    onDeleteItem: (index: number) => void,
                    onCustomEditItem: (type: CustomEditType, name: string, systemCode: SystemCode, item: ApItemVO) => void,
                    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
                    showImportDialog: (field: string) => void,
                    aeId?: number,
                    formData?: ApPartFormVO,
) => {
    const item = fields.get(index) as ApItemVO;
    let itemType = codelist.itemTypesMap[item.itemTypeId];
    let dataType = codelist.dataTypesMap[itemType.dataTypeId];
    let systemCode = dataType.systemCode;

    const fieldDisabled = disabled || deleteMode;
    let customFieldRender = false;  // pro ty, co chtějí jinak renderovat skupinu...,  pokud je true, task se nerenderuje specifikace, ale pouze valueField a v tom musí být již vše...

    let withInterpiSearch = false;
    if (itemType.itemTypeInfo) {
        for (const itemTypeInfo of itemType.itemTypeInfo) {
            if (itemTypeInfo.interpiItemTypeId && itemTypeInfo.interpiItemSpecId) {
                if (formData) {
                    for (const it of formData.items) {
                        if (it.itemTypeId === itemTypeInfo.interpiItemTypeId && it.itemSpecId === itemTypeInfo.interpiItemSpecId) {
                            withInterpiSearch = true;
                        }
                    }
                }
            }
        }
    }

    let valueField;
    switch (systemCode) {
        case SystemCode.UNITDATE:
            valueField =
                <Field type={'date'} name={`${name}.value`} label={itemType.name} fieldProps={{disabled: fieldDisabled}}/>;
            break;
        case SystemCode.TEXT:
            valueField = <Field
                type={'text'}
                name={`${name}.value`}
                label={itemType.name}
                fieldProps={{
                    disabled: fieldDisabled,
                    maxLength: dataType.textLenghtLimitUse || undefined
                }}
            />;
            break;
        case SystemCode.STRING:
            if (aeId && itemType.itemTypeInfo && itemType.itemTypeInfo.some(value => value.calc)) {
                valueField = <ff.ActionInput name={`${name}.value`} label={itemType.name}
                                             fieldProps={{
                                                 disabled: fieldDisabled,
                                                 maxLength: dataType.textLenghtLimitUse || undefined,
                                                 clickPromise: async () => {
                                                     let formDataSubmit;
                                                     if (formData) {
                                                         formDataSubmit = filterPartFormForSubmit(formData)
                                                     }
                                                     return EntitiesClientApiCall.standardApi
                                                         .fillDescription(aeId, itemType.id, formDataSubmit)
                                                         .then(x => x.data.itemDescription);
                                                 },
                                             }}/>
            } else if (withInterpiSearch) {
                valueField = <Row className={'d-flex'}>
                    <Col style={{flex: 1}}>
                        <Field
                            type={'text'}
                            name={`${name}.value`}
                            label={itemType.name}
                            fieldProps={{
                                disabled: fieldDisabled,
                                maxLength: dataType.textLenghtLimitUse || undefined
                            }}
                        />
                    </Col>
                    <Col>
                        <Button
                            title="Vyhledat v INTERPI"
                            htmlType={"button"}
                            size={"large"}
                            disabled={disabled}
                            onClick={() => onCustomEditItem(CustomEditType.INTERPI, name, systemCode, item)}
                        >
                            <Icon fixedWidth className="icon" glyph={'fa-list-alt'}/>
                        </Button>
                    </Col>
                </Row>;
            } else {
                valueField = <Field
                    type={'text'}
                    name={`${name}.value`}
                    label={itemType.name}
                    fieldProps={{
                        disabled: fieldDisabled,
                        maxLength: dataType.textLenghtLimitUse || undefined
                    }}
                />
            }
            break;
        case SystemCode.RECORDREF:
            customFieldRender = true;

            let displayValue;
            if (itemType.useSpecification) {
                displayValue = item.itemSpecId ? `${codelist.itemSpecsMap[item.itemSpecId].name}: ${item.textValue}` : item.textValue;
            } else {
                displayValue = item.textValue;
            }
            valueField = <Row className={'d-flex'}>
                <Col style={{flex: 1}}>
                    <FloatingInput
                        label={itemType.name}
                        value={displayValue}
                        inputProps={{
                            disabled: true,
                            style: {
                                borderRight: 0
                            }
                        }}
                    />
                </Col>
                <Col>
                    <Button
                        htmlType={"button"}
                        size={"large"}
                        disabled={disabled}
                        onClick={() => onCustomEditItem(CustomEditType.RELATION, name, systemCode, item)}
                    >
                        <Icon glyph={'fa-pen'}/>
                    </Button>
                </Col>
            </Row>;
            break;
        case SystemCode.NULL:
            break;
        case SystemCode.LINK:
            valueField = <Row>
                <Col>
                    <Field
                        type={'text'}
                        name={`${name}.value`}
                        label={itemType.name}
                        fieldProps={{
                            disabled: fieldDisabled,
                            maxLength: 1000
                        }}
                    />
                </Col>
                <Col>
                    <Field
                        type={'text'}
                        name={`${name}.name`}
                        label="Název odkazu"
                        fieldProps={{
                            disabled: fieldDisabled,
                            maxLength: 250
                        }}
                    />
                </Col>
            </Row>;
            break;
        case SystemCode.INTEGER:
            valueField =
                <Field type={'number'} name={`${name}.value`} label={itemType.name} fieldProps={{disabled: fieldDisabled}}/>;
            break;
        case SystemCode.COORDINATES:
            valueField = <Row>
                <Col xs={23}>
                    <Field type={'text'} name={`${name}.value`} label={itemType.name} fieldProps={{disabled: fieldDisabled}}/>
                </Col>
                <Col xs={1}>
                    <Button className={classNames("side-container-button", "m-1")} title={"Importovat"}
                            onClick={() => showImportDialog(`${name}.value`)}
                    >
                        <Icon
                            fixedWidth
                            className={classNames("icon", "fa-flip-horizontal")}
                            glyph={'fa-file-import'}
                        />
                    </Button>
                </Col>
            </Row>;
            break;
        case SystemCode.BIT:
            valueField =
                <ff.SwitchSelect name={`${name}.value`} label={itemType.name} fieldProps={{disabled: fieldDisabled}}/>;
            break;
        default:
            console.warn("Nepodporovaný typ", dataType.systemCode);
            break;
    }

    let valueSpecification;
    if (!customFieldRender && itemType.useSpecification) {
        const useItemSpecIds = computeAllowedItemSpecIds(itemTypeAttributeMap, itemType, item.itemSpecId);

        valueSpecification = <ff.Specification
            name={`${name}.itemSpecId`}
            label={valueField ? "Specifikace" : itemType.name}
            fieldProps={{
                itemTypeId: itemType.id,
                itemSpecIds: useItemSpecIds,
                disabled: fieldDisabled
            }}
        />;
    }

    let deleteAction;
    if (deleteMode) {
        deleteAction = <Button
            className="item-delete-action absolute-right"
            size="large"
            htmlType="button"
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
        {valueSpecification && <Col span={valueSpecification && valueField ? 12 : 24} className="spcification-wrapper">
            {valueSpecification}
            {!valueField && deleteMode && <div className="deleted-mode-item-gradient">
                <div>&nbsp;</div>
            </div>}
            {!valueField && deleteAction}
        </Col>}
        {valueField &&
        <Col span={valueSpecification && valueField ? 12 : 24} className={valueSpecification ? "pl-1" : ""}>
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
    codelist: CodelistData,
    partType: PartType,
    deleteMode: boolean,
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    onCustomEditItem: (type: CustomEditType, name: string, systemCode: SystemCode, item: ApItemVO) => void,
    showImportDialog: (field: string) => void,
    formData?: ApPartFormVO,
    aeId?: number,
}): any => {
    const {codelist, partType, disabled, deleteMode, fields, onCustomEditItem, itemTypeAttributeMap, formData, aeId, showImportDialog} = props;

    const items = fields.getAll();
    if (!items) {
        return <div/>
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
        if (codelist.partItemTypeInfoMap[partType]) {
            itemInfo = codelist.partItemTypeInfoMap[partType][items[index].itemTypeId];
        }
        let width = itemInfo ? itemInfo.width : 2;

        let sameItems = items.slice(index, index2);
        const inputs = sameItems.map((item, i) => {
            let name = `items[${i + index}]`;
            return renderItem(name, i + index, fields, codelist, disabled, deleteMode, handleDeleteItem, onCustomEditItem, itemTypeAttributeMap, showImportDialog, aeId, formData);
        });

        result.push(<Col key={index} span={width <= 0 ? 24 : width * 2} className="item-wrapper">
            {inputs}
        </Col>);

        index = index2;
    }

    return result;
};

const renderAddActions = ({attributes, formData, codelist, partType, fields, handleAddItems}: WrappedFieldArrayProps & {
    attributes: Array<ApCreateTypeVO>,
    formData?: ApPartFormVO,
    codelist: CodelistData,
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
        existingItemTypeIds[i.itemTypeId] = true;
    });

    return attributes
        .filter(attr => attr.repeatable || !existingItemTypeIds[attr.itemTypeId])
        .map((attr, index) => {
            return <Button
                key={index}
                type={"link"}
                htmlType="button"
                style={{paddingLeft: 0}}
                onClick={() => {
                    handleAddItems([attr], codelist, formData ? formData.items : [], partType, fields.insert, true);
                }}
            >
                <Icon className="mr-1" glyph={'fa-plus'}/>
                {codelist.itemTypesMap[attr.itemTypeId].name}
            </Button>
        });
};

const PartEditForm = ({
                          codelist,
                          partType,
                          aeTypeId,
                          formData,
                          submitting,
                          handleAddItems,
                          attributes,
                          editAeDetail,
                          setAttributes,
                          onCustomEditItem,
                          customArrayInsert,
                          parentPartId,
                          partId,
                          aeId,
                          showImportDialog
                      }: Props) => {
    const [deleteMode, setDeleteMode] = useState(false);
    const [lastAttributesFetch, setLastAttributesFetch] = useState({id: 0});

    const [editErrors, setEditErrors] = useState<Array<string> | undefined>(undefined);

    const fetchAttributes = (formData: ApPartFormVO, partId?: number, parentPartId?: number) => {
        const form: ApPartFormVO = {
            aeTypeId,
            partForm: {
                ...formData,
                parentPartId,
                items: [
                    ...formData.items
                        .filter(hasItemValue)
                ],
                partId: partId
            },
            archiveEntityId: aeId,
        };

        lastAttributesFetch.id++;
        const fetchId = lastAttributesFetch.id;

        EntitiesClientApiCall.standardApi
            .getAttributes(form)
            .then(attributesInfo => {
                if (fetchId === lastAttributesFetch.id) {
                    const itemTypeInfoMap = codelist.partItemTypeInfoMap[partType] || {};

                    // Seřazení dat
                    let attrs = attributesInfo.data.attributes;
                    attrs.sort((a, b) => {
                        return compareCreateTypes(a, b, partType, codelist);
                    });

                    setAttributes(attrs);
                    setEditErrors(attributesInfo.data.errors);

                    // Přidání povinných atributů, pokud ještě ve formuláři nejsou
                    const existingItemTypeIds = formData.items
                        .map(i => i.itemTypeId);
                    const addAtttrs = attrs
                        .filter(attr => {
                            if (attr.requiredType === RequiredType.REQUIRED) {
                                return existingItemTypeIds.indexOf(attr.itemTypeId) < 0;
                            } else {
                                return false;
                            }
                        });

                    handleAddItems(addAtttrs, codelist, formData.items || [], partType, customArrayInsert(), false);
                }
            });
    };

    useDebouncedEffect(() => {
        if (formData && !deleteMode) {
            fetchAttributes(formData, partId, parentPartId);
        }
    }, 5000, [formData], !attributes.data);

    useEffect(() => {
        setAttributes(null);
    }, []);

    useEffect(() => {
        if (!deleteMode && formData) {
            fetchAttributes(formData, partId, parentPartId);
        }
    }, [deleteMode]);

    if (!codelist) {
        return <div/>;
    }

    const handleDeleteMode = () => {
        deleteMode && setAttributes(null);
        setDeleteMode(!deleteMode);
    };

    const disabled = submitting || !attributes.data;

    const itemTypeAttributeMap: Record<number, ApCreateTypeVO> = {};
    if (attributes && attributes.data) {
        attributes.data.forEach((attribute: ApCreateTypeVO) => {
            itemTypeAttributeMap[attribute.itemTypeId] = attribute;
        });
    }

    const renderValidationErrors = (errors: Array<string>) => {
        return <>
            <ul className="m-0">{errors.map((value) => <li>{value}</li>)}</ul>
        </>
    };

    return <Spin key="spin" spinning={!attributes.data} size={"large"}>
        {editErrors && editErrors.length > 0 &&
        <Row key="validationAlert" className="mb-3">
            <Alert message="Chyby validace formuláře." type={"warning"}
                   description={renderValidationErrors(editErrors)}/>
        </Row>
        }
        <Row key="actions" type="flex" justify="space-between" className="mb-3">
            <Col style={{flex: 1}}>
                {!deleteMode && attributes.data && formData && <FieldArray
                    key="action-items"
                    name="items"
                    component={renderAddActions}
                    partType={partType}
                    attributes={attributes.data}
                    formData={formData}
                    codelist={codelist}
                    handleAddItems={handleAddItems}
                />}
            </Col>
            <Col>
                <Button disabled={disabled}
                        onClick={() => handleDeleteMode()}>{deleteMode ? "Ukončit režim odstraňování položek formuláře" : "Odstranit položky formuláře"}</Button>
            </Col>
        </Row>

        <Row key="inputs" type="flex" gutter={16} className="part-edit-form">
            <FieldArray
                disabled={disabled}
                name="items"
                component={renderItems}
                itemTypeAttributeMap={itemTypeAttributeMap}
                codelist={codelist}
                partType={partType}
                deleteMode={deleteMode}
                onCustomEditItem={(type: CustomEditType, name: string, systemCode: SystemCode, item: AeItemVO) => onCustomEditItem(type, name, systemCode, item, codelist, itemTypeAttributeMap, editAeDetail)}
                formData={formData}
                aeId={aeId}
                showImportDialog={showImportDialog}
            />
        </Row>
    </Spin>
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
        codelist: CodelistData,
        formItems: Array<ApItemVO>,
        partType: PartType,
        arrayInsert: (index: number, value: any) => void,
        userAction: boolean
    ) => {
        const newItems = attributes.map(attribute => {
            const itemType = codelist.itemTypesMap[attribute.itemTypeId];
            const dataType = codelist.dataTypesMap[itemType.dataTypeId];

            const item: ApItemVO = {
                itemTypeId: attribute.itemTypeId,
                "@class": ItemInfo.getItemClass(dataType.systemCode),
                textValue: ""
            };

            // Implicitní hodnoty
            switch (dataType.systemCode) {
                case SystemCode.BIT:
                    (item as AeItemBitVO).value = false;
                    break;
            }

            // Implicitní specifikace - pokud má specifikaci a má právě jednu položku a současně jde o povinnou hodnotu
            // Pokud uživatel přidal ručně i pro nepovinné
            if (itemType.useSpecification && (attribute.requiredType === RequiredType.REQUIRED || userAction)) {
                if (attribute.itemSpecIds && attribute.itemSpecIds.length === 1) {
                    item.itemSpecId = attribute.itemSpecIds[0];
                }
            }

            return item;
        });

        // Vložení do formuláře - od konce
        sortOwnItems(partType, newItems, codelist);
        newItems
            .reverse()
            .forEach(item => {
                let index = findItemPlacePosition(item, formItems, partType, codelist);
                arrayInsert(index, item);
            });
    },
    setAttributes: (attributes: Array<AeCreateTypeVO> | null) => {
        dispatch(DetailActions.setData(Constants.PART_EDIT_FORM_ATTRIBUTES, "", attributes));
    },
    onCustomEditItem: (
        type: CustomEditType,
        name: string,
        systemCode: SystemCode,
        item: AeItemVO,
        codelist: CodelistData,
        itemTypeAttributeMap: Record<number, AeCreateTypeVO>,
        editAeDetail: any
    ) => {
        switch (type) {
            case CustomEditType.INTERPI: {

                let preferredNameItems: Array<AeItemVO> = [];
                if (editAeDetail.fetched) {
                    const parts: AePartVO[] = editAeDetail.data.content;
                    for (const part of parts) {
                        if (part["@class"] === AePartNameClass) {
                            const partName = (part as AePartNameVO);
                            const preferred = partName.preferred;
                            if (preferred) {
                                preferredNameItems = partName.items;
                            }
                        }
                    }
                }

                dispatch(
                    ModalActions.showForm(InterpiItemEditModalForm, {
                        preferredNameItems,
                        codelist,
                        onSubmit: (form: InterpiItemVO) => {
                            let field = props.formInfo.sectionName ? `${props.formInfo.sectionName}.${name}` : name;
                            const fieldValue: any = {
                                ...item,
                                value: form.code
                            };
                            dispatch(change(props.formInfo.formName, field, fieldValue));
                        }
                    }, {
                        title: "Vyhledat v INTERPI",
                        width: "800px"
                    })
                );
                break;
            }
            case CustomEditType.RELATION: {
                const initialValues: any = {
                    onlyMainPart: false,
                    area: Area.ALLNAMES,
                    itemSpecId: item.itemSpecId,
                };

                if ((item as AeItemRecordRefVO).value != null) {
                    initialValues.codeObj = {
                        id: (item as AeItemRecordRefVO).value,
                        name: item.textValue
                    }
                }

                dispatch(
                    ModalActions.showForm(RelationPartItemEditModalForm, {
                        initialValues,
                        itemTypeAttributeMap,
                        itemTypeId: item.itemTypeId,
                        onSubmit: (form: RelationPartItemClientVO) => {
                            let field = props.formInfo.sectionName ? `${props.formInfo.sectionName}.${name}` : name;
                            const fieldValue: any = {
                                ...item,
                                textValue: form.codeObj ? form.codeObj.name : "",
                                itemSpecId: form.itemSpecId,
                                value: form.codeObj ? form.codeObj.id : null
                            };
                            dispatch(change(props.formInfo.formName, field, fieldValue));
                        }
                    }, {
                        title: codelist.itemTypesMap[item.itemTypeId].name,
                        width: "800px"
                    })
                );
                break;
            }
        }
    },
    showImportDialog: (field: string) =>
        dispatch(
            ModalActions.showForm(ImportCoordinateModal, {
                    onCancel: () => dispatch(ModalActions.hide()),
                    onSubmit: async (formData: any) => {
                        const reader = new FileReader();
                        reader.onload = async () => {
                            const data = reader.result;
                            try {
                                const fieldValue = await EntitiesClientApiCall.standardApi.importCoordinates(formData.type, data).then(x => x.data);
                                dispatch(change(props.formInfo.formName, field, fieldValue));
                            } catch (e) {
                                notification.error({message: "Nepodařilo se importovat souřadnice"});
                            }
                        };
                        reader.readAsBinaryString(formData.fileList[0].originFileObj);
                    }
                },
                {
                    title: 'Importovat souřadnice',
                })
        ),
});

const mapStateToProps = (state: any) => {
    const {codelist}: { codelist: CodelistState } = state;

    return {
        codelist: codelist.data,
        attributes: state.app[PART_EDIT_FORM_ATTRIBUTES],
        editAeDetail: state.app[EDIT_AE_DETAIL_AREA]
    }
};

const form = reduxForm({
    form: 'simple' // a unique identifier for this form
})(PartEditForm);

export default connect(mapStateToProps, mapDispatchToProps)(form);
