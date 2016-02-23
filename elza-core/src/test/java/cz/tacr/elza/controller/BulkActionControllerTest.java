package cz.tacr.elza.controller;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.BulkActionStateVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;


/**
 * @author Petr Compel
 * @since 23.2.2016
 */
public class BulkActionControllerTest extends AbstractControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(BulkActionControllerTest.class);

    private static final String BULK_ACTIONS = BULK_ACTION_CONTROLLER_URL + "/{versionId}/{mandatory}";
    private static final String BULK_ACTION_STATES = BULK_ACTION_CONTROLLER_URL + "/states/{versionId}";
    private static final String BULK_ACTION_RUN = BULK_ACTION_CONTROLLER_URL + "/run/{versionId}/{code}";

    private static final String BULK_ACTION_CLEAN_SERIAL_NUMBER = "CLEAN_SERIAL_NUMBER_ZP2015";
    private static final String BULK_ACTION_FA_VALIDATION = "FINDING_AID_VALIDATION_ZP2015";
    private static final String BULK_ACTION_GENERATOR_UNIT = "GENERATOR_UNIT_ID_ZP2015";
    private static final String BULK_ACTION_SERIAL_NUMBER_GENERATOR = "GENERATOR_SERIAL_NUMBER_ZP2015";

    @Autowired
    private BulkActionService bulkActionService;

    private int importAndGetVersionId() {
        for (RegScopeVO scope : getAllScopes()) {
            if (scope.getName().equals(XmlImportControllerTest.IMPORT_SCOPE)) {
                deleteScope(scope.getId());
                break;
            }
        }
        XmlImportControllerTest.importFA();
        List<ArrFindingAidVO> findingAids = getFindingAids();
        Assert.assertEquals(1, findingAids.size());
        Assert.assertEquals(1, findingAids.get(0).getVersions().size());

        return findingAids.get(0).getVersions().get(0).getId();
    }

    @Test
    @Ignore
    public void testRestGetBulkActionTypes() throws Exception {
        int faVersionId = importAndGetVersionId();
        List<BulkActionVO> bulkActionVOs = Arrays.asList(get(spec -> spec.pathParam("versionId", faVersionId).pathParam("mandatory", false), BULK_ACTIONS).getBody().as(BulkActionVO[].class));

        Assert.assertEquals(4, bulkActionVOs.size());

        Boolean clean = false, unit = false, serial = false, fa = false;

        for (BulkActionVO bulkaction : bulkActionVOs) {
            switch (bulkaction.getCode()) {
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

    @Test
    @Ignore
    public void scenarioTest() {
        importAndGetVersionId();
    }

    private List<BulkActionStateVO> getBulkActionStates(int versionId) {
        return Arrays.asList(get(spec -> spec.pathParam("versionId", versionId), BULK_ACTION_STATES).getBody().as(BulkActionStateVO[].class));
    }

    @Test
    @Ignore
    public void testRestGetBulkActionState() throws Exception {
        int faVersionId = importAndGetVersionId();
        List<BulkActionStateVO> bulkActionStates1 = getBulkActionStates(faVersionId);

        //runBulkAction(BULK_ACTION_SERIAL_NUMBER_GENERATOR);
    }

    @Test
    public void testRestGetMandatoryBulkActions() throws Exception {
        importAndGetVersionId();
        /*try {
            bulkActionService.createBulkAction(bulkActionConfigMandatory);

            Response response = get((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()),
                    GET_MANDATORY_BULK_ACTIONS);

            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            List<BulkActionConfig> bulkActionConfigs = Arrays
                    .asList(response.getBody().as(BulkActionConfig[].class));

            Assert.assertEquals(3, bulkActionConfigs.size());

        } finally {
            cleanUpBulkActionConfig(bulkActionConfigMandatory);
        }*/
    }

    @Test
    @Ignore
    public void testRestRun() throws Exception {
        int versionId = importAndGetVersionId();

        /*BulkActionConfig bulkActionConfigSerial = new BulkActionConfig();
        bulkActionConfigSerial.setCode("SERIAL");

        bulkActionConfigSerial.setConfiguration("code_type_bulk_action: GENERATOR_SERIAL_NUMBER\n"
                + "rule_code: " + TEST_CODE + "\n"
                + "serial_id_code: ZP2015_SERIAL_NUMBER");

        BulkActionConfig bulkActionConfigClean = new BulkActionConfig();
        bulkActionConfigClean.setCode("CLEAN");

        bulkActionConfigClean.setConfiguration("code_type_bulk_action: CLEAN_DESCRIPTION_ITEM\n"
                + "rule_code: " + TEST_CODE + "\n"
                + "description_code: ZP2015_SERIAL_NUMBER");

        BulkActionConfig bulkActionConfigUnit = new BulkActionConfig();
        bulkActionConfigUnit.setCode("UNIT");

        bulkActionConfigUnit.setConfiguration("code_type_bulk_action: GENERATOR_UNIT_ID\n"
                + "rule_code: " + TEST_CODE + "\n"
                + "unit_id_code: ZP2015_UNIT_ID\n"
                + "previous_id_code: ZP2015_OTHER_ID\n"
                + "previous_id_spec_code: ZP2015_OTHERID_SIG\n"
                + "delimiter_major: //\n"
                + "delimiter_minor: /\n"
                + "level_type_code: ZP2015_LEVEL_TYPE");

        BulkActionConfig bulkActionConfigFinding = new BulkActionConfig();
        bulkActionConfigFinding.setCode("FINDING");

        bulkActionConfigFinding.setConfiguration("code_type_bulk_action: FINDING_AID_VALIDATION\n"
                + "rule_code: " + TEST_CODE + "\n"
                + "evaluation_type: COMPLETE\n"
                + "mandatory_arrangement_type: INV|MAN|KAT");

        try {
            bulkActionService.createBulkAction(bulkActionConfigSerial);
            bulkActionService.createBulkAction(bulkActionConfigClean);
            bulkActionService.createBulkAction(bulkActionConfigUnit);
            bulkActionService.createBulkAction(bulkActionConfigFinding);

            //
            //  Spusteni prvni hromadne akce
            //

            List<BulkActionState> bulkActionStates = runBulkActionAndWaitForResult(version, bulkActionConfigSerial, 0);
            Assert.assertEquals(1, bulkActionStates.size());

            // kontrola root uzlu

            ArrLevelExt levelExt = getLevelByNodeId(version.getRootLevel().getNode().getNodeId(),
                    version.getFindingAidVersionId());
            Assert.assertEquals(1, levelExt.getDescItemList().size());

            ArrDescItemInt descItemInt = (ArrDescItemInt) levelExt.getDescItemList().get(0);
            Assert.assertEquals(1, (long) descItemInt.getValue());

            // kontrola prvni urovne uzlu

            List<ArrLevel> levels = getSubLevels(version.getRootLevel().getNode(), version);
            Assert.assertEquals(2, levels.size());

            levelExt = getLevelByNodeId(levels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(1, levelExt.getDescItemList().size());

            descItemInt = (ArrDescItemInt) levelExt.getDescItemList().get(0);
            Assert.assertEquals(2, (long) descItemInt.getValue());

            levelExt = getLevelByNodeId(levels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(1, levelExt.getDescItemList().size());

            descItemInt = (ArrDescItemInt) levelExt.getDescItemList().get(0);
            Assert.assertEquals(5, (long) descItemInt.getValue());

            // kontrola druhe urovni prvniho uzlu

            List<ArrLevel> sublevels = getSubLevels(levels.get(0).getNode(), version);
            Assert.assertEquals(2, sublevels.size());

            levelExt = getLevelByNodeId(sublevels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(1, levelExt.getDescItemList().size());

            descItemInt = (ArrDescItemInt) levelExt.getDescItemList().get(0);
            Assert.assertEquals(3, (long) descItemInt.getValue());

            levelExt = getLevelByNodeId(sublevels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(1, levelExt.getDescItemList().size());

            descItemInt = (ArrDescItemInt) levelExt.getDescItemList().get(0);
            Assert.assertEquals(4, (long) descItemInt.getValue());

            //
            // Spusteni druhe hromadne akce
            //

            bulkActionStates = runBulkActionAndWaitForResult(version, bulkActionConfigClean, 1);
            Assert.assertEquals(2, bulkActionStates.size());

            // kontrola root uzlu

            levelExt = getLevelByNodeId(version.getRootLevel().getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(0, levelExt.getDescItemList().size());

            // kontrola prvni urovne uzlu

            levels = getSubLevels(version.getRootLevel().getNode(), version);
            Assert.assertEquals(2, levels.size());

            levelExt = getLevelByNodeId(levels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(0, levelExt.getDescItemList().size());

            levelExt = getLevelByNodeId(levels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(0, levelExt.getDescItemList().size());

            // kontrola druhe urovni prvniho uzlu

            sublevels = getSubLevels(levels.get(0).getNode(), version);
            Assert.assertEquals(2, sublevels.size());

            levelExt = getLevelByNodeId(sublevels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(0, levelExt.getDescItemList().size());

            levelExt = getLevelByNodeId(sublevels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(0, levelExt.getDescItemList().size());

            //
            // Spusteni treti hromadne akce
            //

            bulkActionStates = runBulkActionAndWaitForResult(version, bulkActionConfigUnit, 2);
            Assert.assertEquals(3, bulkActionStates.size());

            // kontrola root uzlu

            levelExt = getLevelByNodeId(version.getRootLevel().getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(0, levelExt.getDescItemList().size());

            ArrDescItemUnitid descItemUnitid;

            // kontrola prvni urovne uzlu

            levels = getSubLevels(version.getRootLevel().getNode(), version);
            Assert.assertEquals(2, levels.size());

            levelExt = getLevelByNodeId(levels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(1, levelExt.getDescItemList().size());

            descItemUnitid = (ArrDescItemUnitid) levelExt.getDescItemList().get(0);
            Assert.assertEquals("1", descItemUnitid.getValue());

            levelExt = getLevelByNodeId(levels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(1, levelExt.getDescItemList().size());

            descItemUnitid = (ArrDescItemUnitid) levelExt.getDescItemList().get(0);
            Assert.assertEquals("2", descItemUnitid.getValue());

            // kontrola druhe urovni prvniho uzlu

            sublevels = getSubLevels(levels.get(0).getNode(), version);
            Assert.assertEquals(2, sublevels.size());

            levelExt = getLevelByNodeId(sublevels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(1, levelExt.getDescItemList().size());

            descItemUnitid = (ArrDescItemUnitid) levelExt.getDescItemList().get(0);
            Assert.assertEquals("1/1", descItemUnitid.getValue());

            levelExt = getLevelByNodeId(sublevels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(1, levelExt.getDescItemList().size());

            descItemUnitid = (ArrDescItemUnitid) levelExt.getDescItemList().get(0);
            Assert.assertEquals("1/2", descItemUnitid.getValue());

            //
            //  Spusteni ctvrte hromadne akce
            //

            bulkActionStates = runBulkActionAndWaitForResult(version, bulkActionConfigFinding, 3);
            Assert.assertEquals(4, bulkActionStates.size());

            // kontrola root uzlu

            levelExt = getLevelByNodeId(version.getRootLevel().getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(cz.tacr.elza.api.ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

            // kontrola prvni urovne uzlu

            levels = getSubLevels(version.getRootLevel().getNode(), version);
            Assert.assertEquals(2, levels.size());

            levelExt = getLevelByNodeId(levels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(cz.tacr.elza.api.ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

            levelExt = getLevelByNodeId(levels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(cz.tacr.elza.api.ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

            // kontrola druhe urovni prvniho uzlu

            sublevels = getSubLevels(levels.get(0).getNode(), version);
            Assert.assertEquals(2, sublevels.size());

            levelExt = getLevelByNodeId(sublevels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(cz.tacr.elza.api.ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

            levelExt = getLevelByNodeId(sublevels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(cz.tacr.elza.api.ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

            version = getFindingAidOpenVersion(version.getFindingAid());
            ArrVersionConformity conformityInfo = findingAidVersionConformityInfoRepository
                    .findByVersion(version);
            Assert.assertEquals(ArrVersionConformity.State.ERR, conformityInfo.getState());

        } finally {
            cleanUpBulkActionConfig(bulkActionConfigSerial);
            cleanUpBulkActionConfig(bulkActionConfigClean);
            cleanUpBulkActionConfig(bulkActionConfigUnit);
            cleanUpBulkActionConfig(bulkActionConfigFinding);
        }

    }

    @Test
    public void testRestRunValidation() throws Exception {
        ArrFindingAidVersion version = createTestStructure();

        BulkActionConfig bulkActionConfigMandatory = new BulkActionConfig();
        bulkActionConfigMandatory.setCode("TEST");
        bulkActionConfigMandatory.setConfiguration("code_type_bulk_action: GENERATOR_UNIT_ID\n"
                + "rule_code: " + TEST_CODE + "\n"
                + "mandatory_arrangement_type: INV|" + TEST_CODE + "\n"
                + "unit_id_code: ZP2015_UNIT_ID\n"
                + "previous_id_code: ZP2015_OTHER_ID\n"
                + "previous_id_spec_code: ZP2015_OTHERID_SIG\n"
                + "delimiter_major: //\n"
                + "delimiter_minor: /\n"
                + "level_type_code: ZP2015_LEVEL_TYPE\n"
                + "delimiter_major_level_type_not_use: ZP2015_LEVEL_PART\n"
                + "name: generátor referenčního označení");

        try {
            bulkActionService.createBulkAction(bulkActionConfigMandatory);

            Response response = get((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()),
                    VALIDATE_BULK_ACTION);

            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            List<BulkActionConfig> bulkActionConfigs = Arrays
                    .asList(response.getBody().as(BulkActionConfig[].class));

            Assert.assertEquals(3, bulkActionConfigs.size());

            runBulkActionAndWaitForResult(version, bulkActionConfigMandatory, 0);

            response = get((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()),
                    VALIDATE_BULK_ACTION);

            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            bulkActionConfigs = Arrays.asList(response.getBody().as(BulkActionConfig[].class));

            Assert.assertEquals(3, bulkActionConfigs.size());

        } finally {
            cleanUpBulkActionConfig(bulkActionConfigMandatory);
        }*/
    }

    /**
     * Spustí a čeká na dokončení hromadné akce.
     *
     * @param version          verze archivní pomůcky
     * @param bulkActionConfig nastavení hromadné akce
     * @param indexState       index výsledku
     * @return seznam stavů hromadných akcí
     */
    private void runBulkActionAndWaitForResult(final ArrFindingAidVersion version,
                                               final BulkActionConfig bulkActionConfig,
                                               final Integer indexState)
            throws InterruptedException {
        /*post((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()).body(bulkActionConfig),
                RUN_BULK_ACTION);

        int pokusu = 5;

        List<BulkActionState> bulkActionStates;

        do {
            pokusu--;

            logger.info("Čekání na dokončení asynchronních operací...");
            Thread.sleep(5000);

            Response response = get((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()),
                    GET_BULK_ACTION_STATES);

            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            bulkActionStates = Arrays
                    .asList(response.getBody().as(BulkActionState[].class));

            if (bulkActionStates.size() > indexState || pokusu <= 0) {
                if (bulkActionStates.get(indexState).getState().equals(cz.tacr.elza.api.vo.BulkActionState.State.FINISH) || pokusu <= 0) {
                    break;
                } else if (bulkActionStates.get(indexState).getState().equals(cz.tacr.elza.api.vo.BulkActionState.State.ERROR)) {
                    Assert.fail("Hromadná akce skončila chybou");
                }
            }

        } while (true);

        Assert.assertFalse(pokusu <= 0);
        return bulkActionStates;*/
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
     * @return seznam stavů hromadných akcí
     */
    private void runBulkAction(int faVersionId, String code) throws InterruptedException {
        BulkActionStateVO state;
        try {
            post((spec) -> spec.pathParameter("versionId", faVersionId).pathParam("code", BULK_ACTION_CLEAN_SERIAL_NUMBER), BULK_ACTION_RUN);

            int pokusu = 5;

            do {
                pokusu--;

                logger.info("Čekání na dokončení asynchronních operací...");
                Thread.sleep(1000);

                state = getBulkActionState(faVersionId, code);

                Assert.assertNotNull(state);

                if (pokusu <= 0) {
                    if (state.getState().equals(cz.tacr.elza.api.vo.BulkActionState.State.FINISH) || pokusu <= 0) {
                        break;
                    } else if (state.getState().equals(cz.tacr.elza.api.vo.BulkActionState.State.ERROR)) {
                        Assert.fail("Hromadná akce skončila chybou");
                    }
                }

            } while (true);

            Assert.assertFalse(pokusu <= 0);

        } catch (Exception e) {

        }
    }

        /*post((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()).body(bulkActionConfig),
                RUN_BULK_ACTION);

        int pokusu = 5;

        List<BulkActionState> bulkActionStates;

        do {
            pokusu--;

            logger.info("Čekání na dokončení asynchronních operací...");
            Thread.sleep(5000);

            Response response = get((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()),
                    GET_BULK_ACTION_STATES);

            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            bulkActionStates = Arrays
                    .asList(response.getBody().as(BulkActionState[].class));

            if (bulkActionStates.size() > indexState || pokusu <= 0) {
                if (bulkActionStates.get(indexState).getState().equals(cz.tacr.elza.api.vo.BulkActionState.State.FINISH) || pokusu <= 0) {
                    break;
                } else if (bulkActionStates.get(indexState).getState().equals(cz.tacr.elza.api.vo.BulkActionState.State.ERROR)) {
                    Assert.fail("Hromadná akce skončila chybou");
                }
            }

        } while (true);

        Assert.assertFalse(pokusu <= 0);
        return bulkActionStates;*/

}
