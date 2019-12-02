package cz.tacr.elza.dataexchange.input.sections.context;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.StructObjValueService;

/**
 * Represents single imported fund or subtree for specified node.
 */
public class SectionContext {

    private final Map<String, NodeContext> importIdNodeCtxMap = new HashMap<>();

    private final Map<String, StructObjContext> importIdStructObjCtxMap = new HashMap<>();

    /**
     * Map of imported files
     */
    private final Map<String, ArrFile> importIdFileMap = new HashMap<>();

    private final NodeStorageDispatcher nodeStorageDispatcher;

    private final StructObjStorageDispatcher structObjectStorageDispatcher;

    private final ArrChange createChange;

    private final RulRuleSet ruleSet;

    private final StaticDataProvider staticData;

    private final ArrangementService arrangementService;

    private final StructObjValueService structObjService;

    private RulStructuredType processingStructType;

    private SectionRootAdapter rootAdapter;

    private final DmsService dmsService;

    SectionContext(StorageManager storageManager,
                   int batchSize,
                   ArrChange createChange,
                   RulRuleSet ruleSet,
                   StaticDataProvider staticData,
                   ImportInitHelper initHelper) {
        this.nodeStorageDispatcher = new NodeStorageDispatcher(storageManager, batchSize);
        this.structObjectStorageDispatcher = new StructObjStorageDispatcher(storageManager, batchSize);
        this.createChange = Validate.notNull(createChange);
        this.ruleSet = Validate.notNull(ruleSet);
        this.staticData = Validate.notNull(staticData);
        this.arrangementService = initHelper.getArrangementService();
        this.structObjService = initHelper.getStructObjService();
        this.dmsService = initHelper.getDmsService();
    }

    public ArrChange getCreateChange() {
        return createChange;
    }

    public StaticDataProvider getStaticData() {
        return staticData;
    }

    public RulRuleSet getRuleSet() {
        return ruleSet;
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
     * Notification about finished imported sections
     */
    public void filesFinished() {
        // store all previous entities before node processing
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
        StructType st = staticData.getStructuredTypeByCode(structTypeCode);
        Validate.notNull(st);
        this.processingStructType = st.getStructuredType();
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

    public void addFile(String importId, String name,
                        String fileName,
                        String mimetype,
                        Consumer<OutputStream> dataProvider) throws IOException {
        Validate.notNull(importId);
        Validate.notNull(name);
        Validate.notNull(fileName);
        Validate.notNull(mimetype);

        ArrFile dmsFile = new ArrFile();
        dmsFile.setName(name);
        dmsFile.setFileName(fileName);
        dmsFile.setMimeType(mimetype);
        dmsFile.setFund(getFund());
        dmsFile.setFileSize(0);
        // save file
        dmsService.createFile(dmsFile, dataProvider);

        Validate.notNull(dmsFile.getFileId());

        Validate.isTrue(importIdFileMap.get(importId) == null, "Duplicated local id, value: %s", importId);
        importIdFileMap.put(importId, dmsFile);
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

    public ArrFile getFile(String fid) {
        return importIdFileMap.get(fid);
    }
}
