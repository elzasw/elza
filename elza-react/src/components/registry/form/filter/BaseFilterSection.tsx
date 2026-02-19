import React from 'react';
import {Field, FieldArray, FormSection} from 'redux-form';
import i18n from "../../../i18n";
import {FormInputField} from "../../../shared";
import {ApTypeVO} from "../../../../api/ApTypeVO";
import { StatesField } from 'components/registry/field/StatesField';
import {TypesField} from "../../field/TypesField";
import { SyncState } from 'api/SyncState';
import UserField from 'components/admin/UserField';
import { UsrUserVO } from 'api/UsrUserVO';

type OwnProps = {
    submitting: boolean;
    name?: string;
    nameFormSection?: string; // název pro FormSection
    types: ApTypeVO[];
    hideState?: boolean;
    hideType?: boolean;
}

type Props = {} & OwnProps;

const BaseFilterSection = ({submitting, nameFormSection = "", name = 'ap.ext-search.section.base', types = [], hideState = false, hideType = false}: Props) => {


    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section">{i18n(name)}</span>
        {!hideType && <FieldArray
            name="types"
            component={TypesField}
            label={i18n('registry.type')}
            disabled={submitting}
            items={types}
        />}
        {!hideState && <FieldArray
            name="states"
            component={StatesField}
            label={i18n('ap.ext-search.state')}
            disabled={submitting}
        />}
        <Field name="id"
               type="text"
               component={FormInputField}
               label={i18n('ap.ext-search.id')}
               disabled={submitting}
        />
        <Field name="user"
               type="text"
               component={FormInputField}
               label={i18n('ap.ext-search.user')}
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
                    label={i18n('ap.ext-search.assignedTo')}
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
            label={i18n('ap.ext-search.assignedTo')}
            disabled={submitting}
        />
        <Field name="syncState"
               type="select"
               component={FormInputField}
               label={i18n('ap.ext-search.syncState')}
               disabled={submitting}
        >
            <option value={undefined}/>
            {[SyncState.SYNC_OK, SyncState.NOT_SYNCED].map((value) => {
                return <option value={value}>
                    {i18n(`ap.binding.syncState.${value}`)}
                </option>
            })}</Field>
        <Field name="validationResult"
               type="select"
               component={FormInputField}
               label={i18n('ap.ext-search.hasErrors')}
               disabled={submitting}
        >
            <option value={undefined}/>
            {["ok", "error"].map((value) => {
                return <option value={value}>
                    {value === "error" ? i18n('global.title.yes') : i18n('global.title.no')}
                </option>
            })}</Field>
    </FormSection>
};

export default BaseFilterSection;
