import React from 'react';
import {Field, FormSection} from 'redux-form';
import { FormattedMessage, useIntl, type MessageDescriptor } from 'react-intl';
import { filterMessages } from './messages';
import {FormInputField} from "../../../shared";

type OwnProps = {
    submitting: boolean;
    /** Nadpis sekce; ne název pole formuláře. */
    sectionTitle?: MessageDescriptor;
    nameFormSection?: string; // název pro FormSection
}

type Props = {} & OwnProps;

const CreExtFilterSection = ({submitting, nameFormSection = "", sectionTitle = filterMessages.sectionCreExt}: Props) => {
    const intl = useIntl();
    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section"><FormattedMessage {...sectionTitle} /></span>
        <Field name="creation"
               type="text"
               component={FormInputField}
               label={intl.formatMessage(filterMessages.creation)}
               disabled={submitting}
        />
        <Field name="extinction"
               type="text"
               component={FormInputField}
               label={intl.formatMessage(filterMessages.extinction)}
               disabled={submitting}
        />
    </FormSection>
};

export default CreExtFilterSection;
