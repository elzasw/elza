import PropTypes from 'prop-types';
import React from 'react';
import './ListPager.less';
import { Icon } from 'components/shared';

/**
 * Komponenta k ovládání stránkování seznamu
 */
export default class ListPager extends React.Component {
    static PropTypes = {
        from: PropTypes.number.required,
        prev: PropTypes.func.required,
        next: PropTypes.func.required,
        maxSize: PropTypes.number.required,
        totalCount: PropTypes.number.required
    };

    static defaultProps = {
        from: 0,
        maxSize: 0,
        totalCount: 0
    };

    getMax = () => {
        const {from, maxSize, totalCount} = this.props;
        let to = from + maxSize;
        return Math.min(to, totalCount);
    }

    render() {
        const { prev, next, from, totalCount } = this.props;
        let to = this.getMax();

        return (
            <div className="list-pager">
                <Icon onClick={prev} glyph="fa-chevron-left fa-lg" className="arrow-left" />
                <span className="middle-text">
                    {from + 1} - {to} z {totalCount}
                </span>
                <Icon onClick={next} glyph="fa fa-chevron-right fa-lg" className="arrow-right" />
            </div>
        );
    }
}
