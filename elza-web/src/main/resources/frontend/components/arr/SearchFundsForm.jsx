import React from 'react';
import {connect} from "react-redux";
import {Modal} from 'react-bootstrap';
const classNames = require('classnames');
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import {AbstractReactComponent, FormInput, Icon, i18n} from 'components/shared';

import * as fundSearchActions from '../../actions/arr/fundSearch.jsx'
import Search from "../shared/search/Search";
import Loading from "../shared/loading/Loading";
import HorizontalLoader from "../shared/loading/HorizontalLoader";

import './SearchFundsForm.less';

const FUND_NAME_MAX_CHARS = 60

/**
 * Formulář pro vyhledávání nad archivními soubory.
 */
class SearchFundsForm extends AbstractReactComponent {
    static propTypes = {};

    componentWillReceiveProps(nextProps) {
        this.props.dispatch(fundSearchActions.fundSearchFetchIfNeeded());
    }

    componentDidMount() {
        this.props.dispatch(fundSearchActions.fundSearchFetchIfNeeded());
    }

    /**
     * Vyhledání v archivních souborech.
     *
     * @param fulltext hledaný výraz
     */
    handleSearch = (fulltext) => {
        this.props.dispatch(fundSearchActions.fundSearchFulltextChange(fulltext));
    };

    /**
     * Smazání výsledků vyhledávání.
     */
    handleClearSearch = () => {
        this.props.dispatch(fundSearchActions.fundSearchFulltextClear());
    };

    /**
     * Zobrazení seznamu výskytů hledaného výrazu v AS
     */
    handleFundClick = (fund) => {
        this.props.dispatch(fundSearchActions.fundSearchExpandFund(fund));
    };

    /**
     * Renderování vyhledaného archivního souboru.
     * @param fund {Object} uzel
     * @return {Object} view
     */
    renderFund = (fund, totalCount) => {
        const {expanded} = fund;

        const expColCls = 'exp-col ' + (expanded ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o');
        const expCol = <span className={expColCls} onClick={() => this.handleFundClick(fund)}></span>

        const cls = classNames({
            fund: true,
            opened: expanded,
            closed: !expanded,
        });
        const iconClass = classNames({
            "fund-icon": true,
            "fund-icon-color": true
        });

        let name = fund.name;
        if (name.length > FUND_NAME_MAX_CHARS) {
            name = name.substring(0, FUND_NAME_MAX_CHARS - 3) + '...'
        }

        const iconStyle = {
            backgroundColor: '#ffffff',
            color: '#000000' 
        }

        return <div key={fund.id} className={cls}>
            {expCol}
            <Icon className={iconClass} style={iconStyle} fill={iconStyle.backgroundColor} stroke="none" glyph="fa-database"/>
            <div
                title={fund.name}
                className="fund-label"
            >
                {name} ({fund.count})
            </div>
        </div>;
    };

    render() {
        const {fundSearch} = this.props;
        const isFulltext = fundSearch.fulltext.length > 0;
        const totalCount =  this.getTotalCount(fundSearch.funds);

        return (
            <Modal.Body>
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleClearSearch}
                    placeholder={i18n('search.input.search')}
                    value={fundSearch.fulltext}
                />
                {isFulltext && i18n('arr.fund.search.result.count', totalCount)}
                <div className={`fund-search ${isFulltext && totalCount > 0 ? 'result' : 'no-fulltext'}`}>
                    {isFulltext 
                        ? this.renderResult() 
                        : i18n('arr.fund.search.noFulltext'
                    )}
                </div>
            </Modal.Body>
        )
    }

    renderResult = () => {
        const {fundSearch} = this.props;

        const result = [];

        if (fundSearch.isFetching) {
            result.push(<HorizontalLoader hover showText={false} key="loader"/>);
        }

        if (fundSearch.fetched) {
            const totalCount = this.getTotalCount(fundSearch.funds);

            result.push(
                <div key="result" className="result-list">
                    {fundSearch.funds.length > 0 &&
                        <div className="result-list">
                            {fundSearch.funds.map(fund => this.renderFund(fund, totalCount))}
                        </div>        
                    }
                </div>
            )
        }

        return result;
    };

    getTotalCount = (funds) => {
        let count = 0;
        funds.forEach(fund => count += fund.count);
        return count;
    }
}

function mapStateToProps(state) {
    const {fundSearch} = state.arrRegion;
    return {
        fundSearch
    }
}

export default connect(mapStateToProps)(SearchFundsForm);
