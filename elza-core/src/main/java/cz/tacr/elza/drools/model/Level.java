package cz.tacr.elza.drools.model;

import java.util.List;


/**
 * Objekt uzlu pro skripty pravidel.
 *
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
    private boolean hasChildren = false;

    /**
     * Number of child nodes
     */
    private Integer childCount;
    
    /**
     * Default constructor
     */
    public Level() {
    	
    }

    /**
     * Copy constructor 
     * @param level 	source level 
     */
    public Level(Level level) {
		this.childCount = level.childCount;
		this.descItems = level.descItems;
		this.hasChildren = level.hasChildren;
		this.nodeId = level.nodeId;
		this.parent = level.parent;
	}

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

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(final boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public Integer getChildCount() {
        return childCount;
    }

    public void setChildCount(Integer childCount) {
        this.childCount = childCount;
        if(childCount!=null) {
        	this.hasChildren = (childCount.intValue() > 0);
        }
    }

}
