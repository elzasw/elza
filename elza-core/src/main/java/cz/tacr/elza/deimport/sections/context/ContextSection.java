package cz.tacr.elza.deimport.sections.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.Assert;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.service.ArrangementService;

/**
 * Represents single imported fund or subtree for specified node.
 */
public class ContextSection {

    private final Map<String, PacketImportInfo> packetImportIdMap = new HashMap<>();

    private final Map<String, ContextNode> contextNodeImportIdMap = new HashMap<>();

    private final SectionsContext context;

    private final ArrChange createChange;

    private final RuleSystem ruleSystem;

    private final ArrangementService arrangementService;

    private SectionRootAdapter rootAdapter;

    ContextSection(SectionsContext context,
            ArrChange createChange,
            RuleSystem ruleSystem,
            ArrangementService arrangementService) {
        this.context = Objects.requireNonNull(context);
        this.createChange = Objects.requireNonNull(createChange);
        this.ruleSystem = Objects.requireNonNull(ruleSystem);
        this.arrangementService = Objects.requireNonNull(arrangementService);
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
        Assert.notNull(rootAdapter);

        return rootAdapter.getFund();
    }

    public boolean isRootSet() {
        return contextNodeImportIdMap.size() > 0;
    }

    public String generateNodeUuid() {
        return arrangementService.generateUuid();
    }

    public int generateDescItemObjectId() {
        return arrangementService.getNextDescItemObjectId();
    }

    public PacketImportInfo getPacketInfo(String importId) {
        return packetImportIdMap.get(importId);
    }

    public ContextNode getContextNode(String importId) {
        return contextNodeImportIdMap.get(importId);
    }

    public void addPacket(ArrPacket packet, String importId) {
        PacketImportInfo packetInfo = new PacketImportInfo(packet.getPacketType(), packet.getStorageNumber());
        if (packetImportIdMap.putIfAbsent(importId, packetInfo) != null) {
            throw new DEImportException("Fund packet has duplicate id, packetId:" + importId);
        }
        context.addPacket(new ArrPacketWrapper(packet, packetInfo));
    }

    /**
     * Create root node for section and stores all remaining packets.
     */
    public ContextNode addRootNode(ArrNode rootNode, String importId) {
        Assert.notNull(rootAdapter);
        Assert.isTrue(!isRootSet());
        // save processed packets
        context.storePackets();
        // create root context node
        ArrNodeWrapper rootNodeWrapper = rootAdapter.createNodeWrapper(rootNode);
        ArrLevelWrapper rootLevelWrapper = rootAdapter.createLevelWrapper(rootNodeWrapper.getIdHolder());
        return addNode(rootNodeWrapper, rootLevelWrapper, importId, 0);
    }

    public void close() {
        Assert.notNull(rootAdapter);
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

    void addDescItem(ArrDescItemWrapper itemWrapper, ArrDataWrapper dataWrapper, int depth) {
        context.addDescItem(itemWrapper, dataWrapper, depth);
    }

    void setRootAdapter(SectionRootAdapter rootAdapter) {
        this.rootAdapter = rootAdapter;
    }

    interface SectionRootAdapter {

        ArrFund getFund();

        ArrNodeWrapper createNodeWrapper(ArrNode rootNode);

        ArrLevelWrapper createLevelWrapper(IdHolder rootNodeIdHolder);

        /**
         * Called when section is processed.
         */
        void onSectionClose();
    }
}