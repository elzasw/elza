import React from 'react';
import {Field, FormSection} from 'redux-form';
import i18n from "../../../i18n";
import {FormInputField} from "../../../shared";

type OwnProps = {
    submitting: boolean;
    name?: string;
    nameFormSection?: string; // název pro FormSection
}

type Props = {} & OwnProps;

const CreExtFilterSection = ({submitting, nameFormSection = "", name = 'ap.ext-search.section.cre-ext'}: Props) => {
    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section">{i18n(name)}</span>
        <Field name="creation"
               type="text"
               component={FormInputField}
               label={i18n('ap.ext-search.creation')}
               disabled={submitting}
        />
        <Field name="extinction"
               type="text"
               component={FormInputField}
               label={i18n('ap.ext-search.extinction')}
               disabled={submitting}
        />
    </FormSection>
};

export default CreExtFilterSection;
