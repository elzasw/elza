import React from 'react';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Modal} from 'react-bootstrap';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import Search from "../shared/search/Search";
import {connect} from "react-redux";
import * as fundSearchActions from '../../actions/arr/fundSearch.jsx'
import Loading from "../shared/loading/Loading";
import HorizontalLoader from "../shared/loading/HorizontalLoader";

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

    render() {
        const {fundSearch} = this.props;
        return (
            <Modal.Body>
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleClearSearch}
                    placeholder={i18n('search.input.search')}
                    value={fundSearch.fulltext}
                />
                {this.renderResult()}
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
            result.push(<div key="result">
                {i18n('arr.fund.search.result.count', this.getAllCount(fundSearch.funds))}
                {/*TODO ELZA-1656: přidat komponentu pro zobrazení výsledků */}
            </div>)
        }

        return result;
    };

    getAllCount = (funds) => {
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
