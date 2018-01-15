package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.ArrangementService;

/**
 * Represents single imported fund or subtree for specified node.
 */
public class ContextSection {

    private final Map<String, ContextNode> contextNodeImportIdMap = new HashMap<>();

    private final SectionsContext context;

    private final ArrChange createChange;

    private final RuleSystem ruleSystem;

    private final ArrangementService arrangementService;

    private SectionRootAdapter rootAdapter;

    ContextSection(SectionsContext context, ArrChange createChange, RuleSystem ruleSystem, ArrangementService arrangementService) {
        this.context = Validate.notNull(context);
        this.createChange = Validate.notNull(createChange);
        this.ruleSystem = Validate.notNull(ruleSystem);
        this.arrangementService = Validate.notNull(arrangementService);
    }

    public SectionsContext getContext() {
        return context;
    }

    public ArrChange getCreateChange() {
        return createChange;
    }

    public RuleSystem getRuleSystem() {
        return ruleSystem;
    }

    public ArrFund getFund() {
        Validate.notNull(rootAdapter);

        return rootAdapter.getFund();
    }

    public String generateNodeUuid() {
        return arrangementService.generateUuid();
    }

    public int generateDescItemObjectId() {
        return arrangementService.getNextDescItemObjectId();
    }

    public ContextNode getContextNode(String importId) {
        return contextNodeImportIdMap.get(importId);
    }

    /**
     * Create root node for section and stores all remaining packets.
     */
	public ContextNode setRootNode(ArrNode rootNode, String importNodeId) {
        Validate.notNull(rootAdapter);


        // create root context node
		return rootAdapter.createRoot(this, rootNode, importNodeId);
    }

    public void close() {
        Validate.notNull(rootAdapter);

        rootAdapter.onSectionClose();
        rootAdapter = null;
    }

    /* package methods */

    ContextNode addNode(ArrNodeWrapper nodeWrapper, ArrLevelWrapper levelWrapper, String importId, int depth) {
        ContextNode node = new ContextNode(this, nodeWrapper.getIdHolder(), depth);
        if (contextNodeImportIdMap.putIfAbsent(importId, node) != null) {
            throw new DEImportException("Fund level has duplicate id, levelId:" + importId);
        }
        context.addNode(nodeWrapper, depth);
        context.addLevel(levelWrapper, depth);
        return node;
    }

    void addNodeRegister(ArrNodeRegisterWrapper wrapper, int depth) {
        context.addNodeRegister(wrapper, depth);
    }

    void addDescItem(ArrDescItemWrapper itemWrapper, int depth) {
        context.addDescItem(itemWrapper, depth);
    }

    void addData(ArrDataWrapper dataWrapper, int depth) {
        context.addData(dataWrapper, depth);
    }

    void setRootAdapter(SectionRootAdapter rootAdapter) {
        this.rootAdapter = rootAdapter;
    }

	/**
	 * Interface to create wrapper objects
	 * 
	 * This adapter can be used to change parent node of imported item.
	 *
	 */
    interface SectionRootAdapter {

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
		ContextNode createRoot(ContextSection contextSection, ArrNode rootNode, String importNodeId);

        /**
         * Called when section is processed.
         */
        void onSectionClose();
    }
}
