import React from 'react';
import {Field, FormSection} from 'redux-form';
import { FormattedMessage, useIntl, type MessageDescriptor } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { filterMessages } from './messages';
import {FormInputField} from "../../../shared";
import * as FieldUtils from "../../../../utils/FieldUtils";
import * as AreaInfo from "./AreaInfo";

type OwnProps = {
    submitting: boolean;
    /** Nadpis sekce; ne název pole formuláře. */
    sectionTitle?: MessageDescriptor;
    nameFormSection?: string; // název pro FormSection
}

type Props = {} & OwnProps;

const areaItems = FieldUtils.createItems(AreaInfo.getValues, AreaInfo.getName);


const TextFilterSection = ({submitting, nameFormSection = "", sectionTitle = filterMessages.sectionText}: Props) => {
    const intl = useIntl();
    // Skládá se při renderu, aby popisky reagovaly na přepnutí jazyka.
    const onlyMainPartItems = [
        { id: "true", name: intl.formatMessage(globalMessages.yes) },
        { id: "false", name: intl.formatMessage(globalMessages.no) },
    ];
    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section"><FormattedMessage {...sectionTitle} /></span>
        <Field name="search"
               type="text"
               component={FormInputField}
               label={intl.formatMessage(filterMessages.search)}
               disabled={submitting}
        />
        <Field name="area"
               type="autocomplete"
               component={FormInputField}
               label={intl.formatMessage(filterMessages.area)}
               useIdAsValue
               items={areaItems}
               disabled={submitting}
        />
        <Field name="onlyMainPart"
               type="autocomplete"
               component={FormInputField}
               label={intl.formatMessage(filterMessages.onlyMainPart)}
               useIdAsValue
               items={onlyMainPartItems}
               disabled={submitting}
        />
    </FormSection>
};

export default TextFilterSection;
