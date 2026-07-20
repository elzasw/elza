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
    const hasData = !!account || !!customer;

    const accountRatio = account?.allowanceCredits ? account.spentCredits / account.allowanceCredits : null;
    // Meter colour tracks how close the allowance is to exhaustion.
    const accountBarColor = accountRatio == null ? "brand"
        : accountRatio >= 0.95 ? "error"
        : accountRatio >= 0.8 ? "warning"
        : "brand";

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
                                    <FormattedMessage {...aiAssistantMessages.usageAccountHeading} />
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
                                <div className={styles.meta}>
                                    {account.accountType === "personal" && (
                                        <FormattedMessage {...aiAssistantMessages.balanceAccountPersonal} />
                                    )}
                                    {account.accountType === "shared" && (
                                        <FormattedMessage {...aiAssistantMessages.balanceAccountShared} />
                                    )}
                                    {account.plan && (
                                        <>
                                            {account.accountType ? " · " : ""}
                                            <FormattedMessage
                                                {...aiAssistantMessages.balancePlan}
                                                values={{ plan: account.plan }}
                                            />
                                        </>
                                    )}
                                </div>
                                {account.periodEnd && (
                                    <div className={styles.meta}>
                                        <FormattedMessage
                                            {...aiAssistantMessages.balanceResets}
                                            values={{ date: intl.formatDate(account.periodEnd, { dateStyle: "medium" }) }}
                                        />
                                    </div>
                                )}
                            </div>
                        )}
                        {customer && (
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
