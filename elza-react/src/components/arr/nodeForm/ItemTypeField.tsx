import { Autocomplete, i18n } from 'components/shared';
import { forwardRef } from 'react';
// import { FieldArrayFieldsProps } from 'redux-form';
import { DescItemTypeRef } from 'typings/store';

interface Props {
    descItemTypes: DescItemTypeRef[];
}

export const ItemTypeField = forwardRef<Autocomplete, Props>(({descItemTypes, ...props}, ref) => (
    <>
        <Autocomplete
            ref={ref}
            tree={true}
            alwaysExpanded={true}
            label={i18n('subNodeForm.descItemType.all')}
            items={descItemTypes}
            // getItemRenderClass={item => (item.groupItem ? null : ' type-' + item.type.toLowerCase())}
            // allowSelectItem={item => !item.groupItem}
            onBlurValidation={false}
            {...props}
        />
    </>
));
