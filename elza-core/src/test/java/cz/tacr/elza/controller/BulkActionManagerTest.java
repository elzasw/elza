package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.restassured.response.Response;

import cz.tacr.elza.api.ArrNodeConformity;
import cz.tacr.elza.api.vo.BulkActionState.State;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.bulkaction.generator.CleanDescriptionItemBulkAction;
import cz.tacr.elza.bulkaction.generator.FindingAidValidationBulkAction;
import cz.tacr.elza.bulkaction.generator.SerialNumberBulkAction;
import cz.tacr.elza.bulkaction.generator.UnitIdBulkAction;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrDescItemUnitid;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrVersionConformity;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.RulDescItemType;


/**
 * Testy pro {@link BulkActionManager}.
 */
public class BulkActionManagerTest extends AbstractRestTest {

    private static final Logger logger = LoggerFactory.getLogger(BulkActionManagerTest.class);


    @Autowired
    private BulkActionService bulkActionService;

    @Test
    public void testRestGetBulkActionTypes() throws Exception {

        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(GET_BULK_ACTION_TYPES);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());

        List<String> types = Arrays.asList(response.getBody().as(String[].class));

        Assert.assertEquals(4, types.size());

        if (!types.contains(CleanDescriptionItemBulkAction.TYPE)) {
            Assert.fail("Hromadna akce " + CleanDescriptionItemBulkAction.TYPE + " neni v seznamu");
        }

        if (!types.contains(UnitIdBulkAction.TYPE)) {
            Assert.fail("Hromadna akce " + UnitIdBulkAction.TYPE + " neni v seznamu");
        }

        if (!types.contains(SerialNumberBulkAction.TYPE)) {
            Assert.fail("Hromadna akce " + SerialNumberBulkAction.TYPE + " neni v seznamu");
        }

        if (!types.contains(FindingAidValidationBulkAction.TYPE)) {
            Assert.fail("Hromadna akce " + FindingAidValidationBulkAction.TYPE + " neni v seznamu");
        }

    }

    @Test
    public void testRestCreateBulkAction() throws Exception {

        BulkActionConfig bulkActionConfig = createBulkActionConfig();

        try {

            Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                    body(bulkActionConfig).put(CREATE_BULK_ACTION);
            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            BulkActionConfig bulkActionConfigCreated = response.getBody().as(BulkActionConfig.class);

            if (!bulkActionConfig.getCode().equals(bulkActionConfigCreated.getCode())
                    || !bulkActionConfig.getConfiguration().equals(bulkActionConfigCreated.getConfiguration())) {
                Assert.fail();
            }

        } finally {
            // vždy po sobě uklidit
            cleanUpBulkActionConfig(bulkActionConfig);
        }

    }

    @Test
    public void testRestUpdateBulkAction() throws Exception {

        BulkActionConfig bulkActionConfig = createBulkActionConfig();

        BulkActionConfig bulkActionConfigCreated = bulkActionService.createBulkAction(bulkActionConfig);

        try {

            bulkActionConfigCreated.getYaml().setProperty("param1", "test1.1");

            Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                    body(bulkActionConfigCreated).post(UPDATE_BULK_ACTION);
            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            BulkActionConfig bulkActionConfigUpdate = response.getBody().as(BulkActionConfig.class);

            if (!bulkActionConfigCreated.getCode().equals(bulkActionConfigUpdate.getCode())
                    || !bulkActionConfigCreated.getConfiguration().equals(bulkActionConfigUpdate.getConfiguration())) {
                Assert.fail();
            }

        } finally {
            // vždy po sobě uklidit
            cleanUpBulkActionConfig(bulkActionConfig);
        }

    }

    @Test
    public void testRestGetBulkAction() throws Exception {
        BulkActionConfig bulkActionConfig = createBulkActionConfig();

        BulkActionConfig bulkActionConfigCreated = bulkActionService.createBulkAction(bulkActionConfig);

        try {

            Response response = get((spec) -> spec.pathParameter(BULK_ACTION_CODE, bulkActionConfigCreated.getCode()),
                    GET_BULK_ACTION);

            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            BulkActionConfig bulkActionConfigReturn = response.getBody().as(BulkActionConfig.class);

            if (!bulkActionConfigCreated.getCode().equals(bulkActionConfigReturn.getCode())
                    || !bulkActionConfigCreated.getConfiguration().equals(bulkActionConfigReturn.getConfiguration())) {
                Assert.fail();
            }

        } finally {
            // vždy po sobě uklidit
            cleanUpBulkActionConfig(bulkActionConfig);
        }
    }

    @Test
    public void testRestGetBulkActionState() throws Exception {
        ArrFindingAidVersion version = createTestStructure();

        BulkActionConfig bulkActionConfig = new BulkActionConfig();
        bulkActionConfig.setCode("TEST");

        bulkActionConfig.setConfiguration("code_type_bulk_action: CLEAN_DESCRIPTION_ITEM\n"
                + "rule_code: " + TEST_CODE + "\n"
                + "description_code: ZP2015_SERIAL_NUMBER");

        try {
            bulkActionService.createBulkAction(bulkActionConfig);

            post((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()).body(bulkActionConfig),
                    RUN_BULK_ACTION);

            int pokusu = 5;

            List<BulkActionState> bulkActionStates;

            do {
                pokusu--;

                logger.info("Čekání na dokončení asynchronních operací...");
                Thread.sleep(1000);

                Response response = get((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()),
                        GET_BULK_ACTION_STATES);

                logger.info(response.asString());
                Assert.assertEquals(200, response.statusCode());

                bulkActionStates = Arrays
                        .asList(response.getBody().as(BulkActionState[].class));

                if (bulkActionStates.size() > 0 || pokusu <= 0) {
                    if (bulkActionStates.get(0).getState().equals(State.FINISH) || pokusu <= 0) {
                        break;
                    } else if (bulkActionStates.get(0).getState().equals(State.ERROR)) {
                        Assert.fail("Hromadná akce skončila chybou");
                    }
                }

            } while (true);

            Assert.assertFalse(pokusu <= 0);

            Assert.assertEquals(1, bulkActionStates.size());

        } finally {
            cleanUpBulkActionConfig(bulkActionConfig);
        }
    }

    @Test
    public void testRestDeleteBulkAction() throws Exception {

        BulkActionConfig bulkActionConfig = createBulkActionConfig();

        BulkActionConfig bulkActionConfigCreated = bulkActionService.createBulkAction(bulkActionConfig);

        try {

            Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).
                    body(bulkActionConfigCreated).delete(DELETE_BULK_ACTION);
            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            try {
                bulkActionService.getBulkAction(bulkActionConfigCreated.getCode());
                Assert.fail("Nastaveni hromadne akce jiz nemuze existovat");
            } catch (IllegalArgumentException e) {
                // spravne, akce jiz nesmi existovat
            }

        } finally {
            // vždy po sobě uklidit
            cleanUpBulkActionConfig(bulkActionConfig);
        }

    }

    @Test
    public void testRestReload() throws Exception {
        Response response = given().header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE).get(RELOAD_BULK_ACTIONS);
        logger.info(response.asString());
        Assert.assertEquals(200, response.statusCode());
    }

    @Test
    public void testRestGetBulkActions() throws Exception {
        ArrFindingAidVersion version = createTestStructure();

        BulkActionConfig bulkActionConfig = new BulkActionConfig();
        bulkActionConfig.setCode("TEST");
        bulkActionConfig.setConfiguration("code_type_bulk_action: GENERATOR_UNIT_ID\n"
                + "rule_code: " + TEST_CODE + "\n"
                + "unit_id_code: ZP2015_UNIT_ID\n"
                + "previous_id_code: ZP2015_OTHER_ID\n"
                + "previous_id_spec_code: ZP2015_OTHERID_SIG\n"
                + "delimiter_major: //\n"
                + "delimiter_minor: /\n"
                + "level_type_code: ZP2015_LEVEL_TYPE\n"
                + "delimiter_major_level_type_not_use: ZP2015_LEVEL_PART\n"
                + "name: generátor referenčního označení");

        try {
            bulkActionService.createBulkAction(bulkActionConfig);

            Response response = get((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()),
                    GET_ALL_BULK_ACTIONS);

            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            List<BulkActionConfig> bulkActionConfigs = Arrays
                    .asList(response.getBody().as(BulkActionConfig[].class));

            Assert.assertTrue(bulkActionConfigs.size() > 0);

        } finally {
            cleanUpBulkActionConfig(bulkActionConfig);
        }
    }

    @Test
    public void testRestGetMandatoryBulkActions() throws Exception {
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
                    GET_MANDATORY_BULK_ACTIONS);

            logger.info(response.asString());
            Assert.assertEquals(200, response.statusCode());

            List<BulkActionConfig> bulkActionConfigs = Arrays
                    .asList(response.getBody().as(BulkActionConfig[].class));

            Assert.assertEquals(3, bulkActionConfigs.size());

        } finally {
            cleanUpBulkActionConfig(bulkActionConfigMandatory);
        }
    }

    @Test
    public void testRestRun() throws Exception {
        ArrFindingAidVersion version = createTestStructure();

        BulkActionConfig bulkActionConfigSerial = new BulkActionConfig();
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
            Assert.assertEquals(ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

            // kontrola prvni urovne uzlu

            levels = getSubLevels(version.getRootLevel().getNode(), version);
            Assert.assertEquals(2, levels.size());

            levelExt = getLevelByNodeId(levels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

            levelExt = getLevelByNodeId(levels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

            // kontrola druhe urovni prvniho uzlu

            sublevels = getSubLevels(levels.get(0).getNode(), version);
            Assert.assertEquals(2, sublevels.size());

            levelExt = getLevelByNodeId(sublevels.get(0).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

            levelExt = getLevelByNodeId(sublevels.get(1).getNode().getNodeId(), version.getFindingAidVersionId());
            Assert.assertEquals(ArrNodeConformity.State.ERR, levelExt.getNodeConformityInfo().getState());

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
        }
    }

    /**
     * Spustí a čeká na dokončení hromadné akce.
     *
     * @param version          verze archivní pomůcky
     * @param bulkActionConfig nastavení hromadné akce
     * @param indexState       index výsledku
     * @return seznam stavů hromadných akcí
     */
    private List<BulkActionState> runBulkActionAndWaitForResult(final ArrFindingAidVersion version,
                                                                final BulkActionConfig bulkActionConfig,
                                                                final Integer indexState)
            throws InterruptedException {
        post((spec) -> spec.pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()).body(bulkActionConfig),
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
                if (bulkActionStates.get(indexState).getState().equals(State.FINISH) || pokusu <= 0) {
                    break;
                } else if (bulkActionStates.get(indexState).getState().equals(State.ERROR)) {
                    Assert.fail("Hromadná akce skončila chybou");
                }
            }

        } while (true);

        Assert.assertFalse(pokusu <= 0);
        return bulkActionStates;
    }

    /**
     * Vytvoření testovací hromadné akce.
     *
     * @return hromadná akce
     */
    private BulkActionConfig createBulkActionConfig() {
        BulkActionConfig bulkActionConfig = new BulkActionConfig();
        bulkActionConfig.setCode("TEST");
        bulkActionConfig.setConfiguration("param1: test1\nparam2: test2\n");
        return bulkActionConfig;
    }

    /**
     * Pokusí se smazat hromadnou akci.
     *
     * @param bulkActionConfig hromadná akce
     */
    private void cleanUpBulkActionConfig(BulkActionConfig bulkActionConfig) {
        try {
            bulkActionService.delete(bulkActionConfig);
        } catch (Exception e) {
            // ignoruju chyby, pouze snaha o smazani pokud existuje
        }
    }

    /**
     * Vytvoří testovací strukturu stromu s typama hodnot atributů
     *
     * @return verze archivní pomůcky
     */
    private ArrFindingAidVersion createTestStructure() {

        ArrFindingAid findingAid = createFindingAid(TEST_NAME);
        ArrFindingAidVersion version = getFindingAidOpenVersion(findingAid);

        ArrLevel level1 = createLevel(1, version.getRootLevel(), createFaChange(LocalDateTime.now()));
        /*ArrLevel level2 = */
        createLevel(2, version.getRootLevel(), createFaChange(LocalDateTime.now()));

        /*ArrLevel level11 = */
        createLevel(1, level1, createFaChange(LocalDateTime.now()));
        /*ArrLevel level12 = */
        createLevel(2, level1, createFaChange(LocalDateTime.now()));

        RulDescItemType descItemTypeSN = createDescItemType(getDataType(DATA_TYPE_INTEGER), "ZP2015_SERIAL_NUMBER",
                "ZP2015_SERIAL_NUMBER", "SH1", "Desc 1", true,
                false, false, 80001);

        createDescItemType(getDataType(DATA_TYPE_UNITID), "ZP2015_UNIT_ID",
                "ZP2015_UNIT_ID", "SH2", "Desc 2", true,
                false, false, 80002);

        RulDescItemType descItemTypeOther = createDescItemType(getDataType(DATA_TYPE_STRING), "ZP2015_OTHER_ID",
                "Item type 3", "SH3", "Desc 3", false,
                true, true, 80003);

        createDescItemSpec(descItemTypeOther, "ZP2015_OTHERID_SIG_ORIG", "ZP2015_OTHERID_SIG_ORIG", "", "", 80001);
        createDescItemSpec(descItemTypeOther, "ZP2015_OTHERID_SIG", "ZP2015_OTHERID_SIG", "", "", 80002);
        createDescItemSpec(descItemTypeOther, "ZP2015_OTHERID_CJ", "ZP2015_OTHERID_CJ", "", "", 80003);

        RulDescItemType descItemTypeLevel = createDescItemType(getDataType(DATA_TYPE_INTEGER), "ZP2015_LEVEL_TYPE",
                "Item type 4", "SH4", "Desc 4", false,
                false, true, 80004);

        createDescItemSpec(descItemTypeLevel, "ZP2015_LEVEL_SERIES", "ZP2015_LEVEL_SERIES", "", "", 80001);
        createDescItemSpec(descItemTypeLevel, "ZP2015_LEVEL_FOLDER", "ZP2015_LEVEL_FOLDER", "", "", 80002);
        createDescItemSpec(descItemTypeLevel, "ZP2015_LEVEL_ITEM", "ZP2015_LEVEL_ITEM", "", "", 80003);
        createDescItemSpec(descItemTypeLevel, "ZP2015_LEVEL_PART", "ZP2015_LEVEL_PART", "", "", 80004);

        return version;
    }

}
