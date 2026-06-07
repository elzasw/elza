import { useMemo, useState } from 'react';
import {WebApi} from 'actions/index.jsx';
import {Autocomplete} from 'components/shared';
import ListItem from 'components/shared/tree-list/list-item/ListItem.jsx';
import debounce from 'shared/utils/debounce';

function FundField({ value, onChange, excludedId, ...otherProps }) {
    const [dataList, setDataList] = useState([]);

    const handleSearchChange = useMemo(() => debounce((text) => {
        const fulltext = text === '' ? null : text;
        WebApi.findFunds(fulltext).then(json => {
            setDataList(json.funds.filter(fund => fund.id !== excludedId));
        });
    }, 300), [excludedId]);

    return (
        <Autocomplete
            className="form-group"
            customFilter
            value={value}
            items={dataList}
            onSearchChange={handleSearchChange}
            onChange={onChange}
            renderItem={(props) => <ListItem
                {...props}
                renderName={(item) => item.name + (item.internalCode ? " [" + item.internalCode + "]" : "")}
            />}
            {...otherProps}
            tags={false}
        />
    );
}

export default FundField;
