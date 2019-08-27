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
    private Collection<TreeNodeVO> nodes;

    /**
     * Množina všech uzlů, které musejí být rozbaleny pro zviditelnění vybraných uzlů.
     */
    private Set<Integer> expandedIdsExtension;

    /**
     * Příznak, zda-li mám oprávnění pořádat v celém AS.
     * Pokud je true, nevyhodnocuje se pro jednotlivé JP v {@link TreeNodeVO}
     */
    private boolean fullArrPerm;

    public TreeData() {
    }

    public TreeData(final Collection<TreeNodeVO> nodes, final Set<Integer> expandedIdsExtension, final boolean fullArrPerm) {
        this.nodes = nodes;
        this.expandedIdsExtension = expandedIdsExtension;
        this.fullArrPerm = fullArrPerm;
    }

    public Collection<TreeNodeVO> getNodes() {
        return nodes;
    }

    public void setNodes(final Collection<TreeNodeVO> nodes) {
        this.nodes = nodes;
    }

    public Set<Integer> getExpandedIdsExtension() {
        return expandedIdsExtension;
    }

    public void setExpandedIdsExtension(final Set<Integer> expandedIdsExtension) {
        this.expandedIdsExtension = expandedIdsExtension;
    }

    public boolean isFullArrPerm() {
        return fullArrPerm;
    }

    public void setFullArrPerm(final boolean fullArrPerm) {
        this.fullArrPerm = fullArrPerm;
    }
}
