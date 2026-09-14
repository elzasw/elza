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
    fundImportStrategy: {
        id: 'admin.import.dialog.fundImportStrategy',
        defaultMessage: 'Strategie importu fondu',
    },
    fundPairKey: {
        id: 'admin.import.dialog.fundPairKey',
        defaultMessage: 'Klíč pro párování',
    },
    ignoreRootNodes: {
        id: 'admin.import.dialog.ignoreRootNodes',
        defaultMessage: 'Ignorovat kořenové úrovně',
    },
    scope: {
        id: 'admin.import.dialog.scope',
        defaultMessage: 'Oblast entit',
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
    separator: {
        id: 'admin.import.dialog.separator',
        defaultMessage: 'Oddělovač',
    },
    encoding: {
        id: 'admin.import.dialog.encoding',
        defaultMessage: 'Kódování',
    },
    skipError: {
        id: 'admin.import.dialog.skipError',
        defaultMessage: 'Pokračovat po chybě položky',
    },
    keepFiles: {
        id: 'admin.import.dialog.keepFiles',
        defaultMessage: 'Zachovat vstupní soubory',
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
                    fundPairKey: pairKey || undefined,
                    ignoreRootNodes,
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
        && (kind !== 'EDX' || strategy != null)
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
                                    <Field label={intl.formatMessage(messages.fundImportStrategy)} required>
                                        <Dropdown
                                            value={strategy}
                                            selectedOptions={[strategy]}
                                            onOptionSelect={(_, d) => d.optionValue && setStrategy(d.optionValue as FundImportStrategy)}
                                        >
                                            {Object.values(FundImportStrategy).map(v => (
                                                <Option key={v} value={v} text={v}>{v}</Option>
                                            ))}
                                        </Dropdown>
                                    </Field>
                                    <Field label={intl.formatMessage(messages.fundPairKey)}>
                                        <Dropdown
                                            value={pairKey || intl.formatMessage(messages.none)}
                                            selectedOptions={pairKey ? [pairKey] : []}
                                            onOptionSelect={(_, d) => setPairKey((d.optionValue as FundPairKey) ?? '')}
                                        >
                                            <Option value="" text={intl.formatMessage(messages.none)}><FormattedMessage {...messages.none} /></Option>
                                            {Object.values(FundPairKey).map(v => (
                                                <Option key={v} value={v} text={v}>{v}</Option>
                                            ))}
                                        </Dropdown>
                                    </Field>
                                    <Checkbox checked={ignoreRootNodes} onChange={(_, d) => setIgnoreRootNodes(!!d.checked)}
                                        label={intl.formatMessage(messages.ignoreRootNodes)} />
                                    <Field label={intl.formatMessage(messages.scope)}>
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
                                    <Field label={intl.formatMessage(messages.separator)}>
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
                                    <Field label={intl.formatMessage(messages.encoding)}>
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

                            <Checkbox checked={skipError} onChange={(_, d) => setSkipError(!!d.checked)}
                                label={intl.formatMessage(messages.skipError)} />
                            <Checkbox checked={keepFiles} onChange={(_, d) => setKeepFiles(!!d.checked)}
                                label={intl.formatMessage(messages.keepFiles)} />
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
