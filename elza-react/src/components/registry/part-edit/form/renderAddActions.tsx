import React from 'react';
// import { WrappedFieldArrayProps} from 'redux-form';
import { FieldArrayRenderProps } from 'react-final-form-arrays';
import './PartEditForm.scss';
import {Icon} from '../../../index';
import {Button} from 'react-bootstrap';
import {ApItemVO} from '../../../../api/ApItemVO';
import {ApCreateTypeVO} from '../../../../api/ApCreateTypeVO';
import {RulDescItemTypeExtVO} from '../../../../api/RulDescItemTypeExtVO';
import {ApViewSettingRule} from '../../../../api/ApViewSettings';

interface RenderActionsProps extends FieldArrayRenderProps<ApItemVO, any> {
    attributes: Array<ApCreateTypeVO>;
    refTables: any;
    partTypeId: number;
    descItemTypesMap: Record<number, RulDescItemTypeExtVO>;
    apViewSettings: ApViewSettingRule;
    handleAddItems: (
        attributes: Array<ApCreateTypeVO>,
        refTables: any,
        formItems: Array<ApItemVO>,
        partTypeId: number,
        arrayInsert: (index: number, value: any) => void,
        userAction: boolean,
        descItemTypesMap: Record<number, RulDescItemTypeExtVO>,
        apViewSettings: ApViewSettingRule,
    ) => void;
}

export const renderAddActions = ({
    attributes,
    refTables,
    partTypeId,
    fields,
    handleAddItems,
    descItemTypesMap,
    apViewSettings,
}:RenderActionsProps) => {
    const existingItemTypeIds: Record<number, boolean> = {};
    fields.value.forEach(i => {
        existingItemTypeIds[i.typeId] = true;
    });


    return attributes
        .filter(attr => attr.repeatable || !existingItemTypeIds[attr.itemTypeId])
        .map((attr, index) => {
            const itemType = refTables.descItemTypes.itemsMap[attr.itemTypeId] as RulDescItemTypeExtVO;
            return (
                <Button
                    key={index}
                    variant={'link'}
                    title={itemType.name}
                    style={{paddingLeft: 0, color: '#000'}}
                    onClick={() => {
                        handleAddItems(
                            [attr],
                            refTables,
                            fields.value,
                            partTypeId,
                            fields.insert,
                            true,
                            descItemTypesMap,
                            apViewSettings,
                        );
                    }}
                >
                    <Icon className="mr-1" glyph={'fa-plus'} />
                    {itemType.shortcut}
                </Button>
            );
        });
};
