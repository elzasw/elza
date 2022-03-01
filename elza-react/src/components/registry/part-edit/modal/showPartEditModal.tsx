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
import { objectById } from '../../../../shared/utils';
import { sortItems } from '../../../../utils/ItemInfo';
import PartEditModal from './PartEditModal';
import { RefTablesState } from '../../../../typings/store';
import { PartType } from '../../../../api/generated/model';
import * as H from "history";

export const showPartEditModal = (
    part: ApPartVO | undefined,
    updatedPart: ApPartVO | undefined,
    partType: PartType,
    apId: number,
    apTypeId: number,
    ruleSetId: number,
    scopeId: number,
    history: H.History<H.LocationState>,
    refTables: RefTablesState,
    apViewSettings: DetailStoreState<ApViewSettings>,
    revision: boolean,
    onUpdateFinish: () => void = () => {},
) => (dispatch:any) => dispatch(
    modalDialogShow(
        this,
        PartTypeInfo.getPartEditDialogLabel(partType, false),
        ({ onClose }) => {
            const partTypeId = objectById(refTables.partTypes.items, partType, 'code').id;
            const mainPart = updatedPart ? updatedPart : part as ApPartVO;
            const partId = part ? part.id : updatedPart?.id as number;
            const parentPartId = mainPart.partParentId;

            const handleSubmit = async (data: ApPartFormVO) => {
                if (mainPart?.id == undefined) { return; }
                const submitData = {
                    items: data.items.filter(i => {
                        if (i['@class'] === '.ApItemEnumVO') {
                            return i.specId !== undefined;
                        } else {
                            return (i as ApItemBitVO).value !== undefined;
                        }
                    }),
                    parentPartId,
                    partId: mainPart.id,
                    partTypeCode: partType,
                } as ApPartFormVO;

                console.log('SUBMIT EDIT', apId, mainPart.id, submitData);

                const result = part ? await WebApi.updatePart(apId, partId, submitData) : await WebApi.updateRevisionPart(apId, partId, submitData);
                onClose();
                await dispatch(goToAe(history, apId, true))
                onUpdateFinish();
                return result
            }

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
                partId: mainPart?.id,
                parentPartId,
                partTypeCode: refTables.partTypes.itemsMap[mainPart.typeId].code,
                items: mainPart.items ? sortItems(
                    partType as any,
                    mainPart.items,
                    refTables,
                    apViewSettings.data!.rules[ruleSetId],
                ) : [],
            } as ApPartFormVO

            return <PartEditModal
                partTypeId={partTypeId}
                onSubmit={handleSubmit}
                apTypeId={apTypeId}
                scopeId={scopeId}
                initialValues={formData}
                parentPartId={mainPart.partParentId}
                apId={apId}
                partId={mainPart.id}
                onClose={() => onClose()}
                partItems={updatedPart ? part?.items : null}
                />
        },
        'dialog-lg',
    ),
);
