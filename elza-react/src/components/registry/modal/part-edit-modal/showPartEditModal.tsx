import React from 'react';
import { ApPartFormVO } from "../../../../api/ApPartFormVO";
import { WebApi } from '../../../../actions/WebApi';
import { modalDialogShow } from '../../../../actions/global/modalDialog';
import * as PartTypeInfo from '../../../../api/old/PartTypeInfo';
import { ApItemBitVO } from '../../../../api/ApItemBitVO';
import { registryDetailFetchIfNeeded } from '../../../../actions/registry/registry';
import { ApPartVO } from '../../../../api/ApPartVO';
import { RulDescItemTypeExtVO } from '../../../../api/RulDescItemTypeExtVO';
import { DetailStoreState } from '../../../../types';
import { ApViewSettings } from '../../../../api/ApViewSettings';
import { objectById } from '../../../../shared/utils';
import { sortItems } from '../../../../utils/ItemInfo';
import PartEditModal from './PartEditModal';
import { RefTablesState } from '../../../../typings/store';
import { PartType } from '../../../../api/generated/model';

export const showPartEditModal = (
    part: ApPartVO,
    partType: PartType,
    apId: number,
    apTypeId: number,
    ruleSetId: number,
    scopeId: number,
    refTables: RefTablesState,
    descItemTypesMap: Record<number, RulDescItemTypeExtVO>,
    apViewSettings: DetailStoreState<ApViewSettings>,
    parentPartId?: number,
    onUpdateFinish: () => void = () => {},
) => (dispatch:any) => dispatch(
    modalDialogShow(
        this,
        PartTypeInfo.getPartEditDialogLabel(partType, false),
        ({ onClose }) => {
            const partTypeId = objectById(refTables.partTypes.items, partType, 'code').id;

            const handleSubmit = async (data: ApPartFormVO) => {
                if (!part.id) { return; }
                const submitData = {
                    items: data.items.filter(i => {
                        if (i['@class'] === '.ApItemEnumVO') {
                            return i.specId !== undefined;
                        } else {
                            return (i as ApItemBitVO).value !== undefined;
                        }
                    }),
                    parentPartId: parentPartId,
                    partId: part.id,
                    partTypeCode: partType,
                } as ApPartFormVO;

                console.log('SUBMIT EDIT', apId, part.id, submitData);

                const result = await WebApi.updatePart(apId, part.id, submitData)
                onClose();
                await dispatch(registryDetailFetchIfNeeded(apId, true))
                onUpdateFinish();
                return result
            }

            console.log("partType", partType);

            const initialValues = {
                partForm: {
                    items: part.items ? sortItems(
                        parseInt(partType, 10),
                        part.items,
                        refTables,
                        descItemTypesMap,
                        apViewSettings.data!.rules[ruleSetId],
                    ) : [],
                },
            }

            const formData = {
                partId: part.id,
                parentPartId: part.partParentId,
                partTypeCode: refTables.partTypes.itemsMap[part.typeId].code,
                items: part.items ? sortItems(
                    partType as any,
                    part.items,
                    refTables,
                    descItemTypesMap,
                    apViewSettings.data!.rules[ruleSetId],
                ) : [],
            } as ApPartFormVO

            return <PartEditModal
                partTypeId={partTypeId}
                onSubmit={handleSubmit}
                apTypeId={apTypeId}
                scopeId={scopeId}
                initialValues={formData}
                parentPartId={part.partParentId}
                apId={apId}
                partId={part.id}
                onClose={() => onClose()}
                />
        },
        'dialog-lg',
    ),
);
