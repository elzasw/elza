package cz.tacr.elza.drools.model;

import java.util.List;


/**
 * Objekt uzlu pro skripty pravidel.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 1.12.2015
 */
public class VOLevel {

    /**
     * Id nodu.
     */
    private Integer nodeId;
    /**
     * Seznam atributů.
     */
    private List<DescItemVO> descItems;

    /**
     * Rodičovský uzel.
     */
    private VOLevel parent;

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

    public List<DescItemVO> getDescItems() {
        return descItems;
    }

    public void setDescItems(final List<DescItemVO> descItems) {
        this.descItems = descItems;
    }

    public VOLevel getParent() {
        return parent;
    }

    public void setParent(final VOLevel parent) {
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
