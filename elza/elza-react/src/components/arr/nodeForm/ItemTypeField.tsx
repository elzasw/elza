import { Autocomplete} from 'components/shared';
import { FormattedMessage } from 'react-intl';
import { nodeMessages } from 'components/arr/nodeMessages';

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
            label={<FormattedMessage {...nodeMessages.subNodeFormDescItemTypeAll} />}
            items={descItemTypes}
            // getItemRenderClass={item => (item.groupItem ? null : ' type-' + item.type.toLowerCase())}
            // allowSelectItem={item => !item.groupItem}
            onBlurValidation={false}
            {...props}
        />
    </>
));
