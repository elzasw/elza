import React from 'react';
import {change} from 'redux-form';
import './PartEditForm.scss';
import {ApItemVO} from '../../../../api/ApItemVO';
import {ApCreateTypeVO} from '../../../../api/ApCreateTypeVO';
import {RulDataTypeCodeEnum} from '../../../../api/RulDataTypeCodeEnum';
import {RulDescItemTypeExtVO} from '../../../../api/RulDescItemTypeExtVO';
import {RulDataTypeVO} from '../../../../api/RulDataTypeVO';
import {RequiredType} from '../../../../api/RequiredType';
import * as ItemInfo from '../../../../utils/ItemInfo';
import {
    findItemPlacePosition,
    sortOwnItems,
} from '../../../../utils/ItemInfo';
import {ApItemBitVO} from '../../../../api/ApItemBitVO';
import {ApItemAccessPointRefVO} from '../../../../api/ApItemAccessPointRefVO';
import {modalDialogHide, modalDialogShow} from '../../../../actions/global/modalDialog';
import {WebApi} from '../../../../actions/WebApi';
import {Area} from '../../../../api/Area';
import RelationPartItemEditModalForm from '../../modal/RelationPartItemEditModalForm';
import ImportCoordinateModal from '../../Detail/coordinate/ImportCoordinateModal';
import i18n from '../../../i18n';

export const handleAddItems = (
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
}

export const onCustomEditItem = (
    name: string,
    systemCode: RulDataTypeCodeEnum,
    item: ApItemVO,
    refTables: any,
    partTypeId: number,
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    formName: string,
    apTypeId: number,
    scopeId: number,
) => {
return (dispatch) => {
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

    return dispatch(
        modalDialogShow(
            this,
            refTables.descItemTypes.itemsMap[item.typeId].shortcut,
            <RelationPartItemEditModalForm
                initialValues={initialValues}
                itemTypeAttributeMap={itemTypeAttributeMap}
                typeId={item.typeId}
                apTypeId={apTypeId}
                scopeId={scopeId}
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
                    dispatch(change(formName, field, fieldValue));
                    dispatch(modalDialogHide());
                }}
                />,
        ),
    );
    }
}

export const showImportDialog = (field: string, formName: string, sectionName: string) => 
(dispatch) => 
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
                        let realField = sectionName
                        ? `${sectionName}.${field}`
                        : field;
                        dispatch(change(formName, realField, fieldValue));
                    } catch (e) {
                        //notification.error({message: 'Nepodařilo se importovat souřadnice'});
                    }
                };
                reader.readAsBinaryString(formData.file);
            }}
            onSubmitSuccess={(result, dispatch) => dispatch(modalDialogHide())}
            />
    )
)
