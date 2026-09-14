import React from 'react';
import {Field, FormSection} from 'redux-form';
import {Field as FinalField} from 'react-final-form';
import { FormattedMessage, useIntl, type MessageDescriptor } from 'react-intl';
import { filterMessages } from './messages';
import {FormInputField} from "../../../shared";
import { ApExternalSystemSimpleVO } from 'typings/store';

type OwnProps = {
    submitting: boolean;
    nameFormSection?: string; // název pro FormSection
    /** Nadpis sekce; ne název pole formuláře. */
    sectionTitle?: MessageDescriptor;
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
    label,
    extSystems, 
    sectionName,
    
    hideName = false
}: FinalProps) => {
    const intl = useIntl();
    const resolvedLabel = label ?? intl.formatMessage(filterMessages.extSystem);
    const resolvedSectionName = sectionName ?? intl.formatMessage(filterMessages.sectionExtSystems);
    return <div className="filter-section">
        {!hideName && <span className="name-section">{resolvedSectionName}</span>}
        <FinalField name={name}
               label={resolvedLabel}
               type="autocomplete"
               component={FormInputField}
               getItemId={(item:ApExternalSystemSimpleVO) => item && item.code}
               useIdAsValue
               items={extSystems}
               disabled={disabled}
        />
    </div>
};

const ExtSystemFilterSection = ({submitting, nameFormSection = "", extSystems, sectionTitle = filterMessages.sectionExtSystems, hideName = false}: Props) => {
    const intl = useIntl();
    return <FormSection name={nameFormSection} className="filter-section">
        {!hideName && <span className="name-section"><FormattedMessage {...sectionTitle} /></span>}
        <Field name="extSystem"
               label={intl.formatMessage(filterMessages.extSystem)}
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
