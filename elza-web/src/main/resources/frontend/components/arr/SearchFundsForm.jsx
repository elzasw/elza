import React from 'react';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Modal} from 'react-bootstrap';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import Search from "../shared/search/Search";
import {connect} from "react-redux";

//  Actions
import * as types from '../../actions/constants/ActionTypes.js';
import {fundModalFulltextChange, fundModalFulltextSearch} from '../../actions/arr/fundModal.jsx'

/**
 * Formulář pro vyhledávání nad archivními soubory.
 */
class SearchFundsForm extends AbstractReactComponent {
    static propTypes = {};

    componentWillReceiveProps(nextProps) {}

    componentDidMount() {}

    /**
     * Vyhledání v archivních souborech.
     *
     * @param fulltext hledaný výraz
     */
    handleSearch = (fulltext) => {
        console.warn("#handleSearch: " + fulltext);
        this.dispatch(fundModalFulltextSearch(fulltext));
    };

    /**
     * Smazání výsledků vyhledávání.
     */
    handleClearSearch = () => {
        console.warn("#handleClearSearch");
        this.dispatch(fundModalFulltextChange(''));
    };

    render() {
        return (
            <Modal.Body>
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleClearSearch}
                    placeholder={i18n('search.input.search')}
                    value={this.props.searchText}
                />
                <div> {/*TODO ELZA-1656: opřít o výsledky hledání, nezobrazovat pokud není co k zobrazení */}
                    {i18n('arr.fund.search.result.count', this.props.count)}
                    {/*TODO ELZA-1656: přidat komponentu pro zobrazení výsledků */}
                </div>
            </Modal.Body>
        )
    }
}

function mapStateToProps(state) {
    const { fundModal } = state.arrRegion;
    {/*TODO ELZA-1656: mapování potřebný dat ze store pro render */}
    return {
        searchText: fundModal.filterText,
        result: fundModal.searchedIDs,
        count: fundModal.count
    }
}

export default connect(mapStateToProps)(SearchFundsForm);
