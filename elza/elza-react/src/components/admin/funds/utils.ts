import FundsPermissionPanel from '../../admin/FundsPermissionPanel';
import { i18n } from 'components/shared';
import { AdminFunds } from 'typings/store';

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
                id: FundsPermissionPanel.ALL_ID, 
                name: i18n('admin.perms.tabs.funds.items.fundAll')
            },
            ...funds.rows,
        ];
    }

    return [];
};
