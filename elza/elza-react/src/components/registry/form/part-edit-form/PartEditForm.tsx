import React, {useEffect, useState} from 'react';
import {arrayInsert, FieldArray } from 'redux-form';
import {connect} from 'react-redux';
import './PartEditForm.scss';
import {Action} from 'redux';
import {ThunkDispatch} from 'redux-thunk';
import {ApPartFormVO} from '../../../../api/ApPartFormVO';
import {Button, Col, Row} from 'react-bootstrap';
import {ApAccessPointCreateVO} from '../../../../api/ApAccessPointCreateVO';
import {ApItemVO} from '../../../../api/ApItemVO';
import {ApCreateTypeVO} from '../../../../api/ApCreateTypeVO';
import {RulDataTypeCodeEnum} from '../../../../api/RulDataTypeCodeEnum';
import {RulDescItemTypeExtVO} from '../../../../api/RulDescItemTypeExtVO';
import {RequiredType} from '../../../../api/RequiredType';
import {Loading} from '../../../shared';
import { compareCreateTypes, hasItemValue } from '../../../../utils/ItemInfo';
import {WebApi} from '../../../../actions/WebApi';
import {useDebouncedEffect} from '../../../../utils/hooks';
import { ApViewSettings } from '../../../../api/ApViewSettings';
import storeFromArea from '../../../../shared/utils/storeFromArea';
import {AP_VIEW_SETTINGS} from '../../../../constants';
import {DetailStoreState} from '../../../../types';
import { renderAddActions } from './renderAddActions';
import { renderItems } from './renderItems';
import { handleAddItems, onCustomEditItem, showImportDialog } from './actions';

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

const renderValidationErrors = (errors: Array<string>) => {
    return <ul>
        {errors.map(value => (
            <li>
                {value}
            </li>
        ))}
    </ul>
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
    ) => handleAddItems(
        attributes, 
        refTables, 
        formItems,
        partTypeId,
        arrayInsert,
        userAction,
        descItemTypesMap,
        apViewSettings
    ),
    onCustomEditItem: (
        name: string,
        systemCode: RulDataTypeCodeEnum,
        item: ApItemVO,
        refTables: any,
        partTypeId: number,
        itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    ) => dispatch(onCustomEditItem(
        name,
        systemCode,
        item,
        refTables,
        partTypeId,
        itemTypeAttributeMap,
        props.formInfo.formName,
        props.apTypeId,
        props.scopeId
    )),
    showImportDialog: (field: string) =>
        dispatch(showImportDialog(field, props.formInfo.formName, props.formInfo.sectionName)),
});

const mapStateToProps = (state: any) => {
    return {
        descItemTypesMap: state.refTables.descItemTypes.itemsMap as Record<number, RulDescItemTypeExtVO>,
        apViewSettings: storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
        refTables: state.refTables,
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(PartEditForm);
