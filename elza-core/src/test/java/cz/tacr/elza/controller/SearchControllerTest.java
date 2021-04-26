package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.AbstractFilter;
import cz.tacr.elza.test.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.test.controller.vo.ResultEntityRef;
import cz.tacr.elza.test.controller.vo.SearchParams;

public class SearchControllerTest extends AbstractControllerTest {

    @Test
    public void searchEntityTest() throws ApiException {
        ResultEntityRef result = searchApi.searchEntity(createSearchParamEmpty());
        assertNotNull(result);

        assertTrue(result.getCount() == 3);

        SearchParams sp = createSearchParamText("Firma");
        result = searchApi.searchEntity(sp);
        assertNotNull(result);

        assertTrue(result.getCount() == 2);
    }

    @Test
    public void searchArchDescTest() throws ApiException {
        // empty filter
        ResultEntityRef result = searchApi.searchArchDesc(createSearchParamEmpty());
        assertNotNull(result);

        assertTrue(result.getCount() == 0);

        // TODO: Add search test with some data in DB
    }

    private SearchParams createSearchParamEmpty() {
        SearchParams searchParams = new SearchParams();
        return searchParams.offset(0).size(100);
    }

    private SearchParams createSearchParamText(String value) {
        SearchParams searchParams = new SearchParams();
        MultimatchContainsFilter mcf = new MultimatchContainsFilter().value(value);
        mcf.setFilterType("contains");
        return searchParams.addFiltersItem(mcf).offset(0).size(100);
    }
}
