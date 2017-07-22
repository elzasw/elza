package cz.tacr.elza.controller;

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

import com.google.common.collect.Lists;
import cz.tacr.elza.controller.vo.CopyNodesParams;
import cz.tacr.elza.controller.vo.CopyNodesValidate;
import cz.tacr.elza.controller.vo.CopyNodesValidateResult;
import cz.tacr.elza.service.vo.ChangesResult;
import org.apache.commons.csv.CSVRecord;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrOutputDefinitionVO;
import cz.tacr.elza.controller.vo.ArrOutputExtVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.controller.vo.FilterNodePosition;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RulOutputTypeVO;
import cz.tacr.elza.controller.vo.RulPacketTypeVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.service.ArrIOService;
import cz.tacr.elza.service.ArrMoveLevelService;


/**
 * @author Martin Šlapa
 * @since 16.2.2016
 */
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
        Assert.notNull(changesAll);
        Assert.notNull(changesAll.getChanges());
        Assert.isTrue(changesAll.getTotalCount().equals(changesAll.getChanges().size()) && changesAll.getChanges().size() == 28);
        Assert.isTrue(!changesAll.getOutdated());

        ChangesResult changesByNode = findChanges(fundVersion.getId(), MAX_SIZE, 0, null, nodes.get(0).getId());
        Assert.notNull(changesByNode);
        Assert.notNull(changesByNode.getChanges());
        Assert.isTrue(changesByNode.getTotalCount().equals(changesByNode.getChanges().size()) && changesByNode.getChanges().size() == 8);

        final Integer lastChangeId = changesAll.getChanges().get(0).getChangeId();
        final Integer firstChangeId = changesAll.getChanges().get(changesAll.getChanges().size() - 1).getChangeId();
        ChangesResult changesByDate = findChangesByDate(fundVersion.getId(), MAX_SIZE, LocalDateTime.now(), lastChangeId, null);
        Assert.notNull(changesByDate);
        Assert.notNull(changesByDate.getChanges());

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

        Assert.isTrue(changesByDate.getTotalCount().equals(changesByDate.getChanges().size()) && changesByDate.getChanges().size() == 28);
        Assert.isTrue(!changesByDate.getOutdated());

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
        Assert.notNull(validation);
        Assert.notNull(validation.getCount());
        Assert.notEmpty(validation.getItems());

        ArrangementController.ValidationItems validationError = findValidationError(fundVersion.getId(), nodes.get(0).getId(), 1);
        Assert.notNull(validationError);
        Assert.notEmpty(validationError.getItems());

        List<NodeItemWithParent> visiblePolicy = getAllNodesVisiblePolicy(fundVersion.getId());
        Assert.notNull(visiblePolicy); // TODO: přepsat na notEmpty
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
        Assert.notEmpty(filteredNodes);
        List<FilterNodePosition> filteredFulltextNodes = getFilteredFulltextNodes(fundVersion.getId(), "1", false);
        Assert.notEmpty(filteredFulltextNodes);
    }

    /**
     * Testování práci s výstupy.
     *
     * @param fundVersion verze archivní pomůcky
     */
    private void outputs(final ArrFundVersionVO fundVersion) {
        List<ArrOutputExtVO> outputs = getOutputs(fundVersion.getId());
        Assert.isTrue(outputs.size() == 0);

        List<RulOutputTypeVO> outputTypes = getOutputTypes(fundVersion.getId());
        Assert.notEmpty(outputTypes);

        ArrOutputDefinitionVO outputDefinition = createNamedOutput(fundVersion, "Test", "TST", false, outputTypes.iterator().next().getId());
        Assert.notNull(outputDefinition);

        outputs = getOutputs(fundVersion.getId());
        Assert.isTrue(outputs.size() == 1);
        ArrOutputExtVO output = outputs.get(0);

        ArrOutputExtVO outputDetail = getOutput(fundVersion.getId(), output.getId());

        Assert.notNull(outputDetail);
        Assert.isTrue(outputDetail.getId().equals(output.getId()));

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        List<Integer> nodeIds = nodes.stream().map(ArrNodeVO::getId).collect(Collectors.toList());

        addNodesNamedOutput(fundVersion.getId(), outputDetail.getId(), nodeIds);

        outputDetail = getOutput(fundVersion.getId(), output.getId());
        Assert.isTrue(outputDetail.getOutputDefinition().getNodes().size() == nodeIds.size());

        removeNodesNamedOutput(fundVersion.getId(), outputDetail.getId(), nodeIds);

        outputDetail = getOutput(fundVersion.getId(), output.getId());
        Assert.isTrue(outputDetail.getOutputDefinition().getNodes().size() == 0);

        updateNamedOutput(fundVersion, outputDetail, "Test 2", "TST2");
        outputDetail = getOutput(fundVersion.getId(), output.getId());
        Assert.isTrue(outputDetail.getOutputDefinition().getName().equals("Test 2"));
        Assert.isTrue(outputDetail.getOutputDefinition().getInternalCode().equals("TST2"));

        outputLock(fundVersion.getId(), outputDetail.getId());
        outputDetail = getOutput(fundVersion.getId(), output.getId());
        Assert.isTrue(outputDetail.getLockDate() != null);


        outputs = getOutputs(fundVersion.getId());
        outputDefinition = outputs.get(0).getOutputDefinition();

        ArrItemStringVO item = new ArrItemStringVO();
        item.setValue("test1");
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("ZP2015_TITLE");
        ArrangementController.OutputItemResult outputItem = createOutputItem(item, fundVersion.getId(), typeVo.getId(), outputDefinition.getId(), outputDefinition.getVersion());
        ArrItemVO itemCreated = outputItem.getItem();
        Assert.notNull(itemCreated);
        Assert.notNull(itemCreated.getDescItemObjectId());
        Assert.notNull(itemCreated.getPosition());
        Assert.isTrue(itemCreated instanceof ArrItemStringVO);
        Assert.isTrue(((ArrItemStringVO) itemCreated).getValue().equals(item.getValue()));

        ((ArrItemStringVO) itemCreated).setValue("xxx");
        outputItem = updateOutputItem(itemCreated, fundVersion.getId(), outputItem.getParent().getVersion(), true);

        ArrItemVO itemUpdated = outputItem.getItem();
        Assert.notNull(itemUpdated);
        Assert.notNull(itemUpdated.getDescItemObjectId());
        Assert.notNull(itemUpdated.getPosition());
        Assert.isTrue(itemUpdated instanceof ArrItemStringVO);
        Assert.isTrue(((ArrItemStringVO) itemUpdated).getValue().equals(((ArrItemStringVO) itemCreated).getValue()));

        ArrangementController.OutputFormDataNewVO outputFormData = getOutputFormData(outputItem.getParent().getId(), fundVersion.getId());

        Assert.notNull(outputFormData.getParent());
        Assert.isTrue(outputFormData.getGroups().size() == 1);

        outputItem = deleteOutputItem(itemCreated, fundVersion.getId(), outputItem.getParent().getVersion());
        ArrOutputDefinitionVO parent = outputItem.getParent();

        ArrItemVO itemDeleted = outputItem.getItem();
        Assert.isNull(itemDeleted);

        item = new ArrItemStringVO();
        item.setValue("test1");
        outputItem = createOutputItem(item, fundVersion.getId(), typeVo.getId(), outputDefinition.getId(), parent.getVersion());
        parent = outputItem.getParent();
        itemCreated = outputItem.getItem();

        ArrangementController.OutputItemResult outputItemResult = deleteOutputItemsByType(fundVersion.getId(), parent.getId(), parent.getVersion(), typeVo.getId());
        parent = outputItemResult.getParent();

        outputItemResult = setNotIdentifiedOutputItem(fundVersion.getId(), parent.getId(), parent.getVersion(), typeVo.getId(), null, null);
        parent = outputItemResult.getParent();
        Assert.notNull(outputItemResult, "Návratová struktura nesmí být prázdná");
        Assert.notNull(outputItemResult.getItem(), "Hodnota atributu nesmí být prázdná");
        ArrItemTextVO textVO = (ArrItemTextVO) outputItemResult.getItem();
        Assert.isTrue(textVO.getUndefined(), "Hodnota Nezjištěno musí být true");
        Assert.notNull(textVO.getDescItemObjectId(), "Identifikátor nesmí být prázdný");
        Assert.isNull(textVO.getValue(), "Hodnota musí být prázdná");

        outputItemResult = unsetNotIdentifiedOutputItem(fundVersion.getId(), parent.getId(), parent.getVersion(), typeVo.getId(), null, textVO.getDescItemObjectId());
        parent = outputItemResult.getParent();
        Assert.notNull(outputItemResult, "Návratová struktura nesmí být prázdná");
        Assert.isNull(outputItemResult.getItem(), "Hodnota atributu musí být prázdná");

        deleteNamedOutput(fundVersion.getId(), output.getId());

        outputs = getOutputs(fundVersion.getId());
        Assert.isTrue(outputs.size() == 0);
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
        Assert.notEmpty(faTreeNodes);

        List<TreeNodeClient> nodeParents = getNodeParents(rootNode.getId(), fundVersion.getId());
        Assert.notNull(nodeParents);

        ArrangementController.DescFormDataNewVO nodeFormData = getNodeFormData(rootNode.getId(),
                fundVersion.getId());
        Assert.notNull(nodeFormData.getParent());
        Assert.notEmpty(nodeFormData.getGroups());
        Assert.notEmpty(nodeFormData.getTypeGroups());

        ArrangementController.NodeFormsDataVO nodeFormsData = getNodeFormsData(fundVersion.getId(), rootNode.getId());
        Assert.notEmpty(nodeFormsData.getForms());

        nodeFormsData = getNodeWithAroundFormsData(fundVersion.getId(), nodes.get(1).getId(), 5);
        Assert.notEmpty(nodeFormsData.getForms());

        ArrangementController.IdsParam idsParamNodes = new ArrangementController.IdsParam();
        idsParamNodes.setVersionId(fundVersion.getId());
        idsParamNodes.setIds(Arrays.asList(nodes.get(1).getId()));
        List<TreeNodeClient> treeNodeClients = getNodes(idsParamNodes);
        Assert.notEmpty(treeNodeClients);

        ArrangementController.IdsParam idsParamFa = new ArrangementController.IdsParam();
        idsParamFa.setIds(Arrays.asList(fundVersion.getId()));

        List<ArrFundVO> fundsByVersionIds = getFundsByVersionIds(idsParamFa);
        Assert.notEmpty(fundsByVersionIds);
    }

    /**
     * Zavolání metod pro zjištění validací.
     *
     * @param fundVersion verze archivní pomůcky
     */
    protected void validateVersion(final ArrFundVersionVO fundVersion) {
        List<ArrangementController.VersionValidationItem> items = validateVersion(fundVersion.getId());
        Assert.notNull(items);
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
        RulDescItemTypeExtVO type = findDescItemTypeByCode("ZP2015_TITLE");
        ArrItemVO descItem = buildDescItem(type.getCode(), null, "value", null, null);
        ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, rootNode,
                type);
        rootNode = descItemResult.getParent();
        ArrItemVO descItemCreated = descItemResult.getItem();

        Assert.notNull(((ArrItemTextVO) descItem).getValue()
                .equals(((ArrItemTextVO) descItemCreated).getValue()));
        Assert.notNull(descItemCreated.getPosition());
        Assert.notNull(descItemCreated.getDescItemObjectId());

        // aktualizace hodnoty
        ((ArrItemTextVO) descItemCreated).setValue("update value");
        descItemResult = updateDescItem(descItemCreated, fundVersion, rootNode, true);
        rootNode = descItemResult.getParent();
        ArrItemVO descItemUpdated = descItemResult.getItem();

        Assert.isTrue(descItemUpdated.getDescItemObjectId().equals(descItemCreated.getDescItemObjectId()));
        Assert.isTrue(descItemUpdated.getPosition().equals(descItemCreated.getPosition()));
        Assert.isTrue(!descItemUpdated.getId().equals(descItemCreated.getId()));
        Assert.isTrue(((ArrItemTextVO) descItemUpdated).getValue()
                .equals(((ArrItemTextVO) descItemCreated).getValue()));

        // odstranění hodnoty
        descItemResult = deleteDescItem(descItemUpdated, fundVersion, rootNode);
        rootNode = descItemResult.getParent();

        // nastavené nemožné hodnoty
        descItemResult = setNotIdentifiedDescItem(fundVersion.getId(), rootNode.getId(), rootNode.getVersion(), type.getId(), null, null);
        rootNode = descItemResult.getParent();

        Assert.notNull(descItemResult, "Návratová struktura nesmí být prázdná");
        Assert.notNull(descItemResult.getItem(), "Hodnota atributu nesmí být prázdná");
        ArrItemTextVO item = (ArrItemTextVO) descItemResult.getItem();
        Assert.isTrue(item.getUndefined(), "Hodnota Nezjištěno musí být true");
        Assert.notNull(item.getDescItemObjectId(), "Identifikátor nesmí být prázdný");
        Assert.isNull(item.getValue(), "Hodnota musí být prázdná");

        descItemResult = unsetNotIdentifiedDescItem(fundVersion.getId(), rootNode.getId(), rootNode.getVersion(), type.getId(), null, item.getDescItemObjectId());
        rootNode = descItemResult.getParent();

        Assert.notNull(descItemResult, "Návratová struktura nesmí být prázdná");
        Assert.isNull(descItemResult.getItem(), "Hodnota atributu musí být prázdná");

        // vytvoření další hodnoty
        type = findDescItemTypeByCode("ZP2015_TITLE");
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
        type = findDescItemTypeByCode("ZP2015_OTHER_ID");
        RulDescItemSpecExtVO spec = findDescItemSpecByCode("ZP2015_OTHERID_CJ", type);
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

        type = findDescItemTypeByCode("ZP2015_UNIT_DATE");
        descItem = buildDescItem(type.getCode(), null, "1920", 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("ZP2015_LEGEND");
        descItem = buildDescItem(type.getCode(), null, "legenda", 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("ZP2015_POSITION");
        descItem = buildDescItem(type.getCode(), null, "POINT (14 49)", 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("ZP2015_COLL_EXTENT_LENGTH");
        descItem = buildDescItem(type.getCode(), null, BigDecimal.valueOf(20.5), 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        type = findDescItemTypeByCode("ZP2015_STATISTICS");
        ElzaTable table = new ElzaTable();
        table.addRow(new ElzaRow(new AbstractMap.SimpleEntry<>("KEY", "Test 1"), new AbstractMap.SimpleEntry<>("VALUE", "195")));
        table.addRow(new ElzaRow(new AbstractMap.SimpleEntry<>("KEY", "Test 2"), new AbstractMap.SimpleEntry<>("VALUE", "200")));

        descItem = buildDescItem(type.getCode(), null, table, 1, null);
        descItemResult = createDescItem(descItem, fundVersion, node, type);
        node = descItemResult.getParent();

        // Import a export CSV pro atribut JSON_TABLE
        {
            // Import
            type = findDescItemTypeByCode("ZP2015_STATISTICS");
            descItemResult = descItemCsvImport(fundVersion.getId(), node.getVersion(), node.getId(), type.getId(), getFile(JSON_TABLE_CSV));

            // Export a kontrola
            InputStream is = descItemCsvExport(fundVersion.getId(), descItemResult.getItem().getDescItemObjectId());
            Reader in = new InputStreamReader(is, ArrIOService.CSV_EXCEL_ENCODING);
            Iterable<CSVRecord> records = ArrIOService.CSV_EXCEL_FORMAT.withFirstRecordAsHeader().parse(in);
            List<CSVRecord> recordsList = new ArrayList<>();
            records.forEach(recordsList::add);
            Assert.isTrue(recordsList.size() == 6); // šest řádků bez hlavičky

            Assert.isTrue(recordsList.get(0).get("KEY").equals("klic1"));
            Assert.isTrue(recordsList.get(0).get("VALUE").equals("1"));

            Assert.isTrue(recordsList.get(1).get("KEY").equals("klic2"));
            Assert.isTrue(recordsList.get(1).get("VALUE").equals("2"));

            Assert.isTrue(recordsList.get(2).get("KEY").equals("klic3"));
            Assert.isTrue(recordsList.get(2).get("VALUE").equals(""));

            Assert.isTrue(recordsList.get(3).get("KEY").equals(""));
            Assert.isTrue(recordsList.get(3).get("VALUE").equals("4"));

            Assert.isTrue(recordsList.get(4).get("KEY").equals(""));
            Assert.isTrue(recordsList.get(4).get("VALUE").equals(""));

            Assert.isTrue(recordsList.get(5).get("KEY").equals("kk"));
            Assert.isTrue(recordsList.get(5).get("VALUE").equals("11"));
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
        Assert.isTrue(newNodes.size() == 4);
        Assert.isTrue(newNodes.get(0).getId().equals(nodes.get(2).getId()));
        Assert.isTrue(newNodes.get(1).getId().equals(nodes.get(1).getId()));
        Assert.isTrue(newNodes.get(2).getId().equals(nodes.get(3).getId()));
        Assert.isTrue(newNodes.get(3).getId().equals(nodes.get(4).getId()));

        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // přesun druhého uzlu pod první
        moveLevelUnder(fundVersion, newNodes.get(0), rootNode, Arrays.asList(newNodes.get(1)), rootNode);

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(newNodes.get(0).getId());
        treeData = getFundTree(input);
        List<ArrNodeVO> newNodes2 = convertTreeNodes(treeData.getNodes());

        // kontrola přesunu
        Assert.isTrue(newNodes2.size() == 1);
        Assert.isTrue(newNodes2.get(0).getId().equals(newNodes.get(1).getId()));

        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // smazání druhého uzlu v první úrovni
        ArrangementController.NodeWithParent nodeWithParent = deleteLevel(fundVersion, newNodes.get(2), rootNode);

        Assert.isTrue(nodeWithParent.getNode().getId().equals(newNodes.get(2).getId()));
        Assert.isTrue(nodeWithParent.getParentNode().getId().equals(rootNode.getId()));

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(rootNode.getId());
        treeData = getFundTree(input);
        List<ArrNodeVO> newNodes3 = convertTreeNodes(treeData.getNodes());

        Assert.isTrue(newNodes3.size() == 2);
        Assert.isTrue(newNodes3.get(0).getId().equals(newNodes.get(0).getId()));
        Assert.isTrue(newNodes3.get(1).getId().equals(newNodes.get(3).getId()));

        rootNode.setVersion(rootNode.getVersion() + 1);

        // přidání třetího levelu na první pozici pod root
        ArrangementController.NodeWithParent newLevel5 = addLevel(ArrMoveLevelService.AddLevelDirection.CHILD,
                fundVersion, rootNode, rootNode, null);

        parentNode = newLevel5.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(rootNode.getId());
        treeData = getFundTree(input);
        List<ArrNodeVO> newNodes4 = convertTreeNodes(treeData.getNodes());

        Assert.isTrue(newNodes4.size() == 3);

        // přesun posledního za první
        moveLevelAfter(fundVersion, newNodes4.get(2), rootNode, Arrays.asList(newNodes4.get(0)), rootNode);
    }

    /**
     * Vytvoření levelů v archivní pomůcce.
     *
     * @param fundVersion verze archivní pomůcky
     * @return vytvořené levely
     */
    private List<ArrNodeVO> createLevels(final ArrFundVersionVO fundVersion) {

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        TreeNodeClient parentNode;

        Assert.notNull(treeData.getNodes(), "Musí existovat root node");
        Assert.isTrue(treeData.getNodes().size() == 1, "Musí existovat pouze root node");

        TreeNodeClient rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        // přidání prvního levelu pod root
        ArrangementController.NodeWithParent newLevel1 = addLevel(ArrMoveLevelService.AddLevelDirection.CHILD,
                fundVersion, rootNode, rootNode, "Série");

        Assert.isTrue(newLevel1.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel1.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        parentNode = newLevel1.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání druhého levelu pod root
        ArrangementController.NodeWithParent newLevel2 = addLevel(ArrMoveLevelService.AddLevelDirection.CHILD,
                fundVersion, rootNode, rootNode, null);

        Assert.isTrue(newLevel2.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel2.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        parentNode = newLevel2.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání třetího levelu na první pozici pod root
        ArrangementController.NodeWithParent newLevel3 = addLevel(ArrMoveLevelService.AddLevelDirection.BEFORE,
                fundVersion, newLevel1.getNode(), rootNode, null);

        Assert.isTrue(newLevel3.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel3.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        parentNode = newLevel3.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání uzlu za první uzel pod root (za child3)
        ArrangementController.NodeWithParent newLevel4 = addLevel(ArrMoveLevelService.AddLevelDirection.AFTER,
                fundVersion, newLevel3.getNode(), rootNode, null);

        Assert.isTrue(newLevel4.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel4.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

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
        Assert.isTrue(node1.getId().equals(newLevel3.getNode().getId()));
        Assert.isTrue(node2.getId().equals(newLevel4.getNode().getId()));
        Assert.isTrue(node3.getId().equals(newLevel1.getNode().getId()));
        Assert.isTrue(node4.getId().equals(newLevel2.getNode().getId()));

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
        Assert.notNull(fundVersion);
        ArrFundVersionVO newFundVersion = approveVersion(fundVersion, fundVersion.getDateRange());

        Assert.isTrue(!fundVersion.getId().equals(newFundVersion.getId()),
                "Musí být odlišné identifikátory");

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
        Assert.isTrue(RENAME_AP.equals(updatedFund.getName()), "Jméno AP musí být stejné");
        return updatedFund;
    }

    private void deleteFund(final ArrFundVO fund){
        deleteFund(fund.getId());

        fundRepository.findAll().forEach(f -> {
            //není nalezen fond se smazaným id = je smazán
            Assert.isTrue(!f.getFundId().equals(fund.getId()));
        });
    }

    /**
     * Vytvoření AP.
     */
    private ArrFundVO createdFund() {
        ArrFundVO fund = createFund(NAME_AP, "IC1");
        Assert.notNull(fund);

        fund = getFund(fund.getId());
        Assert.notNull(fund);

        return fund;
    }


    @Test
    public void packetsTest() {

        ArrFundVO fund = createFund("Packet Test AP", "IC2");

        List<RulPacketTypeVO> packetTypes = getPacketTypes();

        Assert.notEmpty(packetTypes, "Typy obalů nemůžou být prázdné");

        ArrPacketVO packet = new ArrPacketVO();

        packet.setPacketTypeId(packetTypes.get(0).getId());
        packet.setStorageNumber(STORAGE_NUMBER);
        packet.setState(ArrPacket.State.OPEN);

        ArrPacketVO insertedPacket = insertPacket(fund, packet);

        Assert.notNull(insertedPacket);
        Assert.notNull(insertedPacket.getId());
        Assert.isTrue(packet.getState().equals(insertedPacket.getState()));
        Assert.isTrue(packet.getPacketTypeId().equals(insertedPacket.getPacketTypeId()));
        Assert.isTrue(packet.getStorageNumber().equals(insertedPacket.getStorageNumber()));

        List<ArrPacketVO> packets = findPackets(fund, "", ArrPacket.State.OPEN);
        Assert.isTrue(packets.size() == 1);
        packet = packets.get(0);
        Assert.isTrue(packet.getId().equals(insertedPacket.getId()));

        packets = findFormPackets(fund, null, LIMIT);
        Assert.isTrue(packets.size() == 1);
        packet = packets.get(0);
        Assert.isTrue(packet.getId().equals(insertedPacket.getId()));

        packets = findFormPackets(fund, STORAGE_NUMBER_FOUND, LIMIT);
        Assert.isTrue(packets.size() == 1);
        packet = packets.get(0);
        Assert.isTrue(packet.getId().equals(insertedPacket.getId()));

        packets = findFormPackets(fund, STORAGE_NUMBER_NOT_FOUND, LIMIT);
        Assert.isTrue(packets.size() == 0);

        packet.setStorageNumber(STORAGE_NUMBER_CHANGE);
        List<ArrPacketVO> setStatePackets = Arrays.asList(packet);
        setStatePackets(fund, setStatePackets, ArrPacket.State.CANCELED);

        packets = findFormPackets(fund, null, LIMIT);
        Assert.isTrue(packets.size() == 0);

        packet.setStorageNumber(STORAGE_NUMBER_CHANGE);
        setStatePackets = Arrays.asList(packet);
        setStatePackets(fund, setStatePackets, ArrPacket.State.OPEN);

        List<ArrPacketVO> deletePackets = Arrays.asList(packet);
        deletePackets(fund, deletePackets);

        packets = findFormPackets(fund, null, LIMIT);
        Assert.isTrue(packets.size() == 0);

        generatePackets(fund, "TEST-", packetTypes.get(0).getId(), 10, 5, 7, null);
        packets = findPackets(fund, "TEST-", ArrPacket.State.OPEN);
        Assert.isTrue(packets.size() == 7);

        Assert.isTrue(packets.get(0).getStorageNumber().equals("TEST-00010"));
        Assert.isTrue(packets.get(1).getStorageNumber().equals("TEST-00011"));
        Assert.isTrue(packets.get(2).getStorageNumber().equals("TEST-00012"));
        Assert.isTrue(packets.get(3).getStorageNumber().equals("TEST-00013"));
        Assert.isTrue(packets.get(4).getStorageNumber().equals("TEST-00014"));
        Assert.isTrue(packets.get(5).getStorageNumber().equals("TEST-00015"));
        Assert.isTrue(packets.get(6).getStorageNumber().equals("TEST-00016"));

        generatePackets(fund, "PAM-", packetTypes.get(0).getId(), 1, 4, 7, packets);
        packets = findPackets(fund, "PAM-", ArrPacket.State.OPEN);
        Assert.isTrue(packets.size() == 7);

        Assert.isTrue(packets.get(0).getStorageNumber().equals("PAM-0001"));
        Assert.isTrue(packets.get(1).getStorageNumber().equals("PAM-0002"));
        Assert.isTrue(packets.get(2).getStorageNumber().equals("PAM-0003"));
        Assert.isTrue(packets.get(3).getStorageNumber().equals("PAM-0004"));
        Assert.isTrue(packets.get(4).getStorageNumber().equals("PAM-0005"));
        Assert.isTrue(packets.get(5).getStorageNumber().equals("PAM-0006"));
        Assert.isTrue(packets.get(6).getStorageNumber().equals("PAM-0007"));
    }

    @Test
    public void calendarsTest() {
        List<ArrCalendarTypeVO> calendarTypes = getCalendarTypes();
        Assert.notEmpty(calendarTypes);
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

        Assert.notNull(fulltext);
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

        List<RegRegisterTypeVO> types = getRecordTypes();
        List<RegScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();

        RegRecordVO record = new RegRecordVO();

        record.setRegisterTypeId(getHierarchicalRegRegisterType(types, null, false).getId());

        record.setCharacteristics("Ja jsem regRecordA");

        record.setRecord("RegRecordA");

        record.setScopeId(scopeId);

        record.setHierarchical(true);
        record.setAddRecord(true);

        record = createRecord(record);

        ArrNodeRegisterVO nodeRegister = new ArrNodeRegisterVO();

        nodeRegister.setValue(record.getId());
        nodeRegister.setNodeId(rootNode.getId());
        nodeRegister.setNode(rootNode);

        ArrNodeRegisterVO createdLink = createRegisterLinks(fundVersion.getId(), rootNode.getId(), nodeRegister);

        Assert.notNull(createdLink);

        List<ArrNodeRegisterVO> registerLinks = findRegisterLinks(fundVersion.getId(), rootNode.getId());
        Assert.notEmpty(registerLinks);

        ArrangementController.NodeRegisterDataVO registerLinksForm = findRegisterLinksForm(fundVersion.getId(),
                rootNode.getId());

        Assert.notNull(registerLinksForm.getNode());
        Assert.notEmpty(registerLinksForm.getNodeRegisters());

        ArrNodeRegisterVO updatedLink = updateRegisterLinks(fundVersion.getId(), rootNode.getId(), createdLink);

        Assert.isTrue(!createdLink.getId().equals(updatedLink.getId()));

        ArrNodeRegisterVO deletedLink = deleteRegisterLinks(fundVersion.getId(), rootNode.getId(), updatedLink);

        Assert.isTrue(updatedLink.getId().equals(deletedLink.getId()));
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
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("ZP2015_TITLE");
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
        replaceDataValues(fundVersion.getId(), typeVo.getId(), "value", "valXYZ", body);


        //nalezení hodnot podle změněné hodnoty
        RulItemType type = itemTypeRepository.findOneByCode("ZP2015_TITLE");
        type.setDataType(dataTypeRepository.findByCode("TEXT"));  //kvůli transakci (no session)
        List<ArrData> nodesContainingText = dataRepository.findByNodesContainingText(nodeRepository.findAll(nodeIds),
                type, null, "valXYZ");

        Assert.isTrue(nodesContainingText.size() == nodeIds.size());
        for (ArrData arrData : nodesContainingText) {
            ArrDataText data = (ArrDataText) arrData;
            Assert.isTrue(Pattern.compile("^(\\d+valXYZ\\d+)$").matcher(data.getValue()).matches());
            Assert.isTrue(nodeIds.contains(arrData.getItem().getNodeId()));
        }


        //test nahrazení všech hodnot na konkrétní hodnotu
        allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAll(nodeIds));
        body.setNodes(new HashSet<>(allNodes));
        placeDataValues(fundVersion.getId(), typeVo.getId(), "nova_value", body);

        List<ArrData> byNodesAndDeleteChangeIsNull = dataRepository
                .findByNodesAndDeleteChangeIsNull(nodeRepository.findAll(nodeIds));
        Assert.isTrue(byNodesAndDeleteChangeIsNull.size() >= nodeIds.size());
        for (ArrData arrData : byNodesAndDeleteChangeIsNull) {
            if (arrData.getItem().getItemType().getItemTypeId().equals(typeVo.getId())) {
                ArrDataText text = (ArrDataText) arrData;
                Assert.isTrue(text.getValue().equals("nova_value"));
            }
        }


        //smazání hodnot atributů
        allNodes = clientFactoryVO.createArrNodes(nodeRepository.findAll(nodeIds));
        body.setNodes(new HashSet<>(allNodes));
        deleteDescItems(fundVersion.getId(), typeVo.getId(), body);

        List<ArrDescItem> nodeDescItems = descItemRepository
                .findOpenByNodesAndType(nodeRepository.findAll(nodeIds), type);
        Assert.isTrue(nodeDescItems.isEmpty());
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
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("ZP2015_UNIT_DATE_TEXT");
        int index = 1;
        String value = "value";
        for (ArrNodeVO node : nodes) {
            ArrItemVO descItem = buildDescItem(typeVo.getCode(), null, value + index, null, null);
            ArrangementController.DescItemResult descItemResult = createDescItem(descItem, fundVersion, node,
                    typeVo);
            index = -index;
        }


        List<String> resultList = filterUniqueValues(fundVersion.getId(), typeVo.getId(), "ue1", null);
        Assert.isTrue(resultList.size() < nodes.size());
        Assert.isTrue(resultList.contains("value1"));
        Assert.isTrue(!resultList.contains("value-1"));



        approvedVersion(fundVersion);
        resultList = filterUniqueValues(fundVersion.getId(), typeVo.getId(), "ue1", null);
        Assert.isTrue(resultList.size() < nodes.size());
        Assert.isTrue(resultList.contains("value1"));
        Assert.isTrue(!resultList.contains("value-1"));



    }

    @Test
    @Ignore // TODO po implementaci
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
        Assert.notNull(validateResult, "Výsledek validace nemůže být prázdný");

        CopyNodesParams copyNodesParams = new CopyNodesParams();

        copyNodesParams.setSourceFundVersionId(fundVersionSource.getId());
        copyNodesParams.setSourceNodes(Collections.singleton(nodeSource));
        copyNodesParams.setTargetFundVersionId(fundVersionTarget.getId());
        copyNodesParams.setTargetStaticNode(nodesTarget.get(0));
        copyNodesParams.setTargetStaticNodeParent(null);
        copyNodesParams.setIgnoreRootNodes(true);
        copyNodesParams.setFilesConflictResolve(null);
        copyNodesParams.setPacketsConflictResolve(null);

        copyLevels(copyNodesParams);
    }

}
