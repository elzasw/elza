package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import cz.tacr.elza.test.controller.vo.FilterType;
import cz.tacr.elza.test.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.test.controller.vo.ResultEntityRef;
import cz.tacr.elza.test.controller.vo.SearchParams;

/**
 * Tests of SearchController against the SIMPLE-DEV package fixture
 * (3 pre-loaded access points, 2 matching the text "Firma").
 *
 * <p>Uses per-class lifecycle. Tests are read-only — they only query the
 * search API, no DB mutation — so absolute count assertions against the
 * SIMPLE-DEV fixture remain valid across siblings without any cleanup.
 *
 * <p>{@link #searchEntityTest()} waits for Hibernate Search to finish
 * indexing before querying; this is the dominant cost (several seconds
 * after a fresh package load). Under PER_CLASS the wait happens once per
 * class run, not per test method — that's where the speedup comes from.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchControllerTest extends AbstractControllerTest {

    @BeforeAll
    public void initOnce() throws Exception {
        super.setUp();
    }

    @AfterAll
    public void cleanupOnce() {
        super.tearDown();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        // no-op: setup is done once in @BeforeAll initOnce()
    }

    @Override
    @AfterEach
    public void tearDown() {
        // no-op: cleanup is done once in @AfterAll cleanupOnce()
    }

    /**
     * Search of access-point entities. Waits for Hibernate Search to finish
     * indexing (SIMPLE-DEV loaded 3 APs at class startup; the startup mass
     * indexer and the outbox-polling processor must both settle before search
     * returns meaningful results).
     *
     * <p><b>Creates:</b> nothing.
     * <br><b>Cleans up:</b> n/a.
     * <br><b>Fixture dependency:</b> SIMPLE-DEV package's 3 access points,
     * 2 of which contain the text "Firma".
     */
    @Test
    public void searchEntityTest() {
        helperTestService.waitForIndexUpdate();

        // filter is null
        ResultEntityRef result = searchApi.searchEntity(createSearchParamEmpty());
        assertNotNull(result);
        assertEquals(3, result.getCount().intValue());

        // filter with search text
        result = searchApi.searchEntity(createSearchParamText("Firma"));
        assertNotNull(result);
        assertEquals(2, result.getCount().intValue());
    }

    /**
     * Search of archival descriptions. No fund is imported in this class, so
     * expected count is 0.
     *
     * <p><b>Creates:</b> nothing.
     * <br><b>Cleans up:</b> n/a.
     * <br><b>Fixture dependency:</b> no funds / arch-desc entities exist.
     */
    @Test
    public void searchArchDescTest() {
        helperTestService.waitForIndexUpdate();

        // filter is null
        ResultEntityRef result = searchApi.searchArchDesc(createSearchParamEmpty());
        assertNotNull(result);

        assertEquals(0, result.getCount().intValue());

        // TODO: Add search test with some data in DB
    }

    private SearchParams createSearchParamEmpty() {
        SearchParams searchParams = new SearchParams();
        return searchParams.offset(0).size(100);
    }

    private SearchParams createSearchParamText(String value) {
        SearchParams searchParams = new SearchParams();
        MultimatchContainsFilter mcf = new MultimatchContainsFilter().value(value);
        mcf.setFilterType(FilterType.CONTAINS);
        return searchParams.addFiltersItem(mcf).offset(0).size(100);
    }
}
