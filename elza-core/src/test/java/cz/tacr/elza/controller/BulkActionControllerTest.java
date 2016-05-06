package cz.tacr.elza.controller;

import cz.tacr.elza.api.ArrBulkActionRun.State;
import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.vo.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
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

    private static final String BULK_ACTION_FUND_VALIDATION = "FUND_VALIDATION_ZP2015";
    private static final String BULK_ACTION_GENERATOR_UNIT = "GENERATOR_UNIT_ID_ZP2015";
    private static final String BULK_ACTION_SERIAL_NUMBER_GENERATOR = "GENERATOR_SERIAL_NUMBER_ZP2015";

    private int importAndGetVersionId() {
        importXmlFile(null, null, XmlImportType.FUND, IMPORT_SCOPE, null, XmlImportControllerTest.getFile(XML_FILE));
        List<ArrFundVO> funds = getFunds();
        Assert.assertEquals(1, funds.size());
        Assert.assertEquals(1, funds.get(0).getVersions().size());

        return funds.get(0).getVersions().get(0).getId();
    }

    @After
    public void cleanUp() {
        for (RegScopeVO scope : getAllScopes()) {
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

        Assert.assertEquals(3, bulkActionVOs.size());

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

        Assert.assertTrue("Hromadna akce " + BULK_ACTION_GENERATOR_UNIT + " neni v seznamu", unit);
        Assert.assertTrue("Hromadna akce " + BULK_ACTION_SERIAL_NUMBER_GENERATOR + " neni v seznamu", serial);
        Assert.assertTrue("Hromadna akce " + BULK_ACTION_FUND_VALIDATION + " neni v seznamu", fa);
    }

    private List<BulkActionRunVO> getBulkActionList(int versionId) {
        return Arrays.asList(get(spec -> spec.pathParam("versionId", versionId), BULK_ACTIONS_LIST).getBody().as(BulkActionRunVO[].class));
    }

    private BulkActionRunVO getBulkActionState(int fundVersionId, String code) {
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
        BulkActionRunVO state;
        ArrangementController.FaTreeParam faTreeParam = new ArrangementController.FaTreeParam();
        faTreeParam.setVersionId(fundVersionId);
        TreeData fundTree = getFundTree(faTreeParam);
        Collection<TreeNodeClient> nodes = fundTree.getNodes();
        Assert.assertNotNull(nodes);
        Assert.assertFalse(nodes.isEmpty());
        TreeNodeClient next = nodes.iterator().next();

        post((spec) -> spec.pathParameter("versionId", fundVersionId).pathParam("code", BULK_ACTION_SERIAL_NUMBER_GENERATOR).body(Collections.singletonList(next.getId())), BULK_ACTION_QUEUE);

        int counter = 6;

        boolean hasResult = false;
        do {
            counter--;

            logger.info("Čekání na dokončení asynchronních operací...");
            Thread.sleep(5000);

            state = getBulkActionState(fundVersionId, BULK_ACTION_SERIAL_NUMBER_GENERATOR);

            if (counter >= 0) {
                if (state != null) {
                    if (state.getState().equals(State.FINISHED)) {
                        hasResult = true;
                    } else if (state.getState().equals(State.ERROR)) {
                        Assert.fail("Hromadná akce skončila chybou");
                    }
                }
            } else {
                hasResult = true;
            }

        } while (!hasResult);

        Assert.assertTrue("Čas překročen", counter >= 0);
    }

    /**
     * Spustí a čeká na dokončení hromadné akce.
     *
     * @param fundVersionId verze archivní kod hromadné akce hromadné akce
     * @param code verze archivní kod hromadné akce hromadné akce
     * @return stav
     */
    private BulkActionRunVO runBulkAction(int fundVersionId, String code) throws InterruptedException {
        BulkActionRunVO state;

        get((spec) -> spec.pathParameter("versionId", fundVersionId).pathParam("code", code), BULK_ACTION_QUEUE);

        int counter = 6;

        boolean hasResult = false;
        do {
            counter--;

            logger.info("Čekání na dokončení asynchronních operací...");
            Thread.sleep(5000);

            state = getBulkActionState(fundVersionId, code);

            if (counter >= 0) {
                if (state != null) {
                    if (state.getState().equals(State.FINISHED)) {
                        hasResult = true;
                    } else if (state.getState().equals(State.ERROR)) {
                        Assert.fail("Hromadná akce skončila chybou");
                    }
                }
            } else {
                hasResult = true;
            }

        } while (!hasResult);

        Assert.assertTrue("Čas překročen", counter >= 0);

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
        BulkActionRunVO state;

        state = get((spec) -> spec.pathParameter("versionId", fundVersionId).pathParam("code", BULK_ACTION_SERIAL_NUMBER_GENERATOR), BULK_ACTION_QUEUE).getBody().as(BulkActionRunVO.class);
        int actionId = state.getId();
        Assert.assertEquals(200, get((spec) -> spec.pathParam("id", actionId), BULK_ACTION_INTERRUPT).getStatusCode());

        int counter = 6;

        boolean hasResult = false;
        do {
            counter--;

            logger.info("Čekání na dokončení asynchronních operací...");
            Thread.sleep(5000);

            state = getBulkAction(actionId);

            if (counter >= 0) {
                if (state != null) {
                    if (state.getState().equals(State.INTERRUPTED)) {
                        hasResult = true;
                    } else if (state.getState().equals(State.ERROR)) {
                        Assert.fail("Hromadná akce skončila chybou");
                    }
                }
            } else {
                hasResult = true;
            }

        } while (!hasResult);

        Assert.assertTrue("Čas překročen", counter >= 0);
    }

}
