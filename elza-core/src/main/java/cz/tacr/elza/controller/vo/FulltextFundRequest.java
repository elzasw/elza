package cz.tacr.elza.controller.vo;

/**
 * Fultextove vyhledavani
 */
public class FulltextFundRequest {

    // --- fields ---

    private String searchValue;

    // --- getters/setters ---

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(final String searchValue) {
        this.searchValue = searchValue;
    }

    // --- constructor ---

    public FulltextFundRequest() {
    }

    public FulltextFundRequest(String searchValue) {
        this.searchValue = searchValue;
    }
}
