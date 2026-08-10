import { useEffect } from 'react';
import {
    Combobox,
    Dropdown,
    Field as FluentField,
    InteractionTag,
    InteractionTagPrimary,
    InteractionTagSecondary,
    Input,
    Option,
    TagGroup,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { Field, Form } from 'react-final-form';
import { defineMessages, useIntl } from 'react-intl';
import { useSelector } from 'react-redux';
import { AccessPointPicker } from 'components/registry';
import { outputTypesFetchIfNeeded } from 'actions/refTables/outputTypes';
import { templatesFetchIfNeeded } from 'actions/refTables/templates';
import { WebApi } from 'actions/index';
import { useThunkDispatch } from 'utils/hooks';
import { AppState, OutputType, Template } from 'typings/store';
import { ArrOutputVO } from 'typings/Outputs';
import { AutoSave } from 'components/shared/form/FinalFormAutoSave';
import { AppFetchingStore } from 'typings/globals';
import { ApAccessPointVO } from 'api';

const messages = defineMessages({
    name: {
        id: 'arr.output.form.name',
        defaultMessage: 'Název výstupu',
    },
    internalCode: {
        id: 'arr.output.form.internalCode',
        defaultMessage: 'Interní kód výstupu',
    },
    outputType: {
        id: 'arr.output.form.outputType',
        defaultMessage: 'Typ výstupu',
    },
    template: {
        id: 'arr.output.form.template',
        defaultMessage: 'Šablona',
    },
    templatePlaceholder: {
        id: 'arr.output.form.templatePlaceholder',
        defaultMessage: 'Přidat šablonu',
    },
    outputFilter: {
        id: 'arr.output.form.outputFilter',
        defaultMessage: 'Výstupní filtr',
    },
    outputFilterPlaceholder: {
        id: 'arr.output.form.outputFilterPlaceholder',
        defaultMessage: 'Vyberte filtr',
    },
    anonymizedAp: {
        id: 'arr.output.form.anonymizedAp',
        defaultMessage: 'Anonymizované AP',
    },
    required: {
        id: 'arr.output.form.required',
        defaultMessage: 'Toto pole je povinné',
    },
    unknownOutputType: {
        id: 'arr.output.form.unknownOutputType',
        defaultMessage: 'Neznámý',
    },
    unknownTemplate: {
        id: 'arr.output.form.unknownTemplate',
        defaultMessage: 'Neznámá šablona',
    },
    templateRemove: {
        id: 'arr.output.form.templateRemove',
        defaultMessage: 'Odebrat šablonu',
    },
});

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
    fullWidth: {
        width: '100%',
    },
    tags: {
        marginTop: tokens.spacingVerticalXS,
    },
});

interface Props {
    output?: ArrOutputVO & AppFetchingStore & { subNodeForm: any; lockDate: any }; // TODO - otypovat
    disabled?: boolean;
    onSave?: (values: Partial<ArrOutputVO>) => void;
}

interface Fields {
    name: string;
    internalCode?: string;
    outputFilterId?: number;
    anonymizedAp?: ApAccessPointVO; // TODO - otypovat
}

export default function OutputInlineForm({ output, disabled, onSave }: Props) {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const dispatch = useThunkDispatch();

    const initialValues: Fields = {
        name: output.name,
        internalCode: output.internalCode,
        outputFilterId: output.outputFilterId,
        anonymizedAp: output.anonymizedAp,
    };

    const outputTypeId = output.outputTypeId;
    const outputType = useSelector(
        ({ refTables }: AppState) => refTables.outputTypes.items.find(({ id }) => id === outputTypeId) || null,
    );
    const outputFilters = useSelector(({ refTables }: AppState) => refTables.outputFilters.data);
    const allTemplates = useSelector(({ refTables }: AppState) => refTables.templates.items);

    const validate = (values: Fields) => {
        const errors: Partial<Record<keyof Fields, string>> = {};

        if (!values.name) {
            errors.name = formatMessage(messages.required);
        }

        return errors;
    };

    useEffect(() => {
        dispatch(outputTypesFetchIfNeeded());

        if (outputType) {
            dispatch(templatesFetchIfNeeded(outputType.code));
        } else {
            dispatch(templatesFetchIfNeeded());
        }
    }, [outputTypeId, outputType, dispatch]);

    const getOutputTemplates = (outputType: OutputType) => {
        const templates: Template[] = [];
        if (outputType) {
            const template = allTemplates[outputType.code];
            if (template && template.fetched) {
                templates.push(...template.items);
            }
        }
        return templates;
    };

    const handleRemoveTemplate = (templateId: number) => {
        WebApi.deleteOutputTemplate(output.id, templateId);
    };

    const handleAddTemplate = (templateId: number) => {
        WebApi.addOutputTemplate(output.id, templateId);
        // Zbytek zařídí websocket
    };

    const getOutputAvailableTemplates = (templates: Template[]) => {
        if (!output.templateIds) {
            return templates;
        }
        return templates.filter(item => output.templateIds.findIndex(id => item.id === id) < 0);
    };

    const handleSubmit = (values: Fields) => {
        const {
            id,
            state,
            error,
            nodes,
            outputTypeId,
            templateIds,
            outputResultIds,
            generatedDate,
            version,
            outputSettings,
            createDate,
            deleteDate,
            scopes,
        } = output;

        const _output: Partial<ArrOutputVO> = {
            ...values,
            id,
            state,
            error,
            nodes,
            outputTypeId,
            templateIds,
            outputResultIds,
            generatedDate,
            version,
            outputSettings,
            createDate,
            deleteDate,
            scopes,
        };
        onSave(_output);
    };

    const outputTypeName = outputType ? outputType.name : formatMessage(messages.unknownOutputType);
    const templates = getOutputTemplates(outputType);
    const availableTemplates = getOutputAvailableTemplates(templates);
    const selectedTemplateIds = output.templateIds || [];

    return (
        <Form<Fields>
            initialValues={initialValues}
            onSubmit={handleSubmit}
            validate={validate}
            validateOnBlur={true}
        >
            {({ form }) => (
                <div className={styles.root}>
                    <AutoSave />
                    <Field<string> name="name">
                        {({ input, meta }) => {
                            const showError = meta.touched && meta.error != null;
                            return (
                                <FluentField
                                    label={formatMessage(messages.name)}
                                    required
                                    validationState={showError ? 'error' : 'none'}
                                    validationMessage={showError ? meta.error : undefined}
                                >
                                    <Input
                                        value={input.value}
                                        disabled={disabled}
                                        onChange={(_event, data) => input.onChange(data.value)}
                                        onBlur={input.onBlur}
                                    />
                                </FluentField>
                            );
                        }}
                    </Field>
                    <Field<string> name="internalCode">
                        {({ input }) => (
                            <FluentField label={formatMessage(messages.internalCode)}>
                                <Input
                                    value={input.value ?? ''}
                                    disabled={disabled}
                                    onChange={(_event, data) => input.onChange(data.value)}
                                    onBlur={input.onBlur}
                                />
                            </FluentField>
                        )}
                    </Field>
                    <FluentField label={formatMessage(messages.outputType)}>
                        <Input value={outputTypeName} disabled />
                    </FluentField>
                    <FluentField label={formatMessage(messages.template)}>
                        <Combobox
                            className={styles.fullWidth}
                            placeholder={formatMessage(messages.templatePlaceholder)}
                            selectedOptions={[]}
                            value=""
                            disabled={disabled}
                            onOptionSelect={(_event, data) => {
                                if (data.optionValue) {
                                    handleAddTemplate(parseInt(data.optionValue));
                                }
                            }}
                        >
                            {availableTemplates.map(template => (
                                <Option key={template.id} value={template.id.toString()} text={template.name}>
                                    {template.name}
                                </Option>
                            ))}
                        </Combobox>
                        {selectedTemplateIds.length > 0 && (
                            <TagGroup
                                className={styles.tags}
                                onDismiss={(_event, data) => handleRemoveTemplate(Number(data.value))}
                            >
                                {selectedTemplateIds.map(templateId => {
                                    const template = templates.find(temp => temp.id === templateId);
                                    const label = template ? template.name : formatMessage(messages.unknownTemplate);
                                    return (
                                        <InteractionTag key={templateId} value={templateId.toString()}>
                                            <InteractionTagPrimary
                                                hasSecondaryAction={!disabled}
                                            >
                                                {label}
                                            </InteractionTagPrimary>
                                            {!disabled && (
                                                <InteractionTagSecondary
                                                    aria-label={formatMessage(messages.templateRemove)}
                                                />
                                            )}
                                        </InteractionTag>
                                    );
                                })}
                            </TagGroup>
                        )}
                    </FluentField>
                    <Field<number | undefined> name="outputFilterId">
                        {({ input }) => {
                            const hasValue = typeof input.value === 'number';
                            const selectedFilter = hasValue
                                ? outputFilters?.find(filter => filter.id === input.value)
                                : undefined;
                            return (
                                <FluentField label={formatMessage(messages.outputFilter)}>
                                    <Dropdown
                                        placeholder={formatMessage(messages.outputFilterPlaceholder)}
                                        disabled={disabled}
                                        clearable
                                        selectedOptions={hasValue ? [input.value.toString()] : []}
                                        value={selectedFilter?.name ?? ''}
                                        onFocus={input.onFocus}
                                        onBlur={input.onBlur}
                                        onOptionSelect={(_event, data) => {
                                            input.onChange(
                                                data.optionValue ? parseInt(data.optionValue) : undefined,
                                            );
                                            form.submit();
                                        }}
                                    >
                                        {outputFilters?.map(filter => (
                                            <Option
                                                key={filter.id}
                                                value={filter.id.toString()}
                                                text={filter.name}
                                            >
                                                {filter.name}
                                            </Option>
                                        ))}
                                    </Dropdown>
                                </FluentField>
                            );
                        }}
                    </Field>
                    <Field<ApAccessPointVO | undefined> name="anonymizedAp">
                        {({ input }) => (
                            <FluentField label={formatMessage(messages.anonymizedAp)}>
                                <AccessPointPicker
                                    value={input.value?.id}
                                    onChange={accessPointId => {
                                        input.onChange(accessPointId != null ? { id: accessPointId } : undefined);
                                        form.submit();
                                    }}
                                    clearable
                                    disabled={disabled}
                                />
                            </FluentField>
                        )}
                    </Field>
                </div>
            )}
        </Form>
    );
}
