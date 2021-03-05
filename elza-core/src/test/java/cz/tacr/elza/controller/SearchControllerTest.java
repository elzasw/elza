package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.AbstractFilter;
import cz.tacr.elza.test.controller.vo.ResultEntityRef;
import cz.tacr.elza.test.controller.vo.SearchParams;

public class SearchControllerTest extends AbstractControllerTest {

    @Test
    public void searchEntityTest() throws ApiException {
        ResultEntityRef result = searchApi.searchEntity(createSearchParam());
        assertNotNull(result);

        assertTrue(result.getCount() == 3);
    }

    @Test
    public void searchArchDescTest() throws ApiException {
        ResultEntityRef result = searchApi.searchArchDesc(createSearchParam());
        assertNotNull(result);

        assertTrue(result.getCount() == 0);
    }

    private SearchParams createSearchParam() {
        SearchParams searchParams = new SearchParams();
        return searchParams.addFiltersItem(new AbstractFilter()).offset(0).size(100);
    }
}
