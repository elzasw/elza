import React from 'react';
import {Field, FormSection} from 'redux-form';
import {Field as FinalField} from 'react-final-form';
import i18n from "../../../i18n";
import {FormInputField} from "../../../shared";
import { ApExternalSystemSimpleVO } from 'typings/store';

type OwnProps = {
    submitting: boolean;
    nameFormSection?: string; // název pro FormSection
    name?: string;
    hideName?: boolean;
    extSystems: any[];
}

type Props = {} & OwnProps;

interface FinalProps {
    disabled: boolean;
    name?: string; // název pro FormSection
    sectionName?: string;
    label?: string;
    hideName?: boolean;
    extSystems: ApExternalSystemSimpleVO[];
}

export const ExtSystemFilterSectionFinal = ({
    disabled, 
    name = "extSystem", 
    label = i18n('ap.ext-search.ext-system'),
    extSystems, 
    sectionName = i18n('ap.ext-search.section.ext-systems'), 
    hideName = false
}: FinalProps) => {
    return <div className="filter-section">
        {!hideName && <span className="name-section">{sectionName}</span>}
        <FinalField name={name}
               label={label}
               type="autocomplete"
               component={FormInputField}
               getItemId={(item:ApExternalSystemSimpleVO) => item && item.code}
               useIdAsValue
               items={extSystems}
               disabled={disabled}
        />
    </div>
};

const ExtSystemFilterSection = ({submitting, nameFormSection = "", extSystems, name = 'ap.ext-search.section.ext-systems', hideName = false}: Props) => {
    return <FormSection name={nameFormSection} className="filter-section">
        {!hideName && <span className="name-section">{i18n(name)}</span>}
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
