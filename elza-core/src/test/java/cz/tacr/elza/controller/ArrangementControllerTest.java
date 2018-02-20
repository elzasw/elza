package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.ArrangementController.CopySiblingResult;
import cz.tacr.elza.controller.ArrangementController.DescFormDataNewVO;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrOutputDefinitionVO;
import cz.tacr.elza.controller.vo.ArrOutputExtVO;
import cz.tacr.elza.controller.vo.CopyNodesParams;
import cz.tacr.elza.controller.vo.CopyNodesValidate;
import cz.tacr.elza.controller.vo.CopyNodesValidateResult;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.controller.vo.FilterNodePosition;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.OutputSettingsVO;
import cz.tacr.elza.controller.vo.ApRecordVO;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.RulOutputTypeVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeDescItemsLiteVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemGroupVO;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.service.ArrIOService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.vo.ChangesResult;

public class ArrangementControllerTest extends AbstractControllerTest {

    public static final Logger logger = LoggerFactory.getLogger(ArrangementControllerTest.class);

    public static final String STORAGE_NUMBER = "Test 123";
    public static final String STORAGE_NUMBER_FOUND = "Te";
    public static final String STORAGE_NUMBER_NOT_FOUND = "Sf";
    public static final String STORAGE_NUMBER_CHANGE = "Test 321";

    private static final String JSON_TABLE_CSV = "jsontable/jsontable.csv";

    public static final String NAME_AP = "UseCase ščřžý";
    public static final String RENAME_AP = "Renamed UseCase";
    public static final Integer LIMIT = 100;

    // maximální počet položek pro načtení
    public static final int MAX_SIZE = 999;

    @Test
    public void arrangementTest() throws IOException {

        // vytvoření
        ArrFundVO fund = createdFund();

        // přejmenování
        fund = updatedFund(fund);

        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        // uzavření verze
        fundVersion = approvedVersion(fundVersion);

        // vytvoření uzlů
        List<ArrNodeVO> nodes = createLevels(fundVersion);

        // přesunutí && smazání uzlů
        moveAndDeleteLevels(nodes, fundVersion);

        // atributy
        attributeValues(fundVersion);

        // validace
        validations(fundVersion, nodes);

        // všechny formuláře / stromy / ...
        forms(fundVersion);

        // akce nad výstupy
        outputs(fundVersion);

        // filtry
        filters(fundVersion);

        //smazání fondu
        deleteFund(fund);

    }

    @Test
    public void revertingChangeTest() throws IOException {

        ArrFundVO fund = createdFund();

        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        // vytvoření uzlů
        List<ArrNodeVO> nodes = createLevels(fundVersion);

        // přesunutí && smazání uzlů
        moveAndDeleteLevels(nodes, fundVersion);

        // atributy
        attributeValues(fundVersion);

        ChangesResult changesAll = findChanges(fundVersion.getId(), MAX_SIZE, 0, null, null);
        assertNotNull(changesAll);
        assertNotNull(changesAll.getChanges());
        assertTrue(changesAll.getTotalCount().equals(changesAll.getChanges().size()) && changesAll.getChanges().size() == 28);
        assertTrue(!changesAll.getOutdated());

        ChangesResult changesByNode = findChanges(fundVersion.getId(), MAX_SIZE, 0, null, nodes.get(0).getId());
        assertNotNull(changesByNode);
        assertNotNull(changesByNode.getChanges());
        assertTrue(changesByNode.getTotalCount().equals(changesByNode.getChanges().size()) && changesByNode.getChanges().size() == 8);

        final Integer lastChangeId = changesAll.getChanges().get(0).getChangeId();
        final Integer firstChangeId = changesAll.getChanges().get(changesAll.getChanges().size() - 1).getChangeId();
        ChangesResult changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, LocalDateTime.now(), lastChangeId, null);
        assertNotNull(changesByDate);
        assertNotNull(changesByDate.getChanges());

        // TODO: test
        try {
            logger.info(changesByDate.getTotalCount() + ", " + changesByDate.getChanges().size() + ", xxxxxxxxxxxxxxxxxxxx");
            Thread.sleep(5000);
            changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, LocalDateTime.now(), lastChangeId, null);
            logger.info(changesByDate.getTotalCount() + ", " + changesByDate.getChanges().size() + ", xxxxxxxxxxxxxxxxxxxx");
            Thread.sleep(5000);
            changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, LocalDateTime.now(), lastChangeId, null);
            logger.info(changesByDate.getTotalCount() + ", " + changesByDate.getChanges().size() + ", xxxxxxxxxxxxxxxxxxxx");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(changesByDate.getTotalCount().equals(changesByDate.getChanges().size()) && changesByDate.getChanges().size() == 28);
        assertTrue(!changesByDate.getOutdated());

        // obdoba revertChanges s fail očekáváním
        httpMethod(spec -> spec.pathParam("fundVersionId", fundVersion.getId())
                        .queryParameter("fromChangeId", lastChangeId)
                        .queryParameter("toChangeId", firstChangeId),
                REVERT_CHANGES, HttpMethod.GET, HttpStatus.INTERNAL_SERVER_ERROR);

        final Integer secondChangeId = changesAll.getChanges().get(changesAll.getChanges().size() - 2).getChangeId();
        revertChanges(fundVersion.getId(), lastChangeId, secondChangeId, null);

    }

    /**
     * Testování validací.
     *
     * @param fundVersion verze archivní pomůcky
     * @param nodes
     */
    private void validations(final ArrFundVersionVO fundVersion, final List<ArrNodeVO> nodes) {
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
        List<FilterNodePosition> filteredFulltextNodes = getFilteredFulltextNodes(fundVersion.getId(), "1", false);
        assertTrue(CollectionUtils.isNotEmpty(filteredFulltextNodes));
    }

    /**
     * Testování práci s výstupy.
     *
     * @param fundVersion verze archivní pomůcky
     */
    private void outputs(final ArrFundVersionVO fundVersion) {
        List<ArrOutputExtVO> outputs = getOutputs(fundVersion.getId());
        assertTrue(outputs.size() == 0);

        List<RulOutputTypeVO> outputTypes = getOutputTypes(fundVersion.getId());
        assertTrue(CollectionUtils.isNotEmpty(outputTypes));

        ArrOutputDefinitionVO outputDefinition = createNamedOutput(fundVersion, "Test", "TST", false, outputTypes.iterator().next().getId());
        assertNotNull(outputDefinition);

        outputs = getOutputs(fundVersion.getId());
        assertTrue(outputs.size() == 1);
        ArrOutputExtVO output = outputs.get(0);

        ArrOutputExtVO outputDetail = getOutput(fundVersion.getId(), output.getId());

        assertNotNull(outputDetail);
        assertTrue(outputDetail.getId().equals(output.getId()));

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        List<Integer> nodeIds = nodes.stream().map(ArrNodeVO::getId).collect(Collectors.toList());

        addNodesNamedOutput(fundVersion.getId(), outputDetail.getId(), nodeIds);

        outputDetail = getOutput(fundVersion.getId(), output.getId());
        assertTrue(outputDetail.getOutputDefinition().getNodes().size() == nodeIds.size());

        removeNodesNamedOutput(fundVersion.getId(), outputDetail.getId(), nodeIds);

        outputDetail = getOutput(fundVersion.getId(), output.getId());
        assertTrue(outputDetail.getOutputDefinition().getNodes().size() == 0);

        updateNamedOutput(fundVersion, outputDetail, "Test 2", "TST2");
        outputDetail = getOutput(fundVersion.getId(), output.getId());
        assertTrue(outputDetail.getOutputDefinition().getName().equals("Test 2"));
        assertTrue(outputDetail.getOutputDefinition().getInternalCode().equals("TST2"));

        outputLock(fundVersion.getId(), outputDetail.getId());
        outputDetail = getOutput(fundVersion.getId(), output.getId());
        assertTrue(outputDetail.getLockDate() != null);


        outputs = getOutputs(fundVersion.getId());
        outputDefinition = outputs.get(0).getOutputDefinition();

        ArrItemStringVO item = new ArrItemStringVO();
        item.setValue("test1");
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("SRD_SCALE");
        ArrangementController.OutputItemResult outputItem = createOutputItem(item, fundVersion.getId(), typeVo.getId(), outputDefinition.getId(), outputDefinition.getVersion());
        ArrItemVO itemCreated = outputItem.getItem();
        assertNotNull(itemCreated);
        assertNotNull(itemCreated.getDescItemObjectId());
        assertNotNull(itemCreated.getPosition());
        assertTrue(itemCreated instanceof ArrItemStringVO);
        assertTrue(((ArrItemStringVO) itemCreated).getValue().equals(item.getValue()));

        ((ArrItemStringVO) itemCreated).setValue("xxx");
        outputItem = updateOutputItem(itemCreated, fundVersion.getId(), outputItem.getParent().getVersion(), true);

        ArrItemVO itemUpdated = outputItem.getItem();
        assertNotNull(itemUpdated);
        assertNotNull(itemUpdated.getDescItemObjectId());
        assertNotNull(itemUpdated.getPosition());
        assertTrue(itemUpdated instanceof ArrItemStringVO);
        assertTrue(((ArrItemStringVO) itemUpdated).getValue().equals(((ArrItemStringVO) itemCreated).getValue()));

        ArrangementController.OutputFormDataNewVO outputFormData = getOutputFormData(outputItem.getParent().getId(), fundVersion.getId());

        assertNotNull(outputFormData.getParent());
        assertTrue(outputFormData.getGroups().size() == 1);

        outputItem = deleteOutputItem(itemCreated, fundVersion.getId(), outputItem.getParent().getVersion());
        ArrOutputDefinitionVO parent = outputItem.getParent();

        ArrItemVO itemDeleted = outputItem.getItem();
        Assert.assertNull(itemDeleted);

        item = new ArrItemStringVO();
        item.setValue("test1");
        outputItem = createOutputItem(item, fundVersion.getId(), typeVo.getId(), outputDefinition.getId(), parent.getVersion());
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


        super.setOutputSettings(outputDetail.getOutputDefinition().getId(), outputSettings);
        ArrOutputDefinition one = this.helperTestService.getOutputDefinitionRepository()
                .findOne(outputDetail.getOutputDefinition().getId());

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
        deleteNamedOutput(fundVersion.getId(), output.getId());

        outputs = getOutputs(fundVersion.getId());
        assertTrue(outputs.size() == 0);
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
        List<TreeNodeClient> faTreeNodes = getFundTreeNodes(inputFa);
        assertTrue(CollectionUtils.isNotEmpty(faTreeNodes));

        List<TreeNodeClient> nodeParents = getNodeParents(rootNode.getId(), fundVersion.getId());
        assertNotNull(nodeParents);

        ArrangementController.DescFormDataNewVO nodeFormData = getNodeFormData(rootNode.getId(),
                fundVersion.getId());
        assertNotNull(nodeFormData.getParent());
        assertTrue(CollectionUtils.isNotEmpty(nodeFormData.getGroups()));
        assertTrue(CollectionUtils.isNotEmpty(nodeFormData.getTypeGroups()));

        ArrangementController.NodeFormsDataVO nodeFormsData = getNodeFormsData(fundVersion.getId(), rootNode.getId());
        assertTrue(nodeFormsData.getForms().size()>0);

        nodeFormsData = getNodeWithAroundFormsData(fundVersion.getId(), nodes.get(1).getId(), 5);
        assertTrue(nodeFormsData.getForms().size()>0);

        ArrangementController.IdsParam idsParamNodes = new ArrangementController.IdsParam();
        idsParamNodes.setVersionId(fundVersion.getId());
        idsParamNodes.setIds(Arrays.asList(nodes.get(1).getId()));
        List<TreeNodeClient> treeNodeClients = getNodes(idsParamNodes);
        assertTrue(treeNodeClients.size()>0);

        ArrangementController.IdsParam idsParamFa = new ArrangementController.IdsParam();
        idsParamFa.setIds(Arrays.asList(fundVersion.getId()));

        List<ArrFundVO> fundsByVersionIds = getFundsByVersionIds(idsParamFa);
        assertTrue(fundsByVersionIds.size()>0);
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
    private void attributeValues(final ArrFundVersionVO fundVersion) throws IOException {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);

        // vytvoření hodnoty
        RulDescItemTypeExtVO type = findDescItemTypeByCode("SRD_SCALE");
        ArrItemVO descItem = buildDescItem(type.getCode(), null, "value", null, null);
        ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, rootNode,
                type);
        rootNode = descItemResult.getParent();
        ArrItemVO descItemCreated = descItemResult.getItem();

        assertNotNull(((ArrItemTextVO) descItem).getValue()
                .equals(((ArrItemTextVO) descItemCreated).getValue()));
        assertNotNull(descItemCreated.getPosition());
        assertNotNull(descItemCreated.getDescItemObjectId());

        // aktualizace hodnoty
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
        descItemResult = deleteDescItem(descItemUpdated, fundVersion, rootNode);
        rootNode = descItemResult.getParent();

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

        descItemResult = unsetNotIdentifiedDescItem(fundVersion.getId(), rootNode.getId(), rootNode.getVersion(), type.getId(), null, item.getDescItemObjectId());
        rootNode = descItemResult.getParent();

        // Návratová struktura nesmí být prázdná
        assertNotNull(descItemResult);
        // Hodnota atributu musí být prázdná
        Assert.assertNull(descItemResult.getItem());

        // vytvoření další hodnoty
        type = findDescItemTypeByCode("SRD_SCALE");
        descItem = buildDescItem(type.getCode(), null, "value", null, null);
        descItemResult = createDescItem(descItem, fundVersion, rootNode, type);
        rootNode = descItemResult.getParent();
        descItemCreated = descItemResult.getItem();

        // fulltext
        fulltextTest(fundVersion);

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
        type = findDescItemTypeByCode("SRD_OTHER_ID");
        RulDescItemSpecExtVO spec = findDescItemSpecByCode("SRD_OTHERID_CJ", type);
        descItem = buildDescItem(type.getCode(), spec.getCode(), "1", 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        descItem = buildDescItem(type.getCode(), spec.getCode(), "2", 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        descItem = buildDescItem(type.getCode(), spec.getCode(), "3", 1, null);
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
        descItem = buildDescItem(type.getCode(), null, "1920", 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("SRD_LEGEND");
        descItem = buildDescItem(type.getCode(), null, "legenda", 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("SRD_POSITION");
        descItem = buildDescItem(type.getCode(), null, "POINT (14 49)", 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("SRD_COLL_EXTENT_LENGTH");
        descItem = buildDescItem(type.getCode(), null, BigDecimal.valueOf(20.5), 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("SRD_UNIT_COUNT_TABLE");
        assertNotNull(type);
        ElzaTable table = new ElzaTable();
        table.addRow(new ElzaRow(new AbstractMap.SimpleEntry<>("NAME", "Test 1"), new AbstractMap.SimpleEntry<>("COUNT", "195")));
        table.addRow(new ElzaRow(new AbstractMap.SimpleEntry<>("NAME", "Test 2"), new AbstractMap.SimpleEntry<>("COUNT", "200")));

        descItem = buildDescItem(type.getCode(), null, table, 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        // Import a export CSV pro atribut JSON_TABLE
        {
            // Import
            type = findDescItemTypeByCode("SRD_UNIT_COUNT_TABLE");
            descItemResult = descItemCsvImport(fundVersion.getId(), node.getVersion(), node.getId(), type.getId(), getFile(JSON_TABLE_CSV));

            // Export a kontrola
            InputStream is = descItemCsvExport(fundVersion.getId(), descItemResult.getItem().getDescItemObjectId());
            Reader in = new InputStreamReader(is, ArrIOService.CSV_EXCEL_ENCODING);
            Iterable<CSVRecord> records = ArrIOService.CSV_EXCEL_FORMAT.withFirstRecordAsHeader().parse(in);
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
     * @param nodes             založené uzly (1. je root)
     * @param fundVersion verze archivní pomůcky
     */
    private void moveAndDeleteLevels(final List<ArrNodeVO> nodes,
                                     final ArrFundVersionVO fundVersion) {
        ArrNodeVO rootNode = nodes.get(0);
        TreeNodeClient parentNode;
        // přesun druhého uzlu před první
        moveLevelBefore(fundVersion, nodes.get(1), rootNode, Arrays.asList(nodes.get(2)), rootNode);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(rootNode.getId());
        TreeData treeData = getFundTree(input);
        List<ArrNodeVO> newNodes = convertTreeNodes(treeData.getNodes());

        // kontrola přesunu
        assertTrue(newNodes.size() == 4);
        assertTrue(newNodes.get(0).getId().equals(nodes.get(2).getId()));
        assertTrue(newNodes.get(1).getId().equals(nodes.get(1).getId()));
        assertTrue(newNodes.get(2).getId().equals(nodes.get(3).getId()));
        assertTrue(newNodes.get(3).getId().equals(nodes.get(4).getId()));

        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // přesun druhého uzlu pod první
        moveLevelUnder(fundVersion, newNodes.get(0), rootNode, Arrays.asList(newNodes.get(1)), rootNode);

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(newNodes.get(0).getId());
        treeData = getFundTree(input);
        List<ArrNodeVO> newNodes2 = convertTreeNodes(treeData.getNodes());

        // kontrola přesunu
        assertTrue(newNodes2.size() == 1);
        assertTrue(newNodes2.get(0).getId().equals(newNodes.get(1).getId()));

        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // smazání druhého uzlu v první úrovni
        ArrangementController.NodeWithParent nodeWithParent = deleteLevel(fundVersion, newNodes.get(2), rootNode);

        assertTrue(nodeWithParent.getNode().getId().equals(newNodes.get(2).getId()));
        assertTrue(nodeWithParent.getParentNode().getId().equals(rootNode.getId()));

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(rootNode.getId());
        treeData = getFundTree(input);
        List<ArrNodeVO> newNodes3 = convertTreeNodes(treeData.getNodes());

        assertTrue(newNodes3.size() == 2);
        assertTrue(newNodes3.get(0).getId().equals(newNodes.get(0).getId()));
        assertTrue(newNodes3.get(1).getId().equals(newNodes.get(3).getId()));

        rootNode.setVersion(rootNode.getVersion() + 1);

        // přidání třetího levelu na první pozici pod root
        ArrangementController.NodeWithParent newLevel5 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                fundVersion, rootNode, rootNode, null);

        parentNode = newLevel5.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(rootNode.getId());
        treeData = getFundTree(input);
        List<ArrNodeVO> newNodes4 = convertTreeNodes(treeData.getNodes());

        assertTrue(newNodes4.size() == 3);

        // přesun posledního za první
        moveLevelAfter(fundVersion, newNodes4.get(2), rootNode, Arrays.asList(newNodes4.get(0)), rootNode);
    }

    /**
     * Vytvoření levelů v archivní pomůcce.
     *
     * Create 4 levels under root
     *
     * @param fundVersion verze archivní pomůcky
     * @return vytvořené levely
     */
    private List<ArrNodeVO> createLevels(final ArrFundVersionVO fundVersion) {

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        TreeNodeClient parentNode;

        // Musí existovat root node
        assertNotNull(treeData.getNodes());
        // Musí existovat pouze root node
        assertTrue(treeData.getNodes().size() == 1);

        TreeNodeClient rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        // přidání prvního levelu pod root
        ArrangementController.NodeWithParent newLevel1 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                fundVersion, rootNode, rootNode, "Série");

        // Rodič nového uzlu musí být root
        assertTrue(newLevel1.getParentNode().getId().equals(rootNode.getId()));
        // Verze root uzlu musí být povýšena
        assertTrue(!newLevel1.getParentNode().getVersion().equals(rootNode.getVersion()));

        parentNode = newLevel1.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání druhého levelu pod root
        ArrangementController.NodeWithParent newLevel2 = addLevel(FundLevelService.AddLevelDirection.CHILD,
                fundVersion, rootNode, rootNode, null);

        // Rodič nového uzlu musí být root
        assertTrue(newLevel2.getParentNode().getId().equals(rootNode.getId()));
        // Verze root uzlu musí být povýšena
        assertTrue(!newLevel2.getParentNode().getVersion().equals(rootNode.getVersion()));

        parentNode = newLevel2.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání třetího levelu na první pozici pod root
        ArrangementController.NodeWithParent newLevel3 = addLevel(FundLevelService.AddLevelDirection.BEFORE,
                fundVersion, newLevel1.getNode(), rootNode, null);

        // "Rodič nového uzlu musí být root"
        assertTrue(newLevel3.getParentNode().getId().equals(rootNode.getId()));
        // "Verze root uzlu musí být povýšena"
        assertTrue(!newLevel3.getParentNode().getVersion().equals(rootNode.getVersion()));

        parentNode = newLevel3.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání uzlu za první uzel pod root (za child3)
        ArrangementController.NodeWithParent newLevel4 = addLevel(FundLevelService.AddLevelDirection.AFTER,
                fundVersion, newLevel3.getNode(), rootNode, null);

        // "Rodič nového uzlu musí být root"
        assertTrue(newLevel4.getParentNode().getId().equals(rootNode.getId()));
        // "Verze root uzlu musí být povýšena"
        assertTrue(!newLevel4.getParentNode().getVersion().equals(rootNode.getVersion()));

        parentNode = newLevel4.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(rootNode.getId());
        treeData = getFundTree(input);

        // Kontrola pořadí uzlů
        Iterator<TreeNodeClient> nodeClientIterator = treeData.getNodes().iterator();
        TreeNodeClient node1 = nodeClientIterator.next();
        TreeNodeClient node2 = nodeClientIterator.next();
        TreeNodeClient node3 = nodeClientIterator.next();
        TreeNodeClient node4 = nodeClientIterator.next();
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
        ArrFundVersionVO newFundVersion = approveVersion(fundVersion, fundVersion.getDateRange());

        // "Musí být odlišné identifikátory"
        assertTrue(!fundVersion.getId().equals(newFundVersion.getId()));

        return newFundVersion;
    }

    /**
     * Upravení AP.
     *
     * @param fund archivní pomůcka
     */
    private ArrFundVO updatedFund(final ArrFundVO fund) {
        fund.setName(RENAME_AP);
        Integer ruleSetId = null;
        for (ArrFundVersionVO versionVO : fund.getVersions()) {
            if (versionVO.getLockDate() == null) {
                ruleSetId = versionVO.getRuleSetId();
            }
        }

        ArrFundVO updatedFund = fundAid(fund, ruleSetId);
        // "Jméno AP musí být stejné"
        assertTrue(RENAME_AP.equals(updatedFund.getName()));
        return updatedFund;
    }

    private void deleteFund(final ArrFundVO fund){
        deleteFund(fund.getId());

        this.helperTestService.getFundRepository().findAll().forEach(f -> {
            //není nalezen fond se smazaným id = je smazán
            assertTrue(!f.getFundId().equals(fund.getId()));
        });
    }

    /**
     * Vytvoření AP.
     */
    private ArrFundVO createdFund() {
        ArrFundVO fund = createFund(NAME_AP, "IC1");
        assertNotNull(fund);

        fund = getFund(fund.getId());
        assertNotNull(fund);

        return fund;
    }

    @Test
    public void calendarsTest() {
        List<ArrCalendarTypeVO> calendarTypes = getCalendarTypes();
        assertTrue(calendarTypes.size()>0);
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
    public void registerLinksTest() {

        ArrFundVO fund = createFund("RegisterLinks Test AP", "IC3");

        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);

        List<ApTypeVO> types = getRecordTypes();
        List<ApScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();

        ApRecordVO record = new ApRecordVO();

        record.setApTypeId(getNonHierarchicalApType(types, false).getId());

        record.setCharacteristics("Ja jsem apRecordA");

        record.setRecord("ApRecordA");

        record.setScopeId(scopeId);

        record.setAddRecord(true);

        record = createRecord(record);

        ArrNodeRegisterVO nodeRegister = new ArrNodeRegisterVO();

        nodeRegister.setValue(record.getId());
        nodeRegister.setNodeId(rootNode.getId());
        nodeRegister.setNode(rootNode);

        ArrNodeRegisterVO createdLink = createRegisterLinks(fundVersion.getId(), rootNode.getId(), nodeRegister);

        assertNotNull(createdLink);

        List<ArrNodeRegisterVO> registerLinks = findRegisterLinks(fundVersion.getId(), rootNode.getId());
        assertTrue(registerLinks.size()>0);

        ArrangementController.NodeRegisterDataVO registerLinksForm = findRegisterLinksForm(fundVersion.getId(),
                rootNode.getId());

        assertNotNull(registerLinksForm.getNode());
        assertTrue(registerLinksForm.getNodeRegisters().size()>0);

        ArrNodeRegisterVO updatedLink = updateRegisterLinks(fundVersion.getId(), rootNode.getId(), createdLink);

        assertTrue(!createdLink.getId().equals(updatedLink.getId()));

        ArrNodeRegisterVO deletedLink = deleteRegisterLinks(fundVersion.getId(), rootNode.getId(), updatedLink);

        assertTrue(updatedLink.getId().equals(deletedLink.getId()));
    }

    private ApTypeVO getNonHierarchicalApType(final List<ApTypeVO> list, final boolean hasPartyType) {
        for (ApTypeVO type : list) {
            if (type.getAddRecord() && ((!hasPartyType && type.getPartyTypeId() == null) || (hasPartyType && type.getPartyTypeId() != null))) {
                return type;
            }
        }

        for (ApTypeVO type : list) {
            if (type.getChildren() != null) {
                ApTypeVO res = getNonHierarchicalApType(type.getChildren(), hasPartyType);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    @Test
    public void replaceDataValuesTest() {

        // vytvoření
        ArrFundVersionVO fundVersion = getOpenVersion(createdFund());

        // vytvoření uzlů
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        Set<Integer> nodeIds = new HashSet<>();
        for (ArrNodeVO node : nodes) {
            nodeIds.add(node.getId());
        }

        // vytvoření hodnoty
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("SRD_TITLE");
        int index = 0;
        for (ArrNodeVO node : nodes) {
            ArrItemVO descItem = buildDescItem(typeVo.getCode(), null, index + "value" + index, null, null);
            ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, node,
                    typeVo);
            index++;
        }

        //nahrazení hodnoty value za hodnotu valXYZ
        List<ArrNodeVO> allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAll(nodeIds));
        ArrangementController.ReplaceDataBody body = new ArrangementController.ReplaceDataBody();
        body.setNodes(new HashSet<>(allNodes));
        body.setSelectionType(ArrangementController.SelectionType.NODES);
        replaceDataValues(fundVersion.getId(), typeVo.getId(), "value", "valXYZ", body);


        //nalezení hodnot podle změněné hodnoty
        RulItemType type = itemTypeRepository.findOneByCode("SRD_TITLE");
        type.setDataType(dataTypeRepository.findByCode("TEXT"));  //kvůli transakci (no session)
        List<ArrDescItem> nodesContainingText = descItemRepository.findByNodesContainingText(nodeRepository.findAll(nodeIds),
                type, null, "valXYZ");

        assertTrue(nodesContainingText.size() == nodeIds.size());
        for (ArrDescItem descItem : nodesContainingText) {
            ArrDataText data = (ArrDataText) descItem.getData();
            assertTrue(Pattern.compile("^(\\d+valXYZ\\d+)$").matcher(data.getValue()).matches());
            assertTrue(nodeIds.contains(descItem.getNodeId()));
        }


        //test nahrazení všech hodnot na konkrétní hodnotu
        allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAll(nodeIds));
        body.setNodes(new HashSet<>(allNodes));
        body.setSelectionType(ArrangementController.SelectionType.NODES);
        placeDataValues(fundVersion.getId(), typeVo.getId(), "nova_value", body);

        List<ArrDescItem> byNodesAndDeleteChangeIsNull = descItemRepository
		        .findByNodeIdsAndDeleteChangeIsNull(nodeIds);
        assertTrue(byNodesAndDeleteChangeIsNull.size() >= nodeIds.size());
        for (ArrDescItem descItem : byNodesAndDeleteChangeIsNull) {
			if (descItem.getItemTypeId().equals(typeVo.getId())) {
                ArrDataText text = (ArrDataText) descItem.getData();
                assertTrue(text.getValue().equals("nova_value"));
            }
        }

        //smazání hodnot atributů
        allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAll(nodeIds));
        body.setNodes(new HashSet<>(allNodes));
        body.setSelectionType(ArrangementController.SelectionType.NODES);
        deleteDescItems(fundVersion.getId(), typeVo.getId(), body);

        List<ArrDescItem> nodeDescItems = descItemRepository
                .findOpenByNodesAndType(nodeRepository.findAll(nodeIds), type);
        assertTrue(nodeDescItems.isEmpty());
    }

    @Test
    public void filterUniqueValuesTest(){
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
            ArrItemVO descItem = buildDescItem(typeVo.getCode(), null, value + index, null, null);
            ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, node,
                    typeVo);
            index = -index;
        }


        List<String> resultList = filterUniqueValues(fundVersion.getId(), typeVo.getId(), "ue1", null);
        assertTrue(resultList.size() < nodes.size());
        assertTrue(resultList.contains("value1"));
        assertTrue(!resultList.contains("value-1"));



        approvedVersion(fundVersion);
        resultList = filterUniqueValues(fundVersion.getId(), typeVo.getId(), "ue1", null);
        assertTrue(resultList.size() < nodes.size());
        assertTrue(resultList.contains("value1"));
        assertTrue(!resultList.contains("value-1"));



    }

    /**
     * Test method copyOlderSiblingAttribute
     */
    @Test
    public void copyOlderSiblingAttribute() {
    	ArrFundVO fundSource = createdFund();
    	ArrFundVersionVO fundVersion = getOpenVersion(fundSource);
    	List<ArrNodeVO> nodesSource = createLevels(fundVersion);
    	// append one description item under first sublevel
    	ArrNodeVO node1 = nodesSource.get(1);
    	ArrNodeVO node2 = nodesSource.get(2);

        // vytvoření hodnoty
        RulDescItemTypeExtVO type = findDescItemTypeByCode("SRD_TITLE");
        ArrItemVO descItem = buildDescItem(type.getCode(), null, "value", null, null);
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
        List<ArrItemVO> items = new ArrayList<>();
        DescFormDataNewVO formData = getNodeFormData(node2.getId(), fundVersion.getId());
    	List<ItemGroupVO> groups = formData.getGroups();
    	for(ItemGroupVO group: groups) {
    		List<ItemTypeDescItemsLiteVO> voItems = group.getTypes();
    		for(ItemTypeDescItemsLiteVO voItem: voItems) {
    			if(voItem.getId().equals(type.getId())) {
    				items.addAll(voItem.getDescItems());
    			}
    		}
    	}
    	assertTrue(items.size()==1);
    	ArrItemVO result = items.get(0);
    	ArrItemTextVO textVo = (ArrItemTextVO)result;
    	assertTrue(textVo.getValue().equals("value"));

    }

    @Test
    public void copyLevelsTest() {

        ArrFundVO fundSource = createdFund();
        ArrFundVersionVO fundVersionSource = getOpenVersion(fundSource);
        List<ArrNodeVO> nodesSource = createLevels(fundVersionSource);

        ArrFundVO fundTarget = createdFund();
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

}
