import {FC, useEffect} from 'react';
import {useSelector} from 'react-redux';
import { i18n, ListBox, Search, StoreHorizontalLoader} from 'components/shared';
import {indexById} from 'stores/app/utils';
import storeFromArea from '../../shared/utils/storeFromArea';
import ListPager from '../shared/listPager/ListPager';
import {Aip} from 'typings/store';
import { getAipRows } from './utils';

import './AipList.scss';
import { useHistory } from 'react-router';
import {urlAip} from '../../constants';
import { useThunkDispatch } from 'utils/hooks';
import {aipsFetchIfNeeded, aipsFilter, AREA_AIPS} from "../../actions/aip/aip";
import {ArrAipVO} from "../../api/ArrAipVO.ts";
import {renderAipItem} from "./aipRenderUtils";

export const AipList:FC<{
    activeAip?: Aip;
}> = ({
                               activeAip,
}) => {
    const dispatch = useThunkDispatch();
    const aips = useSelector((state: any) => storeFromArea(state, AREA_AIPS))
    const aipRows = getAipRows(aips);
    const history = useHistory();

    useEffect(()=>{
        dispatch(aipsFilter('',0));
    },[dispatch]);

    useEffect(()=>{
        dispatch(aipsFetchIfNeeded());
    },[
        aips.filter.from,
        aips.filter.text,
        dispatch,
    ]);

    const {from, pageSize, text} = aips.filter;

    const activeIndex = activeAip && activeAip.id !== null ? indexById(aipRows, activeAip.id) : undefined;

    const handleSelect = (item: ArrAipVO) => history.push(urlAip(item.id));
    const handleSearch = (filterText: string) => dispatch(aipsFilter(filterText, from));
    const handleSearchClear = () => dispatch(aipsFilter('', from));
    const handleChangePage = (nextFrom: number) => nextFrom !== from && dispatch(aipsFilter(text, nextFrom))

    return <div className="aip-list-container">
        <Search
            onSearch={handleSearch}
            onClear={handleSearchClear}
            placeholder={i18n('search.input.search')}
            value={aips.filter.text || ''}
        />
        <StoreHorizontalLoader store={aips} />
        {aips.fetched && (
            <ListBox
                key="aips"
                className="aip-listbox"
                items={aipRows}
                activeIndex={activeIndex}
                renderItemContent={renderAipItem}
                onFocus={handleSelect}
                onSelect={handleSelect}
            />
        )}
        {(aips.count > pageSize || from !== 0) && (
            <ListPager
                prev={handleChangePage}
                next={handleChangePage}
                from={from}
                pageSize={pageSize}
                totalCount={aips.count}
            />
        )}
    </div>
}
