import { BulkAction, BulkActionType } from 'elza-api';

/** The part of a fund in the store that holds the bulk actions of its rule set. */
interface FundWithActions {
    fundAction?: {
        config?: {
            data?: BulkAction[];
        };
    };
}

/**
 * Bulk actions of the rule set of the fund, as loaded by fundActionFetchConfigIfNeeded; empty until
 * loaded.
 */
export function getFundBulkActions(fund: FundWithActions | null | undefined): BulkAction[] {
    return fund?.fundAction?.config?.data ?? [];
}

/**
 * The persistent sort of the rule set of the fund. Recognised by its kind: every rule set names its
 * own action file.
 */
export function getPersistentSortAction(fund: FundWithActions | null | undefined): BulkAction | undefined {
    return getFundBulkActions(fund).find(action => action.type === BulkActionType.PersistentSort);
}

/** Whether the code names the persistent sort of the fund. */
export function isPersistentSortAction(fund: FundWithActions | null | undefined, code: string | null | undefined): boolean {
    const action = getPersistentSortAction(fund);
    return action != null && action.code === code;
}

/** Whether the rule set of the fund offers the bulk action of the code. */
export function hasBulkAction(fund: FundWithActions | null | undefined, code: string): boolean {
    return getFundBulkActions(fund).some(action => action.code === code);
}
