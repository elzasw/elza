package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.AbstractFilter;
import cz.tacr.elza.test.controller.vo.ResultEntityRef;
import cz.tacr.elza.test.controller.vo.SearchParams;
import cz.tacr.elza.test.controller.vo.Sorting;

public class SearchControllerTest extends AbstractControllerTest {

    @Test
    public void searchEntityTest() throws ApiException {
        AbstractFilter filter = new AbstractFilter();
        filter.setFilterType("filterType");

        Sorting sortItem = new Sorting();
        sortItem.setField("state");

        SearchParams searchParams = new SearchParams();
        searchParams.addFiltersItem(filter).offset(0).size(10).addSortItem(sortItem);

        //ResultEntityRef responce = searchApi.searchEntity(searchParams);
        //assertNotNull(responce);
    }
    
}
