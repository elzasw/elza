import * as React from 'react';
import {connect} from 'react-redux';
import Autocomplete from '../shared/autocomplete/Autocomplete';

type Props = {
    onChange: (e: React.ChangeEventHandler<Object | number>) => void;
    useIdAsValue?: boolean;
    items: object[];
};

const DescItemTypeField: React.FC<Props> = ({onChange, useIdAsValue, items, ...other}) => {
    return <Autocomplete onChange={onChange} items={items} useIdAsValue={useIdAsValue} {...other} />;
};

export default connect((state: any) => {
    const {
        refTables: {descItemTypes},
    } = state;
    return {
        items: descItemTypes && descItemTypes.fetched ? descItemTypes.items : [],
    };
})(DescItemTypeField);
