package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.service.IndexWorkService;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.test.controller.vo.ResultEntityRef;
import cz.tacr.elza.test.controller.vo.SearchParams;

public class SearchControllerTest extends AbstractControllerTest {

    @Autowired
    IndexWorkService indexWorkService;

    @Test
    public void searchEntityTest() throws ApiException, InterruptedException {
        // wait for ending lucene indexing
        while (indexWorkService.isActive()) {
            Thread.sleep(100);
        }

        // filter is null
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
        // filter is null
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
