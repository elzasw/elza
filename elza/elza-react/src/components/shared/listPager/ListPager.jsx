import PropTypes from 'prop-types';
import React from 'react';
import './ListPager.less';
import { Icon } from 'components/shared';

/**
 * Komponenta k ovládání stránkování seznamu
 */
export default class ListPager extends React.Component {
    static propTypes = {
        from: PropTypes.number.isRequired,
        prev: PropTypes.func.isRequired,
        next: PropTypes.func.isRequired,
        maxSize: PropTypes.number.isRequired,
        totalCount: PropTypes.number.isRequired
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
