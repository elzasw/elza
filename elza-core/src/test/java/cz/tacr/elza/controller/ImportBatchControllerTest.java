package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.web.client.HttpClientErrorException;

import cz.tacr.elza.test.controller.vo.BatchImportType;
import cz.tacr.elza.test.controller.vo.BatchState;
import cz.tacr.elza.test.controller.vo.CreateBatchDescCsv;
import cz.tacr.elza.test.controller.vo.CreateBatchEdx;
import cz.tacr.elza.test.controller.vo.FundImportStrategy;
import cz.tacr.elza.test.controller.vo.ImportBatchDescCsv;
import cz.tacr.elza.test.controller.vo.ImportBatchEdx;
import cz.tacr.elza.test.controller.vo.ImportBatchPage;

/**
 * Integration tests for {@link ImportBatchController}.
 *
 * <p>Same shared-DB per-class lifecycle as {@link AdminControllerTest}: entities
 * created here accumulate across the class run and are wiped by the next class's
 * {@code deleteTables()}. Counts are captured as baselines.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImportBatchControllerTest extends AbstractControllerTest {

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
     * Creates an EDX2 batch, reads it back polymorphically, updates its settings
     * and finally cancels it. Confirms the state machine walks through the
     * expected values.
     */
    @Test
    public void createReadUpdateCancelEdxBatch() {
        CreateBatchEdx params = new CreateBatchEdx();
        params.setName("edx-crud");
        params.setFundImportStrategy(FundImportStrategy.ALWAYS_NEW);
        params.setIgnoreRootNodes(false);
        params.setSkipError(false);
        params.setKeepFiles(false);

        ImportBatchEdx created = importBatchesApi.importBatchCreateEdx(params);
        assertNotNull(created.getBatchId());
        assertEquals(BatchState.PREPARATION, created.getState());
        assertEquals(FundImportStrategy.ALWAYS_NEW, created.getFundImportStrategy());

        Object fetched = importBatchesApi.importBatchGet(created.getBatchId());
        assertNotNull(fetched);

        params.setName("edx-crud-renamed");
        params.setFundImportStrategy(FundImportStrategy.PAIR_AND_CREATE);
        ImportBatchEdx updated = importBatchesApi.importBatchUpdateEdx(created.getBatchId(), params);
        assertEquals("edx-crud-renamed", updated.getName());
        assertEquals(FundImportStrategy.PAIR_AND_CREATE, updated.getFundImportStrategy());

        importBatchesApi.importBatchCancel(created.getBatchId());
    }

    /**
     * Creates a CSV batch, verifies its subtype, and deletes it while still in
     * PREPARATION. A follow-up GET is expected to fail with 404.
     */
    @Test
    public void createAndDeleteCsvBatch() {
        CreateBatchDescCsv params = new CreateBatchDescCsv();
        params.setName("csv-crud");
        params.setSeparator(";");
        params.setEncoding("UTF-8");
        params.setSkipError(true);
        params.setKeepFiles(false);

        ImportBatchDescCsv created = importBatchesApi.importBatchCreateCsv(params);
        assertNotNull(created.getBatchId());
        assertEquals(BatchImportType.ADD_DESC_ITEMS_CSV, created.getImportType());
        assertEquals(";", created.getSeparator());
        assertTrue(created.getSkipError());

        importBatchesApi.importBatchRemove(created.getBatchId());

        assertThrows(HttpClientErrorException.class,
                () -> importBatchesApi.importBatchGet(created.getBatchId()));
    }

    /**
     * Creating an EDX2 batch bumps the paged listing's totalCount by one.
     */
    @Test
    public void listReflectsCreation() {
        ImportBatchPage before = importBatchesApi.importBatchList(0, 10);
        int baseline = before.getTotalCount();

        CreateBatchEdx params = new CreateBatchEdx();
        params.setName("edx-list-baseline");
        params.setFundImportStrategy(FundImportStrategy.ALWAYS_NEW);
        params.setIgnoreRootNodes(false);
        params.setSkipError(false);
        params.setKeepFiles(false);
        importBatchesApi.importBatchCreateEdx(params);

        ImportBatchPage after = importBatchesApi.importBatchList(0, 10);
        assertEquals(baseline + 1, after.getTotalCount().intValue());
        assertTrue(!after.getItems().isEmpty());
    }
}
