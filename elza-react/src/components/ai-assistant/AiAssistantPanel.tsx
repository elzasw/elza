import { useEffect, useRef, useState } from "react";
import { Button, Textarea, Spinner, ProgressBar, makeStyles, mergeClasses, tokens, Badge, Tooltip, Menu, MenuTrigger, MenuPopover, MenuList, MenuItem, MenuItemCheckbox, MenuItemRadio, MenuDivider } from "@fluentui/react-components";
import { SendRegular, FolderRegular, DocumentRegular, PersonRegular, AppsRegular, AddRegular, SparkleRegular, HistoryRegular, ChevronLeftRegular, ChevronRightRegular, SettingsRegular, ChevronDownRegular, MoneyRegular } from "@fluentui/react-icons";
import { useUserSettings } from "contexts/user";
import type { AiContextSegmentLabel } from "./useCurrentAiContext";
import { FormattedMessage, useIntl, IntlShape } from "react-intl";
import { CollapsibleDragWindow } from "components/shared/dialog/FluentModalDialog";
import { AiDisplayBlocks } from "./AiDisplayBlocks";
import { AiRequestActivities, activityTitle, isActivityFinished } from "./AiRequestActivities";
import { useAiConversation, isRequestInProgress } from "./useAiConversation";
import { useAiConversationList } from "./useAiConversationList";
import { useAiProviderInfo } from "./useAiProviderInfo";
import { useAiUsageBalance } from "./useAiUsageBalance";
import { AiUsageDialog } from "./AiUsageDialog";
import { useCurrentAiContext } from "./useCurrentAiContext";
import { aiAssistantMessages, aiContextSegmentLabels, aiModuleLabels } from "./messages";
import type { MessageDescriptor } from "react-intl";

// Provider 402 codes rendered as a readable state instead of a raw error.
const quotaErrorMessages: Record<string, MessageDescriptor> = {
    NO_SUBSCRIPTION: aiAssistantMessages.errorNoSubscription,
    QUOTA_EXCEEDED: aiAssistantMessages.errorQuotaExceeded,
    ACCOUNT_QUOTA_EXCEEDED: aiAssistantMessages.errorAccountQuotaExceeded,
};

// Requests may run for seconds up to hours; show only the non-zero, largest units.
function formatDuration(intl: IntlShape, milliseconds: number): string {
    const totalSeconds = Math.max(0, Math.round(milliseconds / 1000));
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    const unit = (value: number, unit: "hour" | "minute" | "second") =>
        intl.formatNumber(value, { style: "unit", unit, unitDisplay: "narrow" });
    const parts: string[] = [];
    if (hours > 0) parts.push(unit(hours, "hour"));
    if (minutes > 0) parts.push(unit(minutes, "minute"));
    if (seconds > 0 || parts.length === 0) parts.push(unit(seconds, "second"));
    return intl.formatList(parts, { type: "unit" });
}

const contextSegmentIcons: Record<AiContextSegmentLabel, JSX.Element> = {
    module: <AppsRegular />,
    fund: <FolderRegular />,
    node: <DocumentRegular />,
    accessPoint: <PersonRegular />,
};

const useStyles = makeStyles({
    layout: {
        display: "flex",
        flexDirection: "row",
        height: "100%",
        gap: tokens.spacingHorizontalS,
    },
    sidebar: {
        display: "flex",
        flexDirection: "column",
        flexShrink: 0,
        width: "auto",
        minHeight: 0,
        gap: tokens.spacingVerticalXS,
        borderRight: `1px solid ${tokens.colorNeutralStroke2}`,
        paddingRight: tokens.spacingHorizontalS,
        overflow: "hidden",
    },
    sidebarOpen: {
        width: "180px",
    },
    sidebarTop: {
        display: "flex",
        flexDirection: "column",
        flexShrink: 0,
        gap: tokens.spacingVerticalXS,
    },
    sidebarBottom: {
        display: "flex",
        flexShrink: 0,
        marginTop: "auto",
        paddingTop: tokens.spacingVerticalXS,
    },
    newButton: {
        flexShrink: 0,
        justifyContent: "flex-start",
        whiteSpace: "nowrap",
    },
    taskBadge: {
        flexShrink: 0,
    },
    settingsButton: {
        flexShrink: 0,
    },
    settingsWrapper: {
        position: "relative",
        display: "inline-flex",
        flexShrink: 0,
    },
    settingsDot: {
        position: "absolute",
        top: "2px",
        right: "2px",
        width: "8px",
        height: "8px",
        borderRadius: tokens.borderRadiusCircular,
        border: `1px solid ${tokens.colorNeutralBackground1}`,
        pointerEvents: "none",
    },
    contextBarActions: {
        display: "flex",
        alignItems: "center",
        flexShrink: 0,
        marginLeft: "auto",
        gap: tokens.spacingHorizontalXXS,
    },
    profileButton: {
        maxWidth: "160px",
    },
    conversationList: {
        display: "flex",
        flexDirection: "column",
        gap: "2px",
        flexGrow: 1,
        overflowY: "auto",
        minHeight: 0,
    },
    conversationItem: {
        textAlign: "left",
        border: "none",
        background: "none",
        flexShrink: 0,
        padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalS}`,
        borderRadius: tokens.borderRadiusMedium,
        cursor: "pointer",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap",
        color: tokens.colorNeutralForeground1,
        ":hover": {
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    conversationItemActive: {
        backgroundColor: tokens.colorNeutralBackground1Selected,
        fontWeight: tokens.fontWeightSemibold,
    },
    body: {
        display: "flex",
        flexDirection: "column",
        height: "100%",
        flexGrow: 1,
        minWidth: 0,
        gap: tokens.spacingVerticalS,
    },
    composer: {
        display: "flex",
        flexDirection: "column",
        flexShrink: 0,
        gap: tokens.spacingVerticalXS,
        paddingTop: tokens.spacingVerticalS,
        borderTop: `1px solid ${tokens.colorNeutralStroke2}`,
    },
    contextBar: {
        display: "flex",
        flexDirection: "row",
        alignItems: "center",
        flexShrink: 0,
        gap: tokens.spacingHorizontalXS,
        overflow: "hidden",
        maxWidth: "100%",
        minWidth: 0,
    },
    contextLabel: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase200,
        flexShrink: 0,
        whiteSpace: "nowrap",
    },
    contextChipWrapper: {
        maxWidth: "160px",
        flexGrow: 0,
        flexShrink: 3,
        flexBasis: "auto",
        display: "flex",
        minWidth: "48px",
    },
    contextChipWrapperFixed: {
        flex: "0 0 auto",
        maxWidth: "none",
    },
    contextChipWrapperWide: {
        maxWidth: "300px",
        flexShrink: 1,
    },
    contextChip: {
        width: "100%",
        minWidth: 0,
    },
    contextChipText: {
        flex: "1 1 auto",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap",
        minWidth: 0,
    },
    messages: {
        flexGrow: 1,
        overflowY: "auto",
        display: "flex",
        flexDirection: "column",
        gap: tokens.spacingVerticalM,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
        textAlign: "center",
        marginTop: tokens.spacingVerticalXXL,
    },
    quickSelect: {
        display: "flex",
        flexWrap: "wrap",
        justifyContent: "center",
        gap: tokens.spacingHorizontalXS,
        marginTop: tokens.spacingVerticalM,
    },
    quickBubble: {
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        borderRadius: tokens.borderRadiusCircular,
        padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalM}`,
        background: tokens.colorNeutralBackground1,
        color: tokens.colorNeutralForeground1,
        cursor: "pointer",
        fontSize: tokens.fontSizeBase200,
        ":hover": {
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    userMessageRow: {
        alignSelf: "center",
        width: "100%",
        maxWidth: "900px",
        display: "flex",
        justifyContent: "flex-end",
        scrollMarginTop: tokens.spacingVerticalM,
    },
    userMessage: {
        maxWidth: "85%",
        backgroundColor: tokens.colorBrandBackground2,
        borderRadius: tokens.borderRadiusLarge,
        padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalL}`,
        whiteSpace: "pre-wrap",
        wordBreak: "break-word",
    },
    aiMessage: {
        alignSelf: "center",
        width: "100%",
        maxWidth: "900px",
        backgroundColor: "transparent",
        borderRadius: tokens.borderRadiusLarge,
        padding: `${tokens.spacingVerticalXL} ${tokens.spacingHorizontalXXL}`,
        scrollMarginTop: tokens.spacingVerticalM,
    },
    aiMessageFull: {
        maxWidth: "95%",
    },
    aiError: {
        alignSelf: "flex-start",
        color: tokens.colorPaletteRedForeground1,
    },
    steps: {
        marginBottom: tokens.spacingVerticalS,
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground3,
    },
    stepsSummary: {
        cursor: "pointer",
    },
    stepsBody: {
        marginTop: tokens.spacingVerticalXS,
    },
    progressBlock: {
        display: "flex",
        flexDirection: "column",
        alignItems: "flex-start",
        gap: tokens.spacingVerticalS,
    },
    progressBar: {
        width: "100%",
        maxWidth: "320px",
    },
    usage: {
        marginTop: tokens.spacingVerticalS,
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground3,
    },
    usageSummary: {
        cursor: "pointer",
    },
    usageBody: {
        marginTop: tokens.spacingVerticalXXS,
    },
    inputRow: {
        display: "flex",
        gap: tokens.spacingHorizontalS,
        alignItems: "flex-end",
    },
    input: {
        flexGrow: 1,
    },
});

interface Props {
    onClose: () => void;
    externalSystemCode: string;
}

export function AiAssistantPanel({ onClose, externalSystemCode }: Props) {
    const styles = useStyles();
    const intl = useIntl();
    const { settings, update } = useUserSettings();
    const aiFullWidth = !!settings.aiFullWidth;
    const context = useCurrentAiContext();
    const contextRef = useRef(context);
    contextRef.current = context;
    const { requests, pending, error, send, activeConversationId, openConversation, newConversation, replaceRequest } = useAiConversation({
        externalSystemCode,
        getContext: () => contextRef.current,
    });
    const { taskTypes, profiles } = useAiProviderInfo(externalSystemCode);
    // The first task type is the default (used by the free-text input); the rest get quick-action bubbles.
    const defaultTaskType = taskTypes[0]?.code;
    const quickTasks = taskTypes.slice(1);

    const currentTaskType = requests[0]?.taskType;
    const currentTask = currentTaskType ? taskTypes.find(task => task.code === currentTaskType) : undefined;
    const currentTaskLabel = currentTask?.name || currentTaskType;
    const { conversations } = useAiConversationList(activeConversationId);
    const [draft, setDraft] = useState("");
    // null = follow the provider default; user pick overrides it.
    const [selectedProfile, setSelectedProfile] = useState<string | null>(null);
    const defaultProfile = profiles.find(profile => profile.default) ?? profiles[0];
    const activeProfileCode = selectedProfile ?? defaultProfile?.code;
    const activeProfile = profiles.find(profile => profile.code === activeProfileCode);
    const activeProfileLabel = activeProfile?.name || activeProfile?.code;
    const lastRequestRef = useRef<HTMLDivElement>(null);
    const aiMessageRef = useRef<HTMLDivElement>(null);
    const lastRequest = requests[requests.length - 1];
    const lastRequestId = lastRequest?.id;
    const lastRequestState = lastRequest?.state;
    const lastRequestActivityCount = lastRequest?.activities?.length ?? 0;
    const lastRequestFinished = lastRequest ? !isRequestInProgress(lastRequest) : false;

    const { balance, refresh: refreshBalance } = useAiUsageBalance(externalSystemCode);

    // The provider serves the balance from the same state its budget gate uses,
    // so refetching right after an exchange finishes (incl. a quota refusal)
    // always shows the post-exchange number.
    useEffect(() => {
        if (lastRequestFinished) refreshBalance();
    }, [lastRequestId, lastRequestFinished, refreshBalance]);

    // On a new request, scroll its user message to the top of the viewport so the
    // response reads from the beginning. Only the request id triggers this — not the
    // streaming state — so the view doesn't yank while the answer updates.
    useEffect(() => {
        lastRequestRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    }, [lastRequestId]);

    // While the request runs, follow newly added subtasks into view.
    useEffect(() => {
        if (lastRequestFinished) return;
        aiMessageRef.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }, [lastRequestActivityCount, lastRequestFinished]);

    // When the final message arrives, scroll its beginning to the top of the viewport.
    useEffect(() => {
        if (!lastRequestFinished) return;
        aiMessageRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    }, [lastRequestId, lastRequestState, lastRequestFinished]);

    // Credits stay out of the way — the balance lives in a /usage-style dialog
    // opened from the settings menu, not in the composer bar. The only ambient
    // cue is a colored dot on the gear when an allowance runs low, so a user
    // near a limit is not surprised. Both caps count: the weekly smoothing cap
    // refuses tasks just like the monthly allowance, so the dot tracks
    // whichever of the two is closer to exhaustion.
    const [usageOpen, setUsageOpen] = useState(false);
    const monthlyRatio = balance?.account?.allowanceCredits
        ? balance.account.spentCredits / balance.account.allowanceCredits
        : null;
    const weeklyRatio = balance?.account?.weeklyAllowanceCredits
        ? (balance.account.weeklySpentCredits ?? 0) / balance.account.weeklyAllowanceCredits
        : null;
    const balanceRatio = monthlyRatio == null ? weeklyRatio
        : weeklyRatio == null ? monthlyRatio
        : Math.max(monthlyRatio, weeklyRatio);
    const lowCreditsColor = balanceRatio == null ? null
        : balanceRatio >= 0.95 ? tokens.colorPaletteRedBackground3
        : balanceRatio >= 0.8 ? tokens.colorPaletteYellowBackground3
        : null;

    const [listExpanded, setListExpanded] = useState(false);
    const [expanded, setExpanded] = useState(false);
    const sidebarOpen = listExpanded || expanded;
    const sidebarRef = useRef<HTMLDivElement>(null);

    // While temporarily expanded (via the history icon, not the persistent toggle),
    // collapse on a click outside the sidebar.
    useEffect(() => {
        if (!listExpanded) return;
        const onPointerDown = (event: MouseEvent) => {
            if (!sidebarRef.current?.contains(event.target as Node)) {
                setListExpanded(false);
            }
        };
        document.addEventListener("mousedown", onPointerDown);
        return () => document.removeEventListener("mousedown", onPointerDown);
    }, [listExpanded]);

    const handleOpenConversation = (id: number) => {
        openConversation(id);
        if (!expanded) setListExpanded(false);
    };

    const handleNewConversation = () => {
        newConversation();
        if (!expanded) setListExpanded(false);
    };

    const trimmedDraft = draft.trim();
    const hasConversation = requests.length > 0;
    // A new conversation needs a task type (from the provider catalog); follow-ups inherit it.
    const canSend = trimmedDraft.length > 0 && !pending && (hasConversation || defaultTaskType != null);

    const handleSend = () => {
        if (!canSend) return;
        send(trimmedDraft, defaultTaskType, activeProfileCode);
        setDraft("");
    };

    const handleKeyDown = (event: React.KeyboardEvent) => {
        const isSendShortcut = event.key === "Enter" && !event.shiftKey;
        if (isSendShortcut) {
            event.preventDefault();
            handleSend();
        }
    };

    const hasMessages = requests.length > 0;

    return (
        <CollapsibleDragWindow title={intl.formatMessage(aiAssistantMessages.windowTitle)} onClose={onClose} enablePinBottom>
            <div className={styles.layout}>
                <div ref={sidebarRef} className={mergeClasses(styles.sidebar, sidebarOpen && styles.sidebarOpen)}>
                    <div className={styles.sidebarTop}>
                        {sidebarOpen ? (
                            <Button className={styles.newButton} appearance="primary" icon={<AddRegular />} onClick={handleNewConversation}>
                                <FormattedMessage {...aiAssistantMessages.newChat} />
                            </Button>
                        ) : (
                            <Tooltip content={intl.formatMessage(aiAssistantMessages.newChat)} relationship="label">
                                <Button appearance="primary" icon={<AddRegular />} onClick={handleNewConversation} />
                            </Tooltip>
                        )}
                        {!sidebarOpen && (
                            <Tooltip content={intl.formatMessage(aiAssistantMessages.history)} relationship="label">
                                <Button
                                    appearance="subtle"
                                    icon={<HistoryRegular />}
                                    onClick={() => setListExpanded(true)}
                                />
                            </Tooltip>
                        )}
                    </div>
                    {sidebarOpen && (
                        <div className={styles.conversationList}>
                            {conversations.map(conversation => (
                                <button
                                    key={conversation.id}
                                    type="button"
                                    className={mergeClasses(
                                        styles.conversationItem,
                                        conversation.id === activeConversationId && styles.conversationItemActive,
                                    )}
                                    onClick={() => handleOpenConversation(conversation.id)}
                                    title={conversation.title}
                                >
                                    {conversation.title}
                                </button>
                            ))}
                        </div>
                    )}
                    <div className={styles.sidebarBottom}>
                        <Tooltip
                            content={intl.formatMessage(expanded ? aiAssistantMessages.collapsePanel : aiAssistantMessages.expandPanel)}
                            relationship="label"
                        >
                            <Button
                                appearance="subtle"
                                icon={expanded ? <ChevronLeftRegular /> : <ChevronRightRegular />}
                                onClick={() => setExpanded(open => !open)}
                            />
                        </Tooltip>
                    </div>
                </div>
            <div className={styles.body}>
                <div className={styles.messages}>
                    {!hasMessages && !pending && (
                        <div className={styles.empty}>
                            <FormattedMessage {...aiAssistantMessages.empty} />
                            {quickTasks.length > 0 && (
                                <div className={styles.quickSelect}>
                                    {quickTasks.map(task => {
                                        const label = task.name || task.code;
                                        return (
                                            <button
                                                key={task.code}
                                                type="button"
                                                className={styles.quickBubble}
                                                title={task.description}
                                                onClick={() => send(label, task.code, activeProfileCode)}
                                            >
                                                {label}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    )}
                    {requests.map(request => {
                        const activities = request.activities ?? [];
                        // Collapsed step log of a finished exchange ("how the answer was made").
                        const finishedSteps = activities.length > 0 && (
                            <details className={styles.steps}>
                                <summary className={styles.stepsSummary}>
                                    <FormattedMessage {...aiAssistantMessages.steps} />
                                </summary>
                                <div className={styles.stepsBody}>
                                    <AiRequestActivities activities={activities} />
                                </div>
                            </details>
                        );
                        // Live status: a step Elza is executing right now wins over the
                        // provider's advisory phase (that one is stale while tools run).
                        const runningActivity = [...activities].reverse().find(activity => !isActivityFinished(activity));
                        const statusLabel = runningActivity
                            ? activityTitle(runningActivity, intl)
                            : request.progressMessage || intl.formatMessage(aiAssistantMessages.thinking);
                        const isLastRequest = request.id === lastRequest?.id;
                        return (
                        <div key={request.id} style={{ display: "contents" }}>
                            {request.userInstructions && (
                                <div ref={isLastRequest ? lastRequestRef : undefined} className={mergeClasses(styles.userMessageRow, aiFullWidth && styles.aiMessageFull)}>
                                    <div className={styles.userMessage}>{request.userInstructions}</div>
                                </div>
                            )}
                            {request.state === "error" ? (
                                <div ref={isLastRequest ? aiMessageRef : undefined} className={mergeClasses(styles.aiMessage, aiFullWidth && styles.aiMessageFull)}>
                                    {finishedSteps}
                                    <div className={styles.aiError}>
                                        {request.errorCode && quotaErrorMessages[request.errorCode] ? (
                                            <FormattedMessage {...quotaErrorMessages[request.errorCode]} />
                                        ) : (
                                            <>
                                                <FormattedMessage {...aiAssistantMessages.errorPrefix} />
                                                {request.errorMessage ? `: ${request.errorMessage}` : ""}
                                            </>
                                        )}
                                    </div>
                                </div>
                            ) : isRequestInProgress(request) ? (
                                <div ref={isLastRequest ? aiMessageRef : undefined} className={mergeClasses(styles.aiMessage, styles.progressBlock, aiFullWidth && styles.aiMessageFull)}>
                                    {activities.length > 0 && <AiRequestActivities activities={activities} />}
                                    <Spinner size="tiny" label={statusLabel} labelPosition="after" />
                                    {request.progressPercent != null && (
                                        <ProgressBar className={styles.progressBar} value={request.progressPercent / 100} />
                                    )}
                                </div>
                            ) : (
                                request.blocks && (
                                    <div ref={isLastRequest ? aiMessageRef : undefined} className={mergeClasses(styles.aiMessage, aiFullWidth && styles.aiMessageFull)}>
                                        {finishedSteps}
                                        <AiDisplayBlocks
                                            blocks={request.blocks}
                                            requestId={request.id}
                                            onRequestUpdate={replaceRequest}
                                            onClarify={(text) => setDraft(text)}
                                        />
                                        {request.usage && (
                                            <details className={styles.usage}>
                                                <summary className={styles.usageSummary}>
                                                    <FormattedMessage {...aiAssistantMessages.usage} />
                                                </summary>
                                                <div className={styles.usageBody}>
                                                    <div>
                                                        <FormattedMessage
                                                            {...aiAssistantMessages.usageStarted}
                                                            values={{
                                                                datetime: intl.formatDate(request.createDate, {
                                                                    dateStyle: "short",
                                                                    timeStyle: "medium",
                                                                }),
                                                            }}
                                                        />
                                                        {request.finishDate && (
                                                            <>
                                                                {" · "}
                                                                <FormattedMessage
                                                                    {...aiAssistantMessages.usageDuration}
                                                                    values={{
                                                                        duration: formatDuration(
                                                                            intl,
                                                                            new Date(request.finishDate).getTime() - new Date(request.createDate).getTime(),
                                                                        ),
                                                                    }}
                                                                />
                                                            </>
                                                        )}
                                                    </div>
                                                    <div>
                                                        <FormattedMessage
                                                            {...aiAssistantMessages.usageTokens}
                                                            values={{
                                                                input: request.usage.inputTokens,
                                                                output: request.usage.outputTokens,
                                                            }}
                                                        />
                                                        {/* The price is the provider's final charged credits; when the
                                                            provider does credit-free accounting it sends none, and we
                                                            show no price rather than leaking internal cost units. */}
                                                        {request.usage.chargedCredits != null && (
                                                            <>
                                                                {" · "}
                                                                <FormattedMessage
                                                                    {...aiAssistantMessages.usagePrice}
                                                                    values={{ credits: request.usage.chargedCredits }}
                                                                />
                                                            </>
                                                        )}
                                                        {request.profile && (
                                                            <>
                                                                {" · "}
                                                                <FormattedMessage
                                                                    {...aiAssistantMessages.usageProfile}
                                                                    values={{
                                                                        profile: profiles.find(profile => profile.code === request.profile)?.name || request.profile,
                                                                    }}
                                                                />
                                                            </>
                                                        )}
                                                    </div>
                                                </div>
                                            </details>
                                        )}
                                    </div>
                                )
                            )}
                        </div>
                        );
                    })}
                    {error && <div className={styles.aiError}>{error}</div>}
                </div>
                <div className={styles.composer}>
                    <div className={styles.contextBar}>
                        {currentTaskLabel && (
                            <Badge appearance="outline" icon={<SparkleRegular />} className={styles.taskBadge}>
                                {currentTaskLabel}
                            </Badge>
                        )}
                        <span className={styles.contextLabel}>
                            <FormattedMessage {...aiAssistantMessages.contextPromptLabel} />
                        </span>
                        {context ? (
                            [
                                { labelId: "module" as const, value: intl.formatMessage(aiModuleLabels[context.module]) },
                                ...context.segments,
                            ].map(segment => (
                                <Tooltip
                                    key={segment.labelId}
                                    content={`${intl.formatMessage(aiContextSegmentLabels[segment.labelId])}: ${segment.value}`}
                                    relationship="description"
                                >
                                    <div className={mergeClasses(
                                        styles.contextChipWrapper,
                                        segment.labelId === "module" && styles.contextChipWrapperFixed,
                                        segment.labelId === "node" && styles.contextChipWrapperWide,
                                    )}>
                                        <Badge appearance="tint" color={segment.labelId === "module" ? "informative" : "brand"} icon={contextSegmentIcons[segment.labelId]} className={styles.contextChip}>
                                            <span className={styles.contextChipText}>{segment.value}</span>
                                        </Badge>
                                    </div>
                                </Tooltip>
                            ))
                        ) : (
                            <Badge appearance="tint" color="informative">
                                <FormattedMessage {...aiAssistantMessages.contextNone} />
                            </Badge>
                        )}
                        <div className={styles.contextBarActions}>
                            {profiles.length > 1 && (
                                <Menu
                                    checkedValues={{ profile: activeProfileCode ? [activeProfileCode] : [] }}
                                    onCheckedValueChange={(_e, data) => setSelectedProfile(data.checkedItems[0] ?? null)}
                                >
                                    <MenuTrigger disableButtonEnhancement>
                                        <Tooltip content={intl.formatMessage(aiAssistantMessages.profile)} relationship="label">
                                            <Button className={styles.profileButton} appearance="subtle" size="small" iconPosition="after" icon={<ChevronDownRegular />}>
                                                {activeProfileLabel ?? intl.formatMessage(aiAssistantMessages.profileDefault)}
                                            </Button>
                                        </Tooltip>
                                    </MenuTrigger>
                                    <MenuPopover>
                                        <MenuList>
                                            {profiles.map(profile => (
                                                <MenuItemRadio
                                                    key={profile.code}
                                                    name="profile"
                                                    value={profile.code}
                                                    title={profile.description}
                                                >
                                                    {profile.name || profile.code}
                                                </MenuItemRadio>
                                            ))}
                                        </MenuList>
                                    </MenuPopover>
                                </Menu>
                            )}
                            <Menu
                                checkedValues={{ width: aiFullWidth ? ["full"] : [] }}
                                onCheckedValueChange={(_e, data) => update({ aiFullWidth: data.checkedItems.includes("full") })}
                            >
                                <MenuTrigger disableButtonEnhancement>
                                    <Tooltip content={intl.formatMessage(aiAssistantMessages.settings)} relationship="label">
                                        <div className={styles.settingsWrapper}>
                                            <Button className={styles.settingsButton} appearance="subtle" size="small" icon={<SettingsRegular />} />
                                            {/* Ambient low-credit cue: a small dot on the gear when the
                                                allowance is ≥80 % spent — the only always-visible credit signal. */}
                                            {lowCreditsColor && (
                                                <span
                                                    className={styles.settingsDot}
                                                    style={{ backgroundColor: lowCreditsColor }}
                                                />
                                            )}
                                        </div>
                                    </Tooltip>
                                </MenuTrigger>
                                <MenuPopover>
                                    <MenuList>
                                        {balance && (
                                            <>
                                                <MenuItem icon={<MoneyRegular />} onClick={() => setUsageOpen(true)}>
                                                    <FormattedMessage {...aiAssistantMessages.usageMenuItem} />
                                                </MenuItem>
                                                <MenuDivider />
                                            </>
                                        )}
                                        <MenuItemCheckbox name="width" value="full">
                                            <FormattedMessage {...aiAssistantMessages.fullWidthResponses} />
                                        </MenuItemCheckbox>
                                    </MenuList>
                                </MenuPopover>
                            </Menu>
                        </div>
                    </div>
                    <div className={styles.inputRow}>
                        <Textarea
                            className={styles.input}
                            value={draft}
                            onChange={(_event, data) => setDraft(data.value)}
                            onKeyDown={handleKeyDown}
                            placeholder={intl.formatMessage(aiAssistantMessages.inputPlaceholder)}
                            resize="vertical"
                        />
                        <Button
                            appearance="primary"
                            icon={<SendRegular />}
                            disabled={!canSend}
                            onClick={handleSend}
                            aria-label={intl.formatMessage(aiAssistantMessages.send)}
                        />
                    </div>
                </div>
            </div>
            </div>
            <AiUsageDialog open={usageOpen} onClose={() => setUsageOpen(false)} balance={balance} />
        </CollapsibleDragWindow>
    );
}
