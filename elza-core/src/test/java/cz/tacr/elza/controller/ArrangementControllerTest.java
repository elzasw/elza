package cz.tacr.elza.controller;

import static cz.tacr.elza.repository.ExceptionThrow.output;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.vo.UniqueValue;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Receiptable;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.ArrangementController.CopySiblingResult;
import cz.tacr.elza.controller.ArrangementController.DescFormDataNewVO;
import cz.tacr.elza.controller.ArrangementController.DescItemResult;
import cz.tacr.elza.controller.ArrangementWebsocketControllerTest.ReceiptStatus;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ArrFundFulltextResult;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrInhibitedItemVO;
import cz.tacr.elza.controller.vo.ArrOutputVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateEditVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateMapSpecVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateMapTypeVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateVO;
import cz.tacr.elza.controller.vo.CopyNodesParams;
import cz.tacr.elza.controller.vo.CopyNodesValidate;
import cz.tacr.elza.controller.vo.CopyNodesValidateResult;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.controller.vo.FulltextFundRequest;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.OutputSettingsVO;
import cz.tacr.elza.controller.vo.RulOutputTypeVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.filter.Condition;
import cz.tacr.elza.controller.vo.filter.Filter;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.filter.ValuesTypes;
import cz.tacr.elza.controller.vo.nodes.ArrNodeExtendVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.NodeDataVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.vo.ChangesResult;
import cz.tacr.elza.test.controller.vo.DataBit;
import cz.tacr.elza.test.controller.vo.DataString;
import cz.tacr.elza.test.controller.vo.DataText;
import cz.tacr.elza.test.controller.vo.DataType;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.ItemDataResult;
import cz.tacr.elza.test.controller.vo.NodeBase;
import cz.tacr.elza.test.controller.vo.NodeDataParam;
import cz.tacr.elza.test.controller.vo.NodeItem;
import cz.tacr.elza.utils.CsvUtils;
import io.restassured.response.Response;

/**
 * Tests of ArrangementController (fund / version / node / desc-item operations).
 *
 * <p>Uses per-class lifecycle: base {@code setUp} / {@code tearDown} run once per
 * class via {@link #initOnce()} / {@link #cleanupOnce()} — not per method.
 *
 * <h2>No per-test fund cleanup (intentional)</h2>
 * Tests in this class deliberately do <b>not</b> delete the funds they create.
 * The funds accumulate through the class run and are wiped by the next test
 * class's {@code @BeforeEach deleteTables()}.
 *
 * <p>Reason: {@code deleteFund} is a REST call that generates a large burst of
 * Lucene / Hibernate Search outbox events; subsequent tests that call
 * {@code helperTestService.waitForIndexUpdate()} then have to drain those
 * events, which can take tens of seconds. Skipping the per-test delete keeps
 * the index churn localised to the test that caused it.
 *
 * <p>Each {@code @Test} carries a comment describing what it verifies and
 * what state it creates. Tests that inspect absolute counts (for example,
 * {@link #testFilterNodes()}) capture a baseline at their start rather than
 * assuming an empty DB.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArrangementControllerTest extends AbstractControllerTest {

    public static final Logger logger = LoggerFactory.getLogger(ArrangementControllerTest.class);

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

    public static final String STORAGE_NUMBER = "Test 123";
    public static final String STORAGE_NUMBER_FOUND = "Te";
    public static final String STORAGE_NUMBER_NOT_FOUND = "Sf";
    public static final String STORAGE_NUMBER_CHANGE = "Test 321";

    private static final String JSON_TABLE_CSV = "jsontable/jsontable.csv";

    public static final String NAME_AP = "UseCase ščřžý";
    public static final Integer LIMIT = 100;

    // maximální počet položek pro načtení
    public static final int MAX_SIZE = 999;
    
    /**
     * Full fund lifecycle: create fund, open/approve version, build node tree,
     * move/delete nodes, edit description items, run validations, exercise
     * forms/tree/output/filter endpoints.
     *
     * <p><b>Creates:</b> 1 fund + nodes + levels + desc-items + outputs.
     * <br><b>Cleans up:</b> nothing — fund intentionally left for the class-level cleanup (see class javadoc).
     */
    @Test
    public void arrangementTest() throws IOException, InterruptedException, ExecutionException, IllegalAccessException {

        // vytvoření
        Fund fund = createdFund();

        helperTestService.waitForWorkers();
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        // uzavření verze
        helperTestService.waitForWorkers();
        fundVersion = approvedVersion(fundVersion);

        // vytvoření uzlů
        helperTestService.waitForWorkers();
        List<ArrNodeVO> nodes = createLevels(fundVersion);

        // získání informací o ulzu + fundu
        nodeInfo(nodes, fundVersion);

        // přesunutí && smazání uzlů
        helperTestService.waitForWorkers();
        moveAndDeleteLevels(nodes, fundVersion);

        // atributy
        helperTestService.waitForWorkers();
        operationsDescItems(fundVersion);

        // validace
        helperTestService.waitForWorkers();
        validations(fundVersion, nodes);
        
        // všechny formuláře / stromy / ...
        helperTestService.waitForWorkers();
        forms(fundVersion);

        // akce nad výstupy
        outputs(fundVersion);

        // filtry
        filters(fundVersion);
    }

    //TODO: odkomentovat po změně importu institucí @Test
    public void fundFulltextTest() throws InterruptedException {

        final String value = "aaa";
        final int count = 2;

        List<Fund> funds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            helperTestService.waitForWorkers();
            funds.add(createFundFulltext(i, count, value));
        }

        // je třeba počkat na asychronné přeindexování (možná by se mělo řešit úplně jinak)
        Thread.sleep(2000);

        try {

            Set<String> names = funds.stream().map(fund -> fund.getName()).collect(Collectors.toSet());

            List<ArrFundFulltextResult> resultList = fundFulltext(new FulltextFundRequest(value));

            for (ArrFundFulltextResult result : resultList) {
                assertTrue(names.remove(result.getName()), "Invalid fund [" + result.getName() + "]");
                assertEquals(count, result.getCount(), "Invalid count [" + result.getName() + "]");
            }

            assertTrue(names.isEmpty(), "Fund not found [" + StringUtils.join(names, ", ") + "]");

            for (ArrFundFulltextResult result : resultList) {
                List<TreeNodeVO> nodeList = fundFulltextNodeList(result.getId());
                assertEquals(count, nodeList.size(), "Invalid count [" + result.getName() + "]");
                for (TreeNodeVO node : nodeList) {
                    assertEquals(value, node.getName(), "Invalid node value [" + result.getName() + "]");
                }
            }

        } finally {
            //smazání fondu

            helperTestService.waitForWorkers();
            for (Fund fund : funds) {
                helperTestService.waitForWorkers();
                deleteFund(fund.getId());
            }
        }
    }

    private Fund createFundFulltext(int i, int count, String value) throws InterruptedException {

        Fund fund = createFund("Test fulltext " + i, "TST" + 1);

        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode(SRD_TITLE);

        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);

        for (int j = 0; j < count; j++) {
            NodeItem nodeItem = buildNodeItem(typeVo.getCode(), null, DataType.STRING, value, nodes.get(j), null);
            descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        }

        return fund;
    }

    /**
     * Change tracking / revert flow: creates a fund, makes a sequence of
     * changes (~33 changes documented in the test body), and exercises the
     * revert endpoint and change-history filtering.
     *
     * <p><b>Creates:</b> 1 fund + nodes + desc-items + ~33 change records.
     * <br><b>Cleans up:</b> nothing — fund intentionally left for the class-level cleanup (see class javadoc).
     */
    @Test
    public void revertingChangeTest() throws IOException, InterruptedException {

        Fund fund = createdFund();

        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        // vytvoření uzlů
        helperTestService.waitForWorkers();
        List<ArrNodeVO> nodes = createLevels(fundVersion);

        // přesunutí && smazání uzlů
        helperTestService.waitForWorkers();
        moveAndDeleteLevels(nodes, fundVersion);

        // atributy
        helperTestService.waitForWorkers();
        operationsDescItems(fundVersion);

        ChangesResult changesAll = findChanges(fundVersion.getId(), MAX_SIZE, 0, null, null);
        assertNotNull(changesAll);
        assertNotNull(changesAll.getChanges());
        assertTrue(changesAll.getTotalCount().equals(changesAll.getChanges().size()) && changesAll.getChanges().size() == 33);
        assertFalse(changesAll.getOutdated());

        ChangesResult changesByNode = findChanges(fundVersion.getId(), MAX_SIZE, 0, null, nodes.get(0).getId());
        assertNotNull(changesByNode);
        assertNotNull(changesByNode.getChanges());
        assertTrue(changesByNode.getTotalCount().equals(changesByNode.getChanges().size()) && changesByNode.getChanges().size() == 9);

        final Integer lastChangeId = changesAll.getChanges().get(0).getChangeId();
        final Integer firstChangeId = changesAll.getChanges().get(changesAll.getChanges().size() - 1).getChangeId();
        ChangesResult changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, OffsetDateTime.now(), lastChangeId, null);
        assertNotNull(changesByDate);
        assertNotNull(changesByDate.getChanges());

//        // TODO: test
//        try {
//            logger.info(changesByDate.getTotalCount() + ", " + changesByDate.getChanges().size() + ", xxxxxxxxxxxxxxxxxxxx");
//            Thread.sleep(5000);
//            changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, OffsetDateTime.now(), lastChangeId, null);
//            logger.info(changesByDate.getTotalCount() + ", " + changesByDate.getChanges().size() + ", xxxxxxxxxxxxxxxxxxxx");
//            Thread.sleep(5000);
//            changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, OffsetDateTime.now(), lastChangeId, null);
//            logger.info(changesByDate.getTotalCount() + ", " + changesByDate.getChanges().size() + ", xxxxxxxxxxxxxxxxxxxx");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        assertTrue(changesByDate.getTotalCount().equals(changesByDate.getChanges().size()) && changesByDate.getChanges().size() == 33);
//        assertTrue(!changesByDate.getOutdated());

        final Integer lastChangeIdFinal = lastChangeId; // effectively final
        await()
            .atMost(20, SECONDS)
            .pollInterval(500, MILLISECONDS)
            .untilAsserted(() -> {
                ChangesResult result = findChangesByDate(fundVersion.getId(), MAX_SIZE, OffsetDateTime.now(), lastChangeIdFinal, null);
                logger.info("Changes count: {}, total: {}", result.getChanges().size(), result.getTotalCount());
                assertTrue(result.getTotalCount().equals(result.getChanges().size()) && result.getChanges().size() == 33);
            });

        changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, OffsetDateTime.now(), lastChangeId, null);
        assertTrue(!changesByDate.getOutdated());

        // obdoba revertChanges s fail očekáváním
        httpMethod(spec -> spec.pathParam("fundVersionId", fundVersion.getId())
                        .queryParam("fromChangeId", lastChangeId)
                        .queryParam("toChangeId", firstChangeId),
                REVERT_CHANGES, HttpMethod.GET, HttpStatus.INTERNAL_SERVER_ERROR);

        final Integer secondChangeId = changesAll.getChanges().get(changesAll.getChanges().size() - 2).getChangeId();
        helperTestService.waitForWorkers();
        revertChanges(fundVersion.getId(), lastChangeId, secondChangeId, null);
    }

    /**
     * Testování validací.
     *
     * @param fundVersion verze archivní pomůcky
     * @param nodes
     */
    private void validations(final ArrFundVersionVO fundVersion, final List<ArrNodeVO> nodes) {
        logger.info("Validate fundVersion : " + fundVersion.getId());
        validateVersion(fundVersion);

        ArrangementController.ValidationItems validation = getValidation(fundVersion.getId(), 0, 100);
        assertNotNull(validation);
        assertNotNull(validation.getCount());
        //assertTrue(CollectionUtils.isNotEmpty(validation.getItems()));

        ArrangementController.ValidationItems validationError = findValidationError(fundVersion.getId(), nodes.get(0).getId(), 1);
        assertNotNull(validationError);
        //assertTrue(CollectionUtils.isNotEmpty(validationError.getItems()));

        List<NodeItemWithParent> visiblePolicy = getAllNodesVisiblePolicy(fundVersion.getId());
        assertNotNull(visiblePolicy); // TODO: přepsat na notEmpty
    }

    /**
     * Testování filtrů.
     *
     * @param fundVersion verze archivní pomůcky
     */
    private void filters(final ArrFundVersionVO fundVersion) {
        filterNodes(fundVersion.getId(), new Filters());
        Set<Integer> descItemTypeIds = getDescItemTypes().stream().map(item -> item.getId()).collect(Collectors.toSet());
        List<FilterNode> filteredNodes = getFilteredNodes(fundVersion.getId(), 0, 10, descItemTypeIds);
        assertTrue(CollectionUtils.isNotEmpty(filteredNodes));
        //List<FilterNodePosition> filteredFulltextNodes = getFilteredFulltextNodes(fundVersion.getId(), "1", false);
        //assertTrue(CollectionUtils.isNotEmpty(filteredFulltextNodes));
    }

    /**
     * Testování práci s výstupy.
     *
     * @param fundVersion verze archivní pomůcky
     */
    private void outputs(final ArrFundVersionVO fundVersion) {

        {
            List<ArrOutputVO> outputs = getOutputs(fundVersion.getId());
            assertTrue(outputs.size() == 0);
        }

        {
            List<RulOutputTypeVO> outputTypes = getOutputTypes(fundVersion.getId());
            assertTrue(CollectionUtils.isNotEmpty(outputTypes));

            ArrOutputVO output1 = createNamedOutput(fundVersion, "Test", "TST", outputTypes.iterator().next().getId());
            assertNotNull(output1);
        }

        ArrOutputVO output2;
        {
            List<ArrOutputVO> outputs = getOutputs(fundVersion.getId());
            assertTrue(outputs.size() == 1);
            output2 = outputs.get(0);
        }

        ArrOutputVO outputDetail = getOutput(fundVersion.getId(), output2.getId());

        assertNotNull(outputDetail);
        assertTrue(outputDetail.getId().equals(output2.getId()));

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        List<NodeBase> nodes = convertTreeNodes(treeData.getNodes());
        List<Integer> nodeIds = nodes.stream().map(NodeBase::getId).collect(Collectors.toList());

        addNodesNamedOutput(fundVersion.getId(), outputDetail.getId(), nodeIds);

        outputDetail = getOutput(fundVersion.getId(), output2.getId());
        assertTrue(outputDetail.getNodes().size() == nodeIds.size());

        removeNodesNamedOutput(fundVersion.getId(), outputDetail.getId(), nodeIds);

        outputDetail = getOutput(fundVersion.getId(), output2.getId());
        assertTrue(outputDetail.getNodes().size() == 0);

        updateNamedOutput(fundVersion, outputDetail, "Test 2", "TST2");
        outputDetail = getOutput(fundVersion.getId(), output2.getId());
        assertTrue(outputDetail.getName().equals("Test 2"));
        assertTrue(outputDetail.getInternalCode().equals("TST2"));

        ArrOutputVO output3;
        {
            List<ArrOutputVO> outputs = getOutputs(fundVersion.getId());
            output3 = outputs.get(0);
        }

        ArrItemTextVO item = new ArrItemTextVO();
        item.setValue("test1");
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("SRD_SCALE");
        ArrangementController.OutputItemResult outputItem = createOutputItem(item, fundVersion.getId(), typeVo.getId(), output3.getId(), output3.getVersion());
        ArrItemVO itemCreated = outputItem.getItem();
        assertNotNull(itemCreated);
        assertNotNull(itemCreated.getDescItemObjectId());
        assertNotNull(itemCreated.getPosition());
        assertTrue(itemCreated instanceof ArrItemTextVO);
        ArrItemTextVO itemCreatedText = (ArrItemTextVO) itemCreated;
        assertTrue(itemCreatedText.getValue().equals(item.getValue()));

        itemCreatedText.setValue("xxx");
        outputItem = updateOutputItem(itemCreated, fundVersion.getId(), outputItem.getParent().getVersion(), true);

        ArrItemVO itemUpdated = outputItem.getItem();
        assertNotNull(itemUpdated);
        assertNotNull(itemUpdated.getDescItemObjectId());
        assertNotNull(itemUpdated.getPosition());
        assertTrue(itemUpdated instanceof ArrItemTextVO);
        assertTrue(((ArrItemTextVO) itemUpdated).getValue().equals(itemCreatedText.getValue()));

        ArrangementController.OutputFormDataNewVO outputFormData = getOutputFormData(outputItem.getParent().getId(), fundVersion.getId());

        assertNotNull(outputFormData.getParent());

        outputItem = deleteOutputItem(itemCreated.getDescItemObjectId(), fundVersion.getId(), outputItem.getParent().getVersion());
        ArrOutputVO parent = outputItem.getParent();

        ArrItemVO itemDeleted = outputItem.getItem();
        Assertions.assertNull(itemDeleted);

        item = new ArrItemTextVO();
        item.setValue("test1");
        outputItem = createOutputItem(item, fundVersion.getId(), typeVo.getId(), output3.getId(), parent.getVersion());
        parent = outputItem.getParent();
        itemCreated = outputItem.getItem();

        ArrangementController.OutputItemResult outputItemResult = deleteOutputItemsByType(fundVersion.getId(), parent.getId(), parent.getVersion(), typeVo.getId());
        parent = outputItemResult.getParent();

        // docasne zakazano - bude vraceno zpet pri prechodu na vyvojarska pravidla
        /*outputItemResult = setNotIdentifiedOutputItem(fundVersion.getId(), parent.getId(), parent.getVersion(), typeVo.getId(), null, null);
        parent = outputItemResult.getParent();
        // Návratová struktura nesmí být prázdná
        assertNotNull(outputItemResult);
        // Hodnota atributu nesmí být prázdná
        assertNotNull(outputItemResult.getItem());
        ArrItemTextVO textVO = (ArrItemTextVO) outputItemResult.getItem();
        // Hodnota Nezjištěno musí být true
        assertTrue(textVO.getUndefined());
        // Identifikátor nesmí být prázdný
        assertNotNull(textVO.getDescItemObjectId());
        // Hodnota musí být prázdná
        Assertions.assertNull(textVO.getValue());

        outputItemResult = unsetNotIdentifiedOutputItem(fundVersion.getId(), parent.getId(), parent.getVersion(), typeVo.getId(), null, textVO.getDescItemObjectId());
        parent = outputItemResult.getParent();
        // Návratová struktura nesmí být prázdná
        assertNotNull(outputItemResult);
        // Hodnota atributu musí být prázdná
        Assertions.assertNull(outputItemResult.getItem());*/
        OutputSettingsVO outputSettings = new OutputSettingsVO();
        outputSettings.setEvenPageOffsetX(42);
        outputSettings.setEvenPageOffsetY(42);
        outputSettings.setOddPageOffsetX(42);
        outputSettings.setOddPageOffsetY(42);

        super.setOutputSettings(outputDetail.getId(), outputSettings);
        ArrOutput one = this.helperTestService.getOutputRepository()
                .findById(outputDetail.getId())
                .orElseThrow(output(outputDetail.getId()));

        String outputSettings1 = one.getOutputSettings();
        ObjectMapper mapper = new ObjectMapper();
        try {
            OutputSettingsVO settingsVO = mapper.readValue(outputSettings1, OutputSettingsVO.class);
            assertEquals("42", String.valueOf(settingsVO.getEvenPageOffsetX()));
            assertEquals("42", String.valueOf(settingsVO.getEvenPageOffsetY()));
            assertEquals("42", String.valueOf(settingsVO.getOddPageOffsetX()));
            assertEquals("42", String.valueOf(settingsVO.getOddPageOffsetY()));

        } catch (IOException e) {
            e.printStackTrace();
        }
        deleteNamedOutput(fundVersion.getId(), output2.getId());
        outputDetail = getOutput(fundVersion.getId(), output2.getId());
        assertTrue(outputDetail.getDeleteDate() != null);

        {
            List<ArrOutputVO> outputs = getOutputs(fundVersion.getId());
            assertTrue(outputs.size() == 0);
        }
    }

    /**
     * Zavolání metod pro formuláře atd...
     *
     * @param fundVersion verze archivní pomůcky
     * @throws ExecutionException 
     * @throws InterruptedException 
     * @throws IllegalAccessException 
     */
    private void forms(final ArrFundVersionVO fundVersion) throws InterruptedException, ExecutionException, IllegalAccessException {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        List<NodeBase> nodes = convertTreeNodes(treeData.getNodes());
        NodeBase rootNode = nodes.get(0);
        Integer firstNodeId = nodes.get(1).getId();
        Integer secondNodeId = nodes.get(2).getId();

        ArrangementController.FaTreeNodesParam inputFa = new ArrangementController.FaTreeNodesParam();
        inputFa.setVersionId(fundVersion.getId());
        inputFa.setNodeIds(Arrays.asList(rootNode.getId()));
        List<TreeNodeVO> faTreeNodes = getFundTreeNodes(inputFa);
        assertTrue(CollectionUtils.isNotEmpty(faTreeNodes));

        NodeDataParam ndp = new NodeDataParam();
        ndp.setNodeId(rootNode.getId());
        ndp.setFundVersionId(fundVersion.getId());
        ndp.setParents(true);

        NodeDataVO nodeData = getNodeData(ndp);
        Collection<TreeNodeVO> nodeParents = nodeData.getParents();
        assertNotNull(nodeParents);

        ArrangementController.DescFormDataNewVO nodeFormData = getNodeFormData(rootNode.getId(), fundVersion.getId());
        assertNotNull(nodeFormData.getParent());

        ArrangementController.NodeFormsDataVO nodeFormsData = getNodeFormsData(fundVersion.getId(), rootNode.getId(), firstNodeId, secondNodeId);
        assertTrue(nodeFormsData.getForms().size() == nodes.size());

        // kontrola zděděných descItem v nodes.get(1)
        ArrItemVO inheritedItem = findInheritedItem(nodeFormsData.getForms().get(firstNodeId));
        assertNotNull(inheritedItem);
        assertEquals(rootNode.getId(), inheritedItem.getFromNodeId());

        // kontrola zděděných descItem v nodes.get(2)
        inheritedItem = findInheritedItem(nodeFormsData.getForms().get(secondNodeId));
        assertNotNull(inheritedItem);
        assertEquals(rootNode.getId(), inheritedItem.getFromNodeId());

        // zákaz dědictví na úrovni #2 nodes.get(2)
        ArrInhibitedItemVO arrInhibitedItem = new ArrInhibitedItemVO(); 
        arrInhibitedItem.setNodeId(secondNodeId);
        arrInhibitedItem.setDescItemObjectId(inheritedItem.getDescItemObjectId());

    	final Map<String, Message<byte[]>> receiptStore = new HashMap<>();
        MyStompSessionHandler sessionHandler = new MyStompSessionHandler();
        StompSession session = connectWebSocketStompClient(sessionHandler, receiptStore);
        session.setAutoReceipt(true);
        FieldUtils.writeField(StompCommand.RECEIPT, "body", true, true);

        Receiptable receiptable = session.send(ArrangementWebsocketControllerTest.INHIBIT_DESC_ITEM, arrInhibitedItem);
        ReceiptStatus status = waitingForReceipt(receiptable, sessionHandler);
        assertEquals(ReceiptStatus.RCP_RECEIVED, status);

        // zděděný ArrDescItem má pozitivní příznak potlačené dědičnosti na úrovni #2
        nodeFormData = getNodeFormData(secondNodeId, fundVersion.getId());
        inheritedItem = findInheritedItem(nodeFormData);
        assertNotNull(inheritedItem.getInhibited());
        assertTrue(inheritedItem.getInhibited());

        nodeFormsData = getNodeWithAroundFormsData(fundVersion.getId(), nodes.get(1).getId(), 5);
        assertTrue(nodeFormsData.getForms().size() > 0);

        ArrangementController.IdsParam idsParamNodes = new ArrangementController.IdsParam();
        idsParamNodes.setVersionId(fundVersion.getId());
        idsParamNodes.setIds(Arrays.asList(nodes.get(1).getId()));
        List<TreeNodeVO> treeNodeClients = getNodes(idsParamNodes);
        assertTrue(treeNodeClients.size() > 0);

        ArrangementController.IdsParam idsParamFa = new ArrangementController.IdsParam();
        idsParamFa.setIds(Arrays.asList(fundVersion.getId()));

        List<ArrFundVO> fundsByVersionIds = getFundsByVersionIds(idsParamFa);
        assertTrue(fundsByVersionIds.size() > 0);
    }

    /**
     * Nalezení zděděného ArrItemVO
     * 
     * @param nodeFormData
     * @return
     */
    private ArrItemVO findInheritedItem(ArrangementController.DescFormDataNewVO nodeFormData) {
        for (ArrItemVO item : nodeFormData.getDescItems()) {
        	if (item.getFromNodeId() != null) {
        		return item;
        	}
        }
        return null;
    }

    /**
     * Zavolání metod pro zjištění validací.
     *
     * @param fundVersion verze archivní pomůcky
     */
    protected void validateVersion(final ArrFundVersionVO fundVersion) {
        List<ArrangementController.VersionValidationItem> items = validateVersion(fundVersion.getId());
        assertNotNull(items);
        validateVersionCount(fundVersion.getId());
    }

    /**
     * Operace s descItems (create, update, delete).
     *
     * @param fundVersion verze archivní pomůcky
     * @throws ApiException 
     */
    private void operationsDescItems(final ArrFundVersionVO fundVersion) throws IOException, InterruptedException {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        RulDescItemSpecExtVO spec;

        List<NodeBase> nodes = convertTreeNodes(treeData.getNodes());
        NodeBase rootNode = nodes.get(0);

        List<ApAccessPointVO> accessPoints = findRecord(null, null, null, null, null);
        ApAccessPointVO accessPoint = accessPoints.get(0);

        // vytvoření hodnoty
        helperTestService.waitForWorkers();
        RulDescItemTypeExtVO type = findDescItemTypeByCode("SRD_SCALE");
        NodeItem nodeItem = buildNodeItem(type.getCode(), null, DataType.TEXT, "value", convertToArrNode(rootNode), null);
        ItemDataResult itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        rootNode = itemDataResult.getParent();
        NodeItem nodeItemCreated = itemDataResult.getItem();

        assertNotNull(((DataText) nodeItem.getData()).getTextValue().equals(((DataText) nodeItemCreated.getData()).getTextValue()));
        assertNotNull(nodeItemCreated.getPosition());
        assertNotNull(nodeItemCreated.getItemObjectId());

        // aktualizace hodnoty
        helperTestService.waitForWorkers();
        ((DataText) nodeItemCreated.getData()).setTextValue("update value");
        itemDataResult = descitemsApi.descItemUpdateDescItem(fundVersion.getId(), true, nodeItemCreated);
        rootNode = itemDataResult.getParent();
        NodeItem nodeItemUpdated = itemDataResult.getItem();

        assertTrue(nodeItemUpdated.getItemObjectId().equals(nodeItemCreated.getItemObjectId()));
        assertTrue(nodeItemUpdated.getPosition().equals(nodeItemCreated.getPosition()));
        assertTrue(!nodeItemUpdated.getId().equals(nodeItemCreated.getId()));
        assertTrue(((DataText) nodeItemUpdated.getData()).getTextValue().equals(((DataText) nodeItemCreated.getData()).getTextValue()));

        // odstranění hodnoty
        helperTestService.waitForWorkers();
        itemDataResult = descitemsApi.descItemDeleteDescItem(fundVersion.getId(), nodeItemUpdated);
        rootNode = itemDataResult.getParent();

        helperTestService.waitForWorkers();
        // nastavené nemožné hodnoty
        DescItemResult descItemResult = setNotIdentifiedDescItem(fundVersion.getId(), rootNode.getId(), rootNode.getVersion(), type.getId(), null, null);
        rootNode = convertArrNode(descItemResult.getParent());

        // Návratová struktura nesmí být prázdná
        assertNotNull(descItemResult);
        // Hodnota atributu nesmí být prázdná
        assertNotNull(descItemResult.getItem());
        ArrItemTextVO item = (ArrItemTextVO) descItemResult.getItem();
        // Hodnota Nezjištěno musí být true
        assertTrue(item.getUndefined());
        // Identifikátor nesmí být prázdný
        assertNotNull(item.getDescItemObjectId());
        // Hodnota musí být prázdná
        Assertions.assertNull(item.getValue());

        helperTestService.waitForWorkers();
        descItemResult = unsetNotIdentifiedDescItem(fundVersion.getId(), rootNode.getId(), rootNode.getVersion(), type.getId(), null, item.getDescItemObjectId());
        rootNode = convertArrNode(descItemResult.getParent());

        // Návratová struktura nesmí být prázdná
        assertNotNull(descItemResult);
        // Hodnota atributu musí být prázdná
        Assertions.assertNull(descItemResult.getItem());

        // vytvoření další hodnoty
        helperTestService.waitForWorkers();
        type = findDescItemTypeByCode("SRD_SCALE");
        nodeItem = buildNodeItem(type.getCode(), null, DataType.TEXT, "value", rootNode, null);
        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        rootNode = itemDataResult.getParent();

        // fulltext
        fulltextTest(fundVersion);

        helperTestService.waitForWorkers();
        descItemResult = deleteDescItemsByType(fundVersion.getId(), rootNode.getId(), rootNode.getVersion(), type.getId());
        rootNode = convertArrNode(descItemResult.getParent());

        NodeBase node = nodes.get(1);

        ArrangementController.DescriptionItemParam param = new ArrangementController.DescriptionItemParam();
        param.setVersionId(fundVersion.getId());
        param.setNode(convertToArrNode(node));
        param.setDirection(DirectionLevel.ROOT);
        getDescriptionItemTypesForNewLevel(false, param);

        // vytvoření další hodnoty - vícenásobné
        helperTestService.waitForWorkers();
        type = findDescItemTypeByCode(SRD_OTHER_ID);
        spec = findDescItemSpecByCode(SRD_OTHERID_CJ, type);
        nodeItem = buildNodeItem(type.getCode(), spec.getCode(), DataType.STRING, "1", node, null);
        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        node = itemDataResult.getParent();

        nodeItem = buildNodeItem(type.getCode(), spec.getCode(), DataType.STRING, "2", node, null);
        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        node = itemDataResult.getParent();

        nodeItem = buildNodeItem(type.getCode(), spec.getCode(), DataType.STRING, "3", node, null);
        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        node = itemDataResult.getParent();
        NodeItem nodeItemCreated3 = itemDataResult.getItem();

        ((DataString) nodeItemCreated3.getData()).setStringValue("3x");
        nodeItemCreated3.setPosition(5);
        itemDataResult = descitemsApi.descItemUpdateDescItem(fundVersion.getId(), true, nodeItemCreated3);
        node = itemDataResult.getParent();

        ArrangementController.CopySiblingResult copySiblingResult =
                copyOlderSiblingAttribute(fundVersion.getId(), type.getId(), convertToArrNode(nodes.get(2)));

        type = findDescItemTypeByCode(SRD_UNIT_DATE);
        nodeItem = buildNodeItem(type.getCode(), null, DataType.UNITDATE, "1920", node, null);
        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        node = itemDataResult.getParent();

        LocalDate dateNow = LocalDate.now();
        type = findDescItemTypeByCode("SRD_SIMPLE_DATE");
        nodeItem = buildNodeItem(type.getCode(), null, DataType.DATE, dateNow, node, null);
        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        node = itemDataResult.getParent();

        type = findDescItemTypeByCode("SRD_LEGEND");
        nodeItem = buildNodeItem(type.getCode(), null, DataType.TEXT, "legenda", node, null);
        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        node = itemDataResult.getParent();

        helperTestService.waitForWorkers();
        type = findDescItemTypeByCode("SRD_POSITION");
        nodeItem = buildNodeItem(type.getCode(), null, DataType.COORDINATES, "POINT (14 49)", node, null);
        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        node = itemDataResult.getParent();

        helperTestService.waitForWorkers();
        type = findDescItemTypeByCode("SRD_COLL_EXTENT_LENGTH");
        nodeItem = buildNodeItem(type.getCode(), null, DataType.DECIMAL, BigDecimal.valueOf(20.5), node, null);

        // TODO to fix or delete
        Thread.sleep(1000);

        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        node = itemDataResult.getParent();

        type = findDescItemTypeByCode("SRD_UNIT_COUNT_TABLE");
        assertNotNull(type);
        ElzaTable table = new ElzaTable();
        table.addRow(new ElzaRow(new AbstractMap.SimpleEntry<>("NAME", "Test 1"), new AbstractMap.SimpleEntry<>("COUNT", "195")));
        table.addRow(new ElzaRow(new AbstractMap.SimpleEntry<>("NAME", "Test 2"), new AbstractMap.SimpleEntry<>("COUNT", "200")));
        nodeItem = buildNodeItem(type.getCode(), null, DataType.JSON_TABLE, table, node, null);
        itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        node = itemDataResult.getParent();

        // Import a export CSV pro atribut JSON_TABLE
        {
            // Import
            type = findDescItemTypeByCode("SRD_UNIT_COUNT_TABLE");
            descItemResult = descItemCsvImport(fundVersion.getId(), node.getVersion(), node.getId(), type.getId(), getFile(JSON_TABLE_CSV));

            // Export a kontrola
            InputStream is = descItemCsvExport(fundVersion.getId(), descItemResult.getItem().getDescItemObjectId());
            Reader in = new InputStreamReader(is, CsvUtils.CSV_EXCEL_ENCODING);
            Iterable<CSVRecord> records = CsvUtils.CSV_EXCEL_FORMAT.withFirstRecordAsHeader().parse(in);
            List<CSVRecord> recordsList = new ArrayList<>();
            records.forEach(recordsList::add);
            assertTrue(recordsList.size() == 6); // šest řádků bez hlavičky

            assertTrue(recordsList.get(0).get("NAME").equals("klic1"));
            assertTrue(recordsList.get(0).get("COUNT").equals("1"));

            assertTrue(recordsList.get(1).get("NAME").equals("klic2"));
            assertTrue(recordsList.get(1).get("COUNT").equals("2"));

            assertTrue(recordsList.get(2).get("NAME").equals("klic3"));
            assertTrue(recordsList.get(2).get("COUNT").equals(""));

            assertTrue(recordsList.get(3).get("NAME").equals(""));
            assertTrue(recordsList.get(3).get("COUNT").equals("4"));

            assertTrue(recordsList.get(4).get("NAME").equals(""));
            assertTrue(recordsList.get(4).get("COUNT").equals(""));

            assertTrue(recordsList.get(5).get("NAME").equals("kk"));
            assertTrue(recordsList.get(5).get("COUNT").equals("11"));
        }

        // vytváření hodnoty pro dědictví
        helperTestService.waitForWorkers();
        type = findDescItemTypeByCode(SRD_ENTITY_ROLE);
        spec = findDescItemSpecByCode(SRD_ENTITY_ROLE_1, type);
        nodeItem = buildNodeItem(type.getCode(), spec.getCode(), DataType.RECORD_REF, accessPoint, rootNode, null);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
    }

	/**
     * Přesunutí a smazání levelů
     *
     * @param nodes       založené uzly (1. je root)
     * @param fundVersion verze archivní pomůcky
     */
    private void moveAndDeleteLevels(final List<ArrNodeVO> nodes,
                                     final ArrFundVersionVO fundVersion) {
        ArrNodeVO rootNode = nodes.get(0);
        TreeNodeVO parentNode;

        // baseline: all created nodes must be in the cache
        for (ArrNodeVO node : nodes) {
            assertNodeInCache(node.getId());
        }
        assertCacheInvariant();

        // 1. přesun druhého uzlu před první
        helperTestService.waitForWorkers();
        moveLevelBefore(fundVersion, nodes.get(1), rootNode, Arrays.asList(nodes.get(2)), rootNode);

        List<NodeBase> newNodes = getTreeNodes(fundVersion.getId(), rootNode.getId());

        // kontrola přesunu
        assertTrue(newNodes.size() == 4);
        assertTrue(newNodes.get(0).getId().equals(nodes.get(2).getId()));
        assertTrue(newNodes.get(1).getId().equals(nodes.get(1).getId()));
        assertTrue(newNodes.get(2).getId().equals(nodes.get(3).getId()));
        assertTrue(newNodes.get(3).getId().equals(nodes.get(4).getId()));

        // move vytváří nový level pro přesouvaný uzel, uzel má stále aktivní level -> musí být v cache
        for (ArrNodeVO node : nodes) {
            assertNodeInCache(node.getId());
        }
        assertCacheInvariant();

        helperTestService.waitForWorkers();
        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // 2. přesun druhého uzlu pod první
        helperTestService.waitForWorkers();
        moveLevelUnder(fundVersion, convertToArrNode(newNodes.get(0)), rootNode, Arrays.asList(convertToArrNode(newNodes.get(1))), rootNode);

        List<NodeBase> newNodes2 = getTreeNodes(fundVersion.getId(), newNodes.get(0).getId());

        // kontrola přesunu
        assertTrue(newNodes2.size() == 1);
        assertTrue(newNodes2.get(0).getId().equals(newNodes.get(1).getId()));

        // po přesunu pod jiný uzel musí být všechny uzly stále v cache
        for (ArrNodeVO node : nodes) {
            assertNodeInCache(node.getId());
        }
        assertCacheInvariant();

        helperTestService.waitForWorkers();
        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // 3. smazání druhého uzlu v první úrovni
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent nodesWithParent = deleteLevel(fundVersion, convertToArrNode(newNodes.get(2)), rootNode);

        assertTrue(nodesWithParent.getNode().getId().equals(newNodes.get(2).getId()));
        assertTrue(nodesWithParent.getParentNode().getId().equals(rootNode.getId()));

        List<NodeBase> newNodes3 = getTreeNodes(fundVersion.getId(), rootNode.getId());

        // kontrola smazání
        assertTrue(newNodes3.size() == 2);
        assertTrue(newNodes3.get(0).getId().equals(newNodes.get(0).getId()));
        assertTrue(newNodes3.get(1).getId().equals(newNodes.get(3).getId()));

        // smazaný uzel ztratil poslední aktivní level -> musí být odstraněn z cache
        // (fyzické smazání provádí afterCommit hook po commitu této transakce)
        assertNodeNotInCache(newNodes.get(2).getId());
        // zbývající uzly zůstávají v cache
        assertNodeInCache(rootNode.getId());
        assertNodeInCache(newNodes.get(0).getId());
        assertNodeInCache(newNodes.get(1).getId());
        assertNodeInCache(newNodes.get(3).getId());
        assertCacheInvariant();

        helperTestService.waitForWorkers();
        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // 4. přidání uzlu ve druhé úrovni
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent newLevel4 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                                                                  fundVersion, newNodes3.get(0), newNodes3.get(0), null);

        helperTestService.waitForWorkers();
        parentNode = newLevel4.getParentNode();
        newNodes3.get(0).setId(parentNode.getId());
        newNodes3.get(0).setVersion(parentNode.getVersion());

        List<NodeBase> newNodes4 = getTreeNodes(fundVersion.getId(), newNodes.get(0).getId());

        // kontrola přidání
        assertTrue(newNodes4.size() == 2);
        assertTrue(newNodes4.get(0).getId().equals(newNodes.get(1).getId()));
        assertTrue(newNodes4.get(1).getId().equals(newLevel4.getNode().getId()));

        // 5. přidání uzlu na konec seznamu
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent newLevel5 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                                                                  fundVersion, convertArrNode(rootNode), convertArrNode(rootNode), null);

        helperTestService.waitForWorkers();
        parentNode = newLevel5.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // 6. přidání uzlu  na konec seznamu
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent newLevel6 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                                                                  fundVersion, convertArrNode(rootNode), convertArrNode(rootNode), null);

        helperTestService.waitForWorkers();
        parentNode = newLevel6.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        List<NodeBase> newNodes56 = getTreeNodes(fundVersion.getId(), rootNode.getId());

        // kontrola přidání
        assertTrue(newNodes56.size() == 4);
        assertTrue(newNodes56.get(0).getId().equals(newNodes.get(0).getId()));
        assertTrue(newNodes56.get(1).getId().equals(newNodes.get(3).getId()));
        assertTrue(newNodes56.get(2).getId().equals(newLevel5.getNode().getId()));
        assertTrue(newNodes56.get(3).getId().equals(newLevel6.getNode().getId()));

        // 7. přesun posledního za první
        helperTestService.waitForWorkers();
        moveLevelAfter(fundVersion, convertToArrNode(newNodes56.get(3)), rootNode, Arrays.asList(convertToArrNode(newNodes56.get(2))), rootNode);

        List<NodeBase> newNodes7 = getTreeNodes(fundVersion.getId(), rootNode.getId());

        // kontrola přesunu
        assertTrue(newNodes7.size() == 4);
        assertTrue(newNodes7.get(0).getId().equals(newNodes.get(0).getId()));
        assertTrue(newNodes7.get(1).getId().equals(newNodes.get(3).getId()));
        assertTrue(newNodes7.get(2).getId().equals(newNodes56.get(3).getId()));
        assertTrue(newNodes7.get(3).getId().equals(newNodes56.get(2).getId()));

        helperTestService.waitForWorkers();
        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // 8. přesun seznamu uzlů z různých úrovní pod
        helperTestService.waitForWorkers();
        moveLevelUnder(fundVersion, convertToArrNode(newNodes4.get(0)), convertToArrNode(newNodes7.get(0)), 
        		       Arrays.asList(convertToArrNode(newNodes7.get(1)), convertToArrNode(newNodes4.get(1)), convertToArrNode(newNodes7.get(2))), rootNode);

        // přenesené záznamy
        List<NodeBase> moveNodes = getTreeNodes(fundVersion.getId(), newNodes4.get(0).getId());

        // kontrola přenášených záznamů
        assertTrue(moveNodes.size() == 3);
        assertTrue(moveNodes.get(0).getId().equals(newNodes7.get(1).getId()));
        assertTrue(moveNodes.get(1).getId().equals(newNodes4.get(1).getId()));
        assertTrue(moveNodes.get(2).getId().equals(newNodes7.get(2).getId()));

        // výsledek všech akcí od root
        List<NodeBase> resultNodes = getTreeNodes(fundVersion.getId(), rootNode.getId(), Collections.singleton((newNodes.get(0).getId())));

        // kontrola výsledku
        assertTrue(resultNodes.size() == 3);
        assertTrue(resultNodes.get(0).getId().equals(newNodes.get(0).getId()));
        assertTrue(resultNodes.get(1).getId().equals(newNodes4.get(0).getId()));
        assertTrue(resultNodes.get(2).getId().equals(newNodes7.get(3).getId()));

        // po všech přesunech/smazáních musí cache odpovídat invariantu:
        // arr_cached_node existuje právě tehdy, když má uzel alespoň jeden aktivní arr_level
        assertCacheInvariant();
        // smazaný uzel (krok 3) stále nesmí být v cache
        assertNodeNotInCache(newNodes.get(2).getId());
    }

    /**
     * Asserts the row-existence invariant of {@code arr_cached_node}: no cache
     * row exists for a node whose every {@code arr_level} has
     * {@code deleteChange IS NOT NULL}.
     */
    private void assertCacheInvariant() {
        List<Integer> invalid = cachedNodeRepository.findInvalidCachedNodeIds();
        assertTrue(invalid.isEmpty(),
                "Cache invariant violated - these cached nodes have no active level: " + invalid);
    }

    private void assertNodeInCache(Integer nodeId) {
        assertNotNull(cachedNodeRepository.findByNodeId(nodeId),
                "Node " + nodeId + " should be in cache (has active level)");
    }

    private void assertNodeNotInCache(Integer nodeId) {
        assertNull(cachedNodeRepository.findByNodeId(nodeId),
                "Node " + nodeId + " should not be in cache (no active level)");
    }

    /**
     * Získání seznamu uzlů
     *
     * @param fundVersionId
     * @param rootNodeId
     * @return
     */
    private List<NodeBase> getTreeNodes(Integer fundVersionId, Integer rootNodeId) {
        return getTreeNodes(fundVersionId, rootNodeId, null);
    }

    /**
     * Získání seznamu uzlů se seznamem nasazených uzlů
     *
     * @param fundVersionId
     * @param rootNodeId
     * @param expandedIds
     * @return
     */
    private List<NodeBase> getTreeNodes(Integer fundVersionId, Integer rootNodeId, Set<Integer> expandedIds) {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersionId);
        input.setNodeId(rootNodeId);
        input.setExpandedIds(expandedIds);
        TreeData treeData = getFundTree(input);
        return convertTreeNodes(treeData.getNodes());
    }

    /**
     * Získání informací o nodu a fundu
     */
    private void nodeInfo(List<ArrNodeVO> nodes, ArrFundVersionVO fundVersionVO) {
        assertNotNull(nodes);
        assertNotNull(fundVersionVO);
        assertTrue(nodes.size() > 0);
        ArrNodeExtendVO nodeExtendVO = getNodeInfo(fundVersionVO.getId(), nodes.get(0).getId());
        assertNotNull(nodeExtendVO.getUuid());
        assertNotNull(nodeExtendVO.getName());
        assertNotNull(nodeExtendVO.getFundName());
    }

    /**
     * Uzavření verze archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     * @return nová verze archivní pomůcky
     */
    private ArrFundVersionVO approvedVersion(final ArrFundVersionVO fundVersion) {
        assertNotNull(fundVersion);
        ArrFundVersionVO newFundVersion = approveVersion(fundVersion);

        // "Musí být odlišné identifikátory"
        assertTrue(!fundVersion.getId().equals(newFundVersion.getId()));

        return newFundVersion;
    }

    private void deleteFund(final Fund fund) {
        deleteFund(fund.getId());

        this.helperTestService.getFundRepository().findAll().forEach(f -> {
            //není nalezen fond se smazaným id = je smazán
            assertFalse(f.getFundId().equals(fund.getId()));
        });
    }

    /**
     * Vytvoření AP.
     * @throws ApiException
     */
    private Fund createdFund() {
        Fund fund = createFund(NAME_AP, "IC1");
        assertNotNull(fund);

        return fund;
    }

    private void fulltextTest(final ArrFundVersionVO fundVersion) {

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        List<NodeBase> nodes = convertTreeNodes(treeData.getNodes());
        NodeBase rootNode = nodes.get(0);

        List<ArrangementController.TreeNodeFulltext> fulltext = fulltext(fundVersion, convertToArrNode(rootNode), "value", ArrangementController.Depth.SUBTREE);

        // test
        ArrangementController.TreeNodeFulltext test = new ArrangementController.TreeNodeFulltext();
        test.setNodeId(1);
        test.getNodeId();
        test.setParent(null);
        test.getParent();

        assertNotNull(fulltext);
    }

    /**
     * Bulk replace / place / delete of description-item values across many
     * nodes; also exercises the fulltext (Lucene) index update flow.
     *
     * <p><b>Creates:</b> 1 fund + nodes + desc-items of type {@code SRD_TITLE}
     * with varied text values.
     * <br><b>Cleans up:</b> nothing — fund intentionally left for the class-level cleanup (see class javadoc).
     */
    @Test
    public void replaceDataValuesTest() throws InterruptedException {

        // vytvoření
        Fund fund = createdFund();
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        // vytvoření uzlů
        helperTestService.waitForWorkers();
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        Set<Integer> nodeIds = new HashSet<>();
        for (ArrNodeVO node : nodes) {
            nodeIds.add(node.getId());
        }

        // vytvoření hodnoty
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode(SRD_TITLE);
        int index = 0;
        for (ArrNodeVO node : nodes) {
            NodeItem nodeItem = buildNodeItem(typeVo.getCode(), null, DataType.TEXT, index + "value" + index, node, null);
            descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);        	
            index++;
        }

        // update lucene ArrDescItem index
        helperTestService.waitForIndexUpdate();

        // nahrazení hodnoty value za hodnotu valXYZ
        List<ArrNodeVO> allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAllById(nodeIds));
        ArrangementController.ReplaceDataBody body = new ArrangementController.ReplaceDataBody();
        body.setNodes(new HashSet<>(allNodes));
        body.setSelectionType(ArrangementController.SelectionType.NODES);
        Response result = replaceDataValues(fundVersion.getId(), typeVo.getId(), "value", "valXYZ", body);
        Integer resultCount = result.getBody().as(Integer.class);

        assertTrue(resultCount == nodeIds.size());

        // update lucene ArrDescItem index
        helperTestService.waitForIndexUpdate();

        // nalezení hodnot podle změněné hodnoty
        RulItemType type = itemTypeRepository.findOneByCode(SRD_TITLE);
        type.setDataType(dataTypeRepository.findByCode("TEXT"));  // kvůli transakci (no session)
        List<ArrDescItem> itemsContainingText = new TransactionTemplate(tm).execute(a -> {
        	return descItemRepository.findByNodesContainingText(nodeRepository.findAllById(nodeIds), type, null, "valXYZ");
		});

        assertTrue(itemsContainingText.size() == nodeIds.size());
        for (ArrDescItem descItem : itemsContainingText) {
            ArrDataText data = HibernateUtils.unproxy(descItem.getData());
            assertTrue(Pattern.compile("^(\\d+valXYZ\\d+)$").matcher(data.getTextValue()).matches());
            assertTrue(nodeIds.contains(descItem.getNodeId()));
        }

        // test nahrazení všech hodnot na konkrétní hodnotu
        allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAllById(nodeIds));
        body.setNodes(new HashSet<>(allNodes));
        body.setSelectionType(ArrangementController.SelectionType.NODES);
        placeDataValues(fundVersion.getId(), typeVo.getId(), "nova_value", body);

        List<ArrDescItem> byNodesAndDeleteChangeIsNull = descItemService.findByNodeIdsAndDeleteChangeIsNull(nodeIds);
        assertTrue(byNodesAndDeleteChangeIsNull.size() >= nodeIds.size());
        for (ArrDescItem descItem : byNodesAndDeleteChangeIsNull) {
            if (descItem.getItemTypeId().equals(typeVo.getId())) {
            	ArrDataText text = HibernateUtils.unproxy(descItem.getData());
                assertTrue(text.getTextValue().equals("nova_value"));
            }
        }

        // smazání hodnot atributů
        helperTestService.waitForWorkers();
        allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAllById(nodeIds));
        body.setNodes(new HashSet<>(allNodes));
        body.setSelectionType(ArrangementController.SelectionType.NODES);
        deleteDescItems(fundVersion.getId(), typeVo.getId(), body);

        List<ArrDescItem> nodeDescItems = descItemService.findOpenByNodesAndType(nodeRepository.findAllById(nodeIds), type);
        assertTrue(nodeDescItems.isEmpty());
    }

    /**
     * Filter unique values of a description item; verifies filtering works
     * both before and after version approval.
     *
     * <p><b>Creates:</b> 1 fund + nodes + desc-items of type
     * {@code SRD_UNIT_DATE_TEXT} with alternating values.
     * <br><b>Cleans up:</b> nothing — fund intentionally left for the class-level cleanup (see class javadoc).
     */
	@Test
    public void filterUniqueValuesTest() throws InterruptedException {
        // vytvoření
        Fund fund = createdFund();
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        // vytvoření uzlů
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        Set<Integer> nodeIds = new HashSet<>();
        for (ArrNodeVO node : nodes) {
            nodeIds.add(node.getId());
        }

        // vytvoření hodnoty
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("SRD_UNIT_DATE_TEXT");
        int index = 1;
        String value = "value";
        for (ArrNodeVO node : nodes) {
            NodeItem nodeItem = buildNodeItem(typeVo.getCode(), null, DataType.STRING, value + index, node, null);
            descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
            index = -index;
        }

        List<UniqueValue> resultList = filterUniqueValues(fundVersion.getId(), typeVo.getId(), "ue1", null);
        assertTrue(resultList.size() < nodes.size());
        List<String> resultValues = resultList.stream().map(UniqueValue::getValue).collect(Collectors.toList());
        assertTrue(resultValues.contains("value1"));
        assertTrue(!resultValues.contains("value-1"));

        helperTestService.waitForWorkers();
        approvedVersion(fundVersion);
        resultList = filterUniqueValues(fundVersion.getId(), typeVo.getId(), "ue1", null);
        assertTrue(resultList.size() < nodes.size());
        resultValues = resultList.stream().map(UniqueValue::getValue).collect(Collectors.toList());
        assertTrue(resultValues.contains("value1"));
        assertTrue(!resultValues.contains("value-1"));
    }

    /**
     * Test method copyOlderSiblingAttribute
     * @throws ApiException
     */
    /**
     * Copy description-item attribute from an older sibling node to a newer
     * one via the {@code copySibling} endpoint.
     *
     * <p><b>Creates:</b> 1 fund ({@code fundSource}) + nodes + 1 desc-item of
     * type {@code SRD_TITLE} (plus its copy on the sibling node).
     * <br><b>Cleans up:</b> nothing — fund intentionally left for the class-level cleanup (see class javadoc).
     */
    @Test
    public void copyOlderSiblingAttribute() throws InterruptedException {
        Fund fundSource = createdFund();
        ArrFundVersionVO fundVersion = getOpenVersion(fundSource);

        List<ArrNodeVO> nodesSource = createLevels(fundVersion);
        // append one description item under first sublevel
        ArrNodeVO node1 = nodesSource.get(1);
        ArrNodeVO node2 = nodesSource.get(2);

        // vytvoření hodnoty
        RulDescItemTypeExtVO type = findDescItemTypeByCode(SRD_TITLE);
        NodeItem nodeItem = buildNodeItem(type.getCode(), null, DataType.TEXT, "value", node1, null);
        ItemDataResult itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        NodeItem nodeItemCreated = itemDataResult.getItem();

        assertNotNull(((DataText) nodeItem.getData()).getTextValue().equals(((DataText) nodeItemCreated.getData()).getTextValue()));
        assertNotNull(nodeItemCreated.getPosition());
        assertNotNull(nodeItemCreated.getItemObjectId());

        // copy value
        CopySiblingResult copyResult = copyOlderSiblingAttribute(fundVersion.getId(), type.getId(), node2);
        assertNotNull(copyResult);

        // read from server
        DescFormDataNewVO formData = getNodeFormData(node2.getId(), fundVersion.getId());
        List<ArrItemVO> items = formData.getDescItems();
        assertTrue(items.size() == 1);
        ArrItemVO result = items.get(0);
        ArrItemTextVO textVo = (ArrItemTextVO) result;
        assertTrue(textVo.getValue().equals("value"));
    }

    /**
     * Copy a node hierarchy between two separate funds via
     * {@code copyLevels}; exercises the copy-parameters and conflict paths.
     *
     * <p><b>Creates:</b> 2 funds ({@code fundSource}, {@code fundTarget}) +
     * node hierarchies in each + copied nodes.
     * <br><b>Cleans up:</b> nothing — funds intentionally left for the class-level cleanup (see class javadoc).
     */
    @Test
    public void copyLevelsTest() throws InterruptedException {
        Fund fundSource = createdFund();
        ArrFundVersionVO fundVersionSource = getOpenVersion(fundSource);
        List<ArrNodeVO> nodesSource = createLevels(fundVersionSource);

        Fund fundTarget = createdFund();
        ArrFundVersionVO fundVersionTarget = getOpenVersion(fundTarget);
        List<ArrNodeVO> nodesTarget = createLevels(fundVersionTarget);

        CopyNodesValidate copyNodesValidate = new CopyNodesValidate();

        ArrNodeVO nodeSource = nodesSource.get(0);

        copyNodesValidate.setSourceFundVersionId(fundVersionSource.getId());
        copyNodesValidate.setSourceNodes(Collections.singleton(nodeSource));
        copyNodesValidate.setTargetFundVersionId(fundVersionTarget.getId());

        copyNodesValidate.setIgnoreRootNodes(true);

        CopyNodesValidateResult validateResult = copyLevelsValidate(copyNodesValidate);
        // Validation result cannot be empty
        assertNotNull(validateResult);

        CopyNodesParams copyNodesParams = new CopyNodesParams();

        copyNodesParams.setSourceFundVersionId(fundVersionSource.getId());
        copyNodesParams.setSourceNodes(Collections.singleton(nodeSource));
        copyNodesParams.setTargetFundVersionId(fundVersionTarget.getId());
        copyNodesParams.setTargetStaticNode(nodesTarget.get(0));
        copyNodesParams.setTargetStaticNodeParent(null);
        copyNodesParams.setIgnoreRootNodes(true);
        copyNodesParams.setFilesConflictResolve(null);
        copyNodesParams.setStructuredsConflictResolve(null);
        copyNodesParams.setSelectedDirection(FundLevelService.AddLevelDirection.CHILD);

        copyLevels(copyNodesParams);
    }

    /**
     * Create / update / delete of ref-templates and their type / spec mappings
     * (fund-scoped reference templates).
     *
     * <p><b>Creates:</b> 1 fund + 1 ref-template + map types (optionally spec
     * mappings).
     * <br><b>Cleans up:</b> {@code deleteRefTemplate(...)} at end; fund intentionally left.
     */
    @Test
    public void refTemplatesTest() {
        Fund fund = createdFund();
        ArrRefTemplateVO refTemplateVO = createRefTemplate(fund.getId());

        RulItemType itemType = itemTypeRepository.findOneByCode("SOURCE_LINK");

        ArrRefTemplateEditVO refTemplateEditVO = new ArrRefTemplateEditVO();
        refTemplateEditVO.setName("Nová šablona");
        refTemplateEditVO.setItemTypeId(itemType.getItemTypeId());

        refTemplateVO = updateRefTemplate(refTemplateVO.getId(), refTemplateEditVO);
        assertEquals(refTemplateVO.getName(), "Nová šablona");

        RulItemType fromItemType = itemTypeRepository.findOneByCode("SRD_FOLDER_TYPE");
        RulItemType toItemType = itemTypeRepository.findOneByCode("SRD_LEVEL_TYPE");

        ArrRefTemplateMapTypeVO refTemplateMapTypeVO = new ArrRefTemplateMapTypeVO();
        refTemplateMapTypeVO.setFromItemTypeId(fromItemType.getItemTypeId());
        refTemplateMapTypeVO.setToItemTypeId(toItemType.getItemTypeId());
        refTemplateMapTypeVO.setMapAllSpec(true);
        refTemplateMapTypeVO.setFromParentLevel(true);

        ArrRefTemplateMapTypeVO mapTypeVO1 = createRefTemplateMapType(refTemplateVO.getId(), refTemplateMapTypeVO);

        RulItemSpec fromItemSpec = itemSpecRepository.findOneByCode("SRD_FOLDER_UNITS");
        RulItemSpec toItemSpec = itemSpecRepository.findOneByCode("SRD_LEVEL_ITEM");

        List<ArrRefTemplateMapSpecVO> refTemplateMapSpecVOList = new ArrayList<>();
        ArrRefTemplateMapSpecVO refTemplateMapSpecVO = new ArrRefTemplateMapSpecVO();
        refTemplateMapSpecVO.setFromItemSpecId(fromItemSpec.getItemSpecId());
        refTemplateMapSpecVO.setToItemSpecId(toItemSpec.getItemSpecId());
        refTemplateMapSpecVOList.add(refTemplateMapSpecVO);

        List<ArrRefTemplateVO> refTemplateVOList = getRefTemplate(fund.getId());
        ArrRefTemplateVO temp = refTemplateVOList.get(0);
        ArrRefTemplateMapTypeVO mapType = temp.getRefTemplateMapTypeVOList().get(0);
        mapType.setRefTemplateMapSpecVOList(refTemplateMapSpecVOList);

        ArrRefTemplateMapTypeVO mapTypeVO2 = updateRefTemplateMapType(temp.getId(), mapType.getId(), mapType);
        deleteRefTemplateMapType(temp.getId(), mapType.getId());

        deleteRefTemplate(refTemplateVO.getId());
    }

    /**
     * Create a description item of data-type BIT ({@code ZVEREJNENO}) and
     * verify the data-type handling and returned metadata.
     *
     * <p><b>Creates:</b> 1 fund + nodes + 1 desc-item of type
     * {@code ZVEREJNENO} on node 1.
     * <br><b>Cleans up:</b> nothing — fund intentionally left for the class-level cleanup (see class javadoc).
     */
    @Test
    public void createDescItemBit() {
        Fund fundSource = createdFund();
        ArrFundVersionVO fundVersion = getOpenVersion(fundSource);

        List<ArrNodeVO> nodesSource = createLevels(fundVersion);
        ArrNodeVO node1 = nodesSource.get(1);

        // vytvoření itemu typu bit
        RulDescItemTypeExtVO type = findDescItemTypeByCode("ZVEREJNENO");
        NodeItem nodeItem = buildNodeItem(type.getCode(), null, DataType.BIT, true, node1, null);
        ItemDataResult itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        NodeItem nodeItemCreated = itemDataResult.getItem();

        assertEquals(((DataBit) nodeItem.getData()).getBitValue(), ((DataBit) nodeItemCreated.getData()).getBitValue());
        assertNotNull(nodeItemCreated.getPosition());
        assertNotNull(nodeItemCreated.getItemObjectId());
    }

    /**
     * Create a description item with an empty enum value
     * ({@code ZP2015_ARCHDESC_LANG}); also exercises the duplicate-detection
     * error path for single-valued items.
     *
     * <p><b>Creates:</b> 1 fund + nodes + 1 desc-item of type
     * {@code ZP2015_ARCHDESC_LANG}.
     * <br><b>Cleans up:</b> nothing — fund intentionally left for the class-level cleanup (see class javadoc).
     */
    @Test
    public void createDescItemEnumEmpty() {
        Fund fundSource = createdFund();
        ArrFundVersionVO fundVersion = getOpenVersion(fundSource);

        List<ArrNodeVO> nodesSource = createLevels(fundVersion);
        ArrNodeVO node = nodesSource.get(1);

        // vytvoření itemu typu enum empty value
        RulDescItemTypeExtVO type = findDescItemTypeByCode("ZP2015_ARCHDESC_LANG");
        NodeItem nodeItem = buildNodeItem(type.getCode(), null, DataType.ENUM, null, node, true);
        ItemDataResult itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        NodeItem nodeItemCreated = itemDataResult.getItem();

        assertEquals(type.getId(), nodeItemCreated.getItemTypeId());
        assertNotNull(nodeItemCreated.getPosition());
        assertNotNull(nodeItemCreated.getItemObjectId());

        // pokus o opakované přidání by měl způsobit chybu
        node.setVersion(itemDataResult.getParent().getVersion());
        try {
            itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        } catch (HttpServerErrorException e) {
        	String body = e.getResponseBodyAsString();
        	assertTrue(body.contains(ArrangementCode.ALREADY_INDEFINABLE.name()));
            itemDataResult = null;
        }
        assertNull(itemDataResult);
    }

    /**
     * Filter nodes by description-item values; exercises both SELECT and
     * UNSELECTED filter modes.
     *
     * <p><b>Creates:</b> fund imported from {@code fund-filter-nodes.xml} via
     * {@code importXmlFile(...)} — includes 5 nodes with varied desc-items.
     * <br><b>Cleans up:</b> nothing — fund intentionally left for the class-level cleanup (see class javadoc).
     *
     * <p>Does not assume an empty DB. Captures the fundVersion count before
     * the import and asserts it grew by exactly one; then picks the
     * newly-imported fundVersion as the one with the highest id.
     */
    @Test
    public void testFilterNodes() throws InterruptedException {
        long baselineFundVersions = fundVersionRepository.count();

    	// import fund from xml
    	importXmlFile(null, 1, getResourceFile(XML_FUND));

    	List<ArrFundVersion> fundVersions = fundVersionRepository.findAll();
        assertTrue(fundVersions.size() == baselineFundVersions + 1,
                "Expected import to add exactly 1 fundVersion (baseline=" + baselineFundVersions + ", after=" + fundVersions.size() + ")");

        // prepare: newly-imported fundVersion (highest id), plus list of ids of rulDescItem and itemTypeId by code
        ArrFundVersion fundVersion = fundVersions.stream()
                .max(java.util.Comparator.comparing(ArrFundVersion::getFundVersionId))
                .orElseThrow();
        List<RulDescItemTypeExtVO> itemTypes = getDescItemTypes();
        Set<Integer> descItemTypeIds = new HashSet<>();
        Integer itemTypeId = null;
        for (RulDescItemTypeExtVO item : itemTypes) {
        	descItemTypeIds.add(item.getId());
        	if (item.getCode().equals("SRD_STORAGE_ID")) {
        		itemTypeId = item.getId();
        	}
        }
        assertTrue(descItemTypeIds.size() > 0);
        assertNotNull(itemTypeId);

        // filtering without filters -> get all nodes
        filterNodes(fundVersion.getFundVersionId(), new Filters());
        List<FilterNode> filteredNodes = getFilteredNodes(fundVersion.getFundVersionId(), 0, 10, descItemTypeIds);
        assertTrue(filteredNodes.size() == 5);

    	// create filter SELECT type
    	Filter filter = new Filter();
    	filter.setConditionType(Condition.NONE);
    	filter.setValues(Arrays.asList("beta"));
    	filter.setValuesType(ValuesTypes.SELECTED);

        Filters filters = new Filters();
    	Map<Integer, Filter> filterMap = new HashMap<>();
    	filterMap.put(itemTypeId, filter);
    	filters.setFilters(filterMap);

        // filtering with SELECTED filter -> get 1 item (beta)
        await()
            .atMost(10, SECONDS)
            .pollInterval(100, MILLISECONDS)
            .untilAsserted(() -> {
                filterNodes(fundVersion.getFundVersionId(), filters);
                List<FilterNode> nodes = getFilteredNodes(fundVersion.getFundVersionId(), 0, 10, descItemTypeIds);
                assertTrue(nodes.size() == 1, "Expected 1 filtered node, got: " + nodes.size());
            });

        filteredNodes = getFilteredNodes(fundVersion.getFundVersionId(), 0, 10, descItemTypeIds);
        assertTrue(filteredNodes.size() == 1);        
        
        // change filter to UNSELECT type
    	filter.setValues(Arrays.asList(null, "beta", "gamma"));
    	filter.setValuesType(ValuesTypes.UNSELECTED);

        // filtering with UNSELECTED filter -> get 1 item (alfa)
        filterNodes(fundVersion.getFundVersionId(), filters);
        filteredNodes = getFilteredNodes(fundVersion.getFundVersionId(), 0, 10, descItemTypeIds);
        assertTrue(filteredNodes.size() == 1);
    }
}
