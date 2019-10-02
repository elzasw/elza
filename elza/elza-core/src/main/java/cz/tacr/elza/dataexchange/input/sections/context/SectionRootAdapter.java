package cz.tacr.elza.dataexchange.input.sections.context;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;

/**
 * Interface to create wrapper objects
 *
 * This adapter can be used to change parent node of imported item.
 *
 */
public interface SectionRootAdapter {

    /**
     * Return target fund
     *
     * @return return fund
     */
    ArrFund getFund();

    /**
     * Create root node for section
     *
     * @param contextSection
     * @param rootNode
     * @param importNodeId
     *            ID of imported node
     * @return
     */
    NodeContext createRoot(SectionContext contextSection, ArrNode rootNode, String importNodeId);

    /**
     * Called when section is processed.
     */
    void onSectionClose();
}