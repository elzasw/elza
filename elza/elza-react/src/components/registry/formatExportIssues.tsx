import React from "react";
import Icon from "components/shared/icon/FontIcon";
import "./formatExportIssues.scss";

/**
 * Shape of a single issue as serialized by the backend (see `cz.tacr.elza.cam.v2.ApIssue`).
 * Only the fields rendered by the UI are typed — extra fields are ignored.
 */
interface ExportIssue {
    severity: "ERROR" | "WARNING" | string;
    ruleCode?: string | null;
    message: string;
    detail?: string | null;
    partId?: number | null;
    itemId?: number | null;
    entityId?: number | null;
    partName?: string | null;
    itemName?: string | null;
    entityName?: string | null;
}

/**
 * Navigation target produced when the user clicks a resolved name in an issue row.
 * The caller translates this into a DOM scroll (part) or route change (entity).
 */
export type IssueNavTarget =
    | { type: "part"; id: number }
    | { type: "entity"; id: number };

const isIssueArray = (value: unknown): value is ExportIssue[] =>
    Array.isArray(value)
    && value.every(item =>
        item != null
        && typeof item === "object"
        && typeof (item as ExportIssue).message === "string"
        && typeof (item as ExportIssue).severity === "string");

interface NavChip {
    label: string;
    target: IssueNavTarget;
}

/**
 * Figure out which clickable nav chips to render for an issue. At most one
 * local chip (part / item — both scroll to the same part in the current AP)
 * plus one entity chip (navigates to a different AP).
 */
const navChipsFor = (issue: ExportIssue): NavChip[] => {
    const chips: NavChip[] = [];
    // item ref is more specific than partRef when both are present;
    // backend guarantees partId is filled in for item refs (containing part).
    if (issue.itemId != null && issue.itemName && issue.partId != null) {
        chips.push({ label: issue.itemName, target: { type: "part", id: issue.partId } });
    } else if (issue.partId != null && issue.partName) {
        chips.push({ label: issue.partName, target: { type: "part", id: issue.partId } });
    }
    if (issue.entityId != null && issue.entityName) {
        chips.push({ label: issue.entityName, target: { type: "entity", id: issue.entityId } });
    }
    return chips;
};

const IssueRow: React.FC<{
    issue: ExportIssue;
    onNavigate?: (target: IssueNavTarget) => void;
}> = ({ issue, onNavigate }) => {
    const isError = issue.severity === "ERROR";
    const glyph = isError ? "fa-exclamation-circle" : "fa-exclamation-triangle";
    const severityClass = isError ? "export-issue--error" : "export-issue--warning";
    const chips = navChipsFor(issue);

    return (
        <li className={`export-issue ${severityClass}`}>
            <Icon glyph={glyph} className="export-issue__icon" />
            <div className="export-issue__body">
                <div className="export-issue__message">{issue.message}</div>
                {issue.detail && (
                    <div className="export-issue__detail">{issue.detail}</div>
                )}
                {chips.length > 0 && (
                    <div className="export-issue__targets">
                        {chips.map((chip, idx) => (
                            onNavigate ? (
                                <button
                                    key={idx}
                                    type="button"
                                    className="export-issue__target export-issue__target--clickable"
                                    onClick={() => onNavigate(chip.target)}
                                >
                                    {chip.label}
                                </button>
                            ) : (
                                <span key={idx} className="export-issue__target">{chip.label}</span>
                            )
                        ))}
                    </div>
                )}
            </div>
            {issue.ruleCode && (
                <span className="export-issue__rule">{issue.ruleCode}</span>
            )}
        </li>
    );
};

/**
 * Render the {@code state} payload of {@code ACCESS_POINT_EXPORT_NEED_CONFIRM}
 * / {@code ACCESS_POINT_EXPORT_FAILED} websocket events.
 *
 * CAM v2 sends a JSON array of {@link ExportIssue}; CAM v1 sends a plain
 * newline-separated string. We try the structured path first and fall back to
 * a single synthetic issue so the v1 payload gets the same severity icon and
 * layout as v2.
 *
 * @param raw websocket payload ({@code state} field)
 * @param intro optional header line shown above the list
 * @param fallbackSeverity severity used for the v1 / plain-text fallback icon
 * @param onNavigate optional handler invoked when the user clicks a resolved
 *        part/item/entity name. When omitted, names render as plain text
 *        (useful for previews / tests).
 */
export const formatExportIssues = (
    raw: string,
    intro?: React.ReactNode,
    fallbackSeverity: "ERROR" | "WARNING" = "ERROR",
    onNavigate?: (target: IssueNavTarget) => void,
): React.ReactNode => {
    let parsed: unknown;
    try {
        parsed = JSON.parse(raw);
    } catch {
        parsed = null;
    }

    const issues: ExportIssue[] = isIssueArray(parsed)
        ? parsed
        : [{ severity: fallbackSeverity, message: raw }];

    return (
        <div className="export-issues">
            {intro && <div className="export-issues__intro">{intro}</div>}
            <ul className="export-issues__list">
                {issues.map((issue, idx) => (
                    <IssueRow key={idx} issue={issue} onNavigate={onNavigate} />
                ))}
            </ul>
        </div>
    );
};
