package cz.tacr.elza.service.cache;

import cz.tacr.elza.domain.ArrNode;

/**
 * Restored node from cache
 * 
 * This class contains attached ArrNode
 */
public class RestoredNode extends CachedNode {

	protected ArrNode node;

	public ArrNode getNode() {
		return node;
	}

	public void setNode(ArrNode node) {
		this.node = node;
	}

}
