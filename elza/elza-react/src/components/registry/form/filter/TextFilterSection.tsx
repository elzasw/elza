import React from 'react';
import {Field, FormSection} from 'redux-form';
import i18n from "../../../i18n";
import {FormInputField} from "../../../shared";
import * as FieldUtils from "../../../../utils/FieldUtils";
import * as AreaInfo from "./AreaInfo";

type OwnProps = {
    submitting: boolean;
    name?: string;
    nameFormSection?: string; // název pro FormSection
}

type Props = {} & OwnProps;

const areaItems = FieldUtils.createItems(AreaInfo.getValues, AreaInfo.getName);
const onlyMainPartItems = [{
    id: "true",
    name: i18n('global.title.yes')
}, {
    id: "false",
    name: i18n('global.title.no')
}];

const TextFilterSection = ({submitting, nameFormSection = "", name = 'ap.ext-search.section.text'}: Props) => {
    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section">{i18n(name)}</span>
        <Field name="search"
               type="text"
               component={FormInputField}
               label={i18n('ap.ext-search.search')}
               disabled={submitting}
        />
        <Field name="area"
               type="autocomplete"
               component={FormInputField}
               label={i18n('ap.ext-search.area')}
               useIdAsValue
               items={areaItems}
               disabled={submitting}
        />
        <Field name="onlyMainPart"
               type="autocomplete"
               component={FormInputField}
               label={i18n('ap.ext-search.only-main-part')}
               useIdAsValue
               items={onlyMainPartItems}
               disabled={submitting}
        />
    </FormSection>
};

export default TextFilterSection;
