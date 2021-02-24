import PropTypes from 'prop-types';
import React, {FC} from 'react';
import './ListPager.scss';
import {Icon} from 'components/shared';

interface ListPagerProps {
    from: number;
    pageSize: number;
    totalCount: number;
    prev: (nextFrom: number, pageSize: number) => void;
    next: (nextFrom: number, pageSize: number) => void;
}

export const ListPager:FC<ListPagerProps> = ({
    from = 0,
    prev,
    next,
    pageSize = 0,
    totalCount = 0,
}) => {
    const handlePrevPage = () => {
        const newFrom = from - pageSize >= 0 ? from - pageSize : 0;
        prev(newFrom, pageSize);
    }

    const handleNextPage = () => {
        const newFrom = from + pageSize < totalCount ? from + pageSize : from;
        next(newFrom, pageSize);
    }

    let to = Math.min(from + pageSize, totalCount);

    return (
        <div className="list-pager">
            <Icon 
                onClick={handlePrevPage} 
                glyph="fa-chevron-left fa-lg" 
                className="arrow-left" 
            />
            <span className="middle-text">
                {from + 1} - {to} z {totalCount}
            </span>
            <Icon 
                onClick={handleNextPage} 
                glyph="fa fa-chevron-right fa-lg" 
                className="arrow-right" 
            />
        </div>
    );
}

ListPager.propTypes = {
    from: PropTypes.number.isRequired,
    prev: PropTypes.func.isRequired,
    next: PropTypes.func.isRequired,
    pageSize: PropTypes.number.isRequired,
    totalCount: PropTypes.number.isRequired,
};

export default ListPager;
