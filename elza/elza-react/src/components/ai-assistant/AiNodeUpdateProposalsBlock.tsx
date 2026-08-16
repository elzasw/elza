import { useState } from "react";
import { Button, Link, Spinner, makeStyles, mergeClasses, tokens } from "@fluentui/react-components";
import {
    AddCircleRegular,
    CheckmarkCircleRegular,
    DeleteRegular,
    DismissCircleRegular,
    EditRegular,
    WarningRegular,
} from "@fluentui/react-icons";
import { FormattedMessage, useIntl } from "react-intl";
import { Api } from "api";
import {
    AiNodeUpdateProposalsBlock as AiNodeUpdateProposalsBlockVO,
    AiProposalChange,
    AiProposalOperation,
    AiRequest,
} from "elza-api";
import { useAppThunkDispatch } from "utils/hooks";
import { routerNavigate } from "actions/router";
import { linkPath } from "./AiRequestActivities";
import { aiAssistantMessages } from "./messages";

const STATE_PROPOSED = "PROPOSED";
const STATE_APPLIED = "APPLIED";
const STATE_REJECTED = "REJECTED";
const STATE_BLOCKED = "BLOCKED";
const STATE_SUPERSEDED = "SUPERSEDED";

const KIND_ADD = "ADD";
const KIND_UPDATE = "UPDATE";
const KIND_DELETE = "DELETE";

const useStyles = makeStyles({
    root: {
        display: "flex",
        flexDirection: "column",
        gap: tokens.spacingVerticalS,
    },
    nodeHeader: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground3,
    },
    card: {
        display: "flex",
        flexDirection: "column",
        gap: tokens.spacingVerticalXS,
        padding: tokens.spacingHorizontalS,
        border: `1px solid ${tokens.colorNeutralStroke2}`,
        borderRadius: tokens.borderRadiusMedium,
        background: tokens.colorNeutralBackground1,
    },
    cardMuted: {
        opacity: 0.6,
    },
    reasonRow: {
        display: "flex",
        alignItems: "baseline",
        gap: tokens.spacingHorizontalS,
    },
    reason: {
        flexGrow: 1,
        fontWeight: tokens.fontWeightSemibold,
    },
    confidence: {
        flexShrink: 0,
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground3,
    },
    operations: {
        display: "flex",
        flexDirection: "column",
        gap: "2px",
        fontSize: tokens.fontSizeBase200,
    },
    operation: {
        display: "flex",
        alignItems: "baseline",
        flexWrap: "wrap",
        columnGap: tokens.spacingHorizontalXS,
    },
    opIcon: {
        flexShrink: 0,
        alignSelf: "center",
    },
    opIconAdd: {
        color: tokens.colorPaletteGreenForeground1,
    },
    opIconDelete: {
        color: tokens.colorPaletteRedForeground1,
    },
    opType: {
        fontWeight: tokens.fontWeightSemibold,
    },
    oldValue: {
        textDecorationLine: "line-through",
        color: tokens.colorNeutralForeground3,
        wordBreak: "break-word",
    },
    newValue: {
        wordBreak: "break-word",
    },
    footer: {
        display: "flex",
        alignItems: "center",
        flexWrap: "wrap",
        gap: tokens.spacingHorizontalS,
    },
    stateText: {
        display: "flex",
        alignItems: "center",
        gap: tokens.spacingHorizontalXXS,
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground3,
    },
    stateApplied: {
        color: tokens.colorPaletteGreenForeground1,
    },
    blocked: {
        display: "flex",
        alignItems: "center",
        gap: tokens.spacingHorizontalXS,
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorPaletteDarkOrangeForeground1,
    },
    error: {
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorPaletteRedForeground1,
    },
});

interface Props {
    block: AiNodeUpdateProposalsBlockVO;
    /** Id of the exchange the block belongs to; without it the cards are read-only. */
    requestId?: number;
    /** Receives the refreshed exchange returned by an apply/reject call. */
    onRequestUpdate?: (request: AiRequest) => void;
    /** Prefills the composer with a clarification quote ("Upřesnit…"). */
    onClarify?: (text: string) => void;
}

export function AiNodeUpdateProposalsBlock({ block, requestId, onRequestUpdate, onClarify }: Props) {
    const styles = useStyles();
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const [busyKey, setBusyKey] = useState<string | null>(null);
    const [errors, setErrors] = useState<Record<string, string>>({});

    const nodes = block.nodes ?? [];
    // A single-level proposal repeats the level the user already stands on —
    // the header only earns its place when the block spans several levels.
    const showNodeHeaders = nodes.length > 1;

    const decide = async (change: AiProposalChange, action: "apply" | "reject") => {
        if (requestId == null || busyKey !== null) return;
        setBusyKey(change.changeKey);
        setErrors(previous => {
            const { [change.changeKey]: _removed, ...rest } = previous;
            return rest;
        });
        try {
            const { data } =
                action === "apply"
                    ? await Api.aiprovider.aiProviderApplyProposalChange(requestId, { changeKey: change.changeKey })
                    : await Api.aiprovider.aiProviderRejectProposalChange(requestId, { changeKey: change.changeKey });
            onRequestUpdate?.(data);
        } catch (requestError) {
            const message = (requestError as { message?: string })?.message ?? String(requestError);
            setErrors(previous => ({ ...previous, [change.changeKey]: message }));
        } finally {
            setBusyKey(null);
        }
    };

    return (
        <div className={styles.root}>
            {nodes.map((proposalNode, nodeIndex) => {
                const path = proposalNode.node ? linkPath(proposalNode.node.target) : null;
                const label = proposalNode.node?.label;
                return (
                    <div key={nodeIndex} className={styles.root}>
                        {showNodeHeaders && label && (
                            <div className={styles.nodeHeader}>
                                {path ? (
                                    <Link onClick={() => dispatch(routerNavigate(path))}>{label}</Link>
                                ) : (
                                    label
                                )}
                            </div>
                        )}
                        {(proposalNode.changes ?? []).map(change => (
                            <ChangeCard
                                key={change.changeKey}
                                change={change}
                                styles={styles}
                                busy={busyKey === change.changeKey}
                                actionable={requestId != null && busyKey === null}
                                error={errors[change.changeKey]}
                                onApply={() => decide(change, "apply")}
                                onReject={() => decide(change, "reject")}
                                onClarify={onClarify}
                                formatDate={(value: string) => intl.formatDate(value)}
                                onNavigate={(target: string) => dispatch(routerNavigate(target))}
                            />
                        ))}
                    </div>
                );
            })}
        </div>
    );
}

interface ChangeCardProps {
    change: AiProposalChange;
    styles: ReturnType<typeof useStyles>;
    busy: boolean;
    actionable: boolean;
    error?: string;
    onApply: () => void;
    onReject: () => void;
    onClarify?: (text: string) => void;
    formatDate: (value: string) => string;
    onNavigate: (path: string) => void;
}

function ChangeCard({
    change,
    styles,
    busy,
    actionable,
    error,
    onApply,
    onReject,
    onClarify,
    formatDate,
    onNavigate,
}: ChangeCardProps) {
    const muted = change.state === STATE_REJECTED || change.state === STATE_SUPERSEDED;
    const clarifyText = change.blockedReason ?? change.reason;

    return (
        <div className={mergeClasses(styles.card, muted && styles.cardMuted)}>
            <div className={styles.reasonRow}>
                <span className={styles.reason}>{change.reason}</span>
                {change.confidence != null && <span className={styles.confidence}>{change.confidence}&nbsp;%</span>}
            </div>
            <div className={styles.operations}>
                {(change.operations ?? []).map((operation, index) => (
                    <OperationRow key={index} operation={operation} styles={styles} onNavigate={onNavigate} />
                ))}
            </div>
            {change.state === STATE_BLOCKED && change.blockedReason && (
                <div className={styles.blocked}>
                    <WarningRegular className={styles.opIcon} />
                    <span>{change.blockedReason}</span>
                </div>
            )}
            {error && <div className={styles.error}>
                <FormattedMessage {...aiAssistantMessages.proposalActionFailed} values={{ error }} />
            </div>}
            <div className={styles.footer}>
                {change.state === STATE_PROPOSED && actionable && (
                    <>
                        <Button size="small" appearance="primary" onClick={onApply} disabled={busy}>
                            <FormattedMessage {...aiAssistantMessages.proposalApply} />
                        </Button>
                        <Button size="small" onClick={onReject} disabled={busy}>
                            <FormattedMessage {...aiAssistantMessages.proposalReject} />
                        </Button>
                    </>
                )}
                {busy && <Spinner size="extra-tiny" />}
                {change.state === STATE_APPLIED && (
                    <span className={mergeClasses(styles.stateText, styles.stateApplied)}>
                        <CheckmarkCircleRegular />
                        <FormattedMessage {...aiAssistantMessages.proposalApplied} />
                        {change.decideDate && <span> · {formatDate(change.decideDate)}</span>}
                    </span>
                )}
                {change.state === STATE_REJECTED && (
                    <span className={styles.stateText}>
                        <DismissCircleRegular />
                        <FormattedMessage {...aiAssistantMessages.proposalRejected} />
                    </span>
                )}
                {change.state === STATE_SUPERSEDED && (
                    <span className={styles.stateText}>
                        <FormattedMessage {...aiAssistantMessages.proposalSuperseded} />
                    </span>
                )}
                {(change.state === STATE_PROPOSED || change.state === STATE_BLOCKED) && onClarify && (
                    <Button size="small" appearance="subtle" onClick={() => onClarify(clarifyText)} disabled={busy}>
                        <FormattedMessage {...aiAssistantMessages.proposalClarify} />
                    </Button>
                )}
            </div>
        </div>
    );
}

interface OperationRowProps {
    operation: AiProposalOperation;
    styles: ReturnType<typeof useStyles>;
    onNavigate: (path: string) => void;
}

function OperationRow({ operation, styles, onNavigate }: OperationRowProps) {
    const entityPath = operation.entity ? linkPath(operation.entity.target) : null;
    const showOld = operation.kind !== KIND_ADD && operation.oldValue;
    const showNew = operation.kind !== KIND_DELETE && operation.newValue;

    return (
        <div className={styles.operation}>
            {operation.kind === KIND_ADD ? (
                <AddCircleRegular className={mergeClasses(styles.opIcon, styles.opIconAdd)} />
            ) : operation.kind === KIND_DELETE ? (
                <DeleteRegular className={mergeClasses(styles.opIcon, styles.opIconDelete)} />
            ) : operation.kind === KIND_UPDATE ? (
                <EditRegular className={styles.opIcon} />
            ) : null}
            <span className={styles.opType}>
                {operation.itemTypeName}
                {operation.specName && operation.specName !== operation.newValue && ` (${operation.specName})`}
            </span>
            {showOld && <span className={styles.oldValue}>{operation.oldValue}</span>}
            {showOld && showNew && <span>→</span>}
            {showNew &&
                (entityPath ? (
                    <Link className={styles.newValue} onClick={() => onNavigate(entityPath)}>
                        {operation.newValue}
                    </Link>
                ) : (
                    <span className={styles.newValue}>{operation.newValue}</span>
                ))}
        </div>
    );
}
