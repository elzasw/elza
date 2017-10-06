import React from 'react';
import './ListPager.less';
import { Icon } from 'components/shared';
import { DEFAULT_PARTY_LIST_MAX_SIZE } from '../../../actions/party/party';

/**
 * Komponenta k ovládání stránkování seznamu
 */
export default class ListPager extends React.Component {
    static PropTypes = {
        from: React.PropTypes.number.required,
        prev: React.PropTypes.func.required,
        next: React.PropTypes.func.required,
        maxSize: React.PropTypes.number.required
    };

    static defaultProps = {
        from: 0,
        maxSize: 0
    };

    render() {
        const { prev, next, from, maxSize } = this.props;
        return (
            <div className="list-pager">
                <Icon onClick={prev} glyph="fa-chevron-left fa-lg" className="arrow-left" />
                <span className="middle-text">
                    {from} - {from + DEFAULT_PARTY_LIST_MAX_SIZE} z {maxSize}
                </span>
                <Icon onClick={next} glyph="fa fa-chevron-right fa-lg" className="arrow-right" />
            </div>
        );
    }
}
