import { useEffect, useRef, useState } from "react";
import { Button, Textarea, Spinner, makeStyles, mergeClasses, tokens, Badge, Tooltip, Menu, MenuTrigger, MenuPopover, MenuList, MenuItemCheckbox } from "@fluentui/react-components";
import { SendRegular, FolderRegular, DocumentRegular, PersonRegular, AppsRegular, AddRegular, SparkleRegular, HistoryRegular, ChevronLeftRegular, ChevronRightRegular, SettingsRegular } from "@fluentui/react-icons";
import { useUserSettings } from "contexts/user";
import type { AiContextSegmentLabel } from "./useCurrentAiContext";
import { FormattedMessage, useIntl } from "react-intl";
import { CollapsibleDragWindow } from "components/shared/dialog/FluentModalDialog";
import { AiDisplayBlocks } from "./AiDisplayBlocks";
import { useAiConversation, isRequestInProgress } from "./useAiConversation";
import { useAiConversationList } from "./useAiConversationList";
import { useAiProviderInfo } from "./useAiProviderInfo";
import { useCurrentAiContext } from "./useCurrentAiContext";
import { aiAssistantMessages, aiContextSegmentLabels, aiModuleLabels } from "./messages";

// costUnits are USD cents. CNB USD→CZK fixing rate (09 Jul 2026); update manually.
const USD_CZK_RATE = 21.213;
const costUnitsToCzk = (costUnits: number) => (costUnits / 100) * USD_CZK_RATE;

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
        marginLeft: "auto",
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
    },
    aiMessageFull: {
        maxWidth: "95%",
    },
    aiError: {
        alignSelf: "flex-start",
        color: tokens.colorPaletteRedForeground1,
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
    const { requests, pending, error, send, activeConversationId, openConversation, newConversation } = useAiConversation({
        externalSystemCode,
        getContext: () => contextRef.current,
    });
    const { taskTypes } = useAiProviderInfo(externalSystemCode);
    // The first task type is the default (used by the free-text input); the rest get quick-action bubbles.
    const defaultTaskType = taskTypes[0]?.code;
    const quickTasks = taskTypes.slice(1);

    const currentTaskType = requests[0]?.taskType;
    const currentTask = currentTaskType ? taskTypes.find(task => task.code === currentTaskType) : undefined;
    const currentTaskLabel = currentTask?.name || currentTaskType;
    const { conversations } = useAiConversationList(activeConversationId);
    const [draft, setDraft] = useState("");
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [requests, pending]);

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
        send(trimmedDraft, defaultTaskType);
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
                                                onClick={() => send(label, task.code)}
                                            >
                                                {label}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    )}
                    {requests.map(request => (
                        <div key={request.id} style={{ display: "contents" }}>
                            {request.userInstructions && (
                                <div className={mergeClasses(styles.userMessageRow, aiFullWidth && styles.aiMessageFull)}>
                                    <div className={styles.userMessage}>{request.userInstructions}</div>
                                </div>
                            )}
                            {request.state === "error" ? (
                                <div className={styles.aiError}>
                                    <FormattedMessage {...aiAssistantMessages.errorPrefix} />
                                    {request.errorMessage ? `: ${request.errorMessage}` : ""}
                                </div>
                            ) : isRequestInProgress(request) ? (
                                <Spinner size="tiny" label={intl.formatMessage(aiAssistantMessages.thinking)} labelPosition="after" />
                            ) : (
                                request.blocks && (
                                    <div className={mergeClasses(styles.aiMessage, aiFullWidth && styles.aiMessageFull)}>
                                        <AiDisplayBlocks blocks={request.blocks} />
                                        {request.usage && (
                                            <details className={styles.usage}>
                                                <summary className={styles.usageSummary}>
                                                    <FormattedMessage {...aiAssistantMessages.usage} />
                                                </summary>
                                                <div className={styles.usageBody}>
                                                    <FormattedMessage
                                                        {...aiAssistantMessages.usageDetail}
                                                        values={{
                                                            input: request.usage.inputTokens,
                                                            output: request.usage.outputTokens,
                                                            cost: intl.formatNumber(costUnitsToCzk(request.usage.costUnits), {
                                                                style: "currency",
                                                                currency: "CZK",
                                                            }),
                                                        }}
                                                    />
                                                </div>
                                            </details>
                                        )}
                                    </div>
                                )
                            )}
                        </div>
                    ))}
                    {error && <div className={styles.aiError}>{error}</div>}
                    <div ref={messagesEndRef} />
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
                        <Menu
                            checkedValues={{ width: aiFullWidth ? ["full"] : [] }}
                            onCheckedValueChange={(_e, data) => update({ aiFullWidth: data.checkedItems.includes("full") })}
                        >
                            <MenuTrigger disableButtonEnhancement>
                                <Tooltip content={intl.formatMessage(aiAssistantMessages.settings)} relationship="label">
                                    <Button className={styles.settingsButton} appearance="subtle" size="small" icon={<SettingsRegular />} />
                                </Tooltip>
                            </MenuTrigger>
                            <MenuPopover>
                                <MenuList>
                                    <MenuItemCheckbox name="width" value="full">
                                        <FormattedMessage {...aiAssistantMessages.fullWidthResponses} />
                                    </MenuItemCheckbox>
                                </MenuList>
                            </MenuPopover>
                        </Menu>
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
        </CollapsibleDragWindow>
    );
}
