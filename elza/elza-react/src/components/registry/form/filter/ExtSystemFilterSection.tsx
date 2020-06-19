import React from 'react';
import {Field, FormSection} from 'redux-form';
import i18n from "../../../i18n";
import {FormInputField} from "../../../shared";

type OwnProps = {
    submitting: boolean;
    nameFormSection?: string; // název pro FormSection
    name?: string;
    extSystems: any[];
}

type Props = {} & OwnProps;

const ExtSystemFilterSection = ({submitting, nameFormSection = "", extSystems, name = 'ap.ext-search.section.ext-systems'}: Props) => {
    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section">{i18n(name)}</span>
        <Field name="extSystem"
               label={i18n('ap.ext-search.ext-system')}
               type="autocomplete"
               component={FormInputField}
               getItemId={item => item && item.code}
               useIdAsValue
               items={extSystems}
               disabled={submitting}
        />
    </FormSection>
};

export default ExtSystemFilterSection;
