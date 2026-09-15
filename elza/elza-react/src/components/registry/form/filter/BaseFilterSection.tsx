import React from 'react';
import {Field, FieldArray, FormSection} from 'redux-form';
import { FormattedMessage, useIntl, type MessageDescriptor } from 'react-intl';
import { messageFor } from 'components/shared/lang/dynamicMessage';
import { filterMessages, syncStateMessages, validationResultMessages } from './messages';
import {FormInputField} from "../../../shared";
import {ApTypeVO} from "../../../../api/ApTypeVO";
import {TypesField} from "../../field/TypesField";
import { SyncState } from 'elza-api';
import UserField from 'components/admin/UserField';
import { UsrUserVO } from 'api/UsrUserVO';

type OwnProps = {
    submitting: boolean;
    /** Nadpis sekce; ne název pole formuláře (to je `nameFormSection`). */
    sectionTitle?: MessageDescriptor;
    nameFormSection?: string; // název pro FormSection
    types: ApTypeVO[];
    hideType?: boolean;
}

type Props = {} & OwnProps;

const BaseFilterSection = ({submitting, nameFormSection = "", sectionTitle = filterMessages.sectionBase, types = [], hideType = false}: Props) => {
    const intl = useIntl();


    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section"><FormattedMessage {...sectionTitle} /></span>
        {!hideType && <FieldArray
            name="types"
            component={TypesField}
            label={intl.formatMessage(filterMessages.type)}
            disabled={submitting}
            items={types}
        />}
        <Field name="id"
               type="text"
               component={FormInputField}
               label={intl.formatMessage(filterMessages.id)}
               disabled={submitting}
        />
        <Field name="user"
               type="text"
               component={FormInputField}
               label={intl.formatMessage(filterMessages.user)}
               disabled={submitting}
        />
        <Field name="assignedTo"
            type="text"
            component={({input, meta}) => {
                function handleChange(user: UsrUserVO){
                    input.onChange(user?.id);
                }

                //@ts-expect-error Wrong types on FormInputField
                return <FormInputField
                    type="static"
                    label={intl.formatMessage(filterMessages.assignedTo)}
                >
                    <UserField
                        {...input}
                        {...meta}
                        onChange={handleChange}
                        disabled={submitting}
                        all={true}
                    />
                </FormInputField>
            }}
            label={intl.formatMessage(filterMessages.assignedTo)}
            disabled={submitting}
        />
        <Field name="syncState"
               type="select"
               component={FormInputField}
               label={intl.formatMessage(filterMessages.syncState)}
               disabled={submitting}
        >
            <option value={undefined}/>
            {[SyncState.SyncOk, SyncState.NotSynced].map((value) => {
                return <option value={value}>
                    {intl.formatMessage(messageFor(syncStateMessages, value, syncStateMessages.NOT_SYNCED))}
                </option>
            })}</Field>
        <Field name="validationResult"
               type="select"
               component={FormInputField}
               label={intl.formatMessage(filterMessages.validationResult)}
               disabled={submitting}
        >
            <option value={undefined}/>
            {["ok", "error"].map((value) => {
                return <option value={value}>
                    {intl.formatMessage(messageFor(validationResultMessages, value, validationResultMessages.ok))}
                </option>
            })}</Field>
    </FormSection>
};

export default BaseFilterSection;
