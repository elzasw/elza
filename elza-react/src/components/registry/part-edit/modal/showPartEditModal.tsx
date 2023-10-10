import React from 'react';
import { ApPartFormVO } from "../../../../api/ApPartFormVO";
import { WebApi } from '../../../../actions/WebApi';
import { modalDialogShow } from '../../../../actions/global/modalDialog';
import * as PartTypeInfo from '../../../../api/old/PartTypeInfo';
import { ApItemBitVO } from '../../../../api/ApItemBitVO';
import {goToAe} from '../../../../actions/registry/registry';
import { ApPartVO } from '../../../../api/ApPartVO';
// import { RulDescItemTypeExtVO } from '../../../../api/RulDescItemTypeExtVO';
import { DetailStoreState } from '../../../../types';
import { ApViewSettings } from '../../../../api/ApViewSettings';
// import { objectById } from '../../../../shared/utils';
import { sortItems } from '../../../../utils/partEdit';
import PartEditModal from './PartEditModal';
import { RefTablesState } from '../../../../typings/store';
import { PartType } from '../../../../api/generated/model';
import * as H from "history";
import { getRevisionItems } from '../../revision';
import { RevisionApPartForm } from '../form';

export const showPartEditModal = (
    part: ApPartVO | undefined,
    updatedPart: ApPartVO | undefined,
    partType: PartType,
    apId: number,
    apVersion: number,
    apTypeId: number,
    ruleSetId: number,
    scopeId: number,
    history: H.History<H.LocationState>,
    refTables: RefTablesState,
    apViewSettings: DetailStoreState<ApViewSettings>,
    revision: boolean,
    onUpdateFinish: () => void = () => {},
    select: boolean,
) => (dispatch:any) => dispatch(
    modalDialogShow(
        this,
        PartTypeInfo.getPartEditDialogLabel(partType, false),
        ({ onClose }) => {
            const modifiedPart = updatedPart ? updatedPart : part;
            if(!modifiedPart){throw "No part";}

            const partTypeId = modifiedPart.typeId; // objectById(refTables.partTypes.items, partType, 'code').id;
            // const partId = part ? part.id : updatedPart?.id as number;
            const parentPartId = modifiedPart.partParentId;
            const revParentPartId = modifiedPart.revPartParentId;

            const handleSubmit = async (data: RevisionApPartForm) => {
                const updatedItems = data.items.map(({updatedItem}) => updatedItem);
                const items = updatedItems.filter(item => {
                        if(item?.changeType === "DELETED"){return false;}
                        if (item?.['@class'] === '.ApItemEnumVO') { //TODO - predelat @class na typeId
                            return item.specId !== undefined;
                        } else {
                            return (item as ApItemBitVO)?.value !== undefined;
                        }
                    })
                const submitData:ApPartFormVO = {
                    items,
                    parentPartId,
                    revParentPartId,
                    partId: modifiedPart.id,
                    partTypeCode: partType,
                } as ApPartFormVO;

                // console.log('SUBMIT EDIT', apId, modifiedPart.id, submitData);

                const result = !updatedPart
                        ? await WebApi.updatePart(apId, modifiedPart.id, submitData, apVersion)
                        : await WebApi.updateRevisionPart(apId, modifiedPart.id, submitData, apVersion);
                onClose();
                await dispatch(goToAe(history, apId, true, !select, revision))
                onUpdateFinish();
                return result
            }
            const items = getRevisionItems(
                    revision ? part?.items || [] : undefined,
                    revision ? updatedPart?.items || [] : part?.items || [])

            /*
            const initialValues = {
                partForm: {
                    items: part.items ? sortItems(
                        parseInt(partType, 10),
                        part.items,
                        refTables,
                        apViewSettings.data!.rules[ruleSetId],
                    ) : [],
                },
            }
            */

            const formData = {
                partId: modifiedPart?.id,
                parentPartId,
                partTypeCode: refTables.partTypes.itemsMap[modifiedPart.typeId].code,
                items: sortItems(
                    partTypeId,
                    items,
                    refTables,
                    apViewSettings.data!.rules[ruleSetId],
                ),
            } as ApPartFormVO

            return <PartEditModal
                partTypeId={partTypeId}
                onSubmit={handleSubmit}
                apTypeId={apTypeId}
                scopeId={scopeId}
                initialValues={formData}
                parentPartId={modifiedPart.partParentId}
                revParentPartId={revParentPartId}
                apId={apId}
                partId={modifiedPart.id}
                onClose={() => onClose()}
                revision={revision}
                />
        },
        'dialog-lg',
    ),
);
