package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.service.ArrMoveLevelService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * @author Martin Šlapa
 * @since 16.2.2016
 */
public class ArrangementControllerTest extends AbstractControllerTest {

    public static final Logger logger = LoggerFactory.getLogger(ArrangementControllerTest.class);

    public static final String STORAGE_NUMBER = "Test 123";
    public static final String STORAGE_NUMBER_CHANGE = "Test 321";

    public static final String NAME_AP = "UseCase ščřžý";
    public static final String RENAME_AP = "Renamed UseCase";

    @Test
    public void arrangementTest() {

        // vytvoření
        ArrFindingAidVO findingAid = createdFindingAid();

        // přejmenování
        findingAid = updatedFindingAid(findingAid);

        ArrFindingAidVersionVO findingAidVersion = getOpenVersion(findingAid);

        // uzavření verze
        findingAidVersion = approvedVersion(findingAidVersion);

        // vytvoření uzlů
        List<ArrNodeVO> nodes = createLevels(findingAidVersion);

        // přesunutí && smazání uzlů
        moveAndDeleteLevels(nodes, findingAidVersion);

        // atributy
        attributeValues(findingAidVersion);

        // validace
        validateVersion(findingAidVersion);

        // všechny formuláře / stromy / ...
        forms(findingAidVersion);

    }

    /**
     * Zavolání metod pro formuláře atd...
     *
     * @param findingAidVersion verze archivní pomůcky
     */
    private void forms(final ArrFindingAidVersionVO findingAidVersion) {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        TreeData treeData = getFaTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);


        ArrangementController.FaTreeNodesParam inputFa = new ArrangementController.FaTreeNodesParam();
        inputFa.setVersionId(findingAidVersion.getId());
        inputFa.setNodeIds(Arrays.asList(rootNode.getId()));
        List<TreeNodeClient> faTreeNodes = getFaTreeNodes(inputFa);
        Assert.notEmpty(faTreeNodes);

        List<TreeNodeClient> nodeParents = getNodeParents(rootNode.getId(), findingAidVersion.getId());
        Assert.notNull(nodeParents);

        ArrangementController.NodeFormDataNewVO nodeFormData = getNodeFormData(rootNode.getId(),
                findingAidVersion.getId());
        Assert.notNull(nodeFormData.getNode());
        Assert.notEmpty(nodeFormData.getGroups());
        Assert.notEmpty(nodeFormData.getTypeGroups());

        ArrangementController.NodeFormsDataVO nodeFormsData = getNodeFormsData(findingAidVersion.getId(), rootNode.getId());
        Assert.notEmpty(nodeFormsData.getForms());

        nodeFormsData = getNodeWithAroundFormsData(findingAidVersion.getId(), nodes.get(1).getId(), 5);
        Assert.notEmpty(nodeFormsData.getForms());

        ArrangementController.IdsParam idsParamNodes = new ArrangementController.IdsParam();
        idsParamNodes.setVersionId(findingAidVersion.getId());
        idsParamNodes.setIds(Arrays.asList(nodes.get(1).getId()));
        List<TreeNodeClient> treeNodeClients = getNodes(idsParamNodes);
        Assert.notEmpty(treeNodeClients);

        ArrangementController.IdsParam idsParamFa = new ArrangementController.IdsParam();
        idsParamFa.setIds(Arrays.asList(findingAidVersion.getId()));

        List<ArrFindingAidVO> findingAidsByVersionIds = getFindingAidsByVersionIds(idsParamFa);
        Assert.notEmpty(findingAidsByVersionIds);
    }

    /**
     * Zavolání metod pro zjištění validací.
     *
     * @param findingAidVersion verze archivní pomůcky
     */
    protected void validateVersion(final ArrFindingAidVersionVO findingAidVersion) {
        List<ArrangementController.VersionValidationItem> items = validateVersion(findingAidVersion.getId());
        Assert.notNull(items);
        validateVersionCount(findingAidVersion.getId());
    }

    /**
     * Operace s atributy.
     *
     * @param findingAidVersion verze archivní pomůcky
     */
    private void attributeValues(final ArrFindingAidVersionVO findingAidVersion) {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        TreeData treeData = getFaTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);

        // vytvoření hodnoty
        RulDescItemTypeExtVO type = findDescItemTypeByCode("ZP2015_TITLE");
        ArrDescItemVO descItem = buildDescItem(type.getCode(), null, "value", null, null);
        ArrangementController.DescItemResult descItemResult = createDescItem(descItem, findingAidVersion, rootNode,
                type);
        rootNode = descItemResult.getNode();
        ArrDescItemVO descItemCreated = descItemResult.getDescItem();

        Assert.notNull(((ArrDescItemTextVO) descItem).getValue()
                .equals(((ArrDescItemTextVO) descItemCreated).getValue()));
        Assert.notNull(descItemCreated.getPosition());
        Assert.notNull(descItemCreated.getDescItemObjectId());

        // aktualizace hodnoty
        ((ArrDescItemTextVO) descItemCreated).setValue("update value");
        descItemResult = updateDescItem(descItemCreated, findingAidVersion, rootNode, true);
        rootNode = descItemResult.getNode();
        ArrDescItemVO descItemUpdated = descItemResult.getDescItem();

        Assert.isTrue(descItemUpdated.getDescItemObjectId().equals(descItemCreated.getDescItemObjectId()));
        Assert.isTrue(descItemUpdated.getPosition().equals(descItemCreated.getPosition()));
        Assert.isTrue(!descItemUpdated.getId().equals(descItemCreated.getId()));
        Assert.isTrue(((ArrDescItemTextVO) descItemUpdated).getValue()
                .equals(((ArrDescItemTextVO) descItemCreated).getValue()));

        // odstranění hodnoty
        descItemResult = deleteDescItem(descItemUpdated, findingAidVersion, rootNode);
        rootNode = descItemResult.getNode();

        // vytvoření další hodnoty
        type = findDescItemTypeByCode("ZP2015_TITLE");
        descItem = buildDescItem(type.getCode(), null, "value", null, null);
        descItemResult = createDescItem(descItem, findingAidVersion, rootNode, type);
        rootNode = descItemResult.getNode();
        descItemCreated = descItemResult.getDescItem();

        // fulltext
        fulltextTest(findingAidVersion);

        descItemResult = deleteDescItemsByType(findingAidVersion.getId(),
                rootNode.getId(), rootNode.getVersion(), type.getId());
        rootNode = descItemResult.getNode();

        ArrNodeVO node = nodes.get(1);

        ArrangementController.DescriptionItemParam param = new ArrangementController.DescriptionItemParam();
        param.setVersionId(findingAidVersion.getId());
        param.setNode(node);
        param.setDirection(DirectionLevel.ROOT);
        getDescriptionItemTypesForNewLevel(false, param);

        // vytvoření další hodnoty - vícenásobné
        type = findDescItemTypeByCode("ZP2015_OTHER_ID");
        RulDescItemSpecExtVO spec = findDescItemSpecByCode("ZP2015_OTHERID_CJ", type);
        descItem = buildDescItem(type.getCode(), spec.getCode(), "1", 1, null);
        descItemResult = createDescItem(descItem, findingAidVersion, node, type);
        node = descItemResult.getNode();

        descItem = buildDescItem(type.getCode(), spec.getCode(), "2", 1, null);
        descItemResult = createDescItem(descItem, findingAidVersion, node, type);
        node = descItemResult.getNode();

        descItem = buildDescItem(type.getCode(), spec.getCode(), "3", 1, null);
                descItemResult = createDescItem(descItem, findingAidVersion, node, type);
        node = descItemResult.getNode();
        descItemCreated = descItemResult.getDescItem();

        ((ArrDescItemStringVO) descItemCreated).setValue("3x");
        descItemCreated.setPosition(5);
        descItemResult = updateDescItem(descItemCreated, findingAidVersion, node, true);
        node = descItemResult.getNode();

        ArrangementController.CopySiblingResult copySiblingResult =
                copyOlderSiblingAttribute(findingAidVersion.getId(), type.getId(), nodes.get(2));

        type = findDescItemTypeByCode("ZP2015_UNIT_DATE");
        descItem = buildDescItem(type.getCode(), null, "1920", 1, null);
        descItemResult = createDescItem(descItem, findingAidVersion, node, type);
        node = descItemResult.getNode();

        type = findDescItemTypeByCode("ZP2015_LEGEND");
        descItem = buildDescItem(type.getCode(), null, "legenda", 1, null);
        descItemResult = createDescItem(descItem, findingAidVersion, node, type);
        node = descItemResult.getNode();

        type = findDescItemTypeByCode("ZP2015_POSITION");
        descItem = buildDescItem(type.getCode(), null, "1e;20x", 1, null);
        descItemResult = createDescItem(descItem, findingAidVersion, node, type);
        node = descItemResult.getNode();

        type = findDescItemTypeByCode("ZP2015_COLL_EXTENT_LENGTH");
        descItem = buildDescItem(type.getCode(), null, BigDecimal.valueOf(20.5), 1, null);
        descItemResult = createDescItem(descItem, findingAidVersion, node, type);
        node = descItemResult.getNode();

}

    /**
     * Přesunutí a smazání levelů
     *
     * @param nodes             založené uzly (1. je root)
     * @param findingAidVersion verze archivní pomůcky
     */
    private void moveAndDeleteLevels(final List<ArrNodeVO> nodes,
                                     final ArrFindingAidVersionVO findingAidVersion) {
        ArrNodeVO rootNode = nodes.get(0);
        TreeNodeClient parentNode;
        // přesun druhého uzlu před první
        moveLevelBefore(findingAidVersion, nodes.get(1), rootNode, Arrays.asList(nodes.get(2)), rootNode);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        input.setNodeId(rootNode.getId());
        TreeData treeData = getFaTree(input);
        List<ArrNodeVO> newNodes = convertTreeNodes(treeData.getNodes());

        // kontrola přesunu
        Assert.isTrue(newNodes.size() == 4);
        Assert.isTrue(newNodes.get(0).getId().equals(nodes.get(2).getId()));
        Assert.isTrue(newNodes.get(1).getId().equals(nodes.get(1).getId()));
        Assert.isTrue(newNodes.get(2).getId().equals(nodes.get(3).getId()));
        Assert.isTrue(newNodes.get(3).getId().equals(nodes.get(4).getId()));

        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // přesun druhého uzlu pod první
        moveLevelUnder(findingAidVersion, newNodes.get(0), rootNode, Arrays.asList(newNodes.get(1)), rootNode);

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        input.setNodeId(newNodes.get(0).getId());
        treeData = getFaTree(input);
        List<ArrNodeVO> newNodes2 = convertTreeNodes(treeData.getNodes());

        // kontrola přesunu
        Assert.isTrue(newNodes2.size() == 1);
        Assert.isTrue(newNodes2.get(0).getId().equals(newNodes.get(1).getId()));

        rootNode.setVersion(rootNode.getVersion() + 1); // zvýšení verze root

        // smazání druhého uzlu v první úrovni
        ArrangementController.NodeWithParent nodeWithParent = deleteLevel(findingAidVersion, newNodes.get(2), rootNode);

        Assert.isTrue(nodeWithParent.getNode().getId().equals(newNodes.get(2).getId()));
        Assert.isTrue(nodeWithParent.getParentNode().getId().equals(rootNode.getId()));

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        input.setNodeId(rootNode.getId());
        treeData = getFaTree(input);
        List<ArrNodeVO> newNodes3 = convertTreeNodes(treeData.getNodes());

        Assert.isTrue(newNodes3.size() == 2);
        Assert.isTrue(newNodes3.get(0).getId().equals(newNodes.get(0).getId()));
        Assert.isTrue(newNodes3.get(1).getId().equals(newNodes.get(3).getId()));

        rootNode.setVersion(rootNode.getVersion() + 1);

        // přidání třetího levelu na první pozici pod root
        ArrangementController.NodeWithParent newLevel5 = addLevel(ArrMoveLevelService.AddLevelDirection.CHILD,
                findingAidVersion, rootNode, rootNode, null);

        parentNode = newLevel5.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        input.setNodeId(rootNode.getId());
        treeData = getFaTree(input);
        List<ArrNodeVO> newNodes4 = convertTreeNodes(treeData.getNodes());

        Assert.isTrue(newNodes4.size() == 3);

        // přesun posledního za první
        moveLevelAfter(findingAidVersion, newNodes4.get(2), rootNode, Arrays.asList(newNodes4.get(0)), rootNode);
    }

    /**
     * Vytvoření levelů v archivní pomůcce.
     *
     * @param findingAidVersion verze archivní pomůcky
     * @return vytvořené levely
     */
    private List<ArrNodeVO> createLevels(final ArrFindingAidVersionVO findingAidVersion) {

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        TreeData treeData = getFaTree(input);
        TreeNodeClient parentNode;

        Assert.notNull(treeData.getNodes(), "Musí existovat root node");
        Assert.isTrue(treeData.getNodes().size() == 1, "Musí existovat pouze root node");

        TreeNodeClient rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        // přidání prvního levelu pod root
        ArrangementController.NodeWithParent newLevel1 = addLevel(ArrMoveLevelService.AddLevelDirection.CHILD,
                findingAidVersion, rootNode, rootNode, "Série");

        Assert.isTrue(newLevel1.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel1.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        parentNode = newLevel1.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání druhého levelu pod root
        ArrangementController.NodeWithParent newLevel2 = addLevel(ArrMoveLevelService.AddLevelDirection.CHILD,
                findingAidVersion, rootNode, rootNode, null);

        Assert.isTrue(newLevel2.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel2.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        parentNode = newLevel2.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání třetího levelu na první pozici pod root
        ArrangementController.NodeWithParent newLevel3 = addLevel(ArrMoveLevelService.AddLevelDirection.BEFORE,
                findingAidVersion, newLevel1.getNode(), rootNode, null);

        Assert.isTrue(newLevel3.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel3.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        parentNode = newLevel3.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        // přidání uzlu za první uzel pod root (za child3)
        ArrangementController.NodeWithParent newLevel4 = addLevel(ArrMoveLevelService.AddLevelDirection.AFTER,
                findingAidVersion, newLevel3.getNode(), rootNode, null);

        Assert.isTrue(newLevel4.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel4.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        parentNode = newLevel4.getParentNode();
        rootNode.setId(parentNode.getId());
        rootNode.setVersion(parentNode.getVersion());

        input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        input.setNodeId(rootNode.getId());
        treeData = getFaTree(input);

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
     * @param findingAidVersion verze archivní pomůcky
     * @return nová verze archivní pomůcky
     */
    private ArrFindingAidVersionVO approvedVersion(final ArrFindingAidVersionVO findingAidVersion) {
        Assert.notNull(findingAidVersion);
        List<RulRuleSetVO> ruleSets = getRuleSets();
        RulArrangementTypeVO arrangementType = ruleSets.get(0).getArrangementTypes().get(1);
        ArrFindingAidVersionVO newFindingAidVersion = approveVersion(findingAidVersion, arrangementType);

        Assert.isTrue(!findingAidVersion.getId().equals(newFindingAidVersion.getId()),
                "Musí být odlišné identifikátory");
        Assert.isTrue(!findingAidVersion.getArrangementType().getId().equals(
                newFindingAidVersion.getArrangementType().getId()), "Musí být odlišné typy výstupu");

        return newFindingAidVersion;
    }

    /**
     * Upravení AP.
     *
     * @param findingAid archivní pomůcka
     */
    private ArrFindingAidVO updatedFindingAid(final ArrFindingAidVO findingAid) {
        findingAid.setName(RENAME_AP);
        ArrFindingAidVO updatedFindingAid = updateFindingAid(findingAid);
        Assert.isTrue(RENAME_AP.equals(updatedFindingAid.getName()), "Jméno AP musí být stejné");
        return updatedFindingAid;
    }

    /**
     * Vytvoření AP.
     */
    private ArrFindingAidVO createdFindingAid() {
        ArrFindingAidVO findingAid = createFindingAid(NAME_AP);
        Assert.notNull(findingAid);
        return findingAid;
    }

    @Test
    public void packetsTest() {

        ArrFindingAidVO findingAid = createFindingAid("Packet Test AP");

        List<RulPacketTypeVO> packetTypes = getPacketTypes();

        Assert.notEmpty(packetTypes, "Typy obalů nemůžou být prázdné");

        ArrPacketVO packet = new ArrPacketVO();

        packet.setPacketTypeId(packetTypes.get(0).getId());
        packet.setStorageNumber(STORAGE_NUMBER);
        packet.setInvalidPacket(false);

        ArrPacketVO insertedPacket = insertPacket(findingAid, packet);

        Assert.notNull(insertedPacket);
        Assert.notNull(insertedPacket.getId());
        Assert.isTrue(packet.getInvalidPacket().equals(insertedPacket.getInvalidPacket()));
        Assert.isTrue(packet.getPacketTypeId().equals(insertedPacket.getPacketTypeId()));
        Assert.isTrue(packet.getStorageNumber().equals(insertedPacket.getStorageNumber()));

        List<ArrPacketVO> packets = getPackets(findingAid);
        Assert.isTrue(packets.size() == 1);
        packet = packets.get(0);

        Assert.isTrue(packet.getId().equals(insertedPacket.getId()));

        packet.setStorageNumber(STORAGE_NUMBER_CHANGE);
        ArrPacketVO updatedPacket = updatePacket(findingAid, packet);

        Assert.isTrue(packet.getId().equals(updatedPacket.getId()));

        ArrPacketVO deactivatePacket = deactivatePacket(findingAid, updatedPacket);

        Assert.notNull(deactivatePacket);
        Assert.isTrue(deactivatePacket.getInvalidPacket().equals(true));
    }

    @Test
    public void calendarsTest() {
        List<ArrCalendarTypeVO> calendarTypes = getCalendarTypes();
        Assert.notEmpty(calendarTypes);
    }

    private void fulltextTest(final ArrFindingAidVersionVO findingAidVersion) {

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        TreeData treeData = getFaTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);

        List<ArrangementController.TreeNodeFulltext> fulltext = fulltext(findingAidVersion, rootNode, "value",
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

        ArrFindingAidVO findingAid = createFindingAid("RegisterLinks Test AP");

        ArrFindingAidVersionVO findingAidVersion = getOpenVersion(findingAid);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        TreeData treeData = getFaTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);

        List<RegRegisterTypeVO> types = getRecordTypes();
        List<RegScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();

        RegRecordVO record = new RegRecordVO();

        record.setRegisterTypeId(getHierarchicalRegRegisterType(types, null).getId());

        record.setCharacteristics("Ja jsem regRecordA");

        record.setRecord("RegRecordA");

        record.setScopeId(scopeId);

        record.setHierarchical(true);
        record.setAddRecord(true);

        record = createRecord(record);

        ArrNodeRegisterVO nodeRegister = new ArrNodeRegisterVO();

        nodeRegister.setValue(record.getRecordId());
        nodeRegister.setNodeId(rootNode.getId());
        nodeRegister.setNode(rootNode);

        ArrNodeRegisterVO createdLink = createRegisterLinks(findingAidVersion.getId(), rootNode.getId(), nodeRegister);

        Assert.notNull(createdLink);

        List<ArrNodeRegisterVO> registerLinks = findRegisterLinks(findingAidVersion.getId(), rootNode.getId());
        Assert.notEmpty(registerLinks);

        ArrangementController.NodeRegisterDataVO registerLinksForm = findRegisterLinksForm(findingAidVersion.getId(),
                rootNode.getId());

        Assert.notNull(registerLinksForm.getNode());
        Assert.notEmpty(registerLinksForm.getNodeRegisters());

        ArrNodeRegisterVO updatedLink = updateRegisterLinks(findingAidVersion.getId(), rootNode.getId(), createdLink);

        Assert.isTrue(!createdLink.getId().equals(updatedLink.getId()));

        ArrNodeRegisterVO deletedLink = deleteRegisterLinks(findingAidVersion.getId(), rootNode.getId(), updatedLink);

        Assert.isTrue(updatedLink.getId().equals(deletedLink.getId()));
    }

}
