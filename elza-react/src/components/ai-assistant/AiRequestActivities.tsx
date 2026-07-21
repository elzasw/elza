import { Link, Spinner, makeStyles, tokens } from "@fluentui/react-components";
import { CheckmarkCircleRegular, DismissCircleRegular } from "@fluentui/react-icons";
import { IntlShape, useIntl } from "react-intl";
import { AiActivityLink, AiContextAccesspoint, AiContextFund, AiContextNode, AiContextType, AiRequestActivity } from "elza-api";
import { useAppThunkDispatch } from "utils/hooks";
import { routerNavigate } from "actions/router";
import { urlEntity, urlFundTree, urlNode } from "../../constants";
import { aiAssistantMessages } from "./messages";

/** Links rendered per step; the rest is only counted (the payload is capped server-side anyway). */
const MAX_LINKS_SHOWN = 10;

const KIND_TOOL_CALL = "TOOL_CALL";
const KIND_PREPARATION = "PREPARATION";
const STATE_DONE = "DONE";
const STATE_ERROR = "ERROR";

// Per the contract an unknown state means "still running".
export function isActivityFinished(activity: AiRequestActivity) {
    return activity.state === STATE_DONE || activity.state === STATE_ERROR;
}

/** Localized title of a step; unknown tools and kinds get a generic label (never dropped). */
export function activityTitle(activity: AiRequestActivity, intl: IntlShape): string {
    if (activity.kind === KIND_TOOL_CALL) {
        switch (activity.tool) {
            case "searchNodes":
                return intl.formatMessage(aiAssistantMessages.activityToolSearchNodes);
            case "getItemTypes":
                return intl.formatMessage(aiAssistantMessages.activityToolGetItemTypes);
            case "search_knowledge":
                return intl.formatMessage(aiAssistantMessages.activityToolSearchKnowledge);
            case "get_section":
                return intl.formatMessage(aiAssistantMessages.activityToolGetSection);
            default:
                if (activity.tool) {
                    return intl.formatMessage(aiAssistantMessages.activityToolGeneric, { tool: activity.tool });
                }
        }
    }
    if (activity.kind === KIND_PREPARATION) {
        return intl.formatMessage(aiAssistantMessages.activityPreparation);
    }
    return intl.formatMessage(aiAssistantMessages.activityStepGeneric);
}

function linkPath(target: AiActivityLink["target"]): string | null {
    switch (target.type) {
        case AiContextType.Node:
            return urlNode((target as AiContextNode).nodeId);
        case AiContextType.Fund:
            return urlFundTree((target as AiContextFund).fundId);
        case AiContextType.Accesspoint:
            return urlEntity((target as AiContextAccesspoint).accessPointId);
        default:
            return null;
    }
}

const useStyles = makeStyles({
    list: {
        display: "flex",
        flexDirection: "column",
        gap: tokens.spacingVerticalXS,
        fontSize: tokens.fontSizeBase200,
    },
    row: {
        display: "flex",
        flexDirection: "column",
        gap: "2px",
    },
    head: {
        display: "flex",
        alignItems: "center",
        flexWrap: "wrap",
        gap: tokens.spacingHorizontalXS,
    },
    title: {
        fontWeight: tokens.fontWeightSemibold,
    },
    query: {
        color: tokens.colorNeutralForeground3,
        fontStyle: "italic",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap",
        maxWidth: "320px",
    },
    result: {
        color: tokens.colorNeutralForeground3,
    },
    errorText: {
        color: tokens.colorPaletteRedForeground1,
        paddingLeft: tokens.spacingHorizontalL,
    },
    links: {
        display: "flex",
        flexWrap: "wrap",
        columnGap: tokens.spacingHorizontalM,
        rowGap: "2px",
        paddingLeft: tokens.spacingHorizontalL,
    },
    link: {
        maxWidth: "280px",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap",
    },
    moreLinks: {
        color: tokens.colorNeutralForeground3,
    },
    iconDone: {
        color: tokens.colorPaletteGreenForeground1,
        flexShrink: 0,
    },
    iconError: {
        color: tokens.colorPaletteRedForeground1,
        flexShrink: 0,
    },
});

interface Props {
    activities: AiRequestActivity[];
}

export function AiRequestActivities({ activities }: Props) {
    const styles = useStyles();
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();

    return (
        <div className={styles.list}>
            {activities.map(activity => {
                const links = activity.links ?? [];
                const shownLinks = links.slice(0, MAX_LINKS_SHOWN);
                return (
                    <div key={activity.id} className={styles.row}>
                        <div className={styles.head}>
                            {activity.state === STATE_DONE ? (
                                <CheckmarkCircleRegular className={styles.iconDone} />
                            ) : activity.state === STATE_ERROR ? (
                                <DismissCircleRegular className={styles.iconError} />
                            ) : (
                                <Spinner size="extra-tiny" />
                            )}
                            <span className={styles.title}>{activity.label ?? activityTitle(activity, intl)}</span>
                            {(activity.summary ?? activity.query) && (
                                <span className={styles.query}>{activity.summary ?? activity.query}</span>
                            )}
                            {activity.state === STATE_DONE && activity.resultCount != null && (
                                <span className={styles.result}>
                                    {intl.formatMessage(aiAssistantMessages.activityResultCount, {
                                        count: activity.resultCount,
                                    })}
                                    {activity.partial &&
                                        ` (${intl.formatMessage(aiAssistantMessages.activityResultPartial)})`}
                                </span>
                            )}
                        </div>
                        {activity.state === STATE_ERROR && activity.error && (
                            <div className={styles.errorText}>{activity.error}</div>
                        )}
                        {shownLinks.length > 0 && (
                            <div className={styles.links}>
                                {shownLinks.map((link, index) => {
                                    const path = linkPath(link.target);
                                    // A `refs`-derived link carries no name — fall back to a generic
                                    // "open" label so it never renders a raw URL.
                                    const label =
                                        link.label ??
                                        (path ? intl.formatMessage(aiAssistantMessages.activityLinkOpen) : null);
                                    if (!label) return null;
                                    return path ? (
                                        <Link
                                            key={index}
                                            className={styles.link}
                                            title={label}
                                            onClick={() => dispatch(routerNavigate(path))}
                                        >
                                            {label}
                                        </Link>
                                    ) : (
                                        <span key={index} className={styles.link} title={label}>
                                            {label}
                                        </span>
                                    );
                                })}
                                {links.length > shownLinks.length && (
                                    <span className={styles.moreLinks}>
                                        {intl.formatMessage(aiAssistantMessages.activityMoreLinks, {
                                            count: links.length - shownLinks.length,
                                        })}
                                    </span>
                                )}
                            </div>
                        )}
                    </div>
                );
            })}
        </div>
    );
}
