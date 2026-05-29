import { Popover, PopoverSurface, PopoverTrigger, Spinner, makeStyles, tokens } from '@fluentui/react-components';
import { ErrorCircleFilled, WarningFilled } from '@fluentui/react-icons';
import { useState } from 'react';
import { ExtEntityBinding, ExtIssue, ExtIssueIconState } from 'elza-api';
import { Api } from 'api/api';
import i18n from 'components/i18n';

const useStyles = makeStyles({
    iconNew: {
        color: 'var(--color-orange)',
        cursor: 'pointer',
    },
    iconNeutral: {
        color: tokens.colorNeutralForeground3,
        cursor: 'pointer',
    },
    iconAttention: {
        color: 'var(--color-red)',
        cursor: 'pointer',
    },
    popover: {
        maxWidth: '550px',
        maxHeight: '450px',
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
    },
    issue: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXXS,
        paddingBottom: tokens.spacingVerticalXS,
        borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
        ':last-child': { borderBottom: 'none' },
    },
    severity: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase200,
    },
    extra: {
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground3,
    },
});

interface Props {
    binding: ExtEntityBinding;
    testIssues?: ExtIssue[];
}

export function BindingIssuesIcon({ binding, testIssues }: Props) {
    const classes = useStyles();
    const [open, setOpen] = useState(false);
    const [issues, setIssues] = useState<ExtIssue[] | null>(testIssues ?? null);
    const [loading, setLoading] = useState(false);

    const iconState = binding.issueSummary?.iconState;
    if (!iconState || iconState === ExtIssueIconState.None) {
        return null;
    }

    const handleOpen = async () => {
        setOpen(true);
        if (issues === null) {
            setLoading(true);
            try {
                const { data } = await Api.accesspointInternal.accessPointBindingGetBindingIssues(binding.id);
                setIssues(data);
            } finally {
                setLoading(false);
            }
        }
    };

    const renderIcon = () => {
        if (iconState === ExtIssueIconState.Attention) {
            return <ErrorCircleFilled className={classes.iconAttention} />;
        }
        if (iconState === ExtIssueIconState.Neutral) {
            return <WarningFilled className={classes.iconNeutral} />;
        }
        return <WarningFilled className={classes.iconNew} />;
    };

    return (
        <Popover
            open={open}
            onOpenChange={(_, data) => {
                if (!data.open) {
                    setOpen(false);
                }
            }}
        >
            <PopoverTrigger disableButtonEnhancement>
                <span onClick={handleOpen}>{renderIcon()}</span>
            </PopoverTrigger>
            <PopoverSurface className={classes.popover}>
                {loading && <Spinner size="tiny" />}
                {issues?.length === 0 && <span>{i18n('ap.binding.issues.none')}</span>}
                {issues?.map((issue) => {
                    const extra = (
                        [
                            issue.uuid && `uuid=${issue.uuid}`,
                            issue.status && `status=${issue.status}`,
                            issue.issueCode && `issueCode=${issue.issueCode}`,
                            issue.source && `source=${issue.source}`,
                            issue.note && `note=${issue.note}`,
                            issue.issueFrom && `issueFrom=${issue.issueFrom}`,
                            issue.extFromRev && `extFromRev=${issue.extFromRev}`,
                            issue.partId != null && `partId=${issue.partId}`,
                            issue.itemId != null && `itemId=${issue.itemId}`,
                            issue.relatedBindingId != null && `relatedBindingId=${issue.relatedBindingId}`,
                            issue.relatedBindingExtValue && `relatedBindingExtValue=${issue.relatedBindingExtValue}`,
                        ] as (string | false)[]
                    )
                        .filter(Boolean)
                        .join(', ');

                    return (
                        <div key={issue.id} className={classes.issue}>
                            <span className={classes.severity}>
                                {issue.severity}
                                {issue.ruleCode ? ` (${issue.ruleCode})` : ''}
                            </span>
                            {issue.message && <span>{issue.message}</span>}
                            {issue.detail && <span>{issue.detail}</span>}
                            {extra && <span className={classes.extra}>{extra}</span>}
                        </div>
                    );
                })}
            </PopoverSurface>
        </Popover>
    );
}
