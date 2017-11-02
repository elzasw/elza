package cz.tacr.elza.service;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.repository.DescItemRepository;

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

	@Autowired
	public ArrangementServiceInternal(DescItemRepository descItemRepository) {
		this.descItemRepository = descItemRepository;
	}

	/**
	 * Return list of description items for the node
	 * 
	 * Method is using NodeChage to read current values.
	 * 
	 * @param lockChange
	 *            Change for which items are returned. lockChange cannot be null
	 * @param nodeId
	 * @return
	 */
	public List<ArrDescItem> getDescItems(final ArrChange lockChange, final int nodeId) {
		Validate.notNull(lockChange);
		List<ArrDescItem> itemList;
		itemList = descItemRepository.findByNodeIdAndChange(nodeId, lockChange);
		return itemList;
	}
}
