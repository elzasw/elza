package cz.tacr.elza.service.arrangement;

import cz.tacr.elza.domain.ArrDescItem;

/**
 * Kontext davkove zmeny
 *
 */
public interface BatchChangeContext {

    void addCreatedItem(ArrDescItem descItemCreated);

    void addUpdatedItem(ArrDescItem descItemUpdated);

    /**
     * Vraci zda se ma ihned po zmene provest flush do nodeCache
     * 
     * @return
     */
    boolean getFlushNodeCache();
}
