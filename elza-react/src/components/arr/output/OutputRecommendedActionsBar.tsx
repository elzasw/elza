import { useEffect, useState } from 'react';
import {
    Badge,
    Button,
    MessageBar,
    MessageBarActions,
    MessageBarBody,
    Spinner,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { defineMessages, useIntl } from 'react-intl';
import { useSelector } from 'react-redux';
import { Api } from 'api/api';
import { OutputMissingAction } from 'elza-api';
import { fundOutputActionRun } from 'actions/arr/fundOutputFunctions';
import { useThunkDispatch } from 'utils/hooks';
import { AppState } from 'typings/store';

const messages = defineMessages({
    pending: {
        id: 'arr.output.form.recommendedActionsPending',
        defaultMessage: 'Některé doporučené akce dosud neproběhly. Před generováním výstupu je doporučeno je spustit.',
    },
    runAll: {
        id: 'arr.output.form.runAllActions',
        defaultMessage: 'Spustit vše',
    },
    unprocessed: {
        id: 'arr.output.form.actionsUnprocessed',
        defaultMessage: 'Nespuštěno: {count}',
    },
    processing: {
        id: 'arr.output.form.actionsProcessing',
        defaultMessage: 'Probíhá: {count}',
    },
    error: {
        id: 'arr.output.form.actionsError',
        defaultMessage: 'Chyba: {count}',
    },
    outdated: {
        id: 'arr.output.form.actionsOutdated',
        defaultMessage: 'Neaktuální: {count}',
    },
});

const PROCESSING_STATES = ['WAITING', 'PLANNED', 'RUNNING'];

const useStyles = makeStyles({
    counts: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: tokens.spacingHorizontalXS,
        marginTop: tokens.spacingVerticalXS,
    },
    countBadge: {
        backgroundColor: tokens.colorNeutralBackground1,
        color: tokens.colorNeutralForeground1,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
    },
});

interface Props {
    outputId: number;
    versionId: number;
    /** Whether the output is in a state where recommended actions are relevant (OPEN/COMPUTING). */
    active: boolean;
    readonly?: boolean;
}

const isRunnable = (state?: string) => !state || !PROCESSING_STATES.includes(state);

/**
 * Upozornění na doporučené akce, které je vhodné spustit před generováním výstupu. Data načítá
 * z endpointu can-generate (vrací pouze nedokončené akce) a umožňuje jejich hromadné spuštění.
 */
export function OutputRecommendedActionsBar({ outputId, versionId, active, readonly }: Props) {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const dispatch = useThunkDispatch();

    const [missingActions, setMissingActions] = useState<OutputMissingAction[]>([]);

    // Refetch on the same signal the right panel (FundOutputFunctions) reacts to, so this bar
    // and the panel stay in sync instead of drifting on separate triggers.
    const functionsDataKey = useSelector(({ arrRegion }: AppState) => {
        const fund = (arrRegion.funds as any[]).find(f => f.versionId === versionId);
        return fund?.fundOutput?.fundOutputFunctions?.currentDataKey;
    });

    useEffect(() => {
        if (!active) {
            setMissingActions([]);
            return;
        }
        let isActive = true;
        (async () => {
            const { data } = await Api.output.outputCanGenerateOutput(outputId);
            if (isActive) {
                setMissingActions(data);
            }
        })();
        return () => {
            isActive = false;
        };
    }, [active, outputId, functionsDataKey]);

    if (missingActions.length === 0) {
        return null;
    }

    const counts = missingActions.reduce(
        (acc, action) => {
            const state = action.actionState;
            if (state === 'ERROR') {
                acc.error += 1;
            } else if (state === 'OUTDATED') {
                acc.outdated += 1;
            } else if (state && PROCESSING_STATES.includes(state)) {
                acc.processing += 1;
            } else {
                acc.unprocessed += 1;
            }
            return acc;
        },
        { unprocessed: 0, processing: 0, error: 0, outdated: 0 },
    );

    const handleRunAll = () => {
        missingActions
            .filter(action => isRunnable(action.actionState))
            .forEach(action => dispatch(fundOutputActionRun(versionId, action.code)));
    };

    return (
        <MessageBar intent="warning" icon={null}>
            <MessageBarBody>
                {formatMessage(messages.pending)}
                <div className={styles.counts}>
                    <Badge appearance="tint" color="subtle" className={styles.countBadge}>
                        {formatMessage(messages.unprocessed, { count: counts.unprocessed })}
                    </Badge>
                    {counts.error > 0 && (
                        <Badge appearance="tint" color="subtle" className={styles.countBadge}>
                            {formatMessage(messages.error, { count: counts.error })}
                        </Badge>
                    )}
                    {counts.outdated > 0 && (
                        <Badge appearance="tint" color="subtle" className={styles.countBadge}>
                            {formatMessage(messages.outdated, { count: counts.outdated })}
                        </Badge>
                    )}
                    {counts.processing > 0 && (
                        <Spinner
                            size="tiny"
                            label={formatMessage(messages.processing, { count: counts.processing })}
                            labelPosition="after"
                        />
                    )}
                </div>
            </MessageBarBody>
            {!readonly && (
                <MessageBarActions>
                    <Button
                        appearance="primary"
                        size="small"
                        onClick={handleRunAll}
                        disabled={counts.unprocessed + counts.error + counts.outdated === 0}
                    >
                        {formatMessage(messages.runAll)}
                    </Button>
                </MessageBarActions>
            )}
        </MessageBar>
    );
}
