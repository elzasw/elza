package cz.tacr.elza.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.TreeNode;


/**
 * Třída pro procházení cache stromu uzlů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 17.03.2016
 */
@Service
public class LevelTreeCacheWalker {

    /**
     * Projde stromem metodou do hloubky a vrací seřazenou množinu id podle úrovně a pozice ve stromu.
     *
     * @param rootNode uzel, odkud začíná prohledávání stromu
     * @return seřazené id stromu
     */
    public LinkedHashSet<Integer> walkThroughDFS(final TreeNode rootNode) {
        LinkedHashSet<Integer> resultTable = new LinkedHashSet<>();
        walkThroughDFS(rootNode, resultTable);

        return resultTable;
    }

    /**
     * Rekurze pro průchod stromem do hloubky.
     *
     * @param node       uzel, který bude přidán do seznamu
     * @param nodesTable seznam prošlých uzlů
     */
    private void walkThroughDFS(final TreeNode node, final Set<Integer> nodesTable) {

        nodesTable.add(node.getId());

        node.getChilds().forEach(child -> {
            walkThroughDFS(child, nodesTable);
        });
    }


}
