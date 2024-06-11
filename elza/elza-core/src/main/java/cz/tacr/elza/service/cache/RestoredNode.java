package cz.tacr.elza.service.cache;

import cz.tacr.elza.domain.ArrNode;

/**
 * Restored node from cache
 * 
 * This class contains attached ArrNode
 */
public class RestoredNode extends CachedNode {

	protected ArrNode node;

	public RestoredNode() {
	}

	public RestoredNode(RestoredNode source) {
	    node = source.getNode();
	    nodeId = source.getNodeId();
	    uuid = source.getUuid();
	    if (source.getDaoLinks() != null) {
	        addDaoLinks(source.getDaoLinks());
	    }
	    if (source.getDescItems() != null) {
	        addDescItems(source.getDescItems());
	    }
	    if (source.getInhibitedItems() != null) {
	    	addInhibitedItems(source.getInhibitedItems());
	    }
	    if (source.getNodeExtensions() != null) {
	        addNodeExtensions(source.getNodeExtensions());
	    }
	}

	public ArrNode getNode() {
		return node;
	}

	public void setNode(ArrNode node) {
		this.node = node;
	}

}
