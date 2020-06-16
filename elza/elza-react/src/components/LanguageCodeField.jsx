import * as React from 'react';
import {WebApi} from '../actions/WebApi';
import StoreSuggestField from '../shared/field/StoreSuggestField';

const apiCall = () => WebApi.getAllLanguages().then(rows => ({rows, count: rows.length}));

const LanguageCodeField = props => {
    return (
        <StoreSuggestField
            area={'shared.languageCode'}
            apiCall={apiCall}
            {...props}
            useIdAsValue={true}
            getItemId={item => (item ? item.code : null)}
            {...props.meta}
        />
    );
};

export default LanguageCodeField;
