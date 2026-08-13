import { useEffect, useState } from 'react';
import {
    Accordion,
    AccordionHeader,
    AccordionItem,
    AccordionPanel,
    Badge,
    Button,
    Caption1,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Field as FluentField,
    Input,
    Menu,
    MenuButton,
    MenuItem,
    MenuList,
    MenuPopover,
    MenuTrigger,
    MessageBar,
    MessageBarBody,
    MessageBarTitle,
    Tag,
    TagGroup,
    Title3,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    AddRegular,
    ArrowUndoRegular,
    CopyRegular,
    DeleteRegular,
    EditRegular,
    ErrorCircle12Filled,
    MoreHorizontalRegular,
    PlayRegular,
} from '@fluentui/react-icons';
import { Field, Form } from 'react-final-form';
import { FormattedDate, FormattedTime, MessageDescriptor, defineMessages, useIntl } from 'react-intl';
import { useSelector } from 'react-redux';
import { AccessPointPicker } from 'components/registry';
import { outputTypesFetchIfNeeded } from 'actions/refTables/outputTypes';
import { templatesFetchIfNeeded } from 'actions/refTables/templates';
import { fundOutputGenerate, fundOutputDelete, fundOutputClone, fundOutputRevert } from 'actions/arr/fundOutput';
import { showConfirmDialog } from 'components/shared/dialog';
import { WebApi } from 'actions/index';
import { useThunkDispatch } from 'utils/hooks';
import { AppState, OutputType, Template } from 'typings/store';
import { ApScopeVO, ArrOutputVO } from 'typings/Outputs';
import { AutoSave } from 'components/shared/form/FinalFormAutoSave';
import { ApAccessPointVO } from 'api';
import { ExceptionData } from 'components/shared/exception/Exception';
import FundNodesList from '../FundNodesList';
import { OutputRecommendedActionsBar } from './OutputRecommendedActionsBar';
import { OutputLayoutProps } from './outputLayoutTypes';

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
        defaultMessage: 'Bez filtru',
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
    noTemplate: {
        id: 'arr.output.form.noTemplate',
        defaultMessage: 'Není vybrána šablona',
    },
    noScope: {
        id: 'arr.output.form.noScope',
        defaultMessage: 'Bez omezení',
    },
    editDetails: {
        id: 'arr.output.form.editDetails',
        defaultMessage: 'Upravit údaje výstupu',
    },
    actionsMenu: {
        id: 'arr.output.form.actionsMenu',
        defaultMessage: 'Akce výstupu',
    },
    editAction: {
        id: 'arr.output.form.editAction',
        defaultMessage: 'Upravit',
    },
    deleteAction: {
        id: 'arr.output.form.deleteAction',
        defaultMessage: 'Smazat',
    },
    copyAction: {
        id: 'arr.output.form.copyAction',
        defaultMessage: 'Kopírovat',
    },
    revertAction: {
        id: 'arr.output.form.revertAction',
        defaultMessage: 'Vrátit do přípravy',
    },
    deleteConfirm: {
        id: 'arr.output.form.deleteConfirm',
        defaultMessage: 'Opravdu chcete smazat výstup?',
    },
    editDialogTitle: {
        id: 'arr.output.form.editDialogTitle',
        defaultMessage: 'Úprava výstupu',
    },
    save: {
        id: 'arr.output.form.save',
        defaultMessage: 'Uložit',
    },
    cancel: {
        id: 'arr.output.form.cancel',
        defaultMessage: 'Zrušit',
    },
    errorTitle: {
        id: 'arr.output.form.errorTitle',
        defaultMessage: 'Chyba výstupu',
    },
    scopeAdd: {
        id: 'arr.output.form.scopeAdd',
        defaultMessage: 'Přidat oblast',
    },
    scopeRemove: {
        id: 'arr.output.form.scopeRemove',
        defaultMessage: 'Odebrat oblast',
    },
    generatedAt: {
        id: 'arr.output.form.generatedAt',
        defaultMessage: 'Vygenerováno',
    },
    generateOutput: {
        id: 'arr.output.form.generateOutput',
        defaultMessage: 'Vygenerovat výstup',
    },
    scopesLabel: {
        id: 'arr.output.form.scopesLabel',
        defaultMessage: 'Omezení na oblasti přístupových bodů',
    },
    nodesLabel: {
        id: 'arr.output.form.nodesLabel',
        defaultMessage: 'Napojení na AS',
    },
    errorProperties: {
        id: 'arr.output.form.errorProperties',
        defaultMessage: 'Rozšiřující parametry',
    },
    errorStack: {
        id: 'arr.output.form.errorStack',
        defaultMessage: 'Stack',
    },
    stateOpen: {
        id: 'arr.output.form.stateOpen',
        defaultMessage: 'V přípravě',
    },
    stateComputing: {
        id: 'arr.output.form.stateComputing',
        defaultMessage: 'Přepočítává se',
    },
    stateGenerating: {
        id: 'arr.output.form.stateGenerating',
        defaultMessage: 'Generuje se',
    },
    stateFinished: {
        id: 'arr.output.form.stateFinished',
        defaultMessage: 'Dokončeno',
    },
    stateOutdated: {
        id: 'arr.output.form.stateOutdated',
        defaultMessage: 'Neaktuální',
    },
});

type BadgeColor = 'informative' | 'warning' | 'success' | 'danger' | 'subtle';

const STATE_BADGE: Record<string, { label: MessageDescriptor; color: BadgeColor }> = {
    OPEN: { label: messages.stateOpen, color: 'informative' },
    COMPUTING: { label: messages.stateComputing, color: 'warning' },
    GENERATING: { label: messages.stateGenerating, color: 'warning' },
    FINISHED: { label: messages.stateFinished, color: 'success' },
    OUTDATED: { label: messages.stateOutdated, color: 'danger' },
};

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
    settings: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
    templateRow: {
        marginTop: tokens.spacingVerticalXS,
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        gap: tokens.spacingHorizontalXS,
    },
    tags: {
        display: 'contents',
    },
    subtleTag: {
        color: tokens.colorNeutralForeground3,
        fontStyle: 'italic',
    },
    noTemplateTag: {
        backgroundColor: tokens.colorStatusDangerBackground1,
        color: tokens.colorStatusDangerForeground1,
        border: `1px solid ${tokens.colorStatusDangerBorder1}`,
        '& .fui-Tag__icon': {
            color: tokens.colorStatusDangerForeground1,
            width: '24px',
            height: '24px',
            fontSize: '24px',
            '& svg': {
                width: '24px',
                height: '24px',
            },
        },
    },
    detailsHeader: {
        display: 'flex',
        alignItems: 'flex-start',
        columnGap: tokens.spacingHorizontalS,
    },
    detailsFields: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalXS,
        flex: '1 1 auto',
        minWidth: 0,
    },
    title: {
        wordBreak: 'break-word',
    },
    subtitle: {
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap',
        columnGap: tokens.spacingHorizontalS,
        rowGap: tokens.spacingVerticalXXS,
        color: tokens.colorNeutralForeground3,
    },
    internalCode: {
        color: tokens.colorNeutralForeground3,
    },
    dialogFields: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
    errorDetails: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalXS,
        marginTop: tokens.spacingVerticalXS,
        alignItems: 'flex-start',
    },
    errorText: {
        margin: 0,
        marginTop: tokens.spacingVerticalXXS,
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
        fontFamily: tokens.fontFamilyMonospace,
        fontSize: tokens.fontSizeBase200,
        maxHeight: '240px',
        overflowY: 'auto',
    },
});

interface DetailsFields {
    name: string;
    internalCode?: string;
}

interface SettingsFields {
    outputFilterId?: number;
    anonymizedAp?: ApAccessPointVO; // TODO - otypovat
}

interface DetailsDialogProps {
    open: boolean;
    initialValues: DetailsFields;
    onClose: () => void;
    onSubmit: (values: DetailsFields) => void;
}

function OutputDetailsDialog({ open, initialValues, onClose, onSubmit }: DetailsDialogProps) {
    const styles = useStyles();
    const { formatMessage } = useIntl();

    const validate = (values: DetailsFields) => {
        const errors: Partial<Record<keyof DetailsFields, string>> = {};
        if (!values.name) {
            errors.name = formatMessage(messages.required);
        }
        return errors;
    };

    const handleSubmit = (values: DetailsFields) => {
        onSubmit(values);
        onClose();
    };

    return (
        <Dialog open={open} onOpenChange={(_event, data) => !data.open && onClose()}>
            <DialogSurface>
                <Form<DetailsFields> initialValues={initialValues} onSubmit={handleSubmit} validate={validate}>
                    {({ handleSubmit: submit, invalid }) => (
                        <form onSubmit={submit}>
                            <DialogBody>
                                <DialogTitle>{formatMessage(messages.editDialogTitle)}</DialogTitle>
                                <DialogContent className={styles.dialogFields}>
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
                                                    onChange={(_event, data) => input.onChange(data.value)}
                                                    onBlur={input.onBlur}
                                                />
                                            </FluentField>
                                        )}
                                    </Field>
                                </DialogContent>
                                <DialogActions>
                                    <Button appearance="secondary" type="button" onClick={onClose}>
                                        {formatMessage(messages.cancel)}
                                    </Button>
                                    <Button appearance="primary" type="submit" disabled={invalid}>
                                        {formatMessage(messages.save)}
                                    </Button>
                                </DialogActions>
                            </DialogBody>
                        </form>
                    )}
                </Form>
            </DialogSurface>
        </Dialog>
    );
}

function parseExceptionError(error: string): ExceptionData<Record<string, unknown>> | null {
    try {
        const parsed = JSON.parse(error);
        const isException =
            parsed && typeof parsed === 'object' && (parsed.message != null || parsed.code != null);
        return isException ? parsed : null;
    } catch {
        return null;
    }
}

function OutputErrorReadout({ error }: { error: string }) {
    const styles = useStyles();
    const { formatMessage } = useIntl();

    const exception = parseExceptionError(error);
    const hasProperties = exception?.properties && Object.keys(exception.properties).length > 0;

    return (
        <MessageBar intent="error" layout="multiline">
            <MessageBarBody>
                <MessageBarTitle>
                    {exception?.message || formatMessage(messages.errorTitle)}
                </MessageBarTitle>
                {exception ? (
                    <div className={styles.errorDetails}>
                        {exception.code && (
                            <Badge appearance="tint" color="danger">
                                {exception.code}
                            </Badge>
                        )}
                        {(hasProperties || exception.stackTrace) && (
                            <Accordion collapsible>
                                {hasProperties && (
                                    <AccordionItem value="properties">
                                        <AccordionHeader>
                                            {formatMessage(messages.errorProperties)}
                                        </AccordionHeader>
                                        <AccordionPanel>
                                            <pre className={styles.errorText}>
                                                {JSON.stringify(exception.properties, null, 2)}
                                            </pre>
                                        </AccordionPanel>
                                    </AccordionItem>
                                )}
                                {exception.stackTrace && (
                                    <AccordionItem value="stack">
                                        <AccordionHeader>
                                            {formatMessage(messages.errorStack)}
                                        </AccordionHeader>
                                        <AccordionPanel>
                                            <pre className={styles.errorText}>{exception.stackTrace}</pre>
                                        </AccordionPanel>
                                    </AccordionItem>
                                )}
                            </Accordion>
                        )}
                    </div>
                ) : (
                    <pre className={styles.errorText}>{error}</pre>
                )}
            </MessageBarBody>
        </MessageBar>
    );
}

type Props = Pick<
    OutputLayoutProps,
    | 'fundOutputDetail'
    | 'versionId'
    | 'readonly'
    | 'nodesReadOnly'
    | 'connectableScopes'
    | 'outputFiles'
    | 'onSaveOutput'
    | 'onAddScope'
    | 'onRemoveScope'
    | 'onAddNodes'
    | 'onRemoveNode'
>;

/**
 * Definiční sloupec výstupu – hlavička (název/typ/kód + dialog úprav), šablony, filtr,
 * anonymizované AP, chyba, soubory, rozsahy a uzly. Vše v jedné komponentě; pořadí sekcí
 * se mění úpravou tohoto souboru. Rozvržení pouze umisťuje tento sloupec, dovnitř nezasahuje.
 */
export function OutputDefinition({
    fundOutputDetail,
    versionId,
    readonly,
    nodesReadOnly,
    connectableScopes,
    outputFiles,
    onSaveOutput,
    onAddScope,
    onRemoveScope,
    onAddNodes,
    onRemoveNode,
}: Props) {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const dispatch = useThunkDispatch();

    const [dialogOpen, setDialogOpen] = useState(false);

    const settingsInitialValues: SettingsFields = {
        outputFilterId: fundOutputDetail.outputFilterId,
        anonymizedAp: fundOutputDetail.anonymizedAp,
    };

    const outputTypeId = fundOutputDetail.outputTypeId;
    const outputType = useSelector(
        ({ refTables }: AppState) => refTables.outputTypes.items.find(({ id }) => id === outputTypeId) || null,
    );
    const outputFilters = useSelector(({ refTables }: AppState) => refTables.outputFilters.data);
    const allTemplates = useSelector(({ refTables }: AppState) => refTables.templates.items);

    useEffect(() => {
        dispatch(outputTypesFetchIfNeeded());

        if (outputType) {
            dispatch(templatesFetchIfNeeded(outputType.code));
        } else {
            dispatch(templatesFetchIfNeeded());
        }
    }, [outputTypeId, outputType, dispatch]);

    const showsRecommendedActions = fundOutputDetail.state === 'OPEN' || fundOutputDetail.state === 'COMPUTING';

    const canGenerate =
        fundOutputDetail.state === 'OPEN' &&
        (fundOutputDetail.outputResultIds == null || fundOutputDetail.outputResultIds.length === 0) &&
        (fundOutputDetail.templateIds?.length ?? 0) > 0 &&
        (fundOutputDetail.nodes?.length ?? 0) > 0;

    const handleGenerateOutput = () => {
        dispatch(fundOutputGenerate(fundOutputDetail.id));
    };

    const handleDelete = async () => {
        const response = (await dispatch(showConfirmDialog(formatMessage(messages.deleteConfirm)))) as any;
        if (response) {
            dispatch(fundOutputDelete(versionId, fundOutputDetail.id));
        }
    };

    const handleClone = () => {
        dispatch(fundOutputClone(versionId, fundOutputDetail.id));
    };

    const handleRevert = () => {
        dispatch(fundOutputRevert(versionId, fundOutputDetail.id));
    };

    const canRevert = !!fundOutputDetail.generatedDate;

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
        WebApi.deleteOutputTemplate(fundOutputDetail.id, templateId);
    };

    const handleAddTemplate = (templateId: number) => {
        WebApi.addOutputTemplate(fundOutputDetail.id, templateId);
        // Zbytek zařídí websocket
    };

    const getOutputAvailableTemplates = (templates: Template[]) => {
        if (!fundOutputDetail.templateIds) {
            return templates;
        }
        return templates.filter(item => fundOutputDetail.templateIds.findIndex(id => item.id === id) < 0);
    };

    const buildOutput = (overrides: Partial<ArrOutputVO>): Partial<ArrOutputVO> => {
        const {
            id,
            name,
            internalCode,
            outputFilterId,
            anonymizedAp,
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
        } = fundOutputDetail;

        return {
            id,
            name,
            internalCode,
            outputFilterId,
            anonymizedAp,
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
            ...overrides,
        };
    };

    const handleSettingsSubmit = (values: SettingsFields) => {
        onSaveOutput(buildOutput(values));
    };

    const handleDetailsSubmit = (values: DetailsFields) => {
        onSaveOutput(buildOutput(values));
    };

    const outputTypeName = outputType ? outputType.name : formatMessage(messages.unknownOutputType);
    const templates = getOutputTemplates(outputType);
    const availableTemplates = getOutputAvailableTemplates(templates);
    const selectedTemplateIds = fundOutputDetail.templateIds || [];

    return (
        <div className={styles.root}>
            <div className={styles.detailsHeader}>
                <div className={styles.detailsFields}>
                    {fundOutputDetail.internalCode && (
                        <Caption1 className={styles.internalCode}>{fundOutputDetail.internalCode}</Caption1>
                    )}
                    <Title3 className={styles.title}>{fundOutputDetail.name}</Title3>
                    <div className={styles.subtitle}>
                        <Badge appearance="tint" color="informative">
                            {outputTypeName}
                        </Badge>
                        {STATE_BADGE[fundOutputDetail.state] && (
                            <Badge appearance="tint" color={STATE_BADGE[fundOutputDetail.state].color}>
                                {formatMessage(STATE_BADGE[fundOutputDetail.state].label)}
                            </Badge>
                        )}
                        {fundOutputDetail.generatedDate && (
                            <Caption1 className={styles.internalCode}>
                                {formatMessage(messages.generatedAt)}{' '}
                                <FormattedDate value={fundOutputDetail.generatedDate} />{' '}
                                <FormattedTime value={fundOutputDetail.generatedDate} />
                            </Caption1>
                        )}
                    </div>
                </div>
                <Menu>
                    <MenuTrigger disableButtonEnhancement>
                        <MenuButton
                            appearance="subtle"
                            icon={<MoreHorizontalRegular />}
                            aria-label={formatMessage(messages.actionsMenu)}
                        />
                    </MenuTrigger>
                    <MenuPopover>
                        <MenuList>
                            {!readonly && (
                                <MenuItem icon={<EditRegular />} onClick={() => setDialogOpen(true)}>
                                    {formatMessage(messages.editAction)}
                                </MenuItem>
                            )}
                            {canRevert && (
                                <MenuItem icon={<ArrowUndoRegular />} onClick={handleRevert}>
                                    {formatMessage(messages.revertAction)}
                                </MenuItem>
                            )}
                            <MenuItem icon={<CopyRegular />} onClick={handleClone}>
                                {formatMessage(messages.copyAction)}
                            </MenuItem>
                            <MenuItem icon={<DeleteRegular />} onClick={handleDelete}>
                                {formatMessage(messages.deleteAction)}
                            </MenuItem>
                        </MenuList>
                    </MenuPopover>
                </Menu>
            </div>

            <OutputDetailsDialog
                open={dialogOpen}
                initialValues={{ name: fundOutputDetail.name, internalCode: fundOutputDetail.internalCode }}
                onClose={() => setDialogOpen(false)}
                onSubmit={handleDetailsSubmit}
            />

            {fundOutputDetail.error && <OutputErrorReadout error={fundOutputDetail.error} />}

            <OutputRecommendedActionsBar
                outputId={fundOutputDetail.id}
                versionId={versionId}
                active={showsRecommendedActions}
                readonly={readonly}
            />

            {canGenerate && (
                <div>
                    <Button appearance="primary" icon={<PlayRegular />} onClick={handleGenerateOutput}>
                        {formatMessage(messages.generateOutput)}
                    </Button>
                </div>
            )}

            {outputFiles}

            <Form<SettingsFields>
                initialValues={settingsInitialValues}
                onSubmit={handleSettingsSubmit}
                validateOnBlur={true}
            >
                {({ form }) => (
                    <div className={styles.settings}>
                        <AutoSave />
                        <FluentField label={formatMessage(messages.template)}>
                            <div className={styles.templateRow}>
                                {selectedTemplateIds.length > 0 && (
                                    <TagGroup
                                        className={styles.tags}
                                        onDismiss={(_event, data) => handleRemoveTemplate(Number(data.value))}
                                    >
                                        {selectedTemplateIds.map(templateId => {
                                            const template = templates.find(temp => temp.id === templateId);
                                            const label = template
                                                ? template.name
                                                : formatMessage(messages.unknownTemplate);
                                            return (
                                                <Tag
                                                    key={templateId}
                                                    value={templateId.toString()}
                                                    dismissible={readonly ? undefined : true}
                                                    dismissIcon={readonly ? undefined : { 'aria-label': formatMessage(messages.templateRemove) }}
                                                >
                                                    {label}
                                                </Tag>
                                            );
                                        })}
                                    </TagGroup>
                                )}
                                {selectedTemplateIds.length === 0 && (
                                    <Tag className={styles.noTemplateTag} icon={<ErrorCircle12Filled />}>

                                        {formatMessage(messages.noTemplate)}
                                    </Tag>
                                )}
                                {!readonly && availableTemplates.length > 0 && (
                                    <Menu>
                                        <MenuTrigger disableButtonEnhancement>
                                            <MenuButton
                                                icon={<AddRegular />}
                                                aria-label={formatMessage(messages.templatePlaceholder)}
                                                title={formatMessage(messages.templatePlaceholder)}
                                            />
                                        </MenuTrigger>
                                        <MenuPopover>
                                            <MenuList>
                                                {availableTemplates.map(template => (
                                                    <MenuItem
                                                        key={template.id}
                                                        onClick={() => handleAddTemplate(template.id)}
                                                    >
                                                        {template.name}
                                                    </MenuItem>
                                                ))}
                                            </MenuList>
                                        </MenuPopover>
                                    </Menu>
                                )}
                            </div>
                        </FluentField>
                        <Field<number | undefined> name="outputFilterId">
                            {({ input }) => {
                                const hasValue = typeof input.value === 'number';
                                const selectedFilter = hasValue
                                    ? outputFilters?.find(filter => filter.id === input.value)
                                    : undefined;
                                const selectFilter = (filterId: number | undefined) => {
                                    input.onChange(filterId);
                                    form.submit();
                                };
                                return (
                                    <FluentField label={formatMessage(messages.outputFilter)}>
                                        <div>
                                            <Menu>
                                                <MenuTrigger disableButtonEnhancement>
                                                    <MenuButton disabled={readonly}>
                                                        {selectedFilter?.name ??
                                                            formatMessage(messages.outputFilterPlaceholder)}
                                                    </MenuButton>
                                                </MenuTrigger>
                                                <MenuPopover>
                                                    <MenuList>
                                                        <MenuItem onClick={() => selectFilter(undefined)}>
                                                            {formatMessage(messages.outputFilterPlaceholder)}
                                                        </MenuItem>
                                                        {outputFilters?.map(filter => (
                                                            <MenuItem
                                                                key={filter.id}
                                                                onClick={() => selectFilter(filter.id)}
                                                            >
                                                                {filter.name}
                                                            </MenuItem>
                                                        ))}
                                                    </MenuList>
                                                </MenuPopover>
                                            </Menu>
                                        </div>
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
                                        disabled={readonly}
                                    />
                                </FluentField>
                            )}
                        </Field>
                    </div>
                )}
            </Form>

            <FluentField label={formatMessage(messages.scopesLabel)}>
                <div className={styles.templateRow}>
                    {(fundOutputDetail.scopes || []).length > 0 && (
                        <TagGroup
                            className={styles.tags}
                            onDismiss={(_event, data) => {
                                const scope = (fundOutputDetail.scopes || []).find(
                                    s => s.id.toString() === data.value,
                                );
                                if (scope) {
                                    onRemoveScope(scope);
                                }
                            }}
                        >
                            {(fundOutputDetail.scopes || []).map(scope => (
                                <Tag
                                    key={scope.id}
                                    value={scope.id.toString()}
                                    dismissible={readonly ? undefined : true}
                                    dismissIcon={
                                        readonly ? undefined : { 'aria-label': formatMessage(messages.scopeRemove) }
                                    }
                                >
                                    {scope.name}
                                </Tag>
                            ))}
                        </TagGroup>
                    )}
                    {(fundOutputDetail.scopes || []).length === 0 && (
                        <Tag className={styles.subtleTag}>{formatMessage(messages.noScope)}</Tag>
                    )}
                    {!readonly && connectableScopes && connectableScopes.length > 0 && (
                        <Menu>
                            <MenuTrigger disableButtonEnhancement>
                                <MenuButton
                                    icon={<AddRegular />}
                                    aria-label={formatMessage(messages.scopeAdd)}
                                    title={formatMessage(messages.scopeAdd)}
                                />
                            </MenuTrigger>
                            <MenuPopover>
                                <MenuList>
                                    {connectableScopes.map((scope: ApScopeVO) => (
                                        <MenuItem key={scope.id} onClick={() => onAddScope(scope)}>
                                            {scope.name}
                                        </MenuItem>
                                    ))}
                                </MenuList>
                            </MenuPopover>
                        </Menu>
                    )}
                </div>
            </FluentField>

            <div>
                <label className="control-label">{formatMessage(messages.nodesLabel)}</label>
                <FundNodesList
                    nodes={fundOutputDetail.nodes}
                    onDeleteNode={onRemoveNode}
                    onAddNode={onAddNodes}
                    readOnly={nodesReadOnly}
                />
            </div>
        </div>
    );
}
