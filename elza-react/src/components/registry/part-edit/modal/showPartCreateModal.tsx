import React from 'react';
import { ApPartFormVO } from "../../../../api/ApPartFormVO";
// import i18n from "../../../i18n";
import { RulPartTypeVO } from '../../../../api/RulPartTypeVO';
import { WebApi } from '../../../../actions/WebApi';
import { modalDialogShow } from '../../../../actions/global/modalDialog';
import * as PartTypeInfo from '../../../../api/old/PartTypeInfo';
import { PartType } from '../../../../api/generated/model';
import { ApItemBitVO } from '../../../../api/ApItemBitVO';
import {goToAe} from '../../../../actions/registry/registry';
import { globalFundTreeInvalidate } from '../../../../actions/arr/globalFundTree';
import PartEditModal from './PartEditModal';
import * as H from "history";
import { RevisionApPartForm } from '../form';

export const showPartCreateModal = (
    partType: RulPartTypeVO,
    apId: number,
    apTypeId: number,
    scopeId: number,
    history: H.History<H.LocationState>,
    select: boolean,
    parentPartId?: number,
    onUpdateFinish: () => void = () => { },
    revParentPartId?: number,
) => (dispatch: any) => dispatch(
    modalDialogShow(
        this,
        // TODO: není rozmyšleno, kde brát skloňované popisky!
        PartTypeInfo.getPartEditDialogLabel(partType.code as PartType, true),
        ({ onClose }) => {
            const handleClose = () => onClose();

            const handleSubmit = async (data: RevisionApPartForm) => {
                    /*
                const submitData = {
                    items: data.items.filter(i => {
                        if (i['@class'] === '.ApItemEnumVO') {
                            return i.specId !== undefined;
                        } else {
                            return (i as ApItemBitVO).value !== undefined;
                        }
                    }),
                    parentPartId: parentPartId,
                    partTypeCode: partType.code,
                } as ApPartFormVO;
                    */
                const updatedItems = data.items.map(({updatedItem}) => updatedItem);
                const items = updatedItems.filter(item => {
                        if (item?.['@class'] === '.ApItemEnumVO') {
                            return item.specId !== undefined;
                        } else {
                            return (item as ApItemBitVO)?.value !== undefined;
                        }
                    })

                const submitData = {
                    items,
                    parentPartId,
                    revParentPartId,
                    partTypeCode: partType.code,
                } as ApPartFormVO;

                // console.log('SUBMIT ADD', apId, submitData);

                await WebApi.createPart(apId, submitData)
                onClose();
                await dispatch(goToAe(history, apId, true, !select))
                onUpdateFinish();
            }

            const formData = {
                partTypeCode: partType.code,
                items: [],
            } as ApPartFormVO

            return <PartEditModal
                apId={apId}
                apTypeId={apTypeId}
                scopeId={scopeId}
                partTypeId={partType.id}
                parentPartId={parentPartId}
                revParentPartId={revParentPartId}
                initialValues={formData}
                onSubmit={handleSubmit}
                onClose={handleClose}
                />},
        'dialog-lg',
        dispatch(globalFundTreeInvalidate()),
    ),
);
