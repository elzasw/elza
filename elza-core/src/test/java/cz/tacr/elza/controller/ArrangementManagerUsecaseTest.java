package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.domain.vo.ArrLevelWithExtraNode;
import cz.tacr.elza.domain.vo.ArrNodeHistoryItem;
import cz.tacr.elza.domain.vo.ArrNodeHistoryPack;
import cz.tacr.elza.domain.vo.ArrNodeRegisterPack;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Kompletní test {@link ArrangementManager}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 9. 2015
 */
public class ArrangementManagerUsecaseTest extends AbstractRestTest {

    private static final Integer TEST_VALUE_123 = 123;
    private static final Integer TEST_VALUE_456 = 456;
    private static final Integer TEST_VALUE_789 = 789;

    private static final Integer PRVNI = 0;
    private static final Integer DRUHY = 1;
    private static final Integer TRETI = 2;

    private RulRuleSet ruleSet;
    private RulArrangementType arrangementType;

    /** Příprava dat. */
    @Override
    @Before
    public void setUp() {
        super.setUp();

        ruleSet = createRuleSet();
        arrangementType = createArrangementType(ruleSet);

        RulDataType dataType = getDataType(DATA_TYPE_INTEGER);

        // vytvoření závislých dat
        RulDescItemType descItemType = createDescItemType(dataType, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, null, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, null, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, null, true, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, null, true, null, 50);

        // vytvoření závislých dat
        RulDescItemType descItemType2 = createDescItemType(dataType, "ITEM_TYPE2", "Item type 2", "SH3", "Desc 3", false, false, true, 2);
        RulDescItemSpec descItemSpec2 = createDescItemSpec(descItemType2, "ITEM_SPEC2", "Item spec 2", "SH4", "Desc 4", 2);
        createDescItemConstrain(descItemType2, descItemSpec2, null, null, "[0-9]*", null);
        createDescItemConstrain(descItemType2, descItemSpec2, null, null, null, 50);
    }

    @Test
    public void UsecaseTest() {
        ArrFindingAid findingAid = testCreateFindingAid();
        findingAid = testUpdateFindingAid(findingAid);
        testApproveFindingAidVersion(findingAid);
        testAddLevels(findingAid);
        testMoveAndDeleteLevels(findingAid);
        testAttributeValues(findingAid);
        testArrangementTypeRelations(findingAid);
        testShowHistoryByNode(findingAid);
        testNodeRecordLink(findingAid);
    }

    /**
     * Otestuje zobrazení navrácení hodnot pro historie podle uzlu
     *
     * @param findingAid    archivní pomůcka
     */
    private void testShowHistoryByNode(final ArrFindingAid findingAid) {
        ArrFindingAidVersion faVersion = getFindingAidOpenVersion(findingAid);
        ArrNode node = faVersion.getRootLevel().getNode();
        ArrNodeHistoryPack historyForNode = getHistoryForNode(node.getNodeId(), faVersion.getFindingAid().getFindingAidId());
        Assert.notNull(historyForNode);
        Assert.notNull(historyForNode.getItems());
        Map<Integer, List<ArrNodeHistoryItem>> items = historyForNode.getItems();
        Assert.isTrue(items.size() == 2);
    }

    /**
     * Otestuje vazby mezi pravidly tvorby a typy výstupu.
     *
     * @param findingAid archivní pomůcka
     */
    private void testArrangementTypeRelations(final ArrFindingAid findingAid) {
        List<ArrFindingAidVersion> versions = getFindingAidVersions(findingAid);
        for (final ArrFindingAidVersion version : versions) {
            RulRuleSet versionRuleSet = version.getRuleSet();
            RulArrangementType versionArrangementType = version.getArrangementType();

            Assert.isTrue(versionRuleSet.equals(versionArrangementType.getRuleSet()));
        }

        RulRuleSet rs1 = createRuleSet();
        createArrangementType(rs1);
        createArrangementType(rs1);
        createArrangementType(rs1);

        RulRuleSet rs2 = createRuleSet();
        RulArrangementType rs2AT1 = createArrangementType(rs2);
        createArrangementType(rs2);

        List<RulArrangementType> arrangementTypes1 = getArrangementTypes(rs1);
        Assert.isTrue(arrangementTypes1.size() == 3);
        Assert.isTrue(arrangementTypes1.get(0).getRuleSet().equals(rs1));
        Assert.isTrue(arrangementTypes1.get(1).getRuleSet().equals(rs1));
        Assert.isTrue(arrangementTypes1.get(2).getRuleSet().equals(rs1));

        List<RulArrangementType> arrangementTypes2 = getArrangementTypes(rs2);
        Assert.isTrue(arrangementTypes2.size() == 2);
        Assert.isTrue(arrangementTypes2.get(0).getRuleSet().equals(rs2));
        Assert.isTrue(arrangementTypes2.get(1).getRuleSet().equals(rs2));

        // Pokus o vytvoření archivní pomůcky s typem výstupu nepatřícím pravidlům tvorby
        createFindingAid(rs1, rs2AT1, HttpStatus.INTERNAL_SERVER_ERROR);

        // Pokus o uzavření verze a vytvoření nové verze archivní pomůcky s typem výstupu nepatřícím pravidlům tvorby
        ArrFindingAidVersion openVersion = getFindingAidOpenVersion(findingAid);
        approveVersion(openVersion, rs1, rs2AT1, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Otestuje práci s atritbuty a hodnotami.
     *
     * @param findingAid archivní pomůcka
     */
    private void testAttributeValues(final ArrFindingAid findingAid) {
        ArrFindingAidVersion version = getFindingAidOpenVersion(findingAid);
        ArrNode node = version.getRootLevel().getNode();

        List<RulDescItemTypeExt> descItemTypes = getAllRulDescItemTypExt();
        Assert.isTrue(descItemTypes.size() == 2);

        // Vytvoření hodnoty atributu pro kořenový uzel
        RulDescItemTypeExt rulDescItemTypeExt = descItemTypes.get(0);
        ArrDescItem descItemExt = createArrDescItem(node, rulDescItemTypeExt, version, TEST_VALUE_123);
        Assert.notNull(descItemExt);
        Assert.notNull(((ArrDescItemInt) descItemExt).getValue());
        Assert.isTrue(((ArrDescItemInt) descItemExt).getValue().equals(TEST_VALUE_123));

        ArrLevelExt arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        List<ArrDescItem> descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.size() == 1);
        Assert.isTrue(((ArrDescItemInt) descItemList.get(0)).getValue().equals(TEST_VALUE_123));
        Assert.isTrue(descItemList.get(0).getDescItemObjectId().equals(descItemExt.getDescItemObjectId()));

        // Aktualizace hodnoty
        ArrDescItem arrDescItemExtToUpdate = descItemList.get(0);
        ArrDescItem updatedDescItemExt = updateArrDescItem(arrDescItemExtToUpdate, TEST_VALUE_456, version, true);
        Assert.notNull(updatedDescItemExt);
        Assert.notNull(((ArrDescItemInt) updatedDescItemExt).getValue());
        Assert.isTrue(((ArrDescItemInt) updatedDescItemExt).getValue().equals(TEST_VALUE_456));

        arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.size() == 1);
        Assert.isTrue(((ArrDescItemInt) descItemList.get(0)).getValue().equals(TEST_VALUE_456));
        Assert.isTrue(descItemList.get(0).getDescItemObjectId().equals(descItemExt.getDescItemObjectId()));
        Assert.isTrue(descItemList.get(0).getDescItemObjectId().equals(updatedDescItemExt.getDescItemObjectId()));

        // Odstranění hodnoty
        ArrDescItem arrDescItemExtToDelete = descItemList.get(0);
        ArrDescItem deletedDescItemExt = deleteDescriptionItem(arrDescItemExtToDelete, version);
        Assert.notNull(deletedDescItemExt);
        Assert.notNull(((ArrDescItemInt) deletedDescItemExt).getValue());
        Assert.isTrue(((ArrDescItemInt) deletedDescItemExt).getValue().equals(((ArrDescItemInt) arrDescItemExtToDelete).getValue()));
        Assert.isTrue(deletedDescItemExt.getDeleteChange() != null);

        arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.isEmpty());

        // Manipulace s více hodnotami najednou
        ArrFindingAidVersion faVersion = getFindingAidOpenVersion(findingAid);
        node = faVersion.getRootLevel().getNode();
        ArrDescItemSavePack savePack = prepareSavePack(node, version, descItemTypes);
        List<ArrDescItem> arrDescItemExts = storeSavePack(savePack);
        Assert.isTrue(arrDescItemExts.size() == 2);
        Assert.isTrue(
                (((ArrDescItemInt) arrDescItemExts.get(0)).getValue().equals(TEST_VALUE_123) && ((ArrDescItemInt) arrDescItemExts.get(1)).getValue().equals(TEST_VALUE_456)) || (
                        ((ArrDescItemInt) arrDescItemExts.get(1)).getValue().equals(TEST_VALUE_123) && ((ArrDescItemInt) arrDescItemExts.get(0)).getValue()
                                .equals(TEST_VALUE_456)));
        Assert.isTrue(arrDescItemExts.get(0).getPosition().equals(1));
        Assert.isTrue(arrDescItemExts.get(1).getPosition().equals(1));

        arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.size() == 2);

        // Uložení s verzováním změn v uzavřené verzi, očekává se chyba
        List<ArrFindingAidVersion> versions = getFindingAidVersions(findingAid);
        savePack = prepareSavePack(node, versions.get(0), descItemTypes);
        storeSavePackWithError(savePack);

        // Aktualizace hodnot - odebrání a změna
        node = getFindingAidOpenVersion(findingAid).getRootLevel().getNode();
        ArrDescItemSavePack updateSavePack = prepareUpdateSavePack(node, version, arrDescItemExts);
        List<ArrDescItem> updatedArrDescItemExts = storeSavePack(updateSavePack);
        Assert.isTrue(updatedArrDescItemExts.size() == 2);
        Assert.isTrue(updatedArrDescItemExts.get(0).getPosition().equals(1));
        Assert.isTrue(updatedArrDescItemExts.get(1).getPosition().equals(1));

        arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.size() == 1);
        Assert.isTrue(((ArrDescItemInt) descItemList.get(0)).getValue().equals(TEST_VALUE_789));
        Assert.isTrue(descItemList.get(0).getPosition().equals(1));
        Assert.isTrue(descItemList.get(0).getDescItemObjectId().equals(updatedArrDescItemExts.get(1).getDescItemObjectId()));
    }

    private void storeSavePackWithError(final ArrDescItemSavePack savePack) {
        post((spec) -> spec.body(savePack), SAVE_DESCRIPTION_ITEMS_URL, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Vytvoří balík hodnot pro uložení.
     *
     * @param node uzel na který se dají hodnoty
     * @param version verze
     * @param descItemTypes seznam typů atributů
     *
     * @return balík hodnot
     */
    private ArrDescItemSavePack prepareSavePack(final ArrNode node, final ArrFindingAidVersion version, final List<RulDescItemTypeExt> descItemTypes) {
        ArrDescItemSavePack savePack = new ArrDescItemSavePack();
        savePack.setCreateNewVersion(true);
        savePack.setFaVersionId(version.getFindingAidVersionId());
        savePack.setNode(node);
        savePack.setDeleteDescItems(new ArrayList<>());

        List<ArrDescItem> descItemExtList = new ArrayList<>(2);
        savePack.setDescItems(descItemExtList);

        ArrDescItem descItem1 = createArrDescItemExt(node, descItemTypes.get(0), TEST_VALUE_123);
        ArrDescItem descItem2 = createArrDescItemExt(node, descItemTypes.get(1), TEST_VALUE_456);

        descItemExtList.add(descItem1);
        descItemExtList.add(descItem2);

        return savePack;
    }

    /**
     * Vytvoří balík hodnot pro aktualizaci.
     *
     * @param node uzel na který se dají hodnoty
     * @param version verze
     * @param originalValues původní hodnoty
     *
     * @return balík hodnot
     */
    private ArrDescItemSavePack prepareUpdateSavePack(final ArrNode node, final ArrFindingAidVersion version, final List<ArrDescItem> originalValues) {
        ArrDescItemSavePack updateSavePack = new ArrDescItemSavePack();
        updateSavePack.setCreateNewVersion(true);
        updateSavePack.setFaVersionId(version.getFindingAidVersionId());
        updateSavePack.setNode(node);

        List<ArrDescItem> updateValues = new ArrayList<>();
        List<ArrDescItem> deleteValues = new ArrayList<>();
        updateValues.add(originalValues.get(0));
        ((ArrDescItemInt) originalValues.get(0)).setValue(TEST_VALUE_789);
        updateSavePack.setDescItems(updateValues);

        deleteValues.add(originalValues.get(1));
        updateSavePack.setDeleteDescItems(deleteValues);

        return updateSavePack;
    }

    /**
     * Vytvoří hodnotu atributu.
     *
     * @param node uzel
     * @param rulDescItemTypeExt typ atributu
     * @param value hodnota atributu
     *
     * @return hodnota atributu
     */
    private ArrDescItem createArrDescItemExt(final ArrNode node, final RulDescItemTypeExt rulDescItemTypeExt, final Integer value) {
        RulDescItemType rulDescItemType = new RulDescItemType();
        BeanUtils.copyProperties(rulDescItemTypeExt, rulDescItemType);

        RulDescItemSpecExt rulDescItemSpecExt1 = rulDescItemTypeExt.getRulDescItemSpecList().get(0);
        RulDescItemSpec rulDescItemSpec = new RulDescItemSpec();
        BeanUtils.copyProperties(rulDescItemSpecExt1, rulDescItemSpec);

        ArrDescItem descItem = new ArrDescItemInt();
        descItem.setDescItemType(rulDescItemType);
        descItem.setDescItemSpec(rulDescItemSpec);
        ((ArrDescItemInt) descItem).setValue(value);
        descItem.setNode(node);

        return descItem;
    }

    /**
     * Vytvoří hodnotu přes REST.
     *
     * @param node uzel na kterém se má hodnota vytvořit
     * @param rulDescItemTypeExt typ atributu
     * @param version verze
     *
     * @return vytvořená hodnota
     */
    private ArrDescItem createArrDescItem(final ArrNode node, final RulDescItemTypeExt rulDescItemTypeExt, final ArrFindingAidVersion version,
                                             final Integer value) {
        RulDescItemType rulDescItemType = new RulDescItemType();
        BeanUtils.copyProperties(rulDescItemTypeExt, rulDescItemType);

        RulDescItemSpecExt rulDescItemSpecExt = rulDescItemTypeExt.getRulDescItemSpecList().get(0);
        RulDescItemSpec rulDescItemSpec = new RulDescItemSpec();
        BeanUtils.copyProperties(rulDescItemSpecExt, rulDescItemSpec);

        ArrDescItem descItem = new ArrDescItemInt();
        descItem.setDescItemType(rulDescItemType);
        descItem.setDescItemSpec(rulDescItemSpec);
        ((ArrDescItemInt) descItem).setValue(value);
        descItem.setNode(node);

        Response response = post((spec) -> spec.body(descItem).pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()),
                CREATE_DESCRIPTION_ITEM_URL);

        return response.getBody().as(ArrDescItem.class);
    }

    /**
     * Aktualizuje hodnotu přes REST.
     *
     * @param descItem hodnota atributu
     * @param value nová hodnota
     * @param version verze
     * @param createNewVersion příznak zda se má změna hodnoty verzovat
     *
     * @return vytvořená hodnota
     */
    private ArrDescItem updateArrDescItem(final ArrDescItem descItem, final Integer value, final ArrFindingAidVersion version,
                                             final boolean createNewVersion) {
        ((ArrDescItemInt) descItem).setValue(value);

        Response response = post((spec) -> spec.body(descItem).pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId())
                .pathParameter(CREATE_NEW_VERSION_ATT, createNewVersion), UPDATE_DESCRIPTION_ITEM_URL);

        return response.getBody().as(ArrDescItem.class);
    }

    /**
     * Odstraní hodnotu přes REST.
     *
     * @param descItem hodnota která se má odstranit
     *
     * @return smazaná hodnota
     */
    private ArrDescItem deleteDescriptionItem(final ArrDescItem descItem, final ArrFindingAidVersion version) {
        Response response = delete((spec) -> spec.body(descItem).pathParameter(VERSION_ID_ATT, version.getFindingAidVersionId()), DELETE_DESCRIPTION_ITEM_URL );

        return response.getBody().as(ArrDescItem.class);
    }

    /** @return všechny typy atributů */
    private List<RulDescItemTypeExt> getAllRulDescItemTypExt() {
        Response response = get((spec) -> spec.parameter(RULE_SET_ID_ATT, 1), GET_DIT_URL);

        return Arrays.asList(response.getBody().as(RulDescItemTypeExt[].class));
    }

    /**
     * Otestuje přesun a mazání uzlů.
     *
     * @param findingAid archivní pomůcka
     */
    private void testMoveAndDeleteLevels(final ArrFindingAid findingAid) {
        ArrFindingAidVersion version = getFindingAidOpenVersion(findingAid);
        ArrNode rootNode = version.getRootLevel().getNode();

        List<ArrLevel> originalChildren = getSubLevels(rootNode, version);
        Assert.isTrue(originalChildren.size() == 4);

        // přesun druhého uzlu před první
        ArrLevelWithExtraNode faLevelWithExtraNode = moveLevelBefore(originalChildren.get(1), originalChildren.get(0), version);
        ArrLevel movedLevel = faLevelWithExtraNode.getLevel();
        Assert.notNull(movedLevel);
        Assert.notNull(movedLevel.getNode().equals(originalChildren.get(1).getNode()));

        List<ArrLevel> children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 4);
        Assert.isTrue(children.get(0).getNode().equals(originalChildren.get(1).getNode()));
        Assert.isTrue(children.get(1).getNode().equals(originalChildren.get(0).getNode()));
        Assert.isTrue(children.get(2).getNode().equals(originalChildren.get(2).getNode()));
        Assert.isTrue(children.get(3).getNode().equals(originalChildren.get(3).getNode()));
        Assert.isTrue(children.get(0).getPosition().equals(1));
        Assert.isTrue(children.get(1).getPosition().equals(2));
        Assert.isTrue(children.get(2).getPosition().equals(3));
        Assert.isTrue(children.get(3).getPosition().equals(4));

        // opakování přesunu druhého uzlu před první, očekává se chyba
        moveLevelBeforeWithError(originalChildren.get(1), originalChildren.get(0), version);

        // přesun druhého uzlu pod první
        faLevelWithExtraNode = moveLevelUnder(children.get(1), children.get(0), version);
        movedLevel = faLevelWithExtraNode.getLevel();
        Assert.notNull(movedLevel);
        Assert.notNull(movedLevel.getNode().equals(children.get(1).getNode()));
        Assert.notNull(movedLevel.getNodeParent().equals(children.get(0).getNode()));
        Assert.notNull(movedLevel.getPosition().equals(1));

        children = getSubLevels(children.get(0).getNode(), version);
        Assert.isTrue(children.size() == 1);
        Assert.isTrue(children.get(0).getNode().equals(movedLevel.getNode()));
        Assert.isTrue(children.get(0).getPosition().equals(1));

        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 3);
        Assert.isTrue(children.get(0).getNode().equals(originalChildren.get(1).getNode()));
        Assert.isTrue(children.get(1).getNode().equals(originalChildren.get(2).getNode()));
        Assert.isTrue(children.get(2).getNode().equals(originalChildren.get(3).getNode()));
        Assert.isTrue(children.get(0).getPosition().equals(1));
        Assert.isTrue(children.get(1).getPosition().equals(2));
        Assert.isTrue(children.get(2).getPosition().equals(3));

        // přesun prvního uzlu za druhý uzel v první úrovni stromu
        faLevelWithExtraNode = moveLevelAfter(children.get(0), children.get(1), version);
        movedLevel = faLevelWithExtraNode.getLevel();
        Assert.notNull(movedLevel);
        Assert.notNull(movedLevel.getNode().equals(rootNode));
        Assert.notNull(movedLevel.getPosition().equals(2));

        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 3);
        Assert.isTrue(children.get(0).getNode().equals(originalChildren.get(2).getNode()));
        Assert.isTrue(children.get(1).getNode().equals(originalChildren.get(1).getNode()));
        Assert.isTrue(children.get(2).getNode().equals(originalChildren.get(3).getNode()));
        Assert.isTrue(children.get(0).getPosition().equals(1));
        Assert.isTrue(children.get(1).getPosition().equals(2));
        Assert.isTrue(children.get(2).getPosition().equals(3));

        // smazání druhého uzlu v první úrovni
        ArrLevel levelToDelete = children.get(1);
        ArrLevel deletedLevel = deleteLevel(levelToDelete, version).getLevel();
        Assert.notNull(deletedLevel);
        Assert.notNull(deletedLevel.getDeleteChange());

        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 2);
        Assert.isTrue(children.get(0).getNode().equals(originalChildren.get(2).getNode()));
        Assert.isTrue(children.get(1).getNode().equals(originalChildren.get(3).getNode()));
        Assert.isTrue(children.get(0).getPosition().equals(1));
        Assert.isTrue(children.get(1).getPosition().equals(2));
    }

    /**
     * Otestuje vytváření uzlů.
     *
     * @param findingAid archivní pomůcka
     */
    private void testAddLevels(final ArrFindingAid findingAid) {
        ArrFindingAidVersion version = getFindingAidOpenVersion(findingAid);
        ArrNode rootNode = version.getRootLevel().getNode();
        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(version.getRootLevel());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        // přidání prvního levelu pod root
        ArrLevelWithExtraNode childLevelWithExtraNode = createLevelChild(levelWithExtraNode);
        ArrLevel child1 = childLevelWithExtraNode.getLevel();
        Assert.notNull(child1);
        Assert.isTrue(child1.getPosition().equals(1));
        Assert.isTrue(child1.getNodeParent().equals(rootNode));

        List<ArrLevel> children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 1);
        Assert.isTrue(children.get(0).getLevelId().equals(child1.getLevelId()));

        // Opakované přidání bez přenačtení uzlu - očekává se chyba kvůli optimistickému zámku
        createLevelChildWithError(levelWithExtraNode);
        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 1);
        Assert.isTrue(children.get(0).getLevelId().equals(child1.getLevelId()));

        // přidání druhého levelu pod root
        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(getLevelByNodeId(rootNode.getNodeId()));
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        childLevelWithExtraNode = createLevelChild(levelWithExtraNode);
        ArrLevel child2 = childLevelWithExtraNode.getLevel();
        Assert.notNull(child2);
        Assert.isTrue(child2.getPosition().equals(2));
        Assert.isTrue(child2.getNodeParent().equals(rootNode));

        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 2);
        Assert.isTrue(children.get(0).getLevelId().equals(child1.getLevelId()));
        Assert.isTrue(children.get(1).getLevelId().equals(child2.getLevelId()));

        // přidání třetího levelu na první pozici pod root
        levelWithExtraNode = new ArrLevelWithExtraNode();
        child1 = getLevelByNodeId(child1.getNode().getNodeId());
        levelWithExtraNode.setLevel(child1);
        levelWithExtraNode.setExtraNode(child1.getNodeParent());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        childLevelWithExtraNode = createLevelBefore(levelWithExtraNode);
        ArrLevel child3 = childLevelWithExtraNode.getLevel();
        Assert.notNull(child3);
        Assert.isTrue(child3.getPosition().equals(1));
        Assert.isTrue(child3.getNodeParent().equals(rootNode));

        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 3);
        Assert.isTrue(children.get(0).getNode().equals(child3.getNode()));
        Assert.isTrue(children.get(1).getNode().equals(child1.getNode()));
        Assert.isTrue(children.get(2).getNode().equals(child2.getNode()));
        Assert.isTrue(children.get(0).getPosition().equals(1));
        Assert.isTrue(children.get(1).getPosition().equals(2));
        Assert.isTrue(children.get(2).getPosition().equals(3));

        // přidání uzlu za první uzel pod root (za child3)
        levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(child3);
        levelWithExtraNode.setExtraNode(child3.getNodeParent());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        childLevelWithExtraNode = createLevelAfter(levelWithExtraNode);
        ArrLevel child4 = childLevelWithExtraNode.getLevel();
        Assert.notNull(child4);
        Assert.isTrue(child4.getPosition().equals(2));
        Assert.isTrue(child4.getNodeParent().equals(rootNode));

        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 4);
        Assert.isTrue(children.get(0).getNode().equals(child3.getNode()));
        Assert.isTrue(children.get(1).getNode().equals(child4.getNode()));
        Assert.isTrue(children.get(2).getNode().equals(child1.getNode()));
        Assert.isTrue(children.get(3).getNode().equals(child2.getNode()));
        Assert.isTrue(children.get(0).getPosition().equals(1));
        Assert.isTrue(children.get(1).getPosition().equals(2));
        Assert.isTrue(children.get(2).getPosition().equals(3));
        Assert.isTrue(children.get(3).getPosition().equals(4));
    }

    /**
     * Uzavření verze archivní pomůcky a následné kontroly.
     *
     * @param findingAid archivní pomůcka
     */
    private void testApproveFindingAidVersion(final ArrFindingAid findingAid) {
        ArrFindingAidVersion openVersion = getFindingAidOpenVersion(findingAid);

        ArrFindingAidVersion newOpenVersion = approveVersion(openVersion);
        Assert.notNull(newOpenVersion);
        Assert.isTrue(!openVersion.getFindingAidVersionId().equals(newOpenVersion.getFindingAidVersionId()));
        Assert.isTrue(newOpenVersion.getArrangementType().getArrangementTypeId().equals(arrangementType.getArrangementTypeId()));
        Assert.isTrue(newOpenVersion.getRuleSet().getRuleSetId().equals(ruleSet.getRuleSetId()));
        Assert.isTrue(newOpenVersion.getLockChange() == null);

        ArrFindingAidVersion newOpenVersionCheck = getFindingAidOpenVersion(findingAid);
        Assert.isTrue(newOpenVersionCheck.equals(newOpenVersion));

        ArrFindingAidVersion closedVersion = getVersionById(openVersion.getFindingAidVersionId());
        Assert.notNull(closedVersion);
        Assert.isTrue(closedVersion.getFindingAidVersionId().equals(openVersion.getFindingAidVersionId()));
        Assert.isTrue(closedVersion.getLockChange() != null);

        List<ArrFindingAidVersion> versions = getFindingAidVersions(findingAid);
        Assert.isTrue(versions.size() == 2);
        Assert.isTrue(versions.get(0).getLockChange() != null);
        Assert.isTrue(versions.get(1).getLockChange() == null);

        approveVersionWithError(closedVersion);
    }

    /**
     * Načte všechny verze archivní pomůcky.
     *
     * @param findingAid archivní pomůcka
     *
     * @return seznam verzí archivní pomůcky
     */
    private List<ArrFindingAidVersion> getFindingAidVersions(final ArrFindingAid findingAid) {
        Response response = get(spec -> spec.parameter(FA_ID_ATT, findingAid.getFindingAidId()),
                GET_FINDING_AID_VERSIONS_URL);

        return Arrays.asList(response.getBody().as(ArrFindingAidVersion[].class));
    }

    /**
     * Načte verzi archivní pomůcky přes REST volání.
     *
     * @param versionId id verze
     *
     * @return verze archivní pomůcky
     */
    private ArrFindingAidVersion getVersionById(final Integer versionId) {
        Response response = get(spec -> spec.parameter(VERSION_ID_ATT, versionId), GET_VERSION_ID_URL);

        return response.getBody().as(ArrFindingAidVersion.class);
    }

    /**
     * Uzavře verzi archivní pomůcky.
     *
     * @param openVersion otevřená verze archivní pomůcky
     *
     * @return nová otevřená verze archivní pomůcky
     */
    private ArrFindingAidVersion approveVersion(final ArrFindingAidVersion openVersion) {
        return approveVersion(openVersion, ruleSet, arrangementType, HttpStatus.OK);
    }

    /**
     * Uzavře verzi archivní pomůcky.
     *
     * @param openVersion otevřená verze archivní pomůcky
     * @param ruleSet pravidla tvorby
     * @param arrangementType typ výstupu
     * @param httpStatus stav jakým má skončit volání
     *
     * @return nová otevřená verze archivní pomůcky
     */
    private ArrFindingAidVersion approveVersion(final ArrFindingAidVersion openVersion, final RulRuleSet ruleSet, final RulArrangementType arrangementType,
            final HttpStatus httpStatus) {
        Response response = put(spec -> spec.body(openVersion).
                parameter(ARRANGEMENT_TYPE_ID_ATT, arrangementType.getArrangementTypeId()).
                parameter(RULE_SET_ID_ATT, ruleSet.getRuleSetId())
                , APPROVE_VERSION_URL, httpStatus);

        if (httpStatus == HttpStatus.OK) {
            return response.getBody().as(ArrFindingAidVersion.class);
        }

        return null;
    }

    /**
     * Pokusí se uzavřít verzi archivní pomůcky. Očekává chybu.
     *
     * @param openVersion otevřená verze archivní pomůcky
     *
     * @return nová otevřená verze archivní pomůcky
     */
    private void approveVersionWithError(final ArrFindingAidVersion openVersion) {
        put(spec -> spec.body(openVersion).
                parameter(ARRANGEMENT_TYPE_ID_ATT, arrangementType.getArrangementTypeId()).
                parameter(RULE_SET_ID_ATT, ruleSet.getRuleSetId())
                , APPROVE_VERSION_URL, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Aktualizace archivní pomůcky a následné kontroly.
     *
     * @param findingAid archivní pomůcka
     *
     * @return archivní pomůcka
     */
    private ArrFindingAid testUpdateFindingAid(final ArrFindingAid findingAid) {
        ArrFindingAid updatedFindingAid = updateFindingAid(findingAid, TEST_UPDATE_NAME);
        testChangedFindingAid(updatedFindingAid, TEST_UPDATE_NAME, 1);

        return updatedFindingAid;
    }

    /**
     * Ajtualizace archivní pomůcky.
     *
     * @return aktualizovaná archivní pomůcka
     */
    private ArrFindingAid updateFindingAid(final ArrFindingAid findingAid, final String testUpdateName) {
        findingAid.setName(testUpdateName);
        Response response = put(spec -> spec.body(findingAid), UPDATE_FA_URL);

        return response.getBody().as(ArrFindingAid.class);
    }

    /**
     * Vytvoření a kontrola archivní pomůcky.
     *
     * @return archivní pomůcka
     */
    private ArrFindingAid testCreateFindingAid() {
        ArrFindingAid findingAid = createFindingAid();
        testChangedFindingAid(findingAid, TEST_NAME, 1);

        return findingAid;
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @param findingAid archivní pomůcka
     * @param testName předpokládaný název archivní pomůcky
     * @param findingAidsCount předpokládaný počet archivních pomůcek
     */
    private void testChangedFindingAid(ArrFindingAid findingAid, final String testName, final int findingAidsCount) {
        Assert.notNull(findingAid);
        Assert.notNull(findingAid.getFindingAidId());

        Integer findingAidId = findingAid.getFindingAidId();
        findingAid = getFindingAid(findingAidId);

        Assert.notNull(findingAid);
        Assert.notNull(findingAid.getFindingAidId());
        Assert.isTrue(findingAid.getFindingAidId().equals(findingAidId));
        Assert.isTrue(findingAid.getName().equals(testName));

        List<ArrFindingAid> findingAids = getFindingAids();
        Assert.isTrue(findingAids.size() == findingAidsCount);

        ArrFindingAidVersion openVersion = getFindingAidOpenVersion(findingAid);
        Assert.notNull(openVersion);
        Assert.isNull(openVersion.getLockChange());

        Integer nodeId = openVersion.getRootLevel().getNode().getNodeId();
        ArrLevelExt rootLevel = getLevelByNodeId(nodeId);
        Assert.notNull(rootLevel);
        Assert.isTrue(rootLevel.getNode().getNodeId().equals(nodeId));
        Assert.isNull(rootLevel.getNodeParent());
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @return archivní pomůcka
     */
    private ArrFindingAid createFindingAid() {
        return createFindingAid(ruleSet, arrangementType, HttpStatus.OK);
    }

    /**
     * Vazby meziuzlem a hesly rejštříku. Zkouší vytvoření tří vazeb od jednoho uzlu na tři různá hesla.
     * Samotné smazání třetí a dále: update první a smazání druhé.
     *
     * @param findingAid    archivní pomůcka
     */
    private void testNodeRecordLink(final ArrFindingAid findingAid) {
        ArrFindingAidVersion version = getFindingAidOpenVersion(findingAid);
        ArrNode node = version.getRootLevel().getNode();

        RegRecord record1 = createRecord();
        RegRecord record2 = createRecord();
        RegRecord record3 = createRecord();

        // just create
        restCreateNodeRegister(node, record1, record2, record3, version.getFindingAidVersionId());

        List<ArrNodeRegister> nodeRegisters = findNodeRegisters(version.getFindingAidVersionId(), node.getNodeId());
        Assert.isTrue(nodeRegisters.size() == 3);
        Assert.isTrue(nodeRegisters.get(PRVNI).getCreateChange() != null);
        Assert.isTrue(nodeRegisters.get(DRUHY).getCreateChange() != null);
        Assert.isTrue(nodeRegisters.get(TRETI).getCreateChange() != null);

        Assert.isTrue(nodeRegisters.get(PRVNI).getNode().equals(node));
        Assert.isTrue(nodeRegisters.get(DRUHY).getNode().equals(node));
        Assert.isTrue(nodeRegisters.get(TRETI).getNode().equals(node));

        Assert.isTrue(nodeRegisters.get(PRVNI).getRecord().equals(record1));
        Assert.isTrue(nodeRegisters.get(DRUHY).getRecord().equals(record2));
        Assert.isTrue(nodeRegisters.get(TRETI).getRecord().equals(record3));

        // just delete - TRETI
        restModifyNodeRegister(new ArrNodeRegisterPack(null, Arrays.asList(nodeRegisters.get(TRETI))), version.getFindingAidVersionId());

        nodeRegisters = findNodeRegisters(version.getFindingAidVersionId(), node.getNodeId());
        Assert.isTrue(nodeRegisters.size() == 2);

        // update and delete  PRVNI, DRUHY
        nodeRegisters.get(PRVNI).setRecord(record3); // změna pro update
        List<ArrNodeRegister> delete = Arrays.asList(nodeRegisters.get(DRUHY)); // DRUHY ke smazání
        nodeRegisters.remove(DRUHY);                                            // a pryč ze save

        restModifyNodeRegister(new ArrNodeRegisterPack(nodeRegisters, delete), version.getFindingAidVersionId());

        nodeRegisters = findNodeRegisters(version.getFindingAidVersionId(), node.getNodeId());
        Assert.isTrue(nodeRegisters.size() == 1);
        Assert.isTrue(nodeRegisters.get(PRVNI).getRecord().equals(record3));  //zbyde PRVNI změněný

    }

    /**
     * Vytvoří tři vazby mezi uzlem a danými hesly.
     * @param node          uzel
     * @param record1       heslo 1
     * @param record2       heslo 2
     * @param record3       heslo 3
     */
    protected void restCreateNodeRegister(final ArrNode node, final RegRecord record1, final RegRecord record2,
                                          final RegRecord record3, final Integer versionId) {

        ArrNodeRegister nodeRegister1 = new ArrNodeRegister();
        nodeRegister1.setNode(node);
        nodeRegister1.setRecord(record1);

        ArrNodeRegister nodeRegister2 = new ArrNodeRegister();
        nodeRegister2.setNode(node);
        nodeRegister2.setRecord(record2);

        ArrNodeRegister nodeRegister3 = new ArrNodeRegister();
        nodeRegister3.setNode(node);
        nodeRegister3.setRecord(record3);

        List<ArrNodeRegister> modifyNodeRegisters = Arrays.asList(nodeRegister1, nodeRegister2, nodeRegister3);

        restModifyNodeRegister(new ArrNodeRegisterPack(modifyNodeRegisters, null), versionId);
    }

    /**
     * Volání úprav vazeb přes rest.
     * @param arrNodeRegisterPack   obalení kolekcí s úpravami vazeb či smazáním
     */
    protected void restModifyNodeRegister(final ArrNodeRegisterPack arrNodeRegisterPack, final Integer versionId) {

        put(spec -> spec.body(arrNodeRegisterPack).pathParameter(VERSION_ID_ATT, versionId), MODIFY_NODE_REGISTER_LINKS_URL);
    }

    /**
     * Hledá vazby.
     * @param versionId     id verze
     * @param nodeId        id uzlu
     * @return              nalezené vazby
     */
    protected List<ArrNodeRegister> findNodeRegisters(final Integer versionId, final Integer nodeId) {
        Response response = get(spec -> spec
                .parameter(VERSION_ID_ATT, versionId)
                .parameter(NODE_ID_ATT, nodeId)
                , FIND_NODE_REGISTER_LINKS_URL);

        return new ArrayList<>(Arrays.asList(response.getBody().as(ArrNodeRegister[].class)));
    }

}
