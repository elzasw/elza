import {
    Button,
    Checkbox,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Dropdown,
    Field,
    Input,
    Option,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { Api } from 'api';
import { WebApi } from 'actions/index.jsx';
import { CreateBatchDescCsv, CreateBatchEdx, FundImportStrategy, FundPairKey, ImportBatchDescCsv, ImportBatchEdx } from 'elza-api';
import { useEffect, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { FundScope } from '../../../types';
import { getIntl } from 'components/shared/lang/intlInstance';
import { globalMessages } from 'components/shared/lang/messages';
import {
    FUND_IMPORT_STRATEGY_HINT,
    FUND_IMPORT_STRATEGY_MESSAGE,
    FUND_PAIR_KEY_MESSAGE,
    batchSettingsHints,
    batchSettingsMessages,
} from './messages';

interface Props {
    kind: 'EDX' | 'CSV';
    open: boolean;
    onClose: () => void;
    onCreated: (batchId: number) => void;
}

const messages = defineMessages({
    titleEdx: {
        id: 'admin.import.dialog.titleEdx',
        defaultMessage: 'Nová EDX2 dávka',
    },
    titleCsv: {
        id: 'admin.import.dialog.titleCsv',
        defaultMessage: 'Nová CSV dávka',
    },
    name: {
        id: 'admin.import.dialog.name',
        defaultMessage: 'Název',
    },
    separatorSemicolon: {
        id: 'admin.import.dialog.separator.semicolon',
        defaultMessage: '; (středník)',
    },
    separatorComma: {
        id: 'admin.import.dialog.separator.comma',
        defaultMessage: ', (čárka)',
    },
    separatorTab: {
        id: 'admin.import.dialog.separator.tab',
        defaultMessage: '\\t (tabulátor)',
    },
    separatorPipe: {
        id: 'admin.import.dialog.separator.pipe',
        defaultMessage: '| (svislítko)',
    },
    save: {
        id: 'admin.import.dialog.save',
        defaultMessage: 'Založit',
    },
    cancel: {
        id: 'admin.import.dialog.cancel',
        defaultMessage: 'Zrušit',
    },
    none: {
        id: 'admin.import.dialog.none',
        defaultMessage: '— nezvoleno —',
    },
});

const useStyles = makeStyles({
    body: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
});

const ENCODING_OPTIONS = [
    { value: 'UTF-8', label: 'UTF-8' },
    { value: 'windows-1250', label: getIntl().formatMessage(globalMessages.encodingCentralEuropean) },
    { value: 'iso-8859-2', label: 'ISO-8859-2 (Latin-2)' },
    { value: 'windows-1252', label: getIntl().formatMessage(globalMessages.encodingWesternEuropean) },
];

const DEFAULT_SEPARATOR = ';';
const DEFAULT_ENCODING = 'windows-1250';

export function CreateBatchDialog({ kind, open, onClose, onCreated }: Props) {
    const styles = useStyles();
    const intl = useIntl();

    const [name, setName] = useState('');
    const [strategy, setStrategy] = useState<FundImportStrategy>(FundImportStrategy.AlwaysNew);
    const [pairKey, setPairKey] = useState<FundPairKey | ''>('');
    const [ignoreRootNodes, setIgnoreRootNodes] = useState(false);
    const [scopeId, setScopeId] = useState('');
    const [separator, setSeparator] = useState<string>(DEFAULT_SEPARATOR);
    const [encoding, setEncoding] = useState<string>(DEFAULT_ENCODING);
    const [skipError, setSkipError] = useState(false);
    const [keepFiles, setKeepFiles] = useState(false);
    const [saving, setSaving] = useState(false);
    const [scopes, setScopes] = useState<FundScope[]>([]);
    // Pairing settings apply only when the payload may go into an existing fund; ALWAYS_NEW never pairs.
    const pairing = strategy !== FundImportStrategy.AlwaysNew;
    const effectivePairKey = pairing ? pairKey : '';

    useEffect(() => {
        if (!open) return;
        if (kind === 'EDX') {
            WebApi.getAllScopes().then((data: FundScope[]) => setScopes(data ?? []));
        }
    }, [kind, open]);

    const submit = async () => {
        setSaving(true);
        try {
            if (kind === 'EDX') {
                const body: CreateBatchEdx = {
                    name,
                    fundImportStrategy: strategy,
                    fundPairKey: effectivePairKey || undefined,
                    ignoreRootNodes: pairing && ignoreRootNodes,
                    scopeId: scopeId ? Number(scopeId) : undefined,
                    skipError,
                    keepFiles,
                };
                const { data } = await Api.importBatches.importBatchCreateEdx(body);
                onCreated((data as ImportBatchEdx).batchId);
            } else {
                const body: CreateBatchDescCsv = {
                    name,
                    separator,
                    encoding,
                    skipError,
                    keepFiles,
                };
                const { data } = await Api.importBatches.importBatchCreateCsv(body);
                onCreated((data as ImportBatchDescCsv).batchId);
            }
        } finally {
            setSaving(false);
        }
    };

    const title = kind === 'EDX' ? messages.titleEdx : messages.titleCsv;
    const canSubmit = name.trim().length > 0
        && (kind !== 'EDX' || !pairing || pairKey !== '')
        && !saving;

    return (
        <Dialog open={open} onOpenChange={(_, data) => { if (!data.open) onClose(); }}>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle><FormattedMessage {...title} /></DialogTitle>
                    <DialogContent>
                        <div className={styles.body}>
                            <Field label={intl.formatMessage(messages.name)} required>
                                <Input value={name} onChange={(_, d) => setName(d.value)} />
                            </Field>

                            {kind === 'EDX' && (
                                <>
                                    <Field
                                        label={intl.formatMessage(batchSettingsMessages.fundImportStrategy)}
                                        hint={intl.formatMessage(FUND_IMPORT_STRATEGY_HINT[strategy])}
                                        required
                                    >
                                        <Dropdown
                                            value={intl.formatMessage(FUND_IMPORT_STRATEGY_MESSAGE[strategy])}
                                            selectedOptions={[strategy]}
                                            onOptionSelect={(_, d) => d.optionValue && setStrategy(d.optionValue as FundImportStrategy)}
                                        >
                                            {Object.values(FundImportStrategy).map(v => (
                                                <Option key={v} value={v} text={intl.formatMessage(FUND_IMPORT_STRATEGY_MESSAGE[v])}>
                                                    <FormattedMessage {...FUND_IMPORT_STRATEGY_MESSAGE[v]} />
                                                </Option>
                                            ))}
                                        </Dropdown>
                                    </Field>
                                    <Field
                                        label={intl.formatMessage(batchSettingsMessages.fundPairKey)}
                                        hint={intl.formatMessage(batchSettingsHints.fundPairKey)}
                                        required={pairing}
                                    >
                                        <Dropdown
                                            disabled={!pairing}
                                            value={effectivePairKey ? intl.formatMessage(FUND_PAIR_KEY_MESSAGE[effectivePairKey]) : intl.formatMessage(messages.none)}
                                            selectedOptions={effectivePairKey ? [effectivePairKey] : []}
                                            onOptionSelect={(_, d) => setPairKey((d.optionValue as FundPairKey) ?? '')}
                                        >
                                            <Option value="" text={intl.formatMessage(messages.none)}><FormattedMessage {...messages.none} /></Option>
                                            {Object.values(FundPairKey).map(v => (
                                                <Option key={v} value={v} text={intl.formatMessage(FUND_PAIR_KEY_MESSAGE[v])}>
                                                    <FormattedMessage {...FUND_PAIR_KEY_MESSAGE[v]} />
                                                </Option>
                                            ))}
                                        </Dropdown>
                                    </Field>
                                    <Field hint={intl.formatMessage(batchSettingsHints.ignoreRootNodes)}>
                                        <Checkbox checked={pairing && ignoreRootNodes} disabled={!pairing}
                                            onChange={(_, d) => setIgnoreRootNodes(!!d.checked)}
                                            label={intl.formatMessage(batchSettingsMessages.ignoreRootNodes)} />
                                    </Field>
                                    <Field
                                        label={intl.formatMessage(batchSettingsMessages.scope)}
                                        hint={intl.formatMessage(batchSettingsHints.scope)}
                                    >
                                        <Dropdown
                                            value={scopes.find(s => String(s.id) === scopeId)?.name ?? intl.formatMessage(messages.none)}
                                            selectedOptions={scopeId ? [scopeId] : []}
                                            onOptionSelect={(_, d) => setScopeId(d.optionValue ?? '')}
                                        >
                                            <Option value="" text={intl.formatMessage(messages.none)}><FormattedMessage {...messages.none} /></Option>
                                            {scopes.map(s => (
                                                <Option key={s.id} value={String(s.id)} text={s.name}>{s.name}</Option>
                                            ))}
                                        </Dropdown>
                                    </Field>
                                </>
                            )}

                            {kind === 'CSV' && (
                                <>
                                    <Field
                                        label={intl.formatMessage(batchSettingsMessages.separator)}
                                        hint={intl.formatMessage(batchSettingsHints.separator)}
                                    >
                                        <Dropdown
                                            value={separator}
                                            selectedOptions={[separator]}
                                            onOptionSelect={(_, d) => setSeparator(d.optionValue ?? DEFAULT_SEPARATOR)}
                                        >
                                            <Option value=";" text={intl.formatMessage(messages.separatorSemicolon)}>
                                                <FormattedMessage {...messages.separatorSemicolon} />
                                            </Option>
                                            <Option value="," text={intl.formatMessage(messages.separatorComma)}>
                                                <FormattedMessage {...messages.separatorComma} />
                                            </Option>
                                            <Option value="\t" text={intl.formatMessage(messages.separatorTab)}>
                                                <FormattedMessage {...messages.separatorTab} />
                                            </Option>
                                            <Option value="|" text={intl.formatMessage(messages.separatorPipe)}>
                                                <FormattedMessage {...messages.separatorPipe} />
                                            </Option>
                                        </Dropdown>
                                    </Field>
                                    <Field
                                        label={intl.formatMessage(batchSettingsMessages.encoding)}
                                        hint={intl.formatMessage(batchSettingsHints.encoding)}
                                    >
                                        <Dropdown
                                            value={ENCODING_OPTIONS.find(o => o.value === encoding)?.label ?? encoding}
                                            selectedOptions={[encoding]}
                                            onOptionSelect={(_, d) => setEncoding(d.optionValue ?? DEFAULT_ENCODING)}
                                        >
                                            {ENCODING_OPTIONS.map(o => (
                                                <Option key={o.value} value={o.value} text={o.label}>{o.label}</Option>
                                            ))}
                                        </Dropdown>
                                    </Field>
                                </>
                            )}

                            <Field hint={intl.formatMessage(batchSettingsHints.skipError)}>
                                <Checkbox checked={skipError} onChange={(_, d) => setSkipError(!!d.checked)}
                                    label={intl.formatMessage(batchSettingsMessages.skipError)} />
                            </Field>
                            <Field hint={intl.formatMessage(batchSettingsHints.keepFiles)}>
                                <Checkbox checked={keepFiles} onChange={(_, d) => setKeepFiles(!!d.checked)}
                                    label={intl.formatMessage(batchSettingsMessages.keepFiles)} />
                            </Field>
                        </div>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="secondary" onClick={onClose}>
                            <FormattedMessage {...messages.cancel} />
                        </Button>
                        <Button appearance="primary" onClick={submit} disabled={!canSubmit}>
                            <FormattedMessage {...messages.save} />
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}

export type CreateBatchDialogProps = Props;
