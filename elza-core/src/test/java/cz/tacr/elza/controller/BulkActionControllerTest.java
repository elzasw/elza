package cz.tacr.elza.controller;

import cz.tacr.elza.api.vo.BulkActionState;
import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.BulkActionStateVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;


/**
 * @author Petr Compel
 * @since 23.2.2016
 */
public class BulkActionControllerTest extends AbstractControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(BulkActionControllerTest.class);

    protected final static String XML_FILE = "bulk-actions-fa-import.xml";
    protected final static String IMPORT_SCOPE = "BULK_ACTIONS_TEST";

    private static final String BULK_ACTIONS = BULK_ACTION_CONTROLLER_URL + "/{versionId}/{mandatory}";
    private static final String BULK_ACTION_STATES = BULK_ACTION_CONTROLLER_URL + "/states/{versionId}";
    private static final String BULK_ACTION_RUN = BULK_ACTION_CONTROLLER_URL + "/run/{versionId}/{code}";
    private static final String BULK_ACTION_VALIDATE = BULK_ACTION_CONTROLLER_URL + "/validate/{versionId}";

    private static final String BULK_ACTION_CLEAN_SERIAL_NUMBER = "CLEAN_SERIAL_NUMBER_ZP2015";
    private static final String BULK_ACTION_FA_VALIDATION = "FINDING_AID_VALIDATION_ZP2015";
    private static final String BULK_ACTION_GENERATOR_UNIT = "GENERATOR_UNIT_ID_ZP2015";
    private static final String BULK_ACTION_SERIAL_NUMBER_GENERATOR = "GENERATOR_SERIAL_NUMBER_ZP2015";

    private int importAndGetVersionId() {
        importXmlFile(null, null, XmlImportType.FINDING_AID, IMPORT_SCOPE, null, XmlImportControllerTest.getFile(XML_FILE));
        List<ArrFindingAidVO> findingAids = getFindingAids();
        Assert.assertEquals(1, findingAids.size());
        Assert.assertEquals(1, findingAids.get(0).getVersions().size());

        return findingAids.get(0).getVersions().get(0).getId();
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
        int faVersionId = importAndGetVersionId();
        List<BulkActionVO> bulkActionVOs = Arrays.asList(get(spec -> spec.pathParam("versionId", faVersionId).pathParam("mandatory", false), BULK_ACTIONS).getBody().as(BulkActionVO[].class));

        Assert.assertEquals(4, bulkActionVOs.size());

        Boolean clean = false, unit = false, serial = false, fa = false;

        for (BulkActionVO bulkAction : bulkActionVOs) {
            switch (bulkAction.getCode()) {
                case BULK_ACTION_CLEAN_SERIAL_NUMBER:
                    clean = true;
                    break;
                case BULK_ACTION_GENERATOR_UNIT:
                    unit = true;
                    break;
                case BULK_ACTION_SERIAL_NUMBER_GENERATOR:
                    serial = true;
                    break;
                case BULK_ACTION_FA_VALIDATION:
                    fa = true;
                    break;
            }
        }

        Assert.assertTrue("Hromadna akce " + BULK_ACTION_CLEAN_SERIAL_NUMBER + " neni v seznamu", clean);
        Assert.assertTrue("Hromadna akce " + BULK_ACTION_GENERATOR_UNIT + " neni v seznamu", unit);
        Assert.assertTrue("Hromadna akce " + BULK_ACTION_SERIAL_NUMBER_GENERATOR + " neni v seznamu", serial);
        Assert.assertTrue("Hromadna akce " + BULK_ACTION_FA_VALIDATION + " neni v seznamu", fa);
    }

    private List<BulkActionStateVO> getBulkActionStates(int versionId) {
        return Arrays.asList(get(spec -> spec.pathParam("versionId", versionId), BULK_ACTION_STATES).getBody().as(BulkActionStateVO[].class));
    }

    @Test
    public void getBulkActionStateTest() throws InterruptedException {
        int faVersionId = importAndGetVersionId();
        List<BulkActionVO> actionVOs = Arrays.asList(get(spec -> spec.pathParam("versionId", faVersionId), BULK_ACTION_VALIDATE).getBody().as(BulkActionVO[].class));
        Assert.assertNotEquals(0, actionVOs.size());

        runBulkAction(faVersionId, BULK_ACTION_CLEAN_SERIAL_NUMBER);
        runBulkAction(faVersionId, BULK_ACTION_SERIAL_NUMBER_GENERATOR);
        runBulkAction(faVersionId, BULK_ACTION_GENERATOR_UNIT);
        runBulkAction(faVersionId, BULK_ACTION_FA_VALIDATION);
        for (BulkActionStateVO action : getBulkActionStates(faVersionId)) {
            Assert.assertEquals(action.getState(), BulkActionState.State.FINISH);
        }
        actionVOs = Arrays.asList(get(spec -> spec.pathParam("versionId", faVersionId), BULK_ACTION_VALIDATE).getBody().as(BulkActionVO[].class));
        Assert.assertEquals(0, actionVOs.size());
    }

    @Test
    public void getBulkActionsMandatoryTest() {
        int faVersionId = importAndGetVersionId();
        List<BulkActionVO> bulkActionVOs = Arrays.asList(get(spec -> spec.pathParam("versionId", faVersionId).pathParam("mandatory", true), BULK_ACTIONS).getBody().as(BulkActionVO[].class));

        Assert.assertEquals(2, bulkActionVOs.size());

        Boolean serial = false, fa = false;

        for (BulkActionVO bulkAction : bulkActionVOs) {
            switch (bulkAction.getCode()) {
                case BULK_ACTION_SERIAL_NUMBER_GENERATOR:
                    serial = true;
                    break;
                case BULK_ACTION_FA_VALIDATION:
                    fa = true;
                    break;
            }
        }

        Assert.assertTrue("Hromadna akce " + BULK_ACTION_SERIAL_NUMBER_GENERATOR + " neni v seznamu", serial);
        Assert.assertTrue("Hromadna akce " + BULK_ACTION_FA_VALIDATION + " neni v seznamu", fa);
    }

    private BulkActionStateVO getBulkActionState(int faVersionId, String code) {
        for (BulkActionStateVO state : getBulkActionStates(faVersionId)) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }

    /**
     * Spustí a čeká na dokončení hromadné akce.
     *
     * @param faVersionId verze archivní kod hromadné akce hromadné akce
     * @param code verze archivní kod hromadné akce hromadné akce
     * @return stav
     */
    private BulkActionStateVO runBulkAction(int faVersionId, String code) throws InterruptedException {
        BulkActionStateVO state;

        get((spec) -> spec.pathParameter("versionId", faVersionId).pathParam("code", code), BULK_ACTION_RUN);

        int counter = 6;

        boolean hasResult = false;
        do {
            counter--;

            logger.info("Čekání na dokončení asynchronních operací...");
            Thread.sleep(1000);

            state = getBulkActionState(faVersionId, code);

            if (counter >= 0) {
                if (state != null) {
                    if (state.getState().equals(BulkActionState.State.FINISH)) {
                        hasResult = true;
                    } else if (state.getState().equals(BulkActionState.State.ERROR)) {
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
}
