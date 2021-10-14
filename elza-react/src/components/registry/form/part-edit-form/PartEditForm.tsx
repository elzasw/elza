import React, {useEffect, useState, useRef} from 'react';
// import {arrayInsert, FieldArray } from 'redux-form';
import {FieldArray} from 'react-final-form-arrays';
import { useForm } from 'react-final-form';
import { useDispatch, useSelector } from 'react-redux';
import './PartEditForm.scss';
// import {Action} from 'redux';
// import {ThunkDispatch} from 'redux-thunk';
import {Button, Col, Row} from 'react-bootstrap';
// import {ApAccessPointCreateVO} from '../../../../api/ApAccessPointCreateVO';
import {ApItemVO} from '../../../../api/ApItemVO';
import {ApCreateTypeVO} from '../../../../api/ApCreateTypeVO';
import {RulDataTypeCodeEnum} from '../../../../api/RulDataTypeCodeEnum';
import {RulDescItemTypeExtVO} from '../../../../api/RulDescItemTypeExtVO';
// import {RequiredType} from '../../../../api/RequiredType';
import {Loading} from '../../../shared';
// import { compareCreateTypes, hasItemValue } from '../../../../utils/ItemInfo';
// import {WebApi} from '../../../../actions/WebApi';
// import {useDebouncedEffect} from '../../../../utils/hooks';
import { ApViewSettings } from '../../../../api/ApViewSettings';
import storeFromArea from '../../../../shared/utils/storeFromArea';
import {AP_VIEW_SETTINGS} from '../../../../constants';
import {DetailStoreState} from '../../../../types';
import { renderAddActions } from './renderAddActions';
import { ItemsWrapper } from './renderItems';
import { addItems, onCustomEditItem, showImportDialog } from './actions';
import { AppState } from 'typings/store'

type Props = {
    partTypeId: number;
    apTypeId: number;
    submitting: boolean;
    scopeId: number;
    formInfo: {
        formName: string;
        sectionName: string;
    };
    availableAttributes?: ApCreateTypeVO[];
    editErrors?: string[];
};

const renderValidationErrors = (errors: Array<string>) => {
    return <ul>
        {errors.map((value, index) => (
            <li key={index}>
                {value}
            </li>
        ))}
    </ul>
};

const PartEditForm = ({
    partTypeId,
    apTypeId,
    submitting,
    scopeId,
    formInfo,
    availableAttributes,
    editErrors,
}: Props) => {
    const dispatch = useDispatch();
    const handleAddItems = (
        attributes: Array<ApCreateTypeVO>,
        refTables: any,
        formItems: ApItemVO[],
        partTypeId: number,
        arrayInsert: (index: number, value: any) => void,
        userAction: boolean,
        descItemTypesMap: Record<number, RulDescItemTypeExtVO>,
        apViewSettings: any,
    ) => addItems(
        attributes, 
        refTables, 
        formItems,
        partTypeId,
        (index: number, value: any) => {
            arrayInsert(index, value);
            form.mutators.attributes("arrayInsert");
        },
        userAction,
        descItemTypesMap,
        apViewSettings
    )
    const handleEditItem = (
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
        formInfo.formName,
        apTypeId,
        scopeId
    ))
    const handleShowImportDialog = (field: string) => dispatch(showImportDialog(field, formInfo.formName, formInfo.sectionName));
    const descItemTypesMap = useSelector((state: AppState) => state.refTables.descItemTypes.itemsMap);
    const apViewSettings = useSelector((state: AppState) => storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>);
    const refTables = useSelector((state:AppState) => state.refTables);
    const [deleteMode, setDeleteMode] = useState<boolean>(false);
    const form = useForm();

    const apViewSettingRule = apViewSettings.data!.rules[apViewSettings.data!.typeRuleSetMap[apTypeId]];

    if (!refTables) {
        return <div />;
    }

    const handleDeleteMode = () => {
        setDeleteMode(!deleteMode);
    };

    const isDisabled = submitting || !availableAttributes;

    const itemTypeAttributeMap: Record<number, ApCreateTypeVO> = {};
    if (availableAttributes) {
        availableAttributes.forEach((attribute: ApCreateTypeVO) => {
            itemTypeAttributeMap[attribute.itemTypeId] = attribute;
        });
    }

    if (!availableAttributes) { return <Loading />; }

    return (
        <FieldArray name="items">{({fields, meta})=>{
            return <>
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
                            {!deleteMode 
                                && availableAttributes 
                                && renderAddActions({
                                    partTypeId, 
                                    attributes: availableAttributes,
                                    refTables,
                                    handleAddItems,
                                    descItemTypesMap,
                                    apViewSettings:apViewSettings.data!.rules[apViewSettings.data!.typeRuleSetMap[apTypeId]],
                                    fields,
                                    meta,
                                })}
                        </Col>
                        <Col xs="auto">
                            <Button disabled={isDisabled} variant={'outline-dark'} onClick={() => handleDeleteMode()}>
                                {deleteMode ? 'Ukončit režim odstraňování' : 'Odstranit položky formuláře'}
                            </Button>
                        </Col>
                    </Row>
                    <Row key="inputs" className="part-edit-form d-flex">
                        <ItemsWrapper
                            disabled={isDisabled}
                            deleteMode={deleteMode}
                            fields={fields}
                            meta={meta}
                            onCustomEditItem={(name: string, systemCode: RulDataTypeCodeEnum, item: ApItemVO) => handleEditItem(name, systemCode, item, refTables, partTypeId, itemTypeAttributeMap)}
                            itemTypeAttributeMap={itemTypeAttributeMap}
                            showImportDialog={handleShowImportDialog}
                            itemTypeSettings={apViewSettingRule?.itemTypes || []}
                            onDeleteItem={() => {form.mutators.attributes("arrayDelete")}}
                            />
                    </Row>
                </div>
                </>
        }}</FieldArray>
    );
};

export default PartEditForm;
