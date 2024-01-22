import {FC, useEffect} from 'react';
import {useSelector} from 'react-redux';
import { i18n, ListBox, Search, StoreHorizontalLoader} from 'components/shared';
import {indexById} from 'stores/app/utils';
import {fundsFetchIfNeeded, fundsFilter} from '../../../actions/admin/fund';
import {renderFundItem} from '../../admin/adminRenderUtils';
import storeFromArea from '../../../shared/utils/storeFromArea';
import {AREA_ADMIN_FUNDS} from '../../../actions/admin/fund';
import ListPager from '../../shared/listPager/ListPager';
import { AdminFund } from 'typings/store';
import { getFundRows } from './utils';

import './FundList.scss';
import { useHistory } from 'react-router';
import { urlAdminFund } from '../../../constants';
import { ArrFundBaseVO } from 'api/ArrFundBaseVO';
import { useThunkDispatch } from 'utils/hooks';

export const FundList:FC<{
    activeFund?: AdminFund;
}> = ({
    activeFund,
}) => {
    const dispatch = useThunkDispatch();
    const funds = useSelector((state: any) => storeFromArea(state, AREA_ADMIN_FUNDS))
    const fundRows = getFundRows(funds);
    const history = useHistory();

    useEffect(()=>{
        dispatch(fundsFilter('',0));
    },[dispatch]);

    useEffect(()=>{
        dispatch(fundsFetchIfNeeded());
    },[
        funds.filter.from,
        funds.filter.text,
        dispatch,
    ]);

    const {from, pageSize, text} = funds.filter;

    const activeIndex = activeFund && activeFund.id !== null ? indexById(fundRows, activeFund.id) : undefined;

    const handleSelect = (item: ArrFundBaseVO) => history.push(urlAdminFund(item.id));
    const handleSearch = (filterText: string) => dispatch(fundsFilter(filterText, from));
    const handleSearchClear = () => dispatch(fundsFilter('', from));
    const handleChangePage = (nextFrom: number) => nextFrom !== from && dispatch(fundsFilter(text, nextFrom))

    return <div className="fund-list-container">
        <Search
            onSearch={handleSearch}
            onClear={handleSearchClear}
            placeholder={i18n('search.input.search')}
            value={funds.filter.text || ''}
        />
        <StoreHorizontalLoader store={funds} />
        {funds.fetched && (
            <ListBox
                key="funds"
                className="fund-listbox"
                items={fundRows}
                activeIndex={activeIndex}
                renderItemContent={renderFundItem}
                onFocus={handleSelect}
                onSelect={handleSelect}
            />
        )}
        {(funds.count > pageSize || from !== 0) && (
            <ListPager
                prev={handleChangePage}
                next={handleChangePage}
                from={from}
                pageSize={pageSize}
                totalCount={funds.count}
            />
        )}
    </div>
}
