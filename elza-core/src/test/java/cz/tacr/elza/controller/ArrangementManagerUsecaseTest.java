package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import com.jayway.restassured.response.Response;

import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaLevelExt;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.domain.vo.ArrFaLevelWithExtraNode;

/**
 * Kompletní test {@link ArrangementManager}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 9. 2015
 */
public class ArrangementManagerUsecaseTest extends AbstractRestTest {

    private static final String TEST_VALUE_123 = "123";
    private static final String TEST_VALUE_456 = "456";
    private static final String TEST_VALUE_789 = "789";

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
        RulDescItemType descItemType = createDescItemType(dataType, true, "ITEM_TYPE1", "Item type 1", "SH1", "Desc 1", false, false, true, 1);
        RulDescItemSpec descItemSpec = createDescItemSpec(descItemType, "ITEM_SPEC1", "Item spec 1", "SH2", "Desc 2", 1);
        createDescItemConstrain(descItemType, descItemSpec, null, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, null, true, null, null);
        createDescItemConstrain(descItemType, descItemSpec, null, true, "[0-9]*", null);
        createDescItemConstrain(descItemType, descItemSpec, null, true, null, 50);

        // vytvoření závislých dat
        RulDescItemType descItemType2 = createDescItemType(dataType, true, "ITEM_TYPE2", "Item type 2", "SH3", "Desc 3", false, false, true, 2);
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
    }

    /**
     * Otestuje vazby mezi pravidly tvorby a typy výstupu.
     *
     * @param findingAid archivní pomůcka
     */
    private void testArrangementTypeRelations(ArrFindingAid findingAid) {
        List<ArrFaVersion> versions = getFindingAidVersions(findingAid);
        for (ArrFaVersion version : versions) {
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
        ArrFaVersion openVersion = getFindingAidOpenVersion(findingAid);
        approveVersion(openVersion, rs1, rs2AT1, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Otestuje práci s atritbuty a hodnotami.
     *
     * @param findingAid archivní pomůcka
     */
    private void testAttributeValues(ArrFindingAid findingAid) {
        ArrFaVersion version = getFindingAidOpenVersion(findingAid);
        ArrNode node = version.getRootFaLevel().getNode();

        List<RulDescItemTypeExt> descItemTypes = getAllRulDescItemTypExt();
        Assert.isTrue(descItemTypes.size() == 2);

        // Vytvoření hodnoty atributu pro kořenový uzel
        RulDescItemTypeExt rulDescItemTypeExt = descItemTypes.get(0);
        ArrDescItemExt descItemExt = createArrDescItemExt(node, rulDescItemTypeExt, version, TEST_VALUE_123);
        Assert.notNull(descItemExt);
        Assert.notNull(descItemExt.getData());
        Assert.isTrue(descItemExt.getData().equals(TEST_VALUE_123));

        ArrFaLevelExt arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        List<ArrDescItemExt> descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.size() == 1);
        Assert.isTrue(descItemList.get(0).getData().equals(TEST_VALUE_123));
        Assert.isTrue(descItemList.get(0).getDescItemObjectId().equals(descItemExt.getDescItemObjectId()));

        // Aktualizace hodnoty
        ArrDescItemExt arrDescItemExtToUpdate = descItemList.get(0);
        ArrDescItemExt updatedDescItemExt = updateArrDescItemExt(arrDescItemExtToUpdate, TEST_VALUE_456, version, true);
        Assert.notNull(updatedDescItemExt);
        Assert.notNull(updatedDescItemExt.getData());
        Assert.isTrue(updatedDescItemExt.getData().equals(TEST_VALUE_456));

        arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.size() == 1);
        Assert.isTrue(descItemList.get(0).getData().equals(TEST_VALUE_456));
        Assert.isTrue(descItemList.get(0).getDescItemObjectId().equals(descItemExt.getDescItemObjectId()));
        Assert.isTrue(descItemList.get(0).getDescItemObjectId().equals(updatedDescItemExt.getDescItemObjectId()));

        // Odstranění hodnoty
        ArrDescItemExt arrDescItemExtToDelete = descItemList.get(0);
        ArrDescItemExt deletedDescItemExt = deleteDescriptionItem(arrDescItemExtToDelete);
        Assert.notNull(deletedDescItemExt);
        Assert.notNull(deletedDescItemExt.getData());
        Assert.isTrue(deletedDescItemExt.getData().equals(arrDescItemExtToDelete.getData()));
        Assert.isTrue(deletedDescItemExt.getDeleteChange() != null);

        arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.isEmpty());

        // Manipulace s více hodnotami najednou
        ArrFaVersion faVersion = getFindingAidOpenVersion(findingAid);
        node = faVersion.getRootFaLevel().getNode();
        ArrDescItemSavePack savePack = prepareSavePack(node, version, descItemTypes);
        List<ArrDescItemExt> arrDescItemExts = storeSavePack(savePack);
        Assert.isTrue(arrDescItemExts.size() == 2);
        Assert.isTrue(arrDescItemExts.get(0).getData().equals(TEST_VALUE_123));
        Assert.isTrue(arrDescItemExts.get(0).getPosition().equals(1));
        Assert.isTrue(arrDescItemExts.get(1).getData().equals(TEST_VALUE_456));
        Assert.isTrue(arrDescItemExts.get(1).getPosition().equals(1));

        arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.size() == 2);

        // Uložení s verzováním změn v uzavřené verzi, očekává se chyba
        List<ArrFaVersion> versions = getFindingAidVersions(findingAid);
        savePack = prepareSavePack(node, versions.get(0), descItemTypes);
        storeSavePackWithError(savePack);

        // Aktualizace hodnot - odebrání a změna
        node = getFindingAidOpenVersion(findingAid).getRootFaLevel().getNode();
        ArrDescItemSavePack updateSavePack = prepareUpdateSavePack(node, version, arrDescItemExts);
        List<ArrDescItemExt> updatedArrDescItemExts = storeSavePack(updateSavePack);
        Assert.isTrue(updatedArrDescItemExts.size() == 2);
        Assert.isTrue(updatedArrDescItemExts.get(0).getData().equals(TEST_VALUE_456));
        Assert.isTrue(updatedArrDescItemExts.get(0).getPosition().equals(1));
        Assert.isTrue(updatedArrDescItemExts.get(0).getDeleteChange() != null);
        Assert.isTrue(updatedArrDescItemExts.get(1).getData().equals(TEST_VALUE_789));
        Assert.isTrue(updatedArrDescItemExts.get(1).getPosition().equals(1));

        arrFaLevelExt = getLevelByNodeId(node.getNodeId());
        descItemList = arrFaLevelExt.getDescItemList();
        Assert.isTrue(descItemList.size() == 1);
        Assert.isTrue(descItemList.get(0).getData().equals(TEST_VALUE_789));
        Assert.isTrue(descItemList.get(0).getPosition().equals(1));
        Assert.isTrue(descItemList.get(0).getDescItemObjectId().equals(updatedArrDescItemExts.get(1).getDescItemObjectId()));
    }

    private void storeSavePackWithError(ArrDescItemSavePack savePack) {
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
    private ArrDescItemSavePack prepareSavePack(ArrNode node, ArrFaVersion version, List<RulDescItemTypeExt> descItemTypes) {
        ArrDescItemSavePack savePack = new ArrDescItemSavePack();
        savePack.setCreateNewVersion(true);
        savePack.setFaVersionId(version.getFaVersionId());
        savePack.setNode(node);
        savePack.setDeleteDescItems(new ArrayList<>());

        List<ArrDescItemExt> descItemExtList = new ArrayList<>(2);
        savePack.setDescItems(descItemExtList);

        ArrDescItemExt descItem1 = createArrDescItemExt(node, descItemTypes.get(0), TEST_VALUE_123);
        ArrDescItemExt descItem2 = createArrDescItemExt(node, descItemTypes.get(1), TEST_VALUE_456);

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
    private ArrDescItemSavePack prepareUpdateSavePack(ArrNode node, ArrFaVersion version, List<ArrDescItemExt> originalValues) {
        ArrDescItemSavePack updateSavePack = new ArrDescItemSavePack();
        updateSavePack.setCreateNewVersion(true);
        updateSavePack.setFaVersionId(version.getFaVersionId());
        updateSavePack.setNode(node);

        List<ArrDescItemExt> updateValues = new ArrayList<ArrDescItemExt>();
        List<ArrDescItemExt> deleteValues = new ArrayList<ArrDescItemExt>();
        updateValues.add(originalValues.get(0));
        originalValues.get(0).setData(TEST_VALUE_789);
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
    private ArrDescItemExt createArrDescItemExt(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt, String value) {
        RulDescItemType rulDescItemType = new RulDescItemType();
        BeanUtils.copyProperties(rulDescItemTypeExt, rulDescItemType);

        RulDescItemSpecExt rulDescItemSpecExt1 = rulDescItemTypeExt.getRulDescItemSpecList().get(0);
        RulDescItemSpec rulDescItemSpec = new RulDescItemSpec();
        BeanUtils.copyProperties(rulDescItemSpecExt1, rulDescItemSpec);

        ArrDescItemExt descItem = new ArrDescItemExt();
        descItem.setDescItemType(rulDescItemType);
        descItem.setDescItemSpec(rulDescItemSpec);
        descItem.setData(value);
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
    private ArrDescItemExt createArrDescItemExt(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt, ArrFaVersion version,
            String value) {
        RulDescItemType rulDescItemType = new RulDescItemType();
        BeanUtils.copyProperties(rulDescItemTypeExt, rulDescItemType);

        RulDescItemSpecExt rulDescItemSpecExt = rulDescItemTypeExt.getRulDescItemSpecList().get(0);
        RulDescItemSpec rulDescItemSpec = new RulDescItemSpec();
        BeanUtils.copyProperties(rulDescItemSpecExt, rulDescItemSpec);

        ArrDescItemExt descItem = new ArrDescItemExt();
        descItem.setDescItemType(rulDescItemType);
        descItem.setDescItemSpec(rulDescItemSpec);
        descItem.setData(value);
        descItem.setNode(node);

        Response response = post((spec) -> spec.body(descItem).pathParameter(VERSION_ID_ATT, version.getFaVersionId()),
                CREATE_DESCRIPTION_ITEM_URL);

        return response.getBody().as(ArrDescItemExt.class);
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
    private ArrDescItemExt updateArrDescItemExt(ArrDescItemExt descItem, String value, ArrFaVersion version,
            boolean createNewVersion) {
        descItem.setData(value);

        Response response = post((spec) -> spec.body(descItem).pathParameter(VERSION_ID_ATT, version.getFaVersionId())
                .pathParameter(CREATE_NEW_VERSION_ATT, createNewVersion), UPDATE_DESCRIPTION_ITEM_URL);

        return response.getBody().as(ArrDescItemExt.class);
    }

    /**
     * Odstraní hodnotu přes REST.
     *
     * @param descItem hodnota která se má odstranit
     *
     * @return smazaná hodnota
     */
    private ArrDescItemExt deleteDescriptionItem(ArrDescItemExt descItem) {
        Response response = delete((spec) -> spec.body(descItem), DELETE_DESCRIPTION_ITEM_URL);

        return response.getBody().as(ArrDescItemExt.class);
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
    private void testMoveAndDeleteLevels(ArrFindingAid findingAid) {
        ArrFaVersion version = getFindingAidOpenVersion(findingAid);
        ArrNode rootNode = version.getRootFaLevel().getNode();

        List<ArrFaLevel> originalChildren = getSubLevels(rootNode, version);
        Assert.isTrue(originalChildren.size() == 4);

        // přesun druhého uzlu před první
        ArrFaLevelWithExtraNode faLevelWithExtraNode = moveLevelBefore(originalChildren.get(1), originalChildren.get(0), version);
        ArrFaLevel movedLevel = faLevelWithExtraNode.getFaLevel();
        Assert.notNull(movedLevel);
        Assert.notNull(movedLevel.getNode().equals(originalChildren.get(1).getNode()));

        List<ArrFaLevel> children = getSubLevels(rootNode, version);
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
        movedLevel = faLevelWithExtraNode.getFaLevel();
        Assert.notNull(movedLevel);
        Assert.notNull(movedLevel.getNode().equals(children.get(1).getNode()));
        Assert.notNull(movedLevel.getParentNode().equals(children.get(0).getNode()));
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
        movedLevel = faLevelWithExtraNode.getFaLevel();
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
        ArrFaLevel levelToDelete = children.get(1);
        ArrFaLevel deletedLevel = deleteLevel(levelToDelete, version).getFaLevel();
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
    private void testAddLevels(ArrFindingAid findingAid) {
        ArrFaVersion version = getFindingAidOpenVersion(findingAid);
        ArrNode rootNode = version.getRootFaLevel().getNode();
        ArrFaLevelWithExtraNode levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(version.getRootFaLevel());
        levelWithExtraNode.setFaVersionId(version.getFaVersionId());

        // přidání prvního levelu pod root
        ArrFaLevelWithExtraNode childLevelWithExtraNode = createLevelChild(levelWithExtraNode);
        ArrFaLevel child1 = childLevelWithExtraNode.getFaLevel();
        Assert.notNull(child1);
        Assert.isTrue(child1.getPosition().equals(1));
        Assert.isTrue(child1.getParentNode().equals(rootNode));

        List<ArrFaLevel> children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 1);
        Assert.isTrue(children.get(0).getFaLevelId().equals(child1.getFaLevelId()));

        // Opakované přidání bez přenačtení uzlu - očekává se chyba kvůli optimistickému zámku
        createLevelChildWithError(levelWithExtraNode);
        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 1);
        Assert.isTrue(children.get(0).getFaLevelId().equals(child1.getFaLevelId()));

        // přidání druhého levelu pod root
        levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(getLevelByNodeId(rootNode.getNodeId()));
        levelWithExtraNode.setFaVersionId(version.getFaVersionId());

        childLevelWithExtraNode = createLevelChild(levelWithExtraNode);
        ArrFaLevel child2 = childLevelWithExtraNode.getFaLevel();
        Assert.notNull(child2);
        Assert.isTrue(child2.getPosition().equals(2));
        Assert.isTrue(child2.getParentNode().equals(rootNode));

        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 2);
        Assert.isTrue(children.get(0).getFaLevelId().equals(child1.getFaLevelId()));
        Assert.isTrue(children.get(1).getFaLevelId().equals(child2.getFaLevelId()));

        // přidání třetího levelu na první pozici pod root
        levelWithExtraNode = new ArrFaLevelWithExtraNode();
        child1 = getLevelByNodeId(child1.getNode().getNodeId());
        levelWithExtraNode.setFaLevel(child1);
        levelWithExtraNode.setExtraNode(child1.getParentNode());
        levelWithExtraNode.setFaVersionId(version.getFaVersionId());

        childLevelWithExtraNode = createLevelBefore(levelWithExtraNode);
        ArrFaLevel child3 = childLevelWithExtraNode.getFaLevel();
        Assert.notNull(child3);
        Assert.isTrue(child3.getPosition().equals(1));
        Assert.isTrue(child3.getParentNode().equals(rootNode));

        children = getSubLevels(rootNode, version);
        Assert.isTrue(children.size() == 3);
        Assert.isTrue(children.get(0).getNode().equals(child3.getNode()));
        Assert.isTrue(children.get(1).getNode().equals(child1.getNode()));
        Assert.isTrue(children.get(2).getNode().equals(child2.getNode()));
        Assert.isTrue(children.get(0).getPosition().equals(1));
        Assert.isTrue(children.get(1).getPosition().equals(2));
        Assert.isTrue(children.get(2).getPosition().equals(3));

        // přidání uzlu za první uzel pod root (za child3)
        levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(child3);
        levelWithExtraNode.setExtraNode(child3.getParentNode());
        levelWithExtraNode.setFaVersionId(version.getFaVersionId());

        childLevelWithExtraNode = createLevelAfter(levelWithExtraNode);
        ArrFaLevel child4 = childLevelWithExtraNode.getFaLevel();
        Assert.notNull(child4);
        Assert.isTrue(child4.getPosition().equals(2));
        Assert.isTrue(child4.getParentNode().equals(rootNode));

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
     * Najde podřízené úrovně.
     *
     * @param rootNode nadřazený uzel pro který hledáme potomky
     * @param version verze, může být null
     *
     * @return potomky předaného uzlu
     */
    private List<ArrFaLevel> getSubLevels(ArrNode rootNode, ArrFaVersion version) {
        Response response;
        if (version == null) {
            response = get(spec -> spec.parameter(NODE_ID_ATT, rootNode.getNodeId()), FIND_SUB_LEVELS_URL);
        } else {
            response = get(spec -> spec.parameter(NODE_ID_ATT, rootNode.getNodeId())
                    .parameter(VERSION_ID_ATT, version.getFaVersionId()), FIND_SUB_LEVELS_URL);
        }

        return Arrays.asList(response.getBody().as(ArrFaLevel[].class));
    }

    /**
     * Vytvoří nový uzel pod předaným uzlem.
     *
     * @param levelWithExtraNode rodičovský uzel
     *
     * @return nový uzel
     */
    private ArrFaLevelWithExtraNode createLevelChild(ArrFaLevelWithExtraNode levelWithExtraNode) {
        Response response = put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_CHILD_URL);
        ArrFaLevelWithExtraNode parent = response.getBody().as(ArrFaLevelWithExtraNode.class);

        return parent;
    }

    /**
     * Vytvoří nový uzel pod předaným uzlem.
     *
     * @param levelWithExtraNode rodičovský uzel
     */
    private void createLevelChildWithError(ArrFaLevelWithExtraNode levelWithExtraNode) {
        put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_CHILD_URL, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Vytvoří nový uzel před předaným uzlem.
     *
     * @param levelWithExtraNode uzal před kterým se vytvoří nový uzel
     *
     * @return nový uzel
     */
    private ArrFaLevelWithExtraNode createLevelBefore(ArrFaLevelWithExtraNode levelWithExtraNode) {
        Response response = put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_BEFORE_URL);
        ArrFaLevelWithExtraNode parent = response.getBody().as(ArrFaLevelWithExtraNode.class);

        return parent;
    }

    /**
     * Vytvoří nový uzel za předaným uzlem.
     *
     * @param levelWithExtraNode uzal za kterým se vytvoří nový uzel
     *
     * @return nový uzel
     */
    private ArrFaLevelWithExtraNode createLevelAfter(ArrFaLevelWithExtraNode levelWithExtraNode) {
        Response response = put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_AFTER_URL);
        ArrFaLevelWithExtraNode parent = response.getBody().as(ArrFaLevelWithExtraNode.class);

        return parent;
    }

    /**
     * Přesune jeden uzel před druhý.
     *
     * @param movedLevel přesouvaný uzel
     * @param targetLevel uzel před který se má vložit přesouvaný uzel
     * @param version verze archivní pomůcky
     *
     * @return přesunutý uzel
     */
    private ArrFaLevelWithExtraNode moveLevelBefore(ArrFaLevel movedLevel, ArrFaLevel targetLevel, ArrFaVersion version) {
        ArrFaLevelWithExtraNode levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(movedLevel);
        levelWithExtraNode.setFaLevelTarget(targetLevel);
        levelWithExtraNode.setFaVersionId(version.getFaVersionId());

        Response response = put(spec -> spec.body(levelWithExtraNode), MOVE_LEVEL_BEFORE_URL);

        return response.getBody().as(ArrFaLevelWithExtraNode.class);
    }

    /**
     * Přesune jeden uzel před druhý. Očekává se chyba.
     *
     * @param movedLevel přesouvaný uzel
     * @param targetLevel uzel před který se má vložit přesouvaný uzel
     * @param version verze archivní pomůcky
     */
    private void moveLevelBeforeWithError(ArrFaLevel movedLevel, ArrFaLevel targetLevel, ArrFaVersion version) {
        ArrFaLevelWithExtraNode levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(movedLevel);
        levelWithExtraNode.setFaLevelTarget(targetLevel);
        levelWithExtraNode.setFaVersionId(version.getFaVersionId());

        put(spec -> spec.body(levelWithExtraNode), MOVE_LEVEL_BEFORE_URL, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Přesune jeden uzel pod druhý.
     *
     * @param movedLevel přesouvaný uzel
     * @param targetLevel uzel pod který se má vložit přesouvaný uzel
     * @param version verze archivní pomůcky
     *
     * @return přesunutý uzel
     */
    private ArrFaLevelWithExtraNode moveLevelUnder(ArrFaLevel movedLevel, ArrFaLevel targetLevel, ArrFaVersion version) {
        ArrFaLevelWithExtraNode levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(movedLevel);
        levelWithExtraNode.setExtraNode(targetLevel.getNode());
        levelWithExtraNode.setFaVersionId(version.getFaVersionId());

        Response response = put(spec -> spec.body(levelWithExtraNode), MOVE_LEVEL_UNDER_URL);

        return response.getBody().as(ArrFaLevelWithExtraNode.class);
    }

    /**
     * Přesune jeden uzel za druhý.
     *
     * @param movedLevel přesouvaný uzel
     * @param targetLevel uzel za který se má vložit přesouvaný uzel
     * @param version verze archivní pomůcky
     *
     * @return přesunutý uzel
     */
    private ArrFaLevelWithExtraNode moveLevelAfter(ArrFaLevel movedLevel, ArrFaLevel targetLevel, ArrFaVersion version) {
        ArrFaLevelWithExtraNode levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(movedLevel);
        levelWithExtraNode.setFaLevelTarget(targetLevel);
        levelWithExtraNode.setFaVersionId(version.getFaVersionId());

        Response response = put(spec -> spec.body(levelWithExtraNode), MOVE_LEVEL_AFTER_URL);

        return response.getBody().as(ArrFaLevelWithExtraNode.class);
    }

    /**
     * Přesune jeden uzel za druhý.
     *
     * @param movedLevel přesouvaný uzel
     * @param targetLevel uzel za který se má vložit přesouvaný uzel
     * @param version verze archivní pomůcky
     *
     * @return přesunutý uzel
     */
    private ArrFaLevelWithExtraNode deleteLevel(ArrFaLevel levelToDelete, ArrFaVersion version) {
        ArrFaLevelWithExtraNode levelWithExtraNode = new ArrFaLevelWithExtraNode();
        levelWithExtraNode.setFaLevel(levelToDelete);
        levelWithExtraNode.setExtraNode(levelToDelete.getParentNode());
        levelWithExtraNode.setFaVersionId(version.getFaVersionId());

        Response response = put(spec -> spec.body(levelWithExtraNode), DELETE_LEVEL_URL);

        return response.getBody().as(ArrFaLevelWithExtraNode.class);
    }

    /**
     * Uzavření verze archivní pomůcky a následné kontroly.
     *
     * @param findingAid archivní pomůcka
     */
    private void testApproveFindingAidVersion(ArrFindingAid findingAid) {
        ArrFaVersion openVersion = getFindingAidOpenVersion(findingAid);

        ArrFaVersion newOpenVersion = approveVersion(openVersion);
        Assert.notNull(newOpenVersion);
        Assert.isTrue(!openVersion.getFaVersionId().equals(newOpenVersion.getFaVersionId()));
        Assert.isTrue(newOpenVersion.getArrangementType().getArrangementTypeId().equals(arrangementType.getArrangementTypeId()));
        Assert.isTrue(newOpenVersion.getRuleSet().getRuleSetId().equals(ruleSet.getRuleSetId()));
        Assert.isTrue(newOpenVersion.getLockChange() == null);

        ArrFaVersion newOpenVersionCheck = getFindingAidOpenVersion(findingAid);
        Assert.isTrue(newOpenVersionCheck.equals(newOpenVersion));

        ArrFaVersion closedVersion = getVersionById(openVersion.getFaVersionId());
        Assert.notNull(closedVersion);
        Assert.isTrue(closedVersion.getFaVersionId().equals(openVersion.getFaVersionId()));
        Assert.isTrue(closedVersion.getLockChange() != null);

        List<ArrFaVersion> versions = getFindingAidVersions(findingAid);
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
    private List<ArrFaVersion> getFindingAidVersions(ArrFindingAid findingAid) {
        Response response = get(spec -> spec.parameter(FA_ID_ATT, findingAid.getFindingAidId()),
                GET_FINDING_AID_VERSIONS_URL);

        return Arrays.asList(response.getBody().as(ArrFaVersion[].class));
    }

    /**
     * Načte verzi archivní pomůcky přes REST volání.
     *
     * @param versionId id verze
     *
     * @return verze archivní pomůcky
     */
    private ArrFaVersion getVersionById(Integer versionId) {
        Response response = get(spec -> spec.parameter(VERSION_ID_ATT, versionId), GET_VERSION_ID_URL);

        return response.getBody().as(ArrFaVersion.class);
    }

    /**
     * Uzavře verzi archivní pomůcky.
     *
     * @param openVersion otevřená verze archivní pomůcky
     *
     * @return nová otevřená verze archivní pomůcky
     */
    private ArrFaVersion approveVersion(ArrFaVersion openVersion) {
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
    private ArrFaVersion approveVersion(ArrFaVersion openVersion, RulRuleSet ruleSet, RulArrangementType arrangementType,
            HttpStatus httpStatus) {
        Response response = put(spec -> spec.body(openVersion).
                parameter(ARRANGEMENT_TYPE_ID_ATT, arrangementType.getArrangementTypeId()).
                parameter(RULE_SET_ID_ATT, ruleSet.getRuleSetId())
                , APPROVE_VERSION_URL, httpStatus);

        if (httpStatus == HttpStatus.OK) {
            return response.getBody().as(ArrFaVersion.class);
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
    private void approveVersionWithError(ArrFaVersion openVersion) {
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
    private ArrFindingAid testUpdateFindingAid(ArrFindingAid findingAid) {
        ArrFindingAid updatedFindingAid = updateFindingAid(findingAid, TEST_UPDATE_NAME);
        testChangedFindingAid(updatedFindingAid, TEST_UPDATE_NAME, 1);

        return updatedFindingAid;
    }

    /**
     * Ajtualizace archivní pomůcky.
     *
     * @return aktualizovaná archivní pomůcka
     */
    private ArrFindingAid updateFindingAid(ArrFindingAid findingAid, String testUpdateName) {
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
    private void testChangedFindingAid(ArrFindingAid findingAid, String testName, int findingAidsCount) {
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

        ArrFaVersion openVersion = getFindingAidOpenVersion(findingAid);
        Assert.notNull(openVersion);
        Assert.isNull(openVersion.getLockChange());

        Integer nodeId = openVersion.getRootFaLevel().getNode().getNodeId();
        ArrFaLevelExt rootLevel = getLevelByNodeId(nodeId);
        Assert.notNull(rootLevel);
        Assert.isTrue(rootLevel.getNode().getNodeId().equals(nodeId));
        Assert.isNull(rootLevel.getParentNode());
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @return archivní pomůcka
     */
    private ArrFindingAid createFindingAid() {
        return createFindingAid(ruleSet, arrangementType, HttpStatus.OK);
    }
}
