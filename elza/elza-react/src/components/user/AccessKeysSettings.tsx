import { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Button,
    Card,
    CardHeader,
    Checkbox,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Field,
    Input,
    MessageBar,
    MessageBarBody,
    MessageBarTitle,
    Select,
    Text,
    Tooltip,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    AddRegular,
    CheckmarkCircleRegular,
    CopyRegular,
    DeleteRegular,
    SubtractCircleRegular,
} from '@fluentui/react-icons';
import { Api } from 'api';
import { ApiKeyCreated, ApiKeyInfo, ApiKeyState } from 'elza-api';
import { globalMessages } from 'components/shared/lang';
import { useConfirmModal } from 'components/shared/dialog/useConfirmModal';
import { keyMessages } from './messages';
import { Form as FinalForm, Field as FinalField } from 'react-final-form';
import { defineMessages, FormattedDate, FormattedMessage, useIntl } from 'react-intl';

/**
 * Days until a key is marked visually as "about to expire".
 * Matches the design doc's 30-day warning window.
 */
const EXPIRING_SOON_DAYS = 30;

type ValidityPreset = '30' | '90' | '365' | 'custom';

interface CreateKeyFields {
    name: string;
    validity: ValidityPreset;
    /** Filled only when validity === 'custom'. Yyyy-mm-dd from <input type="date">. */
    expireDate?: string;
}

const messages = defineMessages({
    sectionHint: {
        id: 'userSettings.accessKeys.hint',
        defaultMessage:
            'Klíč slouží pro přihlášení integrací do Elzy prostřednictvím hlavičky X-API-Key. Plnou hodnotu klíče uvidíte pouze jednou při vytvoření.',
    },
    revokeKey: {
        id: 'userSettings.accessKeys.revoke',
        defaultMessage: 'Zrušit',
    },
    revokeConfirmTitle: {
        id: 'userSettings.accessKeys.revoke.title',
        defaultMessage: 'Zrušit klíč',
    },
    revokeConfirmMessage: {
        id: 'userSettings.accessKeys.revoke.message',
        defaultMessage: 'Přejete si zrušit klíč "{name}"? Tato akce je nevratná.',
    },
    empty: {
        id: 'userSettings.accessKeys.empty',
        defaultMessage: 'Nemáte žádné přístupové klíče.',
    },
    columnName: {
        id: 'userSettings.accessKeys.column.name',
        defaultMessage: 'Název',
    },
    columnCreated: {
        id: 'userSettings.accessKeys.column.created',
        defaultMessage: 'Vytvořeno',
    },
    columnExpires: {
        id: 'userSettings.accessKeys.column.expires',
        defaultMessage: 'Platnost do',
    },
    columnLastUsed: {
        id: 'userSettings.accessKeys.column.lastUsed',
        defaultMessage: 'Naposledy použito',
    },
    columnState: {
        id: 'userSettings.accessKeys.column.state',
        defaultMessage: 'Stav',
    },
    lastUsedNever: {
        id: 'userSettings.accessKeys.lastUsedNever',
        defaultMessage: 'nikdy',
    },
    stateActive: {
        id: 'userSettings.accessKeys.state.active',
        defaultMessage: 'Aktivní',
    },
    stateExpired: {
        id: 'userSettings.accessKeys.state.expired',
        defaultMessage: 'Expirovaný',
    },
    stateRevoked: {
        id: 'userSettings.accessKeys.state.revoked',
        defaultMessage: 'Zrušený',
    },
    createFieldName: {
        id: 'userSettings.accessKeys.create.name',
        defaultMessage: 'Název',
    },
    createFieldValidity: {
        id: 'userSettings.accessKeys.create.validity',
        defaultMessage: 'Platnost',
    },
    createFieldExpireDate: {
        id: 'userSettings.accessKeys.create.expireDate',
        defaultMessage: 'Datum konce platnosti',
    },
    validity30: {
        id: 'userSettings.accessKeys.validity.30',
        defaultMessage: '30 dní',
    },
    validity90: {
        id: 'userSettings.accessKeys.validity.90',
        defaultMessage: '90 dní',
    },
    validity365: {
        id: 'userSettings.accessKeys.validity.365',
        defaultMessage: '1 rok',
    },
    validityCustom: {
        id: 'userSettings.accessKeys.validity.custom',
        defaultMessage: 'Vlastní datum',
    },
    createSubmit: {
        id: 'userSettings.accessKeys.create.submit',
        defaultMessage: 'Vytvořit',
    },
    createdTitle: {
        id: 'userSettings.accessKeys.created.title',
        defaultMessage: 'Klíč vytvořen',
    },
    createdWarning: {
        id: 'userSettings.accessKeys.created.warning',
        defaultMessage: 'Zkopírujte si klíč nyní. Po zavření dialogu jej již nebude možné zobrazit.',
    },
    createdTokenLabel: {
        id: 'userSettings.accessKeys.created.tokenLabel',
        defaultMessage: 'Token',
    },
    copyTooltip: {
        id: 'userSettings.accessKeys.copyTooltip',
        defaultMessage: 'Kopírovat do schránky',
    },
    createFailedFallback: {
        id: 'userSettings.accessKeys.create.failedFallback',
        defaultMessage: 'Klíč se nepodařilo vytvořit.',
    },
    filterShowAll: {
        id: 'userSettings.accessKeys.filter.showAll',
        defaultMessage: 'Zobrazit neaktivní',
    },
});

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
        paddingTop: tokens.spacingVerticalM,
        paddingBottom: tokens.spacingVerticalM,
    },
    hint: {
        color: tokens.colorNeutralForeground3,
    },
    toolbar: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalL,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
        fontStyle: 'italic',
    },
    list: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
    },
    inactive: {
        opacity: 0.6,
    },
    keyId: {
        fontFamily: tokens.fontFamilyMonospace,
        color: tokens.colorNeutralForeground3,
    },
    headerRow: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalXS,
    },
    stateIconActive: {
        display: 'flex',
        color: tokens.colorPaletteGreenForeground1,
    },
    stateIconInactive: {
        display: 'flex',
        color: tokens.colorNeutralForeground3,
    },
    props: {
        display: 'flex',
        flexWrap: 'wrap',
        columnGap: tokens.spacingHorizontalL,
        rowGap: tokens.spacingVerticalXXS,
        alignItems: 'baseline',
    },
    prop: {
        display: 'inline-flex',
        gap: tokens.spacingHorizontalXS,
        alignItems: 'baseline',
    },
    propLabel: {
        color: tokens.colorNeutralForeground3,
    },
    warningSoon: {
        color: tokens.colorPaletteYellowForeground1,
    },
    formFields: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
    },
    formActions: {
        display: 'flex',
        justifyContent: 'flex-end',
        gap: tokens.spacingHorizontalS,
        paddingTop: tokens.spacingVerticalXS,
    },
    tokenRow: {
        display: 'flex',
        gap: tokens.spacingHorizontalXS,
        alignItems: 'flex-end',
    },
    tokenInput: {
        flexGrow: 1,
        fontFamily: tokens.fontFamilyMonospace,
    },
});

function addDaysIso(days: number): string {
    const d = new Date();
    d.setDate(d.getDate() + days);
    return d.toISOString().slice(0, 10);
}

function stateOrder(state: ApiKeyState): number {
    switch (state) {
        case ApiKeyState.Active:
            return 0;
        case ApiKeyState.Expired:
            return 1;
        case ApiKeyState.Revoked:
            return 2;
        default:
            return 3;
    }
}

export function AccessKeysSettings() {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const confirm = useConfirmModal();

    const [keys, setKeys] = useState<ApiKeyInfo[]>([]);
    const [isCreating, setIsCreating] = useState(false);
    const [created, setCreated] = useState<ApiKeyCreated | null>(null);
    const [showAll, setShowAll] = useState(false);

    const load = useCallback(async () => {
        const { data } = await Api.user.apiKeysList(showAll || undefined);
        setKeys(data);
    }, [showAll]);

    useEffect(() => {
        load();
    }, [load]);

    const sortedKeys = useMemo(
        () =>
            [...keys].sort((a, b) => {
                const so = stateOrder(a.state) - stateOrder(b.state);
                if (so !== 0) return so;
                return b.createDate.localeCompare(a.createDate);
            }),
        [keys]
    );

    const stateMessage = (state: ApiKeyState) =>
        state === ApiKeyState.Active
            ? messages.stateActive
            : state === ApiKeyState.Expired
              ? messages.stateExpired
              : messages.stateRevoked;

    const daysUntilExpire = (expireDate: string) => {
        const now = Date.now();
        return Math.floor((new Date(expireDate).getTime() - now) / (1000 * 60 * 60 * 24));
    };

    const handleCreated = (data: ApiKeyCreated) => {
        setCreated(data);
        setIsCreating(false);
        load();
    };

    const handleRevoke = (info: ApiKeyInfo) => async () => {
        const ok = await confirm({
            title: formatMessage(messages.revokeConfirmTitle),
            message: formatMessage(messages.revokeConfirmMessage, { name: info.name }),
            destructive: true,
        });
        if (!ok) return;
        await Api.user.apiKeysRevoke(info.id);
        load();
    };

    const copyToken = async () => {
        if (!created) return;
        try {
            await navigator.clipboard.writeText(created.token);
        } catch (e) {
            console.error('Failed to copy token to clipboard', e);
        }
    };

    return (
        <div className={styles.root}>
            <Text size={200} className={styles.hint}>
                <FormattedMessage {...messages.sectionHint} />
            </Text>
            <div className={styles.toolbar}>
                {!isCreating && (
                    <Button
                        appearance="primary"
                        icon={<AddRegular />}
                        onClick={() => setIsCreating(true)}
                    >
                        <FormattedMessage {...keyMessages.addKey} />
                    </Button>
                )}
                <Checkbox
                    checked={showAll}
                    onChange={(_e, d) => setShowAll(!!d.checked)}
                    label={formatMessage(messages.filterShowAll)}
                />
            </div>
            {isCreating && (
                <CreateKeyForm onCancel={() => setIsCreating(false)} onCreated={handleCreated} />
            )}
            {sortedKeys.length === 0 ? (
                <Text size={200} className={styles.empty}>
                    <FormattedMessage {...messages.empty} />
                </Text>
            ) : (
                <div className={styles.list}>
                    {sortedKeys.map((k) => {
                        const inactive = k.state !== ApiKeyState.Active;
                        const soon =
                            k.state === ApiKeyState.Active &&
                            daysUntilExpire(k.expireDate) < EXPIRING_SOON_DAYS;
                        return (
                            <Card
                                key={k.id}
                                appearance="outline"
                                className={inactive ? styles.inactive : undefined}
                            >
                                <CardHeader
                                    header={
                                        <div className={styles.headerRow}>
                                            <Tooltip
                                                content={formatMessage(stateMessage(k.state))}
                                                relationship="label"
                                            >
                                                <span
                                                    className={
                                                        inactive
                                                            ? styles.stateIconInactive
                                                            : styles.stateIconActive
                                                    }
                                                    aria-label={`${formatMessage(messages.columnState)}: ${formatMessage(stateMessage(k.state))}`}
                                                >
                                                    {inactive ? (
                                                        <SubtractCircleRegular />
                                                    ) : (
                                                        <CheckmarkCircleRegular />
                                                    )}
                                                </span>
                                            </Tooltip>
                                            <Text weight="semibold">{k.name}</Text>
                                            <Text size={200} className={styles.keyId}>
                                                {k.keyId}
                                            </Text>
                                        </div>
                                    }
                                    action={
                                        k.state === ApiKeyState.Active ? (
                                            <Tooltip
                                                content={formatMessage(messages.revokeKey)}
                                                relationship="label"
                                            >
                                                <Button
                                                    appearance="subtle"
                                                    icon={<DeleteRegular />}
                                                    onClick={handleRevoke(k)}
                                                />
                                            </Tooltip>
                                        ) : undefined
                                    }
                                />
                                <div className={styles.props}>
                                    <span className={styles.prop}>
                                        <Text size={200} className={styles.propLabel}>
                                            <FormattedMessage {...messages.columnCreated} />
                                        </Text>
                                        <Text size={200}>
                                            <FormattedDate value={k.createDate} />
                                        </Text>
                                    </span>
                                    <span className={styles.prop}>
                                        <Text size={200} className={styles.propLabel}>
                                            <FormattedMessage {...messages.columnExpires} />
                                        </Text>
                                        <Text size={200} className={soon ? styles.warningSoon : undefined}>
                                            <FormattedDate value={k.expireDate} />
                                        </Text>
                                    </span>
                                    <span className={styles.prop}>
                                        <Text size={200} className={styles.propLabel}>
                                            <FormattedMessage {...messages.columnLastUsed} />
                                        </Text>
                                        <Text size={200}>
                                            {k.lastUsedDate ? (
                                                <FormattedDate value={k.lastUsedDate} />
                                            ) : (
                                                <FormattedMessage {...messages.lastUsedNever} />
                                            )}
                                        </Text>
                                    </span>
                                </div>
                            </Card>
                        );
                    })}
                </div>
            )}

            {created && (
                <CreatedKeyDialog
                    created={created}
                    onCopy={copyToken}
                    onClose={() => setCreated(null)}
                />
            )}
        </div>
    );
}

interface CreateKeyFormProps {
    onCancel: () => void;
    onCreated: (created: ApiKeyCreated) => void;
}

function CreateKeyForm({ onCancel, onCreated }: CreateKeyFormProps) {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const [error, setError] = useState<string | null>(null);

    const onSubmit = async ({ name, validity, expireDate }: CreateKeyFields) => {
        setError(null);
        const expireIso =
            validity === 'custom'
                ? expireDate
                    ? new Date(expireDate + 'T23:59:59Z').toISOString()
                    : undefined
                : new Date(addDaysIso(parseInt(validity, 10)) + 'T23:59:59Z').toISOString();
        try {
            // overrideErrorHandler bypasses the global 400 toaster in api.ts so we can render the
            // real server message inline in the form, next to the fields that caused it.
            const { data } = await Api.user.apiKeysCreate(
                { name, expireDate: expireIso },
                { overrideErrorHandler: true },
            );
            onCreated(data);
        } catch (e: unknown) {
            const serverMessage = (e as { response?: { data?: { message?: string } } })?.response
                ?.data?.message;
            setError(serverMessage ?? formatMessage(messages.createFailedFallback));
        }
    };

    return (
        <FinalForm<CreateKeyFields> onSubmit={onSubmit} initialValues={{ name: '', validity: '365' }}>
            {({ handleSubmit, submitting, values }) => (
                <Card className={styles.formFields} appearance="outline">
                    {error && (
                        <MessageBar intent="error">
                            <MessageBarBody>{error}</MessageBarBody>
                        </MessageBar>
                    )}
                    <FinalField
                        name="name"
                        render={({ input }) => (
                            <Field label={formatMessage(messages.createFieldName)} required>
                                <Input
                                    disabled={submitting}
                                    value={input.value ?? ''}
                                    onChange={(_e, d) => input.onChange(d.value)}
                                    maxLength={250}
                                />
                            </Field>
                        )}
                    />
                    <FinalField
                        name="validity"
                        render={({ input }) => (
                            <Field label={formatMessage(messages.createFieldValidity)}>
                                <Select
                                    disabled={submitting}
                                    value={String(input.value)}
                                    onChange={(_e, d) => input.onChange(d.value)}
                                >
                                    <option value="30">{formatMessage(messages.validity30)}</option>
                                    <option value="90">{formatMessage(messages.validity90)}</option>
                                    <option value="365">{formatMessage(messages.validity365)}</option>
                                    <option value="custom">{formatMessage(messages.validityCustom)}</option>
                                </Select>
                            </Field>
                        )}
                    />
                    {values.validity === 'custom' && (
                        <FinalField
                            name="expireDate"
                            render={({ input }) => (
                                <Field label={formatMessage(messages.createFieldExpireDate)} required>
                                    <Input
                                        type="date"
                                        disabled={submitting}
                                        value={input.value ?? ''}
                                        onChange={(_e, d) => input.onChange(d.value)}
                                    />
                                </Field>
                            )}
                        />
                    )}
                    <div className={styles.formActions}>
                        <Button appearance="secondary" disabled={submitting} onClick={onCancel}>
                            <FormattedMessage {...globalMessages.cancel} />
                        </Button>
                        <Button
                            appearance="primary"
                            disabled={
                                submitting ||
                                !values.name?.trim() ||
                                (values.validity === 'custom' && !values.expireDate)
                            }
                            onClick={handleSubmit}
                        >
                            <FormattedMessage {...messages.createSubmit} />
                        </Button>
                    </div>
                </Card>
            )}
        </FinalForm>
    );
}

interface CreatedKeyDialogProps {
    created: ApiKeyCreated;
    onCopy: () => void;
    onClose: () => void;
}

function CreatedKeyDialog({ created, onCopy, onClose }: CreatedKeyDialogProps) {
    const styles = useStyles();
    const { formatMessage } = useIntl();

    return (
        // modalType="alert" so the dialog can only be closed via the button — clicks outside
        // must not silently discard the one and only view of the token.
        <Dialog open modalType="alert">
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>
                        <FormattedMessage {...messages.createdTitle} />
                    </DialogTitle>
                    <DialogContent>
                        <div className={styles.formFields}>
                            <MessageBar intent="warning">
                                <MessageBarBody>
                                    <MessageBarTitle>{created.apiKey.name}</MessageBarTitle>
                                    <FormattedMessage {...messages.createdWarning} />
                                </MessageBarBody>
                            </MessageBar>
                            <Field label={formatMessage(messages.createdTokenLabel)}>
                                <div className={styles.tokenRow}>
                                    <Input
                                        className={styles.tokenInput}
                                        value={created.token}
                                        readOnly
                                        onFocus={(e) => e.currentTarget.select()}
                                    />
                                    <Tooltip
                                        content={formatMessage(messages.copyTooltip)}
                                        relationship="label"
                                    >
                                        <Button icon={<CopyRegular />} onClick={onCopy} />
                                    </Tooltip>
                                </div>
                            </Field>
                        </div>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="primary" onClick={onClose}>
                            <FormattedMessage {...globalMessages.close} />
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}
