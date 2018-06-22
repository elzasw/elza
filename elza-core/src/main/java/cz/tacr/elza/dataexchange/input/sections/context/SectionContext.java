package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import cz.tacr.elza.service.StructObjService;

/**
 * Represents single imported fund or subtree for specified node.
 */
public class SectionContext {

    private final Map<String, NodeContext> importIdNodeCtxMap = new HashMap<>();

    private final Map<String, StructObjContext> importIdStructObjCtxMap = new HashMap<>();

    private final NodeStorageDispatcher nodeStorageDispatcher;

    private final StructObjStorageDispatcher structObjectStorageDispatcher;

    private final ArrChange createChange;

    private final RuleSystem ruleSystem;

    private final ArrangementService arrangementService;

    private final StructObjService structObjService;

    private RulStructuredType processingStructType;

    private SectionRootAdapter rootAdapter;

    SectionContext(StorageManager storageManager,
            int batchSize,
            ArrChange createChange,
            RuleSystem ruleSystem,
            ImportInitHelper initHelper) {
        this.nodeStorageDispatcher = new NodeStorageDispatcher(storageManager, batchSize);
        this.structObjectStorageDispatcher = new StructObjStorageDispatcher(storageManager, batchSize);
        this.createChange = Validate.notNull(createChange);
        this.ruleSystem = Validate.notNull(ruleSystem);
        this.arrangementService = initHelper.getArrangementService();
        this.structObjService = initHelper.getStructObjService();
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

    public NodeContext getNode(String importId) {
        return importIdNodeCtxMap.get(importId);
    }

    public StructObjContext getStructObject(String importId) {
        return importIdStructObjCtxMap.get(importId);
    }

    /**
     * Notification about finished imported sections
     */
    public void structObjsFinished() {
        // store all previous entities before node processing
        structObjectStorageDispatcher.dispatchAll();
        processingStructType = null;
        // validate and regenerate
        int size = importIdStructObjCtxMap.size();
        if (size > 0) {
            List<Integer> soIds = new ArrayList<>(size);
            for (StructObjContext soc : importIdStructObjCtxMap.values()) {
                soIds.add(soc.getIdHolder().getEntityId());
            }
            structObjService.addIdsToValidate(soIds);
        }
    }

    /**
     * Create root node for section and stores all remaining packets.
     */
    public NodeContext setRootNode(ArrNode rootNode, String importNodeId) {
        Validate.notNull(rootAdapter);

        // create root context node
        return rootAdapter.createRoot(this, rootNode, importNodeId);
    }

    public void setProcessingStructType(String structTypeCode) {
        RulStructuredType st = ruleSystem.getStructuredTypeByCode(structTypeCode);
        this.processingStructType = Validate.notNull(st);
    }

    public StructObjContext addStructObject(ArrStructuredObject entity, String importId) {
        Validate.notNull(processingStructType);

        // update structured type reference
        entity.setStructuredType(processingStructType);

        ArrStructObjectWrapper wrapper = new ArrStructObjectWrapper(entity, importId);
        StructObjContext ctx = new StructObjContext(this, wrapper.getIdHolder(), structObjectStorageDispatcher);
        if (importIdStructObjCtxMap.putIfAbsent(importId, ctx) != null) {
            throw new DEImportException("Structured object has duplicate id, soId:" + importId);
        }

        // store object wrapper
        structObjectStorageDispatcher.addStructObject(wrapper);
        return ctx;
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

    NodeContext addNode(ArrNodeWrapper nodeWrapper, ArrLevelWrapper levelWrapper, String importId, int depth) {
        NodeContext node = new NodeContext(this, nodeWrapper.getIdHolder(), nodeStorageDispatcher, depth);
        if (importIdNodeCtxMap.putIfAbsent(importId, node) != null) {
            throw new DEImportException("Fund level has duplicate id, levelId:" + importId);
        }
        nodeStorageDispatcher.addNode(nodeWrapper, depth);
        nodeStorageDispatcher.addLevel(levelWrapper, depth);
        return node;
    }
}
