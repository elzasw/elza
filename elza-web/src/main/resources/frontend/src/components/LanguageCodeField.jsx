import * as React from "react";
import {WebApi} from "../actions/WebApi";
import StoreSuggestField from "../shared/field/StoreSuggestField";

const apiCall = () => WebApi.getAllLanguages().then(rows => ({rows, count: rows.length}));

export default class LanguageCodeField extends React.Component {
    render() {
        return <StoreSuggestField area={"shared.languageCode"} apiCall={apiCall} {...this.props} useIdAsValue={true} getItemId={item => item ? item.code : null} />
    }
}
