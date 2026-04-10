package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Petr Compel
 * @since 23.2.2016
 */
public class BulkActionControllerTest extends AbstractControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(BulkActionControllerTest.class);

    protected final static String XML_FILE = "bulk-actions-fa-import.xml";
    protected final static String IMPORT_SCOPE = "BULK_ACTIONS_TEST";

    private static final String BULK_ACTION_GET = BULK_ACTION_CONTROLLER_URL + "/action/{id}";
    private static final String BULK_ACTION_INTERRUPT = BULK_ACTION_CONTROLLER_URL + "/action/{id}/interrupt";
    private static final String BULK_ACTIONS = BULK_ACTION_CONTROLLER_URL + "/{versionId}";
    private static final String BULK_ACTIONS_LIST = BULK_ACTION_CONTROLLER_URL + "/list/{versionId}";
    private static final String BULK_ACTION_QUEUE = BULK_ACTION_CONTROLLER_URL + "/queue/{versionId}/{code}";

    private static final String BULK_ACTION_FUND_VALIDATION = "SRD_FUND_VALIDATION";
    private static final String BULK_ACTION_GENERATOR_UNIT = "SRD_GENERATOR_UNIT_ID";
    private static final String BULK_ACTION_SERIAL_NUMBER_GENERATOR = "SRD_GENERATOR_SERIAL_NUMBER";

    private int importAndGetVersionId() {
        importXmlFile(null, 1, getResourceFile(XML_FILE));
        List<ArrFundVO> funds = getFunds();
        Assertions.assertEquals(1, funds.size());
        Assertions.assertEquals(1, funds.get(0).getVersions().size());

        return funds.get(0).getVersions().get(0).getId();
    }

    @AfterEach
    public void cleanUp() {
        for (ApScopeVO scope : getAllScopes()) {
            if (scope.getName().equals(IMPORT_SCOPE)) {
                deleteScope(scope.getId());
                break;
            }
        }
    }

    @Test
    public void getBulkActionsTest() {
        int fundVersionId = importAndGetVersionId();
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

    private BulkActionRunVO getBulkActionState(final int fundVersionId, final String code) {
        for (BulkActionRunVO state : getBulkActionList(fundVersionId)) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }


    /**
     * Spustí a čeká na dokončení hromadné akce.
     */
    @Test
    public void runBulkActionByNode() throws InterruptedException {
		int fundVersionId = importAndGetVersionId();
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
     * Bulk Actions test
     */
    @Test
    public void bulkActionsTest() throws InterruptedException {
        int fundVersionId = importAndGetVersionId();

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
     * Spustí homadnou akci a poté se ji pokusí přerušit
     */
    @Test
    public void interruptBulkAction() throws InterruptedException {
        int fundVersionId = importAndGetVersionId();

        BulkActionRunVO baRunVO = get((spec) -> spec.pathParam("versionId", fundVersionId)
        					  						.pathParam("code", BULK_ACTION_SERIAL_NUMBER_GENERATOR), BULK_ACTION_QUEUE)
        				      						.getBody().as(BulkActionRunVO.class);
        int actionId = baRunVO.getId();

        helperTestService.waitForWorkers();

        Assertions.assertEquals(200, get((spec) -> spec.pathParam("id", actionId), BULK_ACTION_INTERRUPT).getStatusCode());

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
                        // TODO: odebrat stav FINISHED, který nastává v případě, že hromadná akce doběhla ještě před požadavkem na přerušení
                        if (baRunVO.getState().equals(State.INTERRUPTED) || baRunVO.getState().equals(State.FINISHED)) {
                            hasResult = true;
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
    }

}
