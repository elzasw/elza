import {
    Dialog,
    DialogSurface,
    DialogBody,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    ProgressBar,
    makeStyles,
    tokens,
} from "@fluentui/react-components";
import { FormattedMessage, useIntl } from "react-intl";
import { AiUsageBalance } from "elza-api";
import { aiAssistantMessages } from "./messages";

const useStyles = makeStyles({
    surface: {
        width: "420px",
        maxWidth: "95vw",
    },
    section: {
        display: "flex",
        flexDirection: "column",
        gap: tokens.spacingVerticalXS,
        marginBottom: tokens.spacingVerticalL,
    },
    heading: {
        fontSize: tokens.fontSizeBase200,
        fontWeight: tokens.fontWeightSemibold,
        color: tokens.colorNeutralForeground3,
        textTransform: "uppercase",
        letterSpacing: "0.03em",
    },
    amount: {
        fontSize: tokens.fontSizeBase400,
        fontWeight: tokens.fontWeightSemibold,
    },
    bar: {
        marginTop: tokens.spacingVerticalXXS,
        marginBottom: tokens.spacingVerticalXXS,
    },
    meta: {
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground3,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
    },
});

interface Props {
    open: boolean;
    onClose: () => void;
    balance: AiUsageBalance | null;
}

/**
 * Whole days until the given reset instant, rounded up with a minimum of 1 —
 * a counter always resets at a future midnight UTC, so "za 1 den" is the
 * smallest truthful answer even minutes before the reset.
 */
function daysUntil(date: string | Date): number {
    const diff = new Date(date).getTime() - Date.now();
    return Math.max(1, Math.ceil(diff / 86_400_000));
}

/**
 * Consumed share of a limit as a whole percentage. Anything spent rounds up to
 * at least 1 % so a nonzero bar is never captioned "0 %".
 */
function toPercent(ratio: number): number {
    return ratio > 0 ? Math.max(1, Math.round(ratio * 100)) : 0;
}

/** Meter colour tracks how close the limit is to exhaustion. */
function barColor(ratio: number): "brand" | "warning" | "error" {
    return ratio >= 0.95 ? "error" : ratio >= 0.8 ? "warning" : "brand";
}

/**
 * On-demand usage panel — the AI assistant's equivalent of a `/usage` dialog,
 * opened from the settings menu so the balance stays out of the way during
 * normal work. Deliberately soft: it shows relative consumption only (percent
 * of the limit — credits are an internal accounting unit and never appear),
 * with a single meter for whichever cap (weekly smoothing or period allowance)
 * is currently closer to exhaustion, plus that cap's reset date. This is the
 * same "binding limit" the panel's gear dot tracks, so the dialog always
 * explains the dot.
 */
export function AiUsageDialog({ open, onClose, balance }: Props) {
    const styles = useStyles();
    const intl = useIntl();

    const account = balance?.account;
    const customer = balance?.customer;

    const monthlyRatio = account?.allowanceCredits ? account.spentCredits / account.allowanceCredits : null;
    const weeklyRatio = account?.weeklyAllowanceCredits
        ? (account.weeklySpentCredits ?? 0) / account.weeklyAllowanceCredits
        : null;

    // The binding limit: the weekly cap when it is the more exhausted one
    // (labelled "Tento týden"), the period allowance otherwise. Its reset is
    // the moment the user can actually do more, so it is the one date shown.
    const weeklyBinds = weeklyRatio != null && (monthlyRatio == null || weeklyRatio > monthlyRatio);
    const accountRatio = weeklyBinds ? weeklyRatio : monthlyRatio;
    const accountResetDate = weeklyBinds ? account?.weekEnd : account?.periodEnd;

    // The `account` layer is the account the user's own key bills to (their
    // personal seat, or the shared account when they run on the shared key) —
    // always their own consumption, never another key's. The `customer` layer
    // is the organization-wide budget cap above all accounts. For a personal
    // account it is only relevant when its cap is close to (or at) exhaustion:
    // that cap can refuse the user's task even with personal credits left. An
    // org line with no cap can never refuse them, so it is pure noise for a
    // personal user and stays hidden. For a shared account the two layers
    // describe the same pool, so it is always shown.
    const customerRatio = customer?.budgetCredits ? customer.spentCredits / customer.budgetCredits : null;
    const showCustomer = !!customer && (
        account?.accountType !== "personal"
        || (customerRatio != null && customerRatio >= 0.8)
    );
    const hasData = !!account || showCustomer;

    return (
        <Dialog open={open} onOpenChange={(_e, data) => { if (!data.open) onClose(); }}>
            <DialogSurface className={styles.surface}>
                <DialogBody>
                    <DialogTitle>
                        <FormattedMessage {...aiAssistantMessages.usageDialogTitle} />
                    </DialogTitle>
                    <DialogContent>
                        {!hasData && (
                            <div className={styles.empty}>
                                <FormattedMessage {...aiAssistantMessages.usageNoData} />
                            </div>
                        )}
                        {account && (
                            <div className={styles.section}>
                                <div className={styles.heading}>
                                    <FormattedMessage {...(account.accountType === "shared"
                                        ? aiAssistantMessages.usageSharedAccountHeading
                                        : aiAssistantMessages.usageAccountHeading)} />
                                </div>
                                {accountRatio != null ? (
                                    <>
                                        <div className={styles.amount}>
                                            <FormattedMessage
                                                {...(weeklyBinds
                                                    ? aiAssistantMessages.balanceWeeklyUsedPercent
                                                    : aiAssistantMessages.balanceUsedPercent)}
                                                values={{ percent: toPercent(accountRatio) }}
                                            />
                                        </div>
                                        <ProgressBar
                                            className={styles.bar}
                                            value={Math.min(accountRatio, 1)}
                                            color={barColor(accountRatio)}
                                        />
                                        {accountResetDate && (
                                            <div className={styles.meta}>
                                                <FormattedMessage
                                                    {...aiAssistantMessages.balanceResets}
                                                    values={{
                                                        date: intl.formatDate(accountResetDate, { dateStyle: "medium" }),
                                                        days: daysUntil(accountResetDate),
                                                    }}
                                                />
                                            </div>
                                        )}
                                    </>
                                ) : (
                                    <div className={styles.meta}>
                                        <FormattedMessage {...aiAssistantMessages.balanceUnlimited} />
                                    </div>
                                )}
                                {account.planName && (
                                    <div className={styles.meta}>
                                        <FormattedMessage
                                            {...aiAssistantMessages.balancePlan}
                                            values={{ plan: account.planName }}
                                        />
                                    </div>
                                )}
                            </div>
                        )}
                        {showCustomer && customer && (
                            <div className={styles.section}>
                                <div className={styles.heading}>
                                    <FormattedMessage {...aiAssistantMessages.usageCustomerHeading} />
                                </div>
                                {customerRatio != null ? (
                                    <>
                                        <div className={styles.amount}>
                                            <FormattedMessage
                                                {...aiAssistantMessages.balanceUsedPercent}
                                                values={{ percent: toPercent(customerRatio) }}
                                            />
                                        </div>
                                        <ProgressBar
                                            className={styles.bar}
                                            value={Math.min(customerRatio, 1)}
                                            color={barColor(customerRatio)}
                                        />
                                    </>
                                ) : (
                                    <div className={styles.meta}>
                                        <FormattedMessage {...aiAssistantMessages.balanceUnlimited} />
                                    </div>
                                )}
                            </div>
                        )}
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="secondary" onClick={onClose}>
                            <FormattedMessage {...aiAssistantMessages.usageDialogClose} />
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}
