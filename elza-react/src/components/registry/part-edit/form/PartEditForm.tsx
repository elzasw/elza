import React, { useState } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { useForm } from 'react-final-form';
import { FieldArray } from 'react-final-form-arrays';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import { ApCreateTypeVO } from '../../../../api/ApCreateTypeVO';
import { ApItemVO } from '../../../../api/ApItemVO';
import { ApViewSettings } from '../../../../api/ApViewSettings';
import { AP_VIEW_SETTINGS } from '../../../../constants';
import storeFromArea from '../../../../shared/utils/storeFromArea';
import { DetailStoreState } from '../../../../types';
import { Loading } from '../../../shared';
import { addItems } from './actions';
import './PartEditForm.scss';
import { renderAddActions } from './renderAddActions';
import { ItemsWrapper } from './renderItems';
import { handleValueUpdate } from './valueChangeMutators';
import { RevisionItem } from '../../revision'
import { ApPartFormVO } from "api/ApPartFormVO";

export interface RevisionApPartForm extends Omit<ApPartFormVO, 'items'> {
    items: RevisionItem[];
}

type Props = {
    partTypeId: number;
    apTypeId: number;
    submitting: boolean;
    scopeId: number;
    availableAttributes?: ApCreateTypeVO[];
    editErrors?: string[];
    arrayName?: string;
    revision?: boolean;
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

export const PartEditForm = ({
    partTypeId,
    apTypeId,
    submitting,
    scopeId,
    availableAttributes,
    editErrors,
    arrayName = "items",
    revision = false,
}: Props) => {
    const handleAddItems = (
        attributes: Array<ApCreateTypeVO>,
        refTables: any,
        formItems: ApItemVO[],
        partTypeId: number,
        arrayInsert: (index: number, value: any) => void,
        userAction: boolean,
        apViewSettings: any,
    ) => addItems(
        attributes, 
        refTables, 
        formItems,
        partTypeId,
        (index: number, value: any) => {
            arrayInsert(index, value);
            handleValueUpdate(form);
        },
        userAction,
        apViewSettings
    )
    const descItemTypesMap = useSelector((state: AppState) => state.refTables.descItemTypes.itemsMap);
    const apViewSettings = useSelector((state: AppState) => storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>);
    const refTables = useSelector((state:AppState) => state.refTables);
    const [deleteMode, setDeleteMode] = useState<boolean>(false);
    const form = useForm();

    const apViewSettingRule = apViewSettings.data!.rules[apViewSettings.data!.typeRuleSetMap[apTypeId]];


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

    if (!availableAttributes || !refTables) { return <Loading />; }

    return (
        <FieldArray name={arrayName}>{({fields, meta})=>{
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
                            itemTypeAttributeMap={itemTypeAttributeMap}
                            itemTypeSettings={apViewSettingRule?.itemTypes || []}
                            onDeleteItem={() => {handleValueUpdate(form)}}
                            itemPrefix={arrayName}
                            partTypeId={partTypeId}
                            scopeId={scopeId}
                            apTypeId={apTypeId}
                            revision={revision}
                            />
                    </Row>
                </div>
                </>
        }}</FieldArray>
    );
};

export default PartEditForm;
