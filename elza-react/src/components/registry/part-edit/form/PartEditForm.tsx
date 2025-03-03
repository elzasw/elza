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
import { addEmptyItems, createAutoValueItemWithIndex } from './actions';
import { showAutoItemsModal } from './AutoItemsModal';
import './PartEditForm.scss';
import { renderAddActions } from './renderAddActions';
import { ItemsWrapper } from './renderItems';
import { handleValueUpdate } from './valueChangeMutators';
import { AutoValue } from 'elza-api';
import { ApItemStringVO } from 'api/ApItemStringVO';

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
                    ? await Api.accesspoints.accessPointGetRevAutoitems(apId.toString())
                    : await Api.accesspoints.accessPointGetAutoitems(apId.toString());

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
                        const currentItem = currentValue?.updatedItem || currentValue?.item;

                        if(currentItem && !attribute?.repeatable){
                            const isOriginalValue = currentValue?.item?.value === autoValue.value && currentValue?.item?.specId == autoValue.itemSpecId;
                            form.change(`${arrayName}[${currentIndex}].updatedItem`, {
                                ...currentItem,
                                // set change type in case the currentItem is set as being deleted
                                changeType: isOriginalValue ? "ORIGINAL" : "UPDATED",
                                value: autoValue.value,
                                specId: autoValue.itemSpecId,
                            })
                            handleValueUpdate(form);
                        }
                        if(!currentItem || attribute?.repeatable){
                            newItems.push(autoValue);
                        }
                    })
                    const orderedItems:RevisionItem<ApItemVO>[] = [...fields.value]
                    const appendedNewItems:RevisionItem<ApItemStringVO>[] = [];

                    // add all new items from auto values in correct order
                    newItems.forEach((autoValue)=>{
                        const attribute = availableAttributes.find((attribute) => autoValue.itemTypeId === attribute.itemTypeId);
                        if(!attribute){
                            throw Error(`attribute not found: ${autoValue.itemTypeId}`);
                        }
                        const {item, index} = createAutoValueItemWithIndex(
                            attribute,
                            autoValue,
                            refTables,
                            orderedItems,
                            partTypeId
                        )
                        appendedNewItems.push(item);
                        orderedItems.splice(index, 0, item);
                    })

                    orderedItems.forEach((item, index) => {
                        const isNew = appendedNewItems.find((_item) => _item === item) != undefined;
                        // add items in correct order, skip existing
                        if(isNew){
                            fields.insert(index, item);
                        }
                    })

                    handleValueUpdate(form);
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
