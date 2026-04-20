package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tests of bulk-action controller (running SRD_GENERATOR_UNIT_ID,
 * SRD_FUND_VALIDATION, SRD_GENERATOR_SERIAL_NUMBER, interrupt flow).
 *
 * <p>Uses per-class lifecycle. The test XML ({@value #XML_FILE}) contains
 * hardcoded node UUIDs, so re-importing it per test fails with a unique-key
 * violation on {@code arr_node.uuid}. Instead the import runs once in
 * {@link #initOnce()} and all four tests share the same {@link #fundVersionId}.
 *
 * <p>Because bulk actions accumulate across tests on the shared fund,
 * {@link #getBulkActionState(int, String)} returns the <em>latest</em> run by
 * id (not the first one encountered) so that a test querying the state of a
 * run it just queued doesn't see a stale FINISHED entry from a previous test.
 *
 * <h2>Test ordering (enforced via {@link Order})</h2>
 * Tests must run in the order below so that each one sees the fund state it
 * requires — JUnit 5's default method order is not stable across JVM versions,
 * so order is pinned explicitly:
 * <ol>
 *   <li>{@link #getBulkActionsTest()} — read-only, no mutation.</li>
 *   <li>{@link #interruptBulkAction()} — needs the <b>maximum</b> amount of
 *       unprocessed work on the fund so {@code SRD_GENERATOR_SERIAL_NUMBER}
 *       runs long enough for the interrupt to arrive before it finishes.
 *       Runs before any other test queues serial-number or unit-id actions.</li>
 *   <li>{@link #runBulkActionByNode()} — queues a serial-number run scoped to
 *       a single node; small amount of work. Safe after interrupt ran on a
 *       partially-processed fund.</li>
 *   <li>{@link #bulkActionsTest()} — runs all three bulk actions fund-wide;
 *       most destructive, run last.</li>
 * </ol>
 *
 * @author Petr Compel
 * @since 23.2.2016
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BulkActionControllerTest extends AbstractControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(BulkActionControllerTest.class);

    protected final static String XML_FILE = "bulk-actions-fa-import.xml";

    private static final String BULK_ACTION_GET = BULK_ACTION_CONTROLLER_URL + "/action/{id}";
    private static final String BULK_ACTION_INTERRUPT = BULK_ACTION_CONTROLLER_URL + "/action/{id}/interrupt";
    private static final String BULK_ACTIONS = BULK_ACTION_CONTROLLER_URL + "/{versionId}";
    private static final String BULK_ACTIONS_LIST = BULK_ACTION_CONTROLLER_URL + "/list/{versionId}";
    private static final String BULK_ACTION_QUEUE = BULK_ACTION_CONTROLLER_URL + "/queue/{versionId}/{code}";

    private static final String BULK_ACTION_FUND_VALIDATION = "SRD_FUND_VALIDATION";
    private static final String BULK_ACTION_GENERATOR_UNIT = "SRD_GENERATOR_UNIT_ID";
    private static final String BULK_ACTION_SERIAL_NUMBER_GENERATOR = "SRD_GENERATOR_SERIAL_NUMBER";

    /** Fund version shared by all tests in this class (imported once in {@link #initOnce()}). */
    private int fundVersionId;

    @BeforeAll
    public void initOnce() throws Exception {
        super.setUp();
        importXmlFile(null, 1, getResourceFile(XML_FILE));
        List<ArrFundVO> funds = getFunds();
        Assertions.assertEquals(1, funds.size(), "Expected exactly one fund after the one-time import");
        Assertions.assertEquals(1, funds.get(0).getVersions().size());
        fundVersionId = funds.get(0).getVersions().get(0).getId();
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
     * Order 1 — read-only listing of bulk actions available on the fund.
     *
     * <p><b>Assumes:</b> any fund state (no mutation performed).
     * <br><b>Leaves:</b> fund unchanged.
     */
    @Test
    @Order(1)
    public void getBulkActionsTest() {
        List<BulkActionVO> bulkActionVOs = Arrays.asList(get(spec -> spec.pathParam("versionId", fundVersionId), BULK_ACTIONS).getBody().as(BulkActionVO[].class));

		// number of default bulk actions
		// SRD has 4
		Assertions.assertEquals(4, bulkActionVOs.size());

        Boolean unit = false, serial = false, fa = false;

        for (BulkActionVO bulkAction : bulkActionVOs) {
            switch (bulkAction.getCode()) {
                case BULK_ACTION_GENERATOR_UNIT:
                    unit = true;
                    break;
                case BULK_ACTION_SERIAL_NUMBER_GENERATOR:
                    serial = true;
                    break;
                case BULK_ACTION_FUND_VALIDATION:
                    fa = true;
                    break;
            }
        }

        Assertions.assertTrue(unit, "Hromadna akce " + BULK_ACTION_GENERATOR_UNIT + " neni v seznamu");
        Assertions.assertTrue(serial, "Hromadna akce " + BULK_ACTION_SERIAL_NUMBER_GENERATOR + " neni v seznamu");
        Assertions.assertTrue(fa, "Hromadna akce " + BULK_ACTION_FUND_VALIDATION + " neni v seznamu");
    }

    private List<BulkActionRunVO> getBulkActionList(final int versionId) {
        return Arrays.asList(get(spec -> spec.pathParam("versionId", versionId), BULK_ACTIONS_LIST).getBody().as(BulkActionRunVO[].class));
    }

    /**
     * Returns the latest (highest-id) run for the given bulk-action code on the
     * given fund version. Tests share a fund across methods, so the bulk-action
     * list may contain finished runs from earlier tests for the same code; a
     * test polling for the state of a run it just queued must ignore those
     * older entries.
     */
    private BulkActionRunVO getBulkActionState(final int fundVersionId, final String code) {
        return getBulkActionList(fundVersionId).stream()
                .filter(state -> state.getCode().equals(code))
                .max(Comparator.comparing(BulkActionRunVO::getId))
                .orElse(null);
    }


    /**
     * Order 3 — spustí a čeká na dokončení hromadné akce
     * ({@code SRD_GENERATOR_SERIAL_NUMBER}) zaměřené na jeden uzel.
     *
     * <p><b>Assumes:</b> runs after {@link #interruptBulkAction()} — the fund
     * may already have some nodes numbered (both by the interrupted run and
     * by the node this test targets).
     * <br><b>Leaves:</b> target node numbered; generator skips already-numbered
     * siblings so re-running {@code SRD_GENERATOR_SERIAL_NUMBER} elsewhere is
     * still safe.
     */
    @Test
    @Order(3)
    public void runBulkActionByNode() throws InterruptedException {
        ArrangementController.FaTreeParam faTreeParam = new ArrangementController.FaTreeParam();
        faTreeParam.setVersionId(fundVersionId);
        TreeData fundTree = getFundTree(faTreeParam);
        Collection<TreeNodeVO> nodes = fundTree.getNodes();
        Assertions.assertNotNull(nodes);
        Assertions.assertFalse(nodes.isEmpty());
        TreeNodeVO next = nodes.iterator().next();

        post((spec) -> spec.pathParam("versionId", fundVersionId).pathParam("code", BULK_ACTION_SERIAL_NUMBER_GENERATOR).body(Collections.singletonList(next.getId())), BULK_ACTION_QUEUE);

		while (true) {
            logger.info("Čekání na dokončení asynchronních operací...");
            helperTestService.waitForWorkers();
			Thread.sleep(1000);

			BulkActionRunVO stateVo = getBulkActionState(fundVersionId, BULK_ACTION_SERIAL_NUMBER_GENERATOR);
			Assertions.assertNotNull(stateVo);
			State state = stateVo.getState();
			logger.info("Received state: " + state);

			Assertions.assertTrue(state != State.ERROR);
			if (state == State.FINISHED) {
				logger.info("Async action finished");
				break;
			}
		}
    }

    /**
     * Order 4 — spustí všechny tři hromadné akce (GENERATOR_UNIT,
     * FUND_VALIDATION, SRD_GENERATOR_SERIAL_NUMBER) a čeká na FINISHED.
     *
     * <p><b>Assumes:</b> any prior fund state. Akce jsou idempotentní — když
     * už jsou některé uzly zpracované, přeskočí je.
     * <br><b>Leaves:</b> fund plně validovaný a očíslovaný (serial + unit-id).
     * Musí běžet <b>jako poslední</b>, protože plně vyčerpá práci, kterou by
     * {@link #interruptBulkAction()} mohl přerušit.
     */
    @Test
    @Order(4)
    public void bulkActionsTest() throws InterruptedException {
        runBulkAction(fundVersionId, BULK_ACTION_GENERATOR_UNIT);
        runBulkAction(fundVersionId, BULK_ACTION_FUND_VALIDATION);
        runBulkAction(fundVersionId, BULK_ACTION_SERIAL_NUMBER_GENERATOR);
    }

    /**
     * Spustí a čeká na dokončení hromadné akce.
     *
     * @param fundVersionId verze archivní kod hromadné akce hromadné akce
     * @param code verze archivní kod hromadné akce hromadné akce
     * @return stav
     */
    private BulkActionRunVO runBulkAction(final int fundVersionId, final String code) throws InterruptedException {
        BulkActionRunVO state;

        get((spec) -> spec.pathParam("versionId", fundVersionId).pathParam("code", code), BULK_ACTION_QUEUE);

        int counter = 6;

        boolean hasResult = false;
        do {
            counter--;

            logger.info("Čekání na dokončení asynchronních operací...");
            helperTestService.waitForWorkers();
            Thread.sleep(5000);

            state = getBulkActionState(fundVersionId, code);

            if (counter >= 0) {
                if (state != null) {
                    if (state.getState().equals(State.FINISHED)) {
                        hasResult = true;
                    } else if (state.getState().equals(State.ERROR)) {
                        Assertions.fail("Bulk action failed, code: " + code + " error: " + state.getError());
                    }
                }
            } else {
                hasResult = true;
            }

        } while (!hasResult);

        Assertions.assertTrue(counter >= 0, "Čas překročen");

        return state;
    }

    private BulkActionRunVO getBulkAction(final int id) {
        return get((spec) -> spec.pathParam("id", id), BULK_ACTION_GET).getBody().as(BulkActionRunVO.class);
    }

    /**
     * Order 2 — spustí hromadnou akci a poté se ji pokusí přerušit.
     *
     * <p>Po zařazení akce do fronty <b>nečeká</b> na dokončení workerů — kdyby
     * počkal, akce by doběhla dříve, než se stihne poslat interrupt, a test by
     * místo {@code INTERRUPTED} končil ve stavu {@code FINISHED}. Interrupt se
     * proto posílá okamžitě po zařazení a test vyžaduje konečný stav
     * {@code INTERRUPTED}; stav {@code FINISHED} znamená, že akce doběhla
     * dříve, než interrupt dorazil, a je to chyba.
     *
     * <p><b>Assumes:</b> fund s maximálním množstvím nezpracované práce — tedy
     * že ještě neběžel žádný SERIAL_NUMBER_GENERATOR (proto {@code @Order(2)},
     * před {@link #runBulkActionByNode()} i {@link #bulkActionsTest()}). Kdyby
     * fond byl již očíslovaný, akce by skončila instantně a interrupt by
     * nestihl dorazit.
     * <br><b>Leaves:</b> fond v částečně očíslovaném stavu — některé uzly stihl
     * serial-number generator zpracovat dřív, než přišel interrupt.
     */
    @Test
    @Order(2)
    public void interruptBulkAction() throws InterruptedException {
        BulkActionRunVO baRunVO = get((spec) -> spec.pathParam("versionId", fundVersionId)
        					  						.pathParam("code", BULK_ACTION_SERIAL_NUMBER_GENERATOR), BULK_ACTION_QUEUE)
        				      						.getBody().as(BulkActionRunVO.class);
        int actionId = baRunVO.getId();

        // NEČEKÁME na dokončení workerů — interrupt musí dorazit dřív, než akce doběhne.
        Assertions.assertEquals(200, get((spec) -> spec.pathParam("id", actionId), BULK_ACTION_INTERRUPT).getStatusCode());

        // Počkáme na dosažení terminálního stavu (INTERRUPTED očekáváme).
        int counter = 6;
        boolean hasResult = false;
        do {
            counter--;

            logger.info("Čekání na dokončení asynchronních operací...");
            helperTestService.waitForWorkers();

            try {
                baRunVO = getBulkAction(actionId);

                if (counter >= 0) {
                    if (baRunVO != null) {
                        if (baRunVO.getState().equals(State.INTERRUPTED)) {
                            hasResult = true;
                        } else if (baRunVO.getState().equals(State.FINISHED)) {
                            Assertions.fail("Hromadná akce doběhla dřív, než stihl dorazit interrupt — test nepokryl přerušovací scénář");
                        } else if (baRunVO.getState().equals(State.ERROR)) {
                            Assertions.fail("Hromadná akce skončila chybou");
                        }
                    }
                } else {
                    hasResult = true;
                }
            } catch (AssertionError e) {
                logger.warn("Nepodařilo se získat stav hromadné akce", e);
            }

        } while (!hasResult);

        Assertions.assertTrue(counter >= 0, "Čas překročen (poslední stav: " + baRunVO.getState() + ")");
        Assertions.assertEquals(State.INTERRUPTED, baRunVO.getState(),
                "Očekávaný stav INTERRUPTED, skutečný: " + baRunVO.getState());
    }

}
