package cz.tacr.elza.service;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Internal service for arrangement
 * 
 * Service implements operations for arrangement.
 * 
 * No permissions are checked on any operation.
 */
@Service
public class ArrangementServiceInternal {

	private final DescItemRepository descItemRepository;

	private final NodeCacheService nodeCache;

	@Autowired
	public ArrangementServiceInternal(DescItemRepository descItemRepository, NodeCacheService nodeCache) {
		this.descItemRepository = descItemRepository;
		this.nodeCache = nodeCache;
	}

	/**
	 * Získání hodnot atributů podle verze AP a uzlu
	 *
	 * Method is using NodeCache service to read current values
	 * 
	 * @param version
	 *            verze AP
	 * @param node
	 *            uzel
	 * @return seznam hodnot atributů
	 */
	public List<ArrDescItem> getDescItems(final ArrFundVersion version, final ArrNode node) {
		Validate.notNull(node.getNodeId());
		return getDescItems(version.getLockChange(), node.getNodeId());
	}

	/**
	 * Return list of description items for the node
	 * 
	 * Method is using NodeChage to read current values.
	 * 
	 * @param lockChange
	 *            Change for which items are returned. If lockChange is null
	 *            then current items are returned.
	 * @param nodeId
	 * @return
	 */
	public List<ArrDescItem> getDescItems(final ArrChange lockChange, final int nodeId) {
		List<ArrDescItem> itemList;

		if (lockChange == null) {
			CachedNode cachedNode = nodeCache.getNode(nodeId);
			itemList = cachedNode.getDescItems();
		} else {
			itemList = descItemRepository.findByNodeIdAndChange(nodeId, lockChange);
		}

		return itemList;
	}
}
