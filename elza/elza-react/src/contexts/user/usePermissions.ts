import { useMemo } from 'react';
import * as perms from 'actions/user/Permission';
import { useUserContext } from './useUserContext';

type PermissionType = string;

interface PermissionQuery {
    type: PermissionType;
    fundId?: number;
    scopeId?: number;
}

type PermissionArg = PermissionType | PermissionQuery;

function checkPermission(permissionsMap: Record<string, any>, permission: PermissionArg) {
    if (typeof permission === 'string') {
        return !!permissionsMap[permission];
    }

    const perm = permissionsMap[permission.type];
    if (!perm) {
        return false;
    }

    switch (permission.type) {
        case perms.FUND_RD:
        case perms.FUND_ARR:
        case perms.FUND_OUTPUT_WR:
        case perms.FUND_VER_WR:
        case perms.FUND_ISSUE_ADMIN:
        case perms.FUND_EXPORT:
        case perms.FUND_BA:
        case perms.FUND_CL_VER_WR:
            return !!perm.fundIdsMap[permission.fundId!];
        case perms.AP_SCOPE_RD:
        case perms.AP_SCOPE_WR:
        case perms.AP_CONFIRM:
        case perms.AP_EDIT_CONFIRMED:
            return !!perm.scopeIdsMap[permission.scopeId!];
        default:
            return true;
    }
}

export function usePermissions() {
    const { userDetail } = useUserContext();
    const permissionsMap = userDetail.permissionsMap;

    return useMemo(() => {
        const hasOne = (...permissions: PermissionArg[]) => {
            if (permissionsMap[perms.ADMIN]) {
                return true;
            }
            return permissions.some((permission) => checkPermission(permissionsMap, permission));
        };

        const hasAll = (...permissions: PermissionArg[]) => {
            if (permissionsMap[perms.ADMIN]) {
                return true;
            }
            return permissions.every((permission) => checkPermission(permissionsMap, permission));
        };

        const isAdmin = () => !!permissionsMap[perms.ADMIN];

        const canReadFund = (fundId?: number) =>
            hasOne(perms.FUND_ADMIN, perms.FUND_RD_ALL, { type: perms.FUND_RD, fundId }, perms.FUND_ARR_ALL, {
                type: perms.FUND_ARR,
                fundId,
            });

        const canArrangeFund = (fundId?: number) =>
            hasOne(perms.FUND_ADMIN, perms.FUND_ARR_ALL, { type: perms.FUND_ARR, fundId });

        const canWriteOutput = (fundId?: number) =>
            hasOne(perms.FUND_ADMIN, perms.FUND_OUTPUT_WR_ALL, { type: perms.FUND_OUTPUT_WR, fundId });

        const canRunBulkActions = (fundId?: number) =>
            hasOne(perms.FUND_ADMIN, perms.FUND_BA_ALL, { type: perms.FUND_BA, fundId });

        return { hasOne, hasAll, isAdmin, canReadFund, canArrangeFund, canWriteOutput, canRunBulkActions };
    }, [permissionsMap]);
}
