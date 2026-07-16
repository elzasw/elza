package cz.tacr.elza.controller.vo;

import java.util.List;

/**
 * Stránka filtrovaných uzlů včetně jejího čísla.
 */
public class FilterNodesPage {

    /** Číslo vrácené stránky, od 0. */
    private int page;

    /** Uzly na dané stránce (stejný obsah jako getFilterNodes). */
    private List<FilterNode> rows;

    public FilterNodesPage() {
    }

    public FilterNodesPage(final int page, final List<FilterNode> rows) {
        this.page = page;
        this.rows = rows;
    }

    public int getPage() {
        return page;
    }

    public void setPage(final int page) {
        this.page = page;
    }

    public List<FilterNode> getRows() {
        return rows;
    }

    public void setRows(final List<FilterNode> rows) {
        this.rows = rows;
    }
}
