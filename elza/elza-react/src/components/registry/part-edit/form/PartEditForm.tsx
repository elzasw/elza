import { Api } from 'api';
import { ApPartFormVO } from "api/ApPartFormVO";
import { i18n } from 'components/shared';
import React from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { useForm } from 'react-final-form';
import { FieldArray } from 'react-final-form-arrays';
import { useDispatch, useSelector } from 'react-redux';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { AppState, PartTypeCodes, RefTablesState } from 'typings/store';
import { ApCreateTypeVO } from '../../../../api/ApCreateTypeVO';
import { ApItemVO } from '../../../../api/ApItemVO';
import { ApViewSettings } from '../../../../api/ApViewSettings';
import { AP_VIEW_SETTINGS } from '../../../../constants';
import storeFromArea from '../../../../shared/utils/storeFromArea';
import { DetailStoreState } from '../../../../types';
import { Loading } from '../../../shared';
import { RevisionItem } from '../../revision';
import { addEmptyItems, addItemsWithValues } from './actions';
import { showAutoItemsModal } from './AutoItemsModal';
import './PartEditForm.scss';
import { renderAddActions } from './renderAddActions';
import { ItemsWrapper } from './renderItems';
import { handleValueUpdate } from './valueChangeMutators';
import { AutoValue } from 'elza-api';

export interface RevisionApPartForm extends Omit<ApPartFormVO, 'items'> {
    items: RevisionItem[];
}

const useThunkDispatch = <State,>():ThunkDispatch<State, void, AnyAction> => useDispatch()

type Props = {
    partTypeId: number;
    apTypeId: number;
    submitting: boolean;
    scopeId: number;
    availableAttributes?: ApCreateTypeVO[];
    editErrors?: string[];
    arrayName?: string;
    revision?: boolean;
    apId?: string | number;
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
    apId,
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
        refTables: RefTablesState,
        formItems: ApItemVO[],
        partTypeId: number,
        arrayInsert: (index: number, value: any) => void,
        userAction: boolean,
    ) => addEmptyItems(
        attributes, 
        refTables, 
        formItems,
        partTypeId,
        (index: number, value: any) => {
            arrayInsert(index, value);
            handleValueUpdate(form);
        },
        userAction,
    )
    const descItemTypesMap = useSelector((state: AppState) => state.refTables.descItemTypes.itemsMap);
    const apViewSettings = useSelector((state: AppState) => storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>);
    const refTables = useSelector((state:AppState) => state.refTables);
    const dispatch = useThunkDispatch();
    const form = useForm();

    const apViewSettingRule = apViewSettings.data!.rules[apViewSettings.data!.typeRuleSetMap[apTypeId]];

    const isDisabled = submitting || !availableAttributes;
    const partType = refTables.partTypes.itemsMap[partTypeId];
    const isName = partType.code === PartTypeCodes.PT_NAME;

    const itemTypeAttributeMap: Record<number, ApCreateTypeVO> = {};
    if (availableAttributes) {
        availableAttributes.forEach((attribute: ApCreateTypeVO) => {
            itemTypeAttributeMap[attribute.itemTypeId] = attribute;
        });
    }

    if (!availableAttributes || !refTables) { return <Loading />; }

    return (
        <FieldArray name={arrayName}>{({fields, meta})=>{

            const handleAutoItems = async () => {
                if(apId == undefined) {throw Error("no 'apId'");}
                const {data} = revision 
                    ? await Api.accesspoints.getRevAutoitems(apId.toString()) 
                    : await Api.accesspoints.getAutoitems(apId.toString());

                const result = await dispatch(showAutoItemsModal({
                    attributes: availableAttributes,
                    autoItems: data.items || [],
                    values: fields.value
                }));

                if(result){
                    const newItems: AutoValue[] = [];

                    result.forEach((autoValue) => {
                        const attribute = availableAttributes.find((attribute) => autoValue.itemTypeId === attribute.itemTypeId);
                        let currentIndex:number | undefined = undefined;
                        const currentValue = fields.value.find((item, index)=>{
                            if(item.typeId === autoValue.itemTypeId){
                                currentIndex = index;
                                return true;
                            }
                        })
                        const currentItem = currentValue?.item || currentValue?.updatedItem;

                        if(currentItem && !attribute?.repeatable){
                            form.change(`${arrayName}[${currentIndex}].updatedItem`, {
                                ...currentItem, 
                                value: autoValue.value,
                                specId: autoValue.itemSpecId,
                            })
                            handleValueUpdate(form);
                        }
                        if(!currentItem || attribute?.repeatable){
                            newItems.push(autoValue);
                        }
                    })

                    newItems.forEach((autoValue)=>{
                        const attribute = availableAttributes.find((attribute) => autoValue.itemTypeId === attribute.itemTypeId);
                        addItemsWithValues(
                            attribute ? [attribute] : [], 
                            autoValue ? [autoValue] : [],
                            refTables, 
                            fields.value,
                            partTypeId,
                            (index: number, value: any) => {
                                console.log("insert item with value", value, index)
                                fields.insert(index, value);
                                handleValueUpdate(form);
                            },
                        )
                    })
                }
            };

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
                            { availableAttributes 
                                && renderAddActions({
                                    partTypeId, 
                                    attributes: availableAttributes,
                                    refTables,
                                    handleAddItems,
                                    descItemTypesMap,
                                    fields,
                                    meta,
                                })}
                        </Col>
                        { apId != undefined && isName && <Col xs="auto">
                            <Button variant={'outline-dark'} onClick={() => handleAutoItems()}>
                                {i18n('ap.part.complements.create')}
                            </Button>
                        </Col>}
                    </Row>
                    <Row key="inputs" className="part-edit-form d-flex">
                        <ItemsWrapper
                            disabled={isDisabled}
                            deleteMode={false}
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
