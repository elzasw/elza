package cz.tacr.elza.controller.vo;

import java.util.Collection;
import java.util.Set;


/**
 * Data položek ve stromu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 11.01.2016
 */
public class TreeData {

    /**
     * Seznam položek ve stromu.
     */
    private Collection<TreeNodeClient> nodes;

    /**
     * Množina všech uzlů, které musejí být rozbaleny pro zviditelnění vybraných uzlů.
     */
    private Set<Integer> expandedIdsExtension;

    public TreeData() {
    }

    public TreeData(final Collection<TreeNodeClient> nodes, final Set<Integer> expandedIdsExtension) {
        this.nodes = nodes;
        this.expandedIdsExtension = expandedIdsExtension;
    }

    public Collection<TreeNodeClient> getNodes() {
        return nodes;
    }

    public void setNodes(final Collection<TreeNodeClient> nodes) {
        this.nodes = nodes;
    }

    public Set<Integer> getExpandedIdsExtension() {
        return expandedIdsExtension;
    }

    public void setExpandedIdsExtension(final Set<Integer> expandedIdsExtension) {
        this.expandedIdsExtension = expandedIdsExtension;
    }
}
