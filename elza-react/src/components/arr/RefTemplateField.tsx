import * as React from 'react';
import {connect} from 'react-redux';
import Autocomplete from '../shared/autocomplete/Autocomplete';
import {ArrRefTemplateVO} from '../../types';
import {WebApi} from '../../actions/WebApi';
import Loading from '../shared/loading/Loading';

type Props = {
    onChange: (e: React.ChangeEventHandler<Object | number>) => void;
    useIdAsValue?: boolean;
    fundId: number;
    value: string | number | object;
};

const RefTemplateField: React.FC<Props> = ({onChange, useIdAsValue, fundId, value, ...other}) => {
    const [items, setItems] = React.useState<ArrRefTemplateVO[] | null>(null);
    React.useEffect(() => {
        WebApi.getRefTemplates(fundId).then(setItems);
    }, []);

    if (items === null) {
        return <Loading />;
    }

    let realValue = value;
    if (useIdAsValue && typeof value === 'string') {
        realValue = parseInt(value);
    }

    return (
        <Autocomplete
            onChange={onChange}
            items={items || []}
            useIdAsValue={useIdAsValue}
            value={realValue}
            {...other}
        />
    );
};

export default RefTemplateField;
