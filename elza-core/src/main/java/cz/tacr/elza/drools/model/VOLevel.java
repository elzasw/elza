package cz.tacr.elza.drools.model;

import java.util.LinkedList;
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
     * Seznam potomků.
     */
    private List<VOLevel> childs;

    /**
     * Přidá potomka.
     *
     * @param child potomek
     */
    public void addChild(final VOLevel child) {
        if (childs == null) {
            childs = new LinkedList<>();
        }
        childs.add(child);
    }

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

    public List<VOLevel> getChilds() {
        return childs;
    }

    public void setChilds(final List<VOLevel> childs) {
        this.childs = childs;
    }
}
