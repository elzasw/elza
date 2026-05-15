import { useSelector } from "react-redux";
import { PublicationType } from "elza-api";
import { AppState } from "typings/store";
import * as perms from "actions/user/Permission";

function canUsePublicationType(type: PublicationType, fundId: number, userDetail: { hasOne: (...args: any[]) => boolean }): boolean {
    if (!type.allowPermExport && !type.allowPermPublication) {
        return userDetail.hasOne(perms.ADMIN);
    }
    return !!(
        (type.allowPermExport && userDetail.hasOne(perms.FUND_EXPORT_ALL, { type: perms.FUND_EXPORT, fundId })) ||
        (type.allowPermPublication && userDetail.hasOne(perms.FUND_PUBLISH_ALL, { type: perms.FUND_PUBLISH, fundId }))
    );
}

export function useCanUsePublicationType(fundId: number): (type: PublicationType) => boolean {
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
    return (type: PublicationType) => canUsePublicationType(type, fundId, userDetail);
}
