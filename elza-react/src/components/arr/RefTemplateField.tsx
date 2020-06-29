import * as React from 'react';
import {connect} from 'react-redux';
import Autocomplete from '../shared/autocomplete/Autocomplete';
import {ArrRefTemplateVO} from '../../types';
import {WebApi} from '../../actions/WebApi';

type Props = {
    onChange: (e: React.ChangeEventHandler<Object | number>) => void;
    useIdAsValue?: boolean;
    fundId: number;
};

const RefTemplateField: React.FC<Props> = ({onChange, useIdAsValue, fundId, ...other}) => {
    const [items, setItems] = React.useState<ArrRefTemplateVO[] | null>(null);
    React.useEffect(() => {
        WebApi.getRefTemplates(fundId).then(setItems);
    }, []);
    return <Autocomplete onChange={onChange} items={items || []} useIdAsValue={useIdAsValue} {...other} />;
};

export default RefTemplateField;
