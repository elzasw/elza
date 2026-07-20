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
 * On-demand credit-usage panel — the AI assistant's equivalent of a `/usage`
 * dialog, opened from the settings menu so the balance stays out of the way
 * during normal work. All numbers are the provider's final credits; the panel
 * only displays them (and the spent/allowance ratio for the meter), never any
 * multiplier — that stays hidden in the provider/CSC.
 */
export function AiUsageDialog({ open, onClose, balance }: Props) {
    const styles = useStyles();
    const intl = useIntl();
    const credits = (value: number) => intl.formatNumber(value, { maximumFractionDigits: 1 });

    const account = balance?.account;
    const customer = balance?.customer;

    const accountRatio = account?.allowanceCredits ? account.spentCredits / account.allowanceCredits : null;
    // Meter colour tracks how close the allowance is to exhaustion.
    const accountBarColor = accountRatio == null ? "brand"
        : accountRatio >= 0.95 ? "error"
        : accountRatio >= 0.8 ? "warning"
        : "brand";

    // The optional weekly smoothing cap under the monthly allowance — its own
    // meter with the same thresholds, and its own (always sooner) reset date.
    const weeklyRatio = account?.weeklyAllowanceCredits
        ? (account.weeklySpentCredits ?? 0) / account.weeklyAllowanceCredits
        : null;
    const weeklyBarColor = weeklyRatio == null ? "brand"
        : weeklyRatio >= 0.95 ? "error"
        : weeklyRatio >= 0.8 ? "warning"
        : "brand";

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
                                <div className={styles.amount}>
                                    {account.allowanceCredits != null ? (
                                        <FormattedMessage
                                            {...aiAssistantMessages.balanceSpentOfAllowance}
                                            values={{
                                                spent: credits(account.spentCredits),
                                                allowance: credits(account.allowanceCredits),
                                            }}
                                        />
                                    ) : (
                                        <FormattedMessage
                                            {...aiAssistantMessages.balanceSpent}
                                            values={{ spent: credits(account.spentCredits) }}
                                        />
                                    )}
                                </div>
                                {accountRatio != null ? (
                                    <>
                                        <ProgressBar
                                            className={styles.bar}
                                            value={Math.min(accountRatio, 1)}
                                            color={accountBarColor}
                                        />
                                        <div className={styles.meta}>
                                            <FormattedMessage
                                                {...aiAssistantMessages.balanceUsedPercent}
                                                values={{ percent: Math.round(accountRatio * 100) }}
                                            />
                                        </div>
                                    </>
                                ) : (
                                    <div className={styles.meta}>
                                        <FormattedMessage {...aiAssistantMessages.balanceUnlimited} />
                                    </div>
                                )}
                                {account.plan && (
                                    <div className={styles.meta}>
                                        <FormattedMessage
                                            {...aiAssistantMessages.balancePlan}
                                            values={{ plan: account.plan }}
                                        />
                                    </div>
                                )}
                                {account.periodEnd && (
                                    <div className={styles.meta}>
                                        <FormattedMessage
                                            {...aiAssistantMessages.balanceResets}
                                            values={{
                                                date: intl.formatDate(account.periodEnd, { dateStyle: "medium" }),
                                                days: daysUntil(account.periodEnd),
                                            }}
                                        />
                                    </div>
                                )}
                                {account.weeklyAllowanceCredits != null && (
                                    <>
                                        <div className={styles.amount}>
                                            <FormattedMessage
                                                {...aiAssistantMessages.balanceWeekly}
                                                values={{
                                                    spent: credits(account.weeklySpentCredits ?? 0),
                                                    allowance: credits(account.weeklyAllowanceCredits),
                                                }}
                                            />
                                        </div>
                                        {weeklyRatio != null && (
                                            <ProgressBar
                                                className={styles.bar}
                                                value={Math.min(weeklyRatio, 1)}
                                                color={weeklyBarColor}
                                            />
                                        )}
                                        {account.weekEnd && (
                                            <div className={styles.meta}>
                                                <FormattedMessage
                                                    {...aiAssistantMessages.balanceWeeklyResets}
                                                    values={{
                                                        date: intl.formatDate(account.weekEnd, { dateStyle: "medium" }),
                                                        days: daysUntil(account.weekEnd),
                                                    }}
                                                />
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>
                        )}
                        {showCustomer && customer && (
                            <div className={styles.section}>
                                <div className={styles.heading}>
                                    <FormattedMessage {...aiAssistantMessages.usageCustomerHeading} />
                                </div>
                                <div className={styles.amount}>
                                    {customer.budgetCredits != null ? (
                                        <FormattedMessage
                                            {...aiAssistantMessages.balanceCustomer}
                                            values={{
                                                spent: credits(customer.spentCredits),
                                                budget: credits(customer.budgetCredits),
                                            }}
                                        />
                                    ) : (
                                        <FormattedMessage
                                            {...aiAssistantMessages.balanceCustomerNoCap}
                                            values={{ spent: credits(customer.spentCredits) }}
                                        />
                                    )}
                                </div>
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
