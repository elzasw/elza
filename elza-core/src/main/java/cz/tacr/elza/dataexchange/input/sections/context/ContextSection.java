package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.service.ArrangementService;

/**
 * Represents single imported fund or subtree for specified node.
 */
public class ContextSection {

    private final Map<String, ContextNode> contextNodeImportIdMap = new HashMap<>();

    private final Map<String, ContextStructObject> contextStructObjectImportIdMap = new HashMap<>();

    private final NodeStorageDispatcher nodeStorageDispatcher;

    private final StructObjectStorageDispatcher structObjectStorageDispatcher;

    private final ArrChange createChange;

    private final RuleSystem ruleSystem;

    private final ArrangementService arrangementService;

    private RulStructuredType processingStructType;

    private SectionRootAdapter rootAdapter;

    ContextSection(StorageManager storageManager,
                   int batchSize,
                   ArrChange createChange,
                   RuleSystem ruleSystem,
                   ImportInitHelper initHelper) {
        this.nodeStorageDispatcher = new NodeStorageDispatcher(storageManager, batchSize);
        this.structObjectStorageDispatcher = new StructObjectStorageDispatcher(storageManager, batchSize);
        this.createChange = Validate.notNull(createChange);
        this.ruleSystem = Validate.notNull(ruleSystem);
        this.arrangementService = initHelper.getArrangementService();
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

    public ContextStructObject getContextStructObject(String importId) {
        return contextStructObjectImportIdMap.get(importId);
    }

    /**
     * Create root node for section and stores all remaining packets.
     */
    public ContextNode setRootNode(ArrNode rootNode, String importNodeId) {
        Validate.notNull(rootAdapter);

        // store all previous entities before node processing
        structObjectStorageDispatcher.dispatchAll();
        processingStructType = null;

        // create root context node
        return rootAdapter.createRoot(this, rootNode, importNodeId);
    }

    public void setProcessingStructType(String structTypeCode) {
        RulStructuredType st = ruleSystem.getStructuredTypeByCode(structTypeCode);
        this.processingStructType = Validate.notNull(st);
    }

    public ContextStructObject addStructObject(ArrStructuredObject structObject, String importId) {
        Validate.notNull(processingStructType);

        // update structured type reference
        structObject.setStructuredType(processingStructType);

        ArrStructObjectWrapper wrapper = new ArrStructObjectWrapper(structObject, importId);
        ContextStructObject cso = new ContextStructObject(this, wrapper.getIdHolder(), structObjectStorageDispatcher);
        if (contextStructObjectImportIdMap.putIfAbsent(importId, cso) != null) {
            throw new DEImportException("Structured object has duplicate id, soId:" + importId);
        }

        // store object wrapper
        structObjectStorageDispatcher.addStructObject(wrapper);
        return cso;
    }

    /**
     * Store all section nodes and related entities.
     */
    public void storeNodes() {
        nodeStorageDispatcher.dispatchAll();
    }

    public void close() {
        Validate.notNull(rootAdapter);

        rootAdapter.onSectionClose();
        rootAdapter = null;
    }

    /* package methods */

    void setRootAdapter(SectionRootAdapter rootAdapter) {
        this.rootAdapter = rootAdapter;
    }

    ContextNode addNode(ArrNodeWrapper nodeWrapper, ArrLevelWrapper levelWrapper, String importId, int depth) {
        ContextNode node = new ContextNode(this, nodeWrapper.getIdHolder(), nodeStorageDispatcher, depth);
        if (contextNodeImportIdMap.putIfAbsent(importId, node) != null) {
            throw new DEImportException("Fund level has duplicate id, levelId:" + importId);
        }
        nodeStorageDispatcher.addNode(nodeWrapper, depth);
        nodeStorageDispatcher.addLevel(levelWrapper, depth);
        return node;
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
         * @param importNodeId ID of imported node
         * @return
         */
        ContextNode createRoot(ContextSection contextSection, ArrNode rootNode, String importNodeId);

        /**
         * Called when section is processed.
         */
        void onSectionClose();
    }
}
