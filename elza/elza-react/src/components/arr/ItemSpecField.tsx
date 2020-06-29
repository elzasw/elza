import * as React from 'react';
import {connect} from 'react-redux';
import Autocomplete from '../shared/autocomplete/Autocomplete';

type Props = {
    onChange: (e: React.ChangeEventHandler<Object | number>) => void;
    useIdAsValue?: boolean;
    itemTypeId: number;
};

const ItemSpecField: React.FC<Props & ReturnType<typeof mapStateToProps>> = ({
    onChange,
    useIdAsValue,
    itemTypeId,
    descItemTypes,
    ...other
}) => {
    let items = [];
    if (descItemTypes.itemsMap && itemTypeId) {
        const item = descItemTypes.itemsMap[itemTypeId];
        if (item && item.descItemSpecs) {
            items = item.descItemSpecs;
        }
    }

    return <Autocomplete onChange={onChange} items={items} useIdAsValue={useIdAsValue} {...other} />;
};

function mapStateToProps(state: any) {
    const {
        refTables: {descItemTypes},
    } = state;
    return {
        descItemTypes,
    };
}

export default connect(mapStateToProps)(ItemSpecField);
