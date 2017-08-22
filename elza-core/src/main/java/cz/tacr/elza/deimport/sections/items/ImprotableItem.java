package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.sections.context.ContextNode;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;

public interface ImprotableItem {

	/**
	 * Imports description item. Transformed object is added to context.
	 * @see {@link ContextNode#addDescItem(ArrDescItem, ArrData)}
	 */
	void importItem(ContextNode contextNode, ImportContext importContext);
}
