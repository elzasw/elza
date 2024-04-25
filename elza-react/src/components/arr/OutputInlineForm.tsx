// import PropTypes from 'prop-types';
import { useEffect } from 'react';
import { FormInput, i18n } from 'components/shared';
import { outputTypesFetchIfNeeded } from 'actions/refTables/outputTypes';
import { templatesFetchIfNeeded } from 'actions/refTables/templates';
import RegistryField from '../registry/RegistryField';
import { FormInputField } from '../shared';
import { WebApi } from "actions/index";
import Tags from "components/form/Tags";
import { useThunkDispatch } from 'utils/hooks';
import { Form, Field } from 'react-final-form';
import { useSelector } from 'react-redux';
import { AppState, OutputType, Template } from 'typings/store';
import { ArrOutputVO } from 'typings/Outputs';
import { AutoSave } from 'components/shared/form/FinalFormAutoSave';

interface Props {
    initialValues?: ArrOutputVO;
    disabled?: boolean;
    onSave?: (values: Fields) => void;
}

interface Fields {
    name: string;
    internalCode?: string;
    outputFilterId?: number;
}

export default function OutputInlineForm({
    initialValues,
    disabled,
    onSave,
}: Props) {
    const dispatch = useThunkDispatch();

    const outputTypeId = initialValues.outputTypeId;
    const outputType = useSelector(({ refTables }: AppState) => refTables.outputTypes.items.find(({ id }) => id === outputTypeId) || null);
    const outputFilters = useSelector(({ refTables }: AppState) => refTables.outputFilters.data);
    const allTemplates = useSelector(({ refTables }: AppState) => refTables.templates.items);

    /**
     * Validace formuláře.
     */
    const validate = (values: Fields) => {
        const errors: Partial<Record<keyof Fields, string>> = {};

        if (!values.name) {
            errors.name = i18n('global.validation.required');
        }

        return errors;
    }

    useEffect(() => {
        dispatch(outputTypesFetchIfNeeded());

        if (outputType) {
            dispatch(templatesFetchIfNeeded(outputType.code));
        }
        else {
            dispatch(templatesFetchIfNeeded());
        }
    }, [outputTypeId, outputType, dispatch])

    const getOutputTemplates = (outputType: OutputType) => {
        const templates: Template[] = [];
        if (outputType) {
            const template = allTemplates[outputType.code];
            if (template && template.fetched) {
                templates.push(...template.items);
            }
        }
        return templates;
    }

    const handleRemoveTemplate = (templateId: number) => {
        WebApi.deleteOutputTemplate(initialValues.id, templateId);
    };


    const handleAddTemplate = (templateId: number) => {
        WebApi.addOutputTemplate(initialValues.id, templateId);
        // Zbytek zařídí websocket
    };

    const handleChangeTemplate = (template: Template) => {
        handleAddTemplate(template.id);
    }

    const getOutputAvailableTemplates = (templates: Template[]) => {
        if (!initialValues.templateIds) {
            return templates;
        } else {
            return templates.filter((item) => initialValues.templateIds.findIndex((id) => item.id === id) < 0);
        }
    }

    const handleSubmit = (values: Fields) => {
        onSave(values);
    };

    const outputTypeName = outputType ? outputType.name : "Unknown";
    const templates = getOutputTemplates(outputType);
    const availableTemplates = getOutputAvailableTemplates(templates);

    return (
        <Form<Fields>
            initialValues={initialValues}
            onSubmit={handleSubmit}
            validate={validate}
            validateOnBlur={true}
        >
            {() =>
                <div className="edit-output-form-container">
                    <AutoSave />
                    <Field
                        component={FormInputField}
                        type="text"
                        label={i18n('arr.output.name')}
                        disabled={disabled}
                        name={'name'}
                    />
                    <Field
                        component={FormInputField}
                        type="text"
                        label={i18n('arr.output.internalCode')}
                        disabled={disabled}
                        name={'internalCode'}
                    />
                    <FormInput
                        type="text"
                        label={i18n('arr.output.outputType')}
                        disabled={true}
                        value={outputTypeName}
                    />
                    <div>
                        <FormInput
                            type="autocomplete"
                            label={i18n('arr.output.template')}
                            value={null}
                            items={availableTemplates}
                            disabled={disabled}
                            //@ts-expect-error possibly wrong type definition in FormInput
                            onChange={handleChangeTemplate}
                        />
                        <Tags disabled={disabled} items={initialValues.templateIds || []} onRemove={(item) => handleRemoveTemplate(item)} renderItem={({ item }) => {
                            const templateId = item;
                            const template = templates.find((temp) => temp.id === templateId);
                            return template ? template.name : "Unknown template";
                        }} />
                    </div>
                    <Field
                        component={FormInputField}
                        type="select"
                        label={i18n('arr.output.outputFilter')}
                        name={'outputFilterId'}
                        disabled={disabled}
                    >
                        <option key="-outputFilterId" />
                        {outputFilters &&
                            outputFilters.map(i => (
                                <option key={i.id} value={i.id}>
                                    {i.name}
                                </option>
                            ))}
                    </Field>
                    <Field
                        component={FormInputField}
                        as={RegistryField}
                        type="simple"
                        name={'anonymizedAp'}
                        label={i18n('arr.output.title.anonymizedAp')}
                        addEmpty={true}
                        emptyTitle={i18n('arr.output.title.anonymizedAp.remove')}
                        disabled={disabled}
                    />
                </div>
            }
        </Form>
    );
}
