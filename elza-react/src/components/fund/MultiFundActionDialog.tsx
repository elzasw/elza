import { useEffect, useState } from 'react';
import { defineMessages, useIntl } from 'react-intl';
import { Button, Combobox, Option, Radio, RadioGroup, Spinner, makeStyles, tokens } from '@fluentui/react-components';
import { Api } from '../../api';
import type { AbstractFilter, BulkAction, FundsActionGroup, FundsActionGroupResult, MultiFundActionResult } from 'elza-api';
import { modalDialogHide } from '../../actions/global/modalDialog';
import { useAppThunkDispatch } from 'utils/hooks';

/**
 * Průvodce spuštěním hromadné akce nad více archivními soubory najednou.
 *
 * Kroky: načtení skupin podle pravidel → (volba pravidel, je-li jich víc) → volba akce
 * → potvrzení → spuštění → výsledek.
 */

const messages = defineMessages({
    loading: { id: 'multiFundAction.stage.loading', defaultMessage: 'Načítání fondů…' },
    submitting: { id: 'multiFundAction.stage.submitting', defaultMessage: 'Spouštění akce…' },
    empty: {
        id: 'multiFundAction.empty',
        defaultMessage: 'Pro vybrané fondy není k dispozici žádná použitelná hromadná akce.',
    },
    skipped: {
        id: 'multiFundAction.skipped',
        defaultMessage:
            'Přeskočeno {count, plural, one {# fond} few {# fondy} other {# fondů}} (bez otevřené verze nebo akce nepatří do jejich pravidel).',
    },
    chooseRuleSet: {
        id: 'multiFundAction.chooseRuleSet',
        defaultMessage: 'Vybrané fondy používají více sad pravidel. Zvolte, pro kterou skupinu chcete akci spustit:',
    },
    ruleSetOption: {
        id: 'multiFundAction.ruleSetOption',
        defaultMessage: '{name} ({count, plural, one {# fond} few {# fondy} other {# fondů}})',
    },
    chooseAction: {
        id: 'multiFundAction.chooseAction',
        defaultMessage:
            'Akce bude spuštěna nad {count, plural, one {# archivním souborem} few {# archivními soubory} other {# archivními soubory}} (pravidla: {ruleSet}).',
    },
    noActions: {
        id: 'multiFundAction.noActions',
        defaultMessage: 'Pro tato pravidla není dostupná žádná hromadná akce.',
    },
    actionLabel: { id: 'multiFundAction.actionLabel', defaultMessage: 'Hromadná akce' },
    actionPlaceholder: { id: 'multiFundAction.actionPlaceholder', defaultMessage: 'Vyberte akci…' },
    confirm: {
        id: 'multiFundAction.confirm',
        defaultMessage:
            'Akce <b>{action}</b> bude spuštěna nad <b>{count, plural, one {# archivním souborem} few {# archivními soubory} other {# archivními soubory}}</b>. Akce zapisuje do dat archivních souborů a každý soubor získá samostatný běh. Pokračovat?',
    },
    doneQueued: {
        id: 'multiFundAction.doneQueued',
        defaultMessage:
            'Hromadná akce byla naplánována pro <b>{count, plural, one {# fond} few {# fondy} other {# fondů}}</b>.',
    },
    doneSkipped: {
        id: 'multiFundAction.doneSkipped',
        defaultMessage: ' Přeskočeno {count, plural, one {# fond} few {# fondy} other {# fondů}}.',
    },
    doneHint: {
        id: 'multiFundAction.doneHint',
        defaultMessage: 'Průběh jednotlivých běhů lze sledovat ve správě úloh.',
    },
    close: { id: 'multiFundAction.close', defaultMessage: 'Zavřít' },
    cancel: { id: 'multiFundAction.cancel', defaultMessage: 'Zrušit' },
    continue: { id: 'multiFundAction.continue', defaultMessage: 'Pokračovat' },
    run: { id: 'multiFundAction.run', defaultMessage: 'Spustit' },
});

const useStyles = makeStyles({
    root: { padding: '10px' },
    intro: { marginBottom: '8px' },
    combobox: { width: '100%' },
    muted: { color: tokens.colorNeutralForeground3 },
    footer: {
        marginTop: '15px',
        display: 'flex',
        justifyContent: 'flex-end',
        columnGap: '8px',
    },
    footerSingle: { marginTop: '15px', textAlign: 'right' },
    skippedNote: { color: tokens.colorNeutralForeground3, marginTop: '10px', fontSize: '0.9em' },
    doneHint: { color: tokens.colorNeutralForeground3, marginTop: '8px', fontSize: '0.9em' },
});

type Stage = 'loading' | 'empty' | 'chooseRuleSet' | 'chooseAction' | 'confirm' | 'submitting' | 'done';

interface Props {
    /** Identifikátory vybraných fondů (explicitní výběr). */
    fundIds?: number[];
    /**
     * Aktivní filtr fondů (stejný jako u vyhledávání fondů). Použije se, když nejsou
     * vybrány konkrétní fondy — vyhodnocuje se až na serveru, id fondů se nestahují.
     */
    filters?: AbstractFilter[];
}

export type MultiFundActionDialogProps = Props;

function MultiFundActionDialog({ fundIds, filters }: Props) {
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const styles = useStyles();

    const [stage, setStage] = useState<Stage>('loading');
    const [groupResult, setGroupResult] = useState<FundsActionGroupResult>();
    const [selectedRuleSetId, setSelectedRuleSetId] = useState<number>();
    const [selectedActionCode, setSelectedActionCode] = useState<string>();
    const [queueResult, setQueueResult] = useState<MultiFundActionResult>();

    const close = () => {
        dispatch(modalDialogHide());
    };

    useEffect(() => {
        let cancelled = false;
        const loadGroups = async () => {
            try {
                const { data } = await Api.funds.bulkActionGroupFundsByRuleSet({ fundIds, filters });
                if (cancelled) {
                    return;
                }
                const groups = data.groups || [];
                setGroupResult(data);
                if (groups.length === 0) {
                    setStage('empty');
                } else if (groups.length === 1) {
                    setSelectedRuleSetId(groups[0].ruleSetId);
                    setStage('chooseAction');
                } else {
                    setStage('chooseRuleSet');
                }
            } catch {
                if (!cancelled) {
                    close();
                }
            }
        };
        loadGroups();
        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [fundIds, filters]);

    const selectedGroup = (): FundsActionGroup | undefined =>
        groupResult?.groups?.find(group => group.ruleSetId === selectedRuleSetId);

    const selectedAction = (): BulkAction | undefined =>
        selectedGroup()?.actions?.find(action => action.code === selectedActionCode);

    const handleConfirm = async () => {
        const group = selectedGroup();
        const action = selectedAction();
        if (!group || !action) {
            return;
        }
        setStage('submitting');
        try {
            // stejný výběr (fondy/filtr) jako u seskupení — server ho vyhodnotí znovu
            const { data } = await Api.funds.bulkActionQueueMultiFundAction({
                code: action.code,
                ruleSetId: group.ruleSetId,
                fundIds,
                filters,
            });
            setQueueResult(data);
            setStage('done');
        } catch {
            close();
        }
    };

    const renderSkipped = (skipped: FundsActionGroupResult['skipped']) => {
        if (!skipped || skipped.length === 0) {
            return null;
        }
        return (
            <div className={styles.skippedNote}>
                {intl.formatMessage(messages.skipped, { count: skipped.length })}
            </div>
        );
    };

    if (stage === 'loading' || stage === 'submitting') {
        return (
            <div className={styles.root}>
                <Spinner label={intl.formatMessage(stage === 'loading' ? messages.loading : messages.submitting)} />
            </div>
        );
    }

    if (stage === 'empty') {
        return (
            <div className={styles.root}>
                <div>{intl.formatMessage(messages.empty)}</div>
                {renderSkipped(groupResult?.skipped)}
                <div className={styles.footerSingle}>
                    <Button onClick={close}>{intl.formatMessage(messages.close)}</Button>
                </div>
            </div>
        );
    }

    if (stage === 'chooseRuleSet') {
        const groups = groupResult?.groups || [];
        return (
            <div className={styles.root}>
                <div className={styles.intro}>{intl.formatMessage(messages.chooseRuleSet)}</div>
                <RadioGroup
                    value={selectedRuleSetId != null ? String(selectedRuleSetId) : undefined}
                    onChange={(_e, data) => setSelectedRuleSetId(Number(data.value))}
                >
                    {groups.map(group => (
                        <Radio
                            key={group.ruleSetId}
                            value={String(group.ruleSetId)}
                            label={intl.formatMessage(messages.ruleSetOption, {
                                name: group.ruleSetName,
                                count: group.fundCount,
                            })}
                        />
                    ))}
                </RadioGroup>
                {renderSkipped(groupResult?.skipped)}
                <div className={styles.footer}>
                    <Button onClick={close}>{intl.formatMessage(messages.cancel)}</Button>
                    <Button
                        appearance="primary"
                        disabled={selectedRuleSetId == null}
                        onClick={() => setStage('chooseAction')}
                    >
                        {intl.formatMessage(messages.continue)}
                    </Button>
                </div>
            </div>
        );
    }

    if (stage === 'chooseAction') {
        const group = selectedGroup();
        const actions = group?.actions || [];
        const hasActions = actions.length > 0;
        return (
            <div className={styles.root}>
                <div className={styles.intro}>
                    {intl.formatMessage(messages.chooseAction, {
                        count: group?.fundCount,
                        ruleSet: group?.ruleSetName,
                    })}
                </div>
                {hasActions ? (
                    <Combobox
                        aria-label={intl.formatMessage(messages.actionLabel)}
                        placeholder={intl.formatMessage(messages.actionPlaceholder)}
                        className={styles.combobox}
                        selectedOptions={selectedActionCode ? [selectedActionCode] : []}
                        value={selectedAction()?.name ?? ''}
                        onOptionSelect={(_e, data) => setSelectedActionCode(data.optionValue)}
                    >
                        {actions.map(action => (
                            <Option key={action.code} value={action.code} text={action.name}>
                                {action.name}
                            </Option>
                        ))}
                    </Combobox>
                ) : (
                    <div className={styles.muted}>{intl.formatMessage(messages.noActions)}</div>
                )}
                <div className={styles.footer}>
                    <Button onClick={close}>{intl.formatMessage(messages.cancel)}</Button>
                    <Button
                        appearance="primary"
                        disabled={selectedActionCode == null}
                        onClick={() => setStage('confirm')}
                    >
                        {intl.formatMessage(messages.continue)}
                    </Button>
                </div>
            </div>
        );
    }

    if (stage === 'confirm') {
        const group = selectedGroup();
        const action = selectedAction();
        return (
            <div className={styles.root}>
                <div>
                    {intl.formatMessage(messages.confirm, {
                        action: action?.name,
                        count: group?.fundCount,
                        b: chunks => <b>{chunks}</b>,
                    })}
                </div>
                {renderSkipped(groupResult?.skipped)}
                <div className={styles.footer}>
                    <Button onClick={close}>{intl.formatMessage(messages.cancel)}</Button>
                    <Button appearance="primary" onClick={handleConfirm}>
                        {intl.formatMessage(messages.run)}
                    </Button>
                </div>
            </div>
        );
    }

    const skippedCount = queueResult?.skipped?.length || 0;
    return (
        <div className={styles.root}>
            <div>
                {intl.formatMessage(messages.doneQueued, {
                    count: queueResult?.queuedCount,
                    b: chunks => <b>{chunks}</b>,
                })}
                {skippedCount > 0 && intl.formatMessage(messages.doneSkipped, { count: skippedCount })}
            </div>
            <div className={styles.doneHint}>{intl.formatMessage(messages.doneHint)}</div>
            <div className={styles.footerSingle}>
                <Button appearance="primary" onClick={close}>
                    {intl.formatMessage(messages.close)}
                </Button>
            </div>
        </div>
    );
}

export { MultiFundActionDialog };
