import {Aips} from 'typings/store';

export const getAipRows = (aips: Aips) => {
    if(aips.fetched && aips.rows){
        if(
            aips.filter?.from &&
            aips.filter?.pageSize &&
            aips.filter.from > aips.filter.pageSize - 1
        ){
            return aips.rows;
        }
        return [
            ...aips.rows,
        ];
    }

    return [];
};
