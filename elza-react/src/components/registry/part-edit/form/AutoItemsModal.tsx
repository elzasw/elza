import { ApCreateTypeVO } from 'api/ApCreateTypeVO';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import { AutoValue } from 'elza-api';
import React, { FC } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import { RevisionDisplay, RevisionItem } from '../../revision';
import { showConfirmDialog, showInfoDialog } from 'components/shared/dialog';
import { Action, ActionCreator } from "redux";
import { ThunkAction } from "redux-thunk";
import { ApItemVO } from 'api/ApItemVO';
import { i18n } from 'components/shared';

export interface AutoItemsModalProps {
    attributes: ApCreateTypeVO[];
    values: RevisionItem[];
    autoItems: AutoValue[];
}

const convertApItemToAutoValue = (item?: ApItemVO): AutoValue | undefined => {
    if(!item){return undefined;}
    const itemWithValue: ApItemVO & {value: any} = item as ApItemVO & {value: any};
    return {
        value: itemWithValue.value ? itemWithValue.value.toString() : undefined,
        itemSpecId: itemWithValue.specId,
        itemTypeId: itemWithValue.typeId,
    }
}

const getItemValue = (item: AutoValue | undefined, descItem: RulDescItemTypeExtVO) => {
    if(item?.value != undefined) { return item.value }
    if(item?.itemSpecId != undefined){
        return descItem.descItemSpecs.find((spec)=> spec.id === item.itemSpecId)?.name || item.itemSpecId;
    }
    return item?.value || item?.itemSpecId;
}

export const AutoItemsModal:FC<AutoItemsModalProps> = ({
    attributes,
    values,
    autoItems,
}) => {
    const refTables = useSelector((state:AppState) => state.refTables);

    return <>{
        attributes.map((attribute)=>{
            const descItem = refTables.descItemTypes.itemsMap[attribute.itemTypeId];
            const itemName = descItem?.name;
            const currentValue = values.find(({item, updatedItem})=>{
                return updatedItem?.typeId === attribute.itemTypeId || item?.typeId === attribute.itemTypeId
            })

            const currentItem = convertApItemToAutoValue(currentValue?.updatedItem || currentValue?.item);

            const autoItem = autoItems.find((autoItem)=>{
                return autoItem.itemTypeId === attribute.itemTypeId
            })


            const currentItemValue = getItemValue(currentItem, descItem);
            const autoItemValue = getItemValue(autoItem, descItem);

            return <div>
                <label>{itemName}</label>
                <RevisionDisplay 
                    valuesEqual={currentItemValue === autoItemValue}
                    renderPrevValue={() => currentItemValue} 
                    renderValue={() => autoItemValue}
                    isNew={currentValue == undefined || attribute.repeatable}
                    /> 
            </div>
        })
    }</>
}

const checkValuesChanged = (values: RevisionItem[], autoItems: AutoValue[]) => {
    return autoItems.some((autoItem)=>{
        const item = values.find((item) => item.typeId === autoItem.itemTypeId )
        if(!item){return true}

        const convertedItem = convertApItemToAutoValue(item?.updatedItem || item?.item)
        return convertedItem?.value !== autoItem.value || convertedItem?.itemSpecId !== autoItem.itemSpecId;
    })
}

export const showAutoItemsModal:ActionCreator<
ThunkAction<Promise<AutoValue[]>, AppState, void, Action>
> = ({
    attributes = [],
    values = [],
    autoItems = [],
}:AutoItemsModalProps) => async (dispatch) => {
        if(autoItems.length === 0) {
            await dispatch(showInfoDialog({
                message: i18n('ap.part.complements.noItems.message'),
                title: i18n('ap.part.complements.noItems.title')
            }))

            return []
        }

        const valuesChanged = checkValuesChanged(values, autoItems);
        const usedAttributes = attributes
            .filter((attribute) => 
                autoItems?.find((item) => item.itemTypeId === attribute.itemTypeId
                ))

        if(!valuesChanged) {
            await dispatch(showInfoDialog({
                message: <>
                    <p>
                        <i>{i18n('ap.part.complements.noChanges.message')}</i>
                    </p>
                    <div><AutoItemsModal
                        attributes={usedAttributes}
                        autoItems={autoItems}
                        values={values}
                        />
                    </div>
                    </>,
                title: i18n('ap.part.complements.noChanges.title')
            }))

            return []
        }

        const result = await dispatch(showConfirmDialog(<AutoItemsModal
            attributes={usedAttributes}
            autoItems={autoItems}
            values={values}
            />))

        if(result){
            return autoItems
        }
        return []
    }
