import { getIntl } from 'components/shared/lang/intlInstance';
import { permissionScopeMessages } from '../permissionMessages';
import { AdminFunds } from 'typings/store';
import { ALL_ID } from 'actions/admin/adminPermissions';

export const getFundRows = (funds: AdminFunds) => {
    if(funds.fetched && funds.rows){
        if(
            funds.filter?.from && 
            funds.filter?.pageSize && 
            funds.filter.from > funds.filter.pageSize - 1
        ){ 
            return funds.rows; 
        }
        return [
            {
                id: ALL_ID, 
                name: getIntl().formatMessage(permissionScopeMessages.fundAll)
            },
            ...funds.rows,
        ];
    }

    return [];
};
