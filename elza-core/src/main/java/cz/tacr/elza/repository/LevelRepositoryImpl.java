package cz.tacr.elza.repository;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.11.2015
 */
@Component
public class LevelRepositoryImpl implements LevelRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;


    @Override
    public List<ArrLevel> findByParentNode(final ArrNode nodeParent, @Nullable final ArrChange change) {
        if (change == null) {
            return levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(nodeParent);
        } else {
            return levelRepository.findByParentNodeOrderByPositionAsc(nodeParent, change);
        }
    }

    @Override
    public List<ArrLevel> findByNode(final ArrNode node, @Nullable final ArrChange change) {
        if (change == null) {
            return levelRepository.findByNodeAndDeleteChangeIsNull(node);
        } else {
            return levelRepository.findByNodeOrderByPositionAsc(node, change);
        }
    }

    @Override
    public Integer countChildsByParent(final ArrNode node, @Nullable final ArrChange lockChange) {
        if(lockChange == null){
            return levelRepository.countChildsByParent(node);
        }else{
            return levelRepository.countChildsByParentAndChange(node, lockChange);
        }
    }

    @Override
    public List<ArrLevel> findAllParentsByNodeAndVersion(final ArrNode node, final ArrFindingAidVersion version) {
        Assert.notNull(node);
        Assert.notNull(version);


        ArrChange lockChange = version.getLockChange();

        List<ArrLevel> parents = new LinkedList<>();

        boolean found = false;
        for (ArrLevel arrLevel : findByNode(node, lockChange)) {
            if (findParentNodesToRootByNodeId(parents, arrLevel, version.getRootLevel().getNode(), lockChange)) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalStateException(
                    "Nebyl nalezen seznam rodičů pro node " + node.getNodeId() + " ve verzi " + version.toString());
        }


        return parents;
    }

    /**
     * Rekurzivně prochází strom ke kořenu a pokud má uzel jako prarodiče daný kořenový uzel, je přidán do seznamu.
     *
     * @param toRootTree seznam rodičů hledaného uzlu
     * @param level      uzel, pro který hledáme rodiče
     * @param rootNode   kořenový uzel verze
     * @param lockChange čas uzamčení verze
     * @return true pokud má daný uzel za prarodiče kořenový uzel
     */
    private boolean findParentNodesToRootByNodeId(final List<ArrLevel> toRootTree,
                                                  final ArrLevel level,
                                                  final ArrNode rootNode,
                                                  @Nullable final ArrChange lockChange) {
        if (level.getNode().equals(rootNode)) {
            return true;
        }

        for (ArrLevel parentLevel : findByNode(level.getNodeParent(), lockChange)) {
            if (findParentNodesToRootByNodeId(toRootTree, parentLevel, rootNode, lockChange)) {
                toRootTree.add(0, parentLevel);
                return true;
            }
        }

        return false;
    }


    @Override
    public List<ArrLevel> findAllChildrenByNode(final ArrNode node, final ArrChange lockChange) {
        Assert.notNull(node);

        List<ArrLevel> children = findByParentNode(node, lockChange);

        List<ArrLevel> result = new LinkedList<ArrLevel>();
        while (!children.isEmpty()) {
            ArrLevel child = children.remove(0);
            result.add(child);

            if (lockChange == null) {
                children.addAll(
                        levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(child.getNode()));
            } else {
                children.addAll(levelRepository.findByParentNodeOrderByPositionAsc(child.getNode(), lockChange));
            }
        }

        return result;
    }


    @Override
    public boolean isLevelInRootTree(final ArrLevel level,
                                     final ArrNode rootNode,
                                     @Nullable final ArrChange lockChange) {
        if (level.getNode().equals(rootNode) || rootNode.equals(level.getNodeParent())) {
            return true;
        }


        boolean result = false;
        for (ArrLevel parentLevel : findByNode(level.getNodeParent(), lockChange)) {
            result = result || isLevelInRootTree(parentLevel, rootNode, lockChange);
        }

        return result;
    }


    @Override
    public ArrLevel findNodeInRootTreeByNodeId(final ArrNode node,
                                               final ArrNode rootNode,
                                               @Nullable final ArrChange lockChange) {
        List<ArrLevel> levelsByNode = findByNode(node, lockChange);

        if (levelsByNode.isEmpty()) {
            throw new IllegalArgumentException(
                    "Entita byla změněna nebo odstraněna. Načtěte znovu entitu a opakujte akci.");
        } else if (levelsByNode.size() == 1) {
            return levelsByNode.iterator().next();
        }


        for (ArrLevel arrFaLevel : levelsByNode) {
            if (levelRepository.isLevelInRootTree(arrFaLevel, rootNode, lockChange)) {
                return arrFaLevel;
            }
        }

        return null;
    }


    @Override
    public List<ArrLevel> findLevelsByDirection(final ArrLevel level,
                                                final ArrFindingAidVersion version,
                                                final RelatedNodeDirection direction) {
        Assert.notNull(level);
        Assert.notNull(version);
        Assert.notNull(direction);

        switch (direction) {
            case NODE:
                return Arrays.asList(level);
            case PARENTS:

                // pokud je to root level, nemuze mit rodice
                if (level.getNode().equals(version.getRootLevel().getNode())) {
                    return Arrays.asList();
                }

                return Arrays.asList(levelRepository
                        .findNodeInRootTreeByNodeId(level.getNodeParent(), version.getRootLevel().getNode(),
                                version.getLockChange()));
            case ASCENDATNS:
                return levelRepository.findAllParentsByNodeAndVersion(level.getNode(), version);
            case CHILDREN:
                return levelRepository.findByParentNode(level.getNode(), version.getLockChange());
            case DESCENDANTS:
                return levelRepository.findAllChildrenByNode(level.getNode(), version.getLockChange());
            case SIBLINGS:
                List<ArrLevel> siblings = levelRepository
                        .findByParentNode(level.getNodeParent(), version.getLockChange());
                siblings.remove(level); //chceme pouze sourozence, bez nas
                return siblings;
            case ALL:
                return levelRepository.findAllChildrenByNode(version.getRootLevel().getNode(), version.getLockChange());
            default:
                throw new NotImplementedException(
                        "Chybi implementace pro smer prohledavani stromu " + direction.name());
        }
    }
}
