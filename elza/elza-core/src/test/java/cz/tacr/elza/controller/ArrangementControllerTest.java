package cz.tacr.elza.controller;

import static cz.tacr.elza.repository.ExceptionThrow.output;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.vo.UniqueValue;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.ArrangementController.CopySiblingResult;
import cz.tacr.elza.controller.ArrangementController.DescFormDataNewVO;
import cz.tacr.elza.controller.vo.ArrFundFulltextResult;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
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
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.nodes.ArrNodeExtendVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.NodeDataParam;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemBitVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.vo.ChangesResult;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.utils.CsvUtils;

public class ArrangementControllerTest extends AbstractControllerTest {

    public static final Logger logger = LoggerFactory.getLogger(ArrangementControllerTest.class);

    public static final String STORAGE_NUMBER = "Test 123";
    public static final String STORAGE_NUMBER_FOUND = "Te";
    public static final String STORAGE_NUMBER_NOT_FOUND = "Sf";
    public static final String STORAGE_NUMBER_CHANGE = "Test 321";

    private static final String JSON_TABLE_CSV = "jsontable/jsontable.csv";

    public static final String NAME_AP = "UseCase ščřžý";
    public static final Integer LIMIT = 100;

    // maximální počet položek pro načtení
    public static final int MAX_SIZE = 999;

    @Test
    public void arrangementTest() throws IOException, InterruptedException, ApiException {

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
        attributeValues(fundVersion);

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

        //smazání fondu
        helperTestService.waitForWorkers();
        deleteFund(fund);

    }

    //TODO: odkomentovat po změně importu institucí @Test
    public void fundFulltextTest() throws InterruptedException, ApiException {

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
                assertTrue("Invalid fund [" + result.getName() + "]", names.remove(result.getName()));
                assertEquals("Invalid count [" + result.getName() + "]", count, result.getCount());
            }

            assertTrue("Fund not found [" + StringUtils.join(names, ", ") + "]", names.isEmpty());

            for (ArrFundFulltextResult result : resultList) {
                List<TreeNodeVO> nodeList = fundFulltextNodeList(result.getId());
                assertEquals("Invalid count [" + result.getName() + "]", count, nodeList.size());
                for (TreeNodeVO node : nodeList) {
                    assertEquals("Invalid node value [" + result.getName() + "]", value, node.getName());
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

    private Fund createFundFulltext(int i, int count, String value) throws InterruptedException, ApiException {

        Fund fund = createFund("Test fulltext " + i, "TST" + 1);


        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("SRD_TITLE");

        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);

        for (int j = 0; j < count; j++) {
            ArrItemVO descItem = buildDescItem(typeVo.getCode(), null, value, null, null, null);
            ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, nodes.get(j), typeVo);
        }

        return fund;
    }

    @Test
    public void revertingChangeTest() throws IOException, InterruptedException, ApiException {

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
        attributeValues(fundVersion);

        ChangesResult changesAll = findChanges(fundVersion.getId(), MAX_SIZE, 0, null, null);
        assertNotNull(changesAll);
        assertNotNull(changesAll.getChanges());
        assertTrue(changesAll.getTotalCount().equals(changesAll.getChanges().size()) && changesAll.getChanges().size() == 32);
        assertFalse(changesAll.getOutdated());

        ChangesResult changesByNode = findChanges(fundVersion.getId(), MAX_SIZE, 0, null, nodes.get(0).getId());
        assertNotNull(changesByNode);
        assertNotNull(changesByNode.getChanges());
        assertTrue(changesByNode.getTotalCount().equals(changesByNode.getChanges().size()) && changesByNode.getChanges().size() == 8);

        final Integer lastChangeId = changesAll.getChanges().get(0).getChangeId();
        final Integer firstChangeId = changesAll.getChanges().get(changesAll.getChanges().size() - 1).getChangeId();
        ChangesResult changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, OffsetDateTime.now(), lastChangeId, null);
        assertNotNull(changesByDate);
        assertNotNull(changesByDate.getChanges());

        // TODO: test
        try {
            logger.info(changesByDate.getTotalCount() + ", " + changesByDate.getChanges().size() + ", xxxxxxxxxxxxxxxxxxxx");
            Thread.sleep(5000);
            changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, OffsetDateTime.now(), lastChangeId, null);
            logger.info(changesByDate.getTotalCount() + ", " + changesByDate.getChanges().size() + ", xxxxxxxxxxxxxxxxxxxx");
            Thread.sleep(5000);
            changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, OffsetDateTime.now(), lastChangeId, null);
            logger.info(changesByDate.getTotalCount() + ", " + changesByDate.getChanges().size() + ", xxxxxxxxxxxxxxxxxxxx");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(changesByDate.getTotalCount().equals(changesByDate.getChanges().size()) && changesByDate.getChanges().size() == 32);
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
        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        List<Integer> nodeIds = nodes.stream().map(ArrNodeVO::getId).collect(Collectors.toList());

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
        Assert.assertNull(itemDeleted);

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
        Assert.assertNull(textVO.getValue());

        outputItemResult = unsetNotIdentifiedOutputItem(fundVersion.getId(), parent.getId(), parent.getVersion(), typeVo.getId(), null, textVO.getDescItemObjectId());
        parent = outputItemResult.getParent();
        // Návratová struktura nesmí být prázdná
        assertNotNull(outputItemResult);
        // Hodnota atributu musí být prázdná
        Assert.assertNull(outputItemResult.getItem());*/
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
     */
    private void forms(final ArrFundVersionVO fundVersion) {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);


        ArrangementController.FaTreeNodesParam inputFa = new ArrangementController.FaTreeNodesParam();
        inputFa.setVersionId(fundVersion.getId());
        inputFa.setNodeIds(Arrays.asList(rootNode.getId()));
        List<TreeNodeVO> faTreeNodes = getFundTreeNodes(inputFa);
        assertTrue(CollectionUtils.isNotEmpty(faTreeNodes));

        NodeDataParam ndp = new NodeDataParam();
        ndp.setNodeId(rootNode.getId());
        ndp.setFundVersionId(fundVersion.getId());
        ndp.setParents(true);

        Collection<TreeNodeVO> nodeParents = getNodeData(ndp).getParents();
        assertNotNull(nodeParents);

        ArrangementController.DescFormDataNewVO nodeFormData = getNodeFormData(rootNode.getId(),
                fundVersion.getId());
        assertNotNull(nodeFormData.getParent());

        ArrangementController.NodeFormsDataVO nodeFormsData = getNodeFormsData(fundVersion.getId(), rootNode.getId());
        assertTrue(nodeFormsData.getForms().size() > 0);

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
     * Operace s atributy.
     *
     * @param fundVersion verze archivní pomůcky
     */
    private void attributeValues(final ArrFundVersionVO fundVersion) throws IOException, InterruptedException {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);

        // vytvoření hodnoty
        helperTestService.waitForWorkers();
        RulDescItemTypeExtVO type = findDescItemTypeByCode("SRD_SCALE");
        ArrItemVO descItem = buildDescItem(type.getCode(), null, "value", null, null, null);
        ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, rootNode,
                type);
        rootNode = descItemResult.getParent();
        ArrItemVO descItemCreated = descItemResult.getItem();

        assertNotNull(((ArrItemTextVO) descItem).getValue()
                .equals(((ArrItemTextVO) descItemCreated).getValue()));
        assertNotNull(descItemCreated.getPosition());
        assertNotNull(descItemCreated.getDescItemObjectId());

        // aktualizace hodnoty
        helperTestService.waitForWorkers();
        ((ArrItemTextVO) descItemCreated).setValue("update value");
        descItemResult = updateDescItem(descItemCreated, fundVersion, rootNode, true);
        rootNode = descItemResult.getParent();
        ArrItemVO descItemUpdated = descItemResult.getItem();

        assertTrue(descItemUpdated.getDescItemObjectId().equals(descItemCreated.getDescItemObjectId()));
        assertTrue(descItemUpdated.getPosition().equals(descItemCreated.getPosition()));
        assertTrue(!descItemUpdated.getId().equals(descItemCreated.getId()));
        assertTrue(((ArrItemTextVO) descItemUpdated).getValue()
                .equals(((ArrItemTextVO) descItemCreated).getValue()));

        // odstranění hodnoty
        helperTestService.waitForWorkers();
        descItemResult = deleteDescItem(descItemUpdated, fundVersion, rootNode);
        rootNode = descItemResult.getParent();

        helperTestService.waitForWorkers();
        // nastavené nemožné hodnoty
        descItemResult = setNotIdentifiedDescItem(fundVersion.getId(), rootNode.getId(), rootNode.getVersion(), type.getId(), null, null);
        rootNode = descItemResult.getParent();

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
        Assert.assertNull(item.getValue());

        helperTestService.waitForWorkers();
        descItemResult = unsetNotIdentifiedDescItem(fundVersion.getId(), rootNode.getId(), rootNode.getVersion(), type.getId(), null, item.getDescItemObjectId());
        rootNode = descItemResult.getParent();

        // Návratová struktura nesmí být prázdná
        assertNotNull(descItemResult);
        // Hodnota atributu musí být prázdná
        Assert.assertNull(descItemResult.getItem());

        helperTestService.waitForWorkers();
        // vytvoření další hodnoty
        type = findDescItemTypeByCode("SRD_SCALE");
        descItem = buildDescItem(type.getCode(), null, "value", null, null, null);
        descItemResult = createDescItem(descItem, fundVersion, rootNode, type);
        rootNode = descItemResult.getParent();
        descItemCreated = descItemResult.getItem();

        // fulltext

        fulltextTest(fundVersion);

        helperTestService.waitForWorkers();
        descItemResult = deleteDescItemsByType(fundVersion.getId(),
                rootNode.getId(), rootNode.getVersion(), type.getId());
        rootNode = descItemResult.getParent();

        ArrNodeVO node = nodes.get(1);

        ArrangementController.DescriptionItemParam param = new ArrangementController.DescriptionItemParam();
        param.setVersionId(fundVersion.getId());
        param.setNode(node);
        param.setDirection(DirectionLevel.ROOT);
        getDescriptionItemTypesForNewLevel(false, param);

        // vytvoření další hodnoty - vícenásobné
        helperTestService.waitForWorkers();
        type = findDescItemTypeByCode("SRD_OTHER_ID");
        RulDescItemSpecExtVO spec = findDescItemSpecByCode("SRD_OTHERID_CJ", type);
        descItem = buildDescItem(type.getCode(), spec.getCode(), "1", 1, null, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        descItem = buildDescItem(type.getCode(), spec.getCode(), "2", 1, null, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        descItem = buildDescItem(type.getCode(), spec.getCode(), "3", 1, null, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();
        descItemCreated = descItemResult.getItem();

        ((ArrItemStringVO) descItemCreated).setValue("3x");
        descItemCreated.setPosition(5);
        descItemResult = updateDescItem(descItemCreated, fundVersion, node, true);
        node = descItemResult.getParent();

        ArrangementController.CopySiblingResult copySiblingResult =
                copyOlderSiblingAttribute(fundVersion.getId(), type.getId(), nodes.get(2));

        type = findDescItemTypeByCode("SRD_UNIT_DATE");
        descItem = buildDescItem(type.getCode(), null, "1920", 1, null, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        LocalDate dateNow = LocalDate.now();
        type = findDescItemTypeByCode("SRD_SIMPLE_DATE");
        descItem = buildDescItem(type.getCode(), null, dateNow, 1, null, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("SRD_LEGEND");
        descItem = buildDescItem(type.getCode(), null, "legenda", 1, null, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        helperTestService.waitForWorkers();
        type = findDescItemTypeByCode("SRD_POSITION"); //TODO : co to je
        descItem = buildDescItem(type.getCode(), null, "POINT (14 49)", 1, null, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        helperTestService.waitForWorkers();
        type = findDescItemTypeByCode("SRD_COLL_EXTENT_LENGTH");
        descItem = buildDescItem(type.getCode(), null, BigDecimal.valueOf(20.5), 1, null, null);
        Thread.sleep(1000);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("SRD_UNIT_COUNT_TABLE");
        assertNotNull(type);
        ElzaTable table = new ElzaTable();
        table.addRow(new ElzaRow(new AbstractMap.SimpleEntry<>("NAME", "Test 1"), new AbstractMap.SimpleEntry<>("COUNT", "195")));
        table.addRow(new ElzaRow(new AbstractMap.SimpleEntry<>("NAME", "Test 2"), new AbstractMap.SimpleEntry<>("COUNT", "200")));

        descItem = buildDescItem(type.getCode(), null, table, 1, null, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

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

        // 1. přesun druhého uzlu před první
        helperTestService.waitForWorkers();
        moveLevelBefore(fundVersion, nodes.get(1), rootNode, Arrays.asList(nodes.get(2)), rootNode);

        List<ArrNodeVO> newNodes = getTreeNodes(fundVersion.getId(), rootNode.getId());

        // kontrola přesunu
        assertTrue(newNodes.size() == 4);
        assertTrue(newNodes.get(0).getId().equals(nodes.get(2).getId()));
        assertTrue(newNodes.get(1).getId().equals(nodes.get(1).getId()));
        assertTrue(newNodes.get(2).getId().equals(nodes.get(3).getId()));
        assertTrue(newNodes.get(3).getId().equals(nodes.get(4).getId()));

        helperTestService.waitForWorkers();
        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // 2. přesun druhého uzlu pod první
        helperTestService.waitForWorkers();
        moveLevelUnder(fundVersion, newNodes.get(0), rootNode, Arrays.asList(newNodes.get(1)), rootNode);

        List<ArrNodeVO> newNodes2 = getTreeNodes(fundVersion.getId(), newNodes.get(0).getId());

        // kontrola přesunu
        assertTrue(newNodes2.size() == 1);
        assertTrue(newNodes2.get(0).getId().equals(newNodes.get(1).getId()));

        helperTestService.waitForWorkers();
        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // 3. smazání druhého uzlu v první úrovni
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent nodesWithParent = deleteLevel(fundVersion, newNodes.get(2), rootNode);

        assertTrue(nodesWithParent.getNode().getId().equals(newNodes.get(2).getId()));
        assertTrue(nodesWithParent.getParentNode().getId().equals(rootNode.getId()));

        List<ArrNodeVO> newNodes3 = getTreeNodes(fundVersion.getId(), rootNode.getId());

        // kontrola smazání
        assertTrue(newNodes3.size() == 2);
        assertTrue(newNodes3.get(0).getId().equals(newNodes.get(0).getId()));
        assertTrue(newNodes3.get(1).getId().equals(newNodes.get(3).getId()));

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

        List<ArrNodeVO> newNodes4 = getTreeNodes(fundVersion.getId(), newNodes.get(0).getId());

        // kontrola přidání
        assertTrue(newNodes4.size() == 2);
        assertTrue(newNodes4.get(0).getId().equals(newNodes.get(1).getId()));
        assertTrue(newNodes4.get(1).getId().equals(newLevel4.getNode().getId()));

        // 5. přidání uzlu na konec seznamu
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent newLevel5 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                                                                  fundVersion, rootNode, rootNode, null);

        helperTestService.waitForWorkers();
        parentNode = newLevel5.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // 6. přidání uzlu  na konec seznamu
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent newLevel6 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                                                                  fundVersion, rootNode, rootNode, null);

        helperTestService.waitForWorkers();
        parentNode = newLevel6.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        List<ArrNodeVO> newNodes56 = getTreeNodes(fundVersion.getId(), rootNode.getId());

        // kontrola přidání
        assertTrue(newNodes56.size() == 4);
        assertTrue(newNodes56.get(0).getId().equals(newNodes.get(0).getId()));
        assertTrue(newNodes56.get(1).getId().equals(newNodes.get(3).getId()));
        assertTrue(newNodes56.get(2).getId().equals(newLevel5.getNode().getId()));
        assertTrue(newNodes56.get(3).getId().equals(newLevel6.getNode().getId()));

        // 7. přesun posledního za první
        helperTestService.waitForWorkers();
        moveLevelAfter(fundVersion, newNodes56.get(3), rootNode, Arrays.asList(newNodes56.get(2)), rootNode);

        List<ArrNodeVO> newNodes7 = getTreeNodes(fundVersion.getId(), rootNode.getId());

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
        moveLevelUnder(fundVersion, newNodes4.get(0), newNodes7.get(0), Arrays.asList(newNodes7.get(1), newNodes4.get(1), newNodes7.get(2)), rootNode);

        // přenesené záznamy
        List<ArrNodeVO> moveNodes = getTreeNodes(fundVersion.getId(), newNodes4.get(0).getId());

        // kontrola přenášených záznamů
        assertTrue(moveNodes.size() == 3);
        assertTrue(moveNodes.get(0).getId().equals(newNodes7.get(1).getId()));
        assertTrue(moveNodes.get(1).getId().equals(newNodes4.get(1).getId()));
        assertTrue(moveNodes.get(2).getId().equals(newNodes7.get(2).getId()));

        // výsledek všech akcí od root
        List<ArrNodeVO> resultNodes = getTreeNodes(fundVersion.getId(), rootNode.getId(), Collections.singleton((newNodes.get(0).getId())));

        // kontrola výsledku
        assertTrue(resultNodes.size() == 3);
        assertTrue(resultNodes.get(0).getId().equals(newNodes.get(0).getId()));
        assertTrue(resultNodes.get(1).getId().equals(newNodes4.get(0).getId()));
        assertTrue(resultNodes.get(2).getId().equals(newNodes7.get(3).getId()));
    }

    /**
     * Získání seznamu uzlů
     *
     * @param fundVersionId
     * @param rootNodeId
     * @return
     */
    private List<ArrNodeVO> getTreeNodes(Integer fundVersionId, Integer rootNodeId) {
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
    private List<ArrNodeVO> getTreeNodes(Integer fundVersionId, Integer rootNodeId, Set<Integer> expandedIds) {
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
     * Vytvoření levelů v archivní pomůcce.
     * <p>
     * Create 4 levels under root
     *
     * @param fundVersion verze archivní pomůcky
     * @return vytvořené levely
     */
    private List<ArrNodeVO> createLevels(final ArrFundVersionVO fundVersion) {

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        TreeNodeVO parentNode;

        // Musí existovat root node
        assertNotNull(treeData.getNodes());
        // Musí existovat pouze root node
        assertTrue(treeData.getNodes().size() == 1);

        TreeNodeVO rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        // přidání prvního levelu pod root
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent newLevel1 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                fundVersion, rootNode, rootNode, "Série");

        // rodič nového uzlu musí být root
        assertTrue(newLevel1.getParentNode().getId().equals(rootNode.getId()));
        // verze root uzlu musí být povýšena
        assertTrue(!newLevel1.getParentNode().getVersion().equals(rootNode.getVersion()));

        helperTestService.waitForWorkers();
        parentNode = newLevel1.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání druhého levelu pod root
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent newLevel2 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                fundVersion, rootNode, rootNode, null);

        // rodič nového uzlu musí být root
        assertTrue(newLevel2.getParentNode().getId().equals(rootNode.getId()));
        // verze root uzlu musí být povýšena
        assertTrue(!newLevel2.getParentNode().getVersion().equals(rootNode.getVersion()));

        helperTestService.waitForWorkers();
        parentNode = newLevel2.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání třetího levelu na první pozici pod root
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent newLevel3 = addLevel(FundLevelService.AddLevelDirection.BEFORE,
                fundVersion, newLevel1.getNode(), rootNode, null);

        // rodič nového uzlu musí být root
        assertTrue(newLevel3.getParentNode().getId().equals(rootNode.getId()));
        // verze root uzlu musí být povýšena
        assertTrue(!newLevel3.getParentNode().getVersion().equals(rootNode.getVersion()));

        helperTestService.waitForWorkers();
        parentNode = newLevel3.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání uzlu za první uzel pod root (za child3)
        helperTestService.waitForWorkers();
        ArrangementController.NodeWithParent newLevel4 = addLevel(FundLevelService.AddLevelDirection.AFTER,
                fundVersion, newLevel3.getNode(), rootNode, null);

        // rodič nového uzlu musí být root
        assertTrue(newLevel4.getParentNode().getId().equals(rootNode.getId()));
        // verze root uzlu musí být povýšena
        assertTrue(!newLevel4.getParentNode().getVersion().equals(rootNode.getVersion()));

        helperTestService.waitForWorkers();
        parentNode = newLevel4.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(rootNode.getId());
        treeData = getFundTree(input);

        // Kontrola pořadí uzlů
        Iterator<TreeNodeVO> nodeClientIterator = treeData.getNodes().iterator();
        TreeNodeVO node1 = nodeClientIterator.next();
        TreeNodeVO node2 = nodeClientIterator.next();
        TreeNodeVO node3 = nodeClientIterator.next();
        TreeNodeVO node4 = nodeClientIterator.next();
        assertTrue(node1.getId().equals(newLevel3.getNode().getId()));
        assertTrue(node2.getId().equals(newLevel4.getNode().getId()));
        assertTrue(node3.getId().equals(newLevel1.getNode().getId()));
        assertTrue(node4.getId().equals(newLevel2.getNode().getId()));

        List<ArrNodeVO> nodes = new ArrayList<>(treeData.getNodes().size() + 1);
        nodes.add(rootNode);
        nodes.add(newLevel3.getNode());
        nodes.add(newLevel4.getNode());
        nodes.add(newLevel1.getNode());
        nodes.add(newLevel2.getNode());
        return nodes;
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
    private Fund createdFund() throws ApiException {
        Fund fund = createFund(NAME_AP, "IC1");
        assertNotNull(fund);

        return fund;
    }

    private void fulltextTest(final ArrFundVersionVO fundVersion) {

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);

        List<ArrangementController.TreeNodeFulltext> fulltext = fulltext(fundVersion, rootNode, "value",
                ArrangementController.Depth.SUBTREE);

        // test
        ArrangementController.TreeNodeFulltext test = new ArrangementController.TreeNodeFulltext();
        test.setNodeId(1);
        test.getNodeId();
        test.setParent(null);
        test.getParent();

        assertNotNull(fulltext);
    }

    @Test
    public void replaceDataValuesTest() throws InterruptedException, ApiException {

        // vytvoření
        ArrFundVersionVO fundVersion = getOpenVersion(createdFund());

        // vytvoření uzlů
        helperTestService.waitForWorkers();
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        Set<Integer> nodeIds = new HashSet<>();
        for (ArrNodeVO node : nodes) {
            nodeIds.add(node.getId());
        }

        // vytvoření hodnoty
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("SRD_TITLE");
        int index = 0;
        for (ArrNodeVO node : nodes) {
            ArrItemVO descItem = buildDescItem(typeVo.getCode(), null, index + "value" + index, null, null, null);
            ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, node, typeVo);
            index++;
        }

        //nahrazení hodnoty value za hodnotu valXYZ
        List<ArrNodeVO> allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAllById(nodeIds));
        ArrangementController.ReplaceDataBody body = new ArrangementController.ReplaceDataBody();
        body.setNodes(new HashSet<>(allNodes));
        body.setSelectionType(ArrangementController.SelectionType.NODES);
        Thread.sleep(1000);
        helperTestService.waitForWorkers();
        replaceDataValues(fundVersion.getId(), typeVo.getId(), "value", "valXYZ", body);


        //nalezení hodnot podle změněné hodnoty
        RulItemType type = itemTypeRepository.findOneByCode("SRD_TITLE");
        type.setDataType(dataTypeRepository.findByCode("TEXT"));  //kvůli transakci (no session)
        List<ArrDescItem> nodesContainingText = descItemRepository.findByNodesContainingText(nodeRepository.findAllById(nodeIds),
                type, null, "valXYZ");

        assertTrue(nodesContainingText.size() == nodeIds.size());
        for (ArrDescItem descItem : nodesContainingText) {
            ArrDataText data = HibernateUtils.unproxy(descItem.getData());
            assertTrue(Pattern.compile("^(\\d+valXYZ\\d+)$").matcher(data.getTextValue()).matches());
            assertTrue(nodeIds.contains(descItem.getNodeId()));
        }

        //test nahrazení všech hodnot na konkrétní hodnotu
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

        //smazání hodnot atributů
        helperTestService.waitForWorkers();
        allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAllById(nodeIds));
        body.setNodes(new HashSet<>(allNodes));
        body.setSelectionType(ArrangementController.SelectionType.NODES);
        deleteDescItems(fundVersion.getId(), typeVo.getId(), body);

        List<ArrDescItem> nodeDescItems = descItemService.findOpenByNodesAndType(nodeRepository.findAllById(nodeIds), type);
        assertTrue(nodeDescItems.isEmpty());
    }

    @Test
    public void filterUniqueValuesTest() throws InterruptedException, ApiException {
        // vytvoření
        ArrFundVersionVO fundVersion = getOpenVersion(createdFund());

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
            ArrItemVO descItem = buildDescItem(typeVo.getCode(), null, value + index, null, null, null);
            ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, node,
                    typeVo);
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
    @Test
    public void copyOlderSiblingAttribute() throws InterruptedException, ApiException {
        Fund fundSource = createdFund();
        ArrFundVersionVO fundVersion = getOpenVersion(fundSource);

        List<ArrNodeVO> nodesSource = createLevels(fundVersion);
        // append one description item under first sublevel
        ArrNodeVO node1 = nodesSource.get(1);
        ArrNodeVO node2 = nodesSource.get(2);

        // vytvoření hodnoty
        RulDescItemTypeExtVO type = findDescItemTypeByCode("SRD_TITLE");
        ArrItemVO descItem = buildDescItem(type.getCode(), null, "value", null, null, null);
        ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, node1,
                type);
        ArrItemVO descItemCreated = descItemResult.getItem();

        assertNotNull(((ArrItemTextVO) descItem).getValue()
                .equals(((ArrItemTextVO) descItemCreated).getValue()));
        assertNotNull(descItemCreated.getPosition());
        assertNotNull(descItemCreated.getDescItemObjectId());

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

    @Test
    public void copyLevelsTest() throws InterruptedException, ApiException {
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

    @Test
    public void refTemplatesTest() throws ApiException {
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

    @Test
    public void createDescItemBit() throws ApiException {
        Fund fundSource = createdFund();
        ArrFundVersionVO fundVersion = getOpenVersion(fundSource);

        List<ArrNodeVO> nodesSource = createLevels(fundVersion);
        ArrNodeVO node1 = nodesSource.get(1);

        // vytvoření itemu typu bit
        RulDescItemTypeExtVO type = findDescItemTypeByCode("ZVEREJNENO");
        ArrItemVO descItem = buildDescItem(type.getCode(), null, true, null, null, null);
        ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, node1, type);
        ArrItemVO descItemCreated = descItemResult.getItem();

        assertEquals(((ArrItemBitVO) descItem).isValue(), ((ArrItemBitVO) descItemCreated).isValue());
        assertNotNull(descItemCreated.getPosition());
        assertNotNull(descItemCreated.getDescItemObjectId());
    }

    @Test
    public void createDescItemEnumEmpty() throws ApiException {
        Fund fundSource = createdFund();
        ArrFundVersionVO fundVersion = getOpenVersion(fundSource);

        List<ArrNodeVO> nodesSource = createLevels(fundVersion);
        ArrNodeVO node = nodesSource.get(1);

        // vytvoření itemu typu enum empty value
        RulDescItemTypeExtVO type = findDescItemTypeByCode("ZP2015_ARCHDESC_LANG");
        ArrItemVO descItem = buildDescItem(type.getCode(), null, null, null, null, true);
        ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, node, type);
        ArrItemVO descItemCreated = descItemResult.getItem();

        assertEquals(type.getId(), descItemCreated.getItemTypeId());
        assertNotNull(descItemCreated.getPosition());
        assertNotNull(descItemCreated.getDescItemObjectId());

//        // pokus o opakované přidání by měl způsobit chybu
//        node.setVersion(descItemResult.getParent().getVersion());
//        try {
//            descItemResult = createDescItem(descItem, fundVersion, node, type);
//        } catch (BusinessException e) {
//            descItemResult = null;
//            assertEquals(ArrangementCode.ALREADY_INDEFINABLE, e.getErrorCode());
//        }
//        assertNull(descItemResult);
    }
}
