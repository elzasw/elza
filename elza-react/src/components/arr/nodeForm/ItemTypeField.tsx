import { FundScope } from '../../../types';
import { Autocomplete, i18n, Icon } from 'components/shared';
import { Button } from 'components/ui';
import React, { memo } from 'react';
import { FieldArrayFieldsProps } from 'redux-form';

interface IScopesFieldProps {
    fields: FieldArrayFieldsProps<any>
    descItemTypes: any
}

export const ItemTypeField: React.FC<IScopesFieldProps> = memo(({fields, ...props}) => (
    <>
        <Autocomplete
            tree
            alwaysExpanded
            label={i18n('subNodeForm.descItemType.all')}
            items={props.descItemTypes}
            getItemRenderClass={item => (item.groupItem ? null : ' type-' + item.type.toLowerCase())}
            allowSelectItem={item => !item.groupItem}
            onBlurValidation={false}
        />
    </>
));
