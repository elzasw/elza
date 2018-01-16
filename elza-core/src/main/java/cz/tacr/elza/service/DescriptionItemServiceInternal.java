package cz.tacr.elza.service;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.DescItemRepository;

/**
 * Internal service for description items.
 *
 * No permissions are checked on any operation.
 */
@Service
public class DescriptionItemServiceInternal {

	private final DescItemRepository descItemRepository;

	@Autowired
	public DescriptionItemServiceInternal(DescItemRepository descItemRepository) {
		this.descItemRepository = descItemRepository;
	}

	/**
	 * Return list of description items for the node.
	 *
	 * Description items are returned including data.
	 *
	 * Method is using NodeChage to read current values.
	 *
	 * @param lockChange
	 *            Change for which items are returned. lockChange cannot be null
	 * @param node
	 * @return
	 */
	public List<ArrDescItem> getDescItems(final ArrChange lockChange, final ArrNode node) {
		Validate.notNull(lockChange);
		List<ArrDescItem> itemList;
		itemList = descItemRepository.findByNodeAndChange(node, lockChange);
		return itemList;
	}
}