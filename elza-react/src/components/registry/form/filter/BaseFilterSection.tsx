import React from 'react';
import {Field, FieldArray, FormSection} from 'redux-form';
import i18n from "../../../i18n";
import {FormInputField} from "../../../shared";
import {ApTypeVO} from "../../../../api/ApTypeVO";
import { StatesField } from 'components/registry/field/StatesField';
import {TypesField} from "../../field/TypesField";

type OwnProps = {
    submitting: boolean;
    name?: string;
    nameFormSection?: string; // název pro FormSection
    types: ApTypeVO[];
}

type Props = {} & OwnProps;

const BaseFilterSection = ({submitting, nameFormSection = "", name = 'ap.ext-search.section.base', types = []}: Props) => {


    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section">{i18n(name)}</span>
        <FieldArray
            name="types"
            component={TypesField}
            label={i18n('ap.state.title.type')}
            disabled={submitting}
            items={types}
        />
        <FieldArray
            name="states"
            component={StatesField}
            label={i18n('ap.ext-search.state')}
            disabled={submitting}
        />
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
    </FormSection>
};

export default BaseFilterSection;
