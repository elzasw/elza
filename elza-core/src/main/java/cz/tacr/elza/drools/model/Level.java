package cz.tacr.elza.drools.model;

import java.util.List;


/**
 * Objekt uzlu pro skripty pravidel.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 1.12.2015
 */
public class Level {

    /**
     * Id nodu.
     */
    private Integer nodeId;
    /**
     * Seznam atributů.
     */
    private List<DescItem> descItems;

    /**
     * Rodičovský uzel.
     */
    private Level parent;

    /**
     * Má uzel potomky?
     */
    private boolean hasChilds = false;

    /**
     * Počet potomků.
     */
    private int childCount = 0;

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public List<DescItem> getDescItems() {
        return descItems;
    }

    public void setDescItems(final List<DescItem> descItems) {
        this.descItems = descItems;
    }

    public Level getParent() {
        return parent;
    }

    public void setParent(final Level parent) {
        this.parent = parent;
    }

    public boolean isHasChilds() {
        return hasChilds;
    }

    public void setHasChilds(final boolean hasChilds) {
        this.hasChilds = hasChilds;
    }

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(final int childCount) {
        this.childCount = childCount;
        this.hasChilds = childCount == 0 ? false : true;
    }

}
