import React from 'react';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {Modal} from 'react-bootstrap';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import Search from "../shared/search/Search";
import {connect} from "react-redux";

/**
 * Formulář pro vyhledávání nad archivními soubory.
 */
class SearchFundsForm extends AbstractReactComponent {

    static propTypes = {};

    componentWillReceiveProps(nextProps) {

    }

    componentDidMount() {

    }

    /**
     * Vyhledání v archivních souborech.
     *
     * @param fulltext hledaný výraz
     */
    handleSearch = (fulltext) => {
        // TODO ELZA-1656: provede nastavení ve store + předá informaci listového store o vyhledání
        console.warn("#handleSearch: " + fulltext);
    };

    /**
     * Smazání výsledků vyhledávání.
     */
    handleClearSearch = () => {
        // TODO ELZA-1656: vymaže kompletně store pro vyhledávání
        console.warn("#handleClearSearch");
    };

    render() {
        return (
            <Modal.Body>
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleClearSearch}
                    placeholder={i18n('search.input.search')}
                    value={""} // zde se bude opírat hodnotu ze store
                />
                <div> {/*TODO ELZA-1656: opřít o výsledky hledání, nezobrazovat pokud není co k zobrazení */}
                    {i18n('arr.fund.search.result.count', 1)}
                    {/*TODO ELZA-1656: přidat komponentu pro zobrazení výsledků */}
                </div>
            </Modal.Body>
        )
    }
}

function mapStateToProps(state) {
    {/*TODO ELZA-1656: mapování potřebný dat ze store pro render */}
    return {}
}

export default connect(mapStateToProps)(SearchFundsForm);
