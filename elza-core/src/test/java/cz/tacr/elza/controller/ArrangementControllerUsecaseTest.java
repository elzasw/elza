package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.RulArrangementTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.service.ArrMoveLevelService;


/**
 * Testování metod z AdminController.
 *
 * @author Martin Šlapa
 * @since 16.2.2016
 */
public class ArrangementControllerUsecaseTest extends AbstractControllerTest {

    public static final Logger logger = LoggerFactory.getLogger(ArrangementControllerUsecaseTest.class);

    public static final String NAME_AP = "UseCase";
    public static final String RENAME_AP = "Renamed UseCase";

    @Test
    public void usecaseTest() {

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
        deleteDescItem(descItemUpdated, findingAidVersion, rootNode);

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

        Assert.notNull(treeData.getNodes(), "Musí existovat root node");
        Assert.isTrue(treeData.getNodes().size() == 1, "Musí existovat pouze root node");

        TreeNodeClient rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        // přidání prvního levelu pod root
        ArrangementController.NodeWithParent newLevel1 = addLevel(ArrMoveLevelService.AddLevelDirection.CHILD,
                findingAidVersion, rootNode, rootNode);

        Assert.isTrue(newLevel1.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel1.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        rootNode = newLevel1.getParentNode();

        // přidání druhého levelu pod root
        ArrangementController.NodeWithParent newLevel2 = addLevel(ArrMoveLevelService.AddLevelDirection.CHILD,
                findingAidVersion, rootNode, rootNode);

        Assert.isTrue(newLevel2.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel2.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        rootNode = newLevel2.getParentNode();

        // přidání třetího levelu na první pozici pod root
        ArrangementController.NodeWithParent newLevel3 = addLevel(ArrMoveLevelService.AddLevelDirection.BEFORE,
                findingAidVersion, newLevel1.getNode(), rootNode);

        Assert.isTrue(newLevel3.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel3.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        rootNode = newLevel3.getParentNode();

        // přidání uzlu za první uzel pod root (za child3)
        ArrangementController.NodeWithParent newLevel4 = addLevel(ArrMoveLevelService.AddLevelDirection.AFTER,
                findingAidVersion, newLevel3.getNode(), rootNode);

        Assert.isTrue(newLevel4.getParentNode().getId().equals(rootNode.getId()),
                "Rodič nového uzlu musí být root");
        Assert.isTrue(!newLevel4.getParentNode().getVersion().equals(rootNode.getVersion()),
                "Verze root uzlu musí být povýšena");

        rootNode = newLevel4.getParentNode();

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
     * Převod TreeNodeClient na ArrNodeVO.
     *
     * @param treeNodeClients seznam uzlů stromu
     * @return převedený seznam uzlů stromu
     */
    private List<ArrNodeVO> convertTreeNodes(final Collection<TreeNodeClient> treeNodeClients) {
        List<ArrNodeVO> nodes = new ArrayList<>(treeNodeClients.size());
        for (TreeNodeClient treeNodeClient : treeNodeClients) {
            nodes.add(convertTreeNode(treeNodeClient));
        }
        return nodes;
    }

    /**
     * Převod TreeNodeClient na ArrNodeVO.
     *
     * @param treeNodeClient uzel stromu
     * @return převedený uzel stromu
     */
    private ArrNodeVO convertTreeNode(final TreeNodeClient treeNodeClient) {
        ArrNodeVO rootNode = new ArrNodeVO();
        BeanUtils.copyProperties(treeNodeClient, rootNode);
        return rootNode;
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
     * Nalezení otevřené verze AP.
     *
     * @param findingAid archivní pomůcka
     * @return otevřená verze AP
     */
    private ArrFindingAidVersionVO getOpenVersion(final ArrFindingAidVO findingAid) {
        Assert.notNull(findingAid);

        List<ArrFindingAidVO> findingAids = getFindingAids();

        for (ArrFindingAidVO findingAidFound : findingAids) {
            if (findingAidFound.getId().equals(findingAid.getId())) {
                for (ArrFindingAidVersionVO findingAidVersion : findingAidFound.getVersions()) {
                    if (findingAidVersion.getLockDate() == null) {
                        return findingAidVersion;
                    }
                }
            }
        }

        return null;
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

}
