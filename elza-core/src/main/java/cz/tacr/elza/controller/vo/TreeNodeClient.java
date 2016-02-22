package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * Data uzlu stromu odesílané klientovi pro strom fa.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 12.01.2016
 */
public class TreeNodeClient {

    /**
     * Nodeid uzlu.
     */
    private Integer id;

    /**
     * Hloubka zanoření ve stromu.
     */
    private Integer depth;

    /**
     * Název uzlu.
     */
    private String name;

    private String accordionLeft;

    private String accordionRight;

    /** Ikonka. */
    private String icon;

    /**
     * True - uzel má další potomky, false - uzel nemá další potomky.
     */
    private boolean hasChildren;

    /**
     * Referenční označení. Od kořene k uzlu.
     */
    @JsonIgnore
    private Integer[] referenceMarkInt;

    private String[] referenceMark;

    /**
     * Verze uzlu.
     */
    private Integer version;

    /**
     * Informace o stavu JP.
     */
    private NodeConformityVO nodeConformity;

    public TreeNodeClient() {
    }

    public TreeNodeClient(final Integer id,
                          final Integer depth,
                          final String name,
                          final boolean hasChildren,
                          final Integer[] referenceMarkInt,
                          final Integer version) {
        this.id = id;
        this.depth = depth;
        this.name = name;
        this.hasChildren = hasChildren;
        this.referenceMarkInt = referenceMarkInt;
        this.version = version;
    }


    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(final Integer depth) {
        this.depth = depth;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(final boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public Integer[] getReferenceMarkInt() {
        return referenceMarkInt;
    }

    public void setReferenceMarkInt(final Integer[] referenceMarkInt) {
        this.referenceMarkInt = referenceMarkInt;
    }

    public String[] getReferenceMark() {
        return referenceMark;
    }

    public void setReferenceMark(final String[] referenceMark) {
        this.referenceMark = referenceMark;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public NodeConformityVO getNodeConformity() {
        return nodeConformity;
    }

    public void setNodeConformity(final NodeConformityVO nodeConformity) {
        this.nodeConformity = nodeConformity;
    }

    public String getAccordionLeft() {
        return accordionLeft;
    }

    public void setAccordionLeft(String accordionLeft) {
        this.accordionLeft = accordionLeft;
    }

    public String getAccordionRight() {
        return accordionRight;
    }

    public void setAccordionRight(String accordionRight) {
        this.accordionRight = accordionRight;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
