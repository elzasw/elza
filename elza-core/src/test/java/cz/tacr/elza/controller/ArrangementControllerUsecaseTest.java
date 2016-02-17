package cz.tacr.elza.controller;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.RulArrangementTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
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
        List<TreeNodeClient> levels = createLevels(findingAidVersion);

        // přesunutí && smazání uzlů
        moveAndDeleteLevels(levels, findingAidVersion);

        // atributy
        // TODO

        // vazba na rejstříky
        // TODO

    }

    private void moveAndDeleteLevels(final List<TreeNodeClient> levels,
                                     final ArrFindingAidVersionVO findingAidVersion) {

        // přesun druhého uzlu před první

    }

    private List<TreeNodeClient> createLevels(final ArrFindingAidVersionVO findingAidVersion) {

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(findingAidVersion.getId());
        TreeData treeData = getFaTree(input);

        Assert.notNull(treeData.getNodes(), "Musí existovat root node");
        Assert.isTrue(treeData.getNodes().size() == 1, "Musí existovat pouze root node");

        TreeNodeClient rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = new ArrNodeVO();
        BeanUtils.copyProperties(rootTreeNodeClient, rootNode);

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

        return Lists.newArrayList(treeData.getNodes());
    }

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

    private ArrFindingAidVO updatedFindingAid(final ArrFindingAidVO findingAid) {
        findingAid.setName(RENAME_AP);
        ArrFindingAidVO updatedFindingAid = updateFindingAid(findingAid);
        Assert.isTrue(RENAME_AP.equals(updatedFindingAid.getName()), "Jméno AP musí být stejné");
        return updatedFindingAid;
    }

    private ArrFindingAidVO createdFindingAid() {
        ArrFindingAidVO findingAid = createFindingAid(NAME_AP);
        Assert.notNull(findingAid);
        return findingAid;
    }

}
