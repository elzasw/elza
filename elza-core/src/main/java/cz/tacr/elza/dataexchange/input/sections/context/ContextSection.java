package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.service.ArrangementService;

/**
 * Represents single imported fund or subtree for specified node.
 */
public class ContextSection {

    private final Map<String, PacketInfo> packetImportIdMap = new HashMap<>();

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

    public boolean isRootSet() {
        return contextNodeImportIdMap.size() > 0;
    }

    public String generateNodeUuid() {
        return arrangementService.generateUuid();
    }

    public int generateDescItemObjectId() {
        return arrangementService.getNextDescItemObjectId();
    }

    public PacketInfo getPacketInfo(String importId) {
        return packetImportIdMap.get(importId);
    }

    public ContextNode getContextNode(String importId) {
        return contextNodeImportIdMap.get(importId);
    }

    public void addPacket(ArrPacket packet, String importId) {
        PacketInfo packetInfo = new PacketInfo(packet.getPacketType(), packet.getStorageNumber());
        if (packetImportIdMap.putIfAbsent(importId, packetInfo) != null) {
            throw new DEImportException("Fund packet has duplicate id, packetId:" + importId);
        }
        context.addPacket(new ArrPacketWrapper(packet, packetInfo));
    }

    /**
     * Create root node for section and stores all remaining packets.
     */
    public ContextNode addRootNode(ArrNode rootNode, String importId) {
        Validate.notNull(rootAdapter);
        Validate.isTrue(!isRootSet());

        // save processed packets
        context.storePackets();
        // create root context node
        ArrNodeWrapper rootNodeWrapper = rootAdapter.createNodeWrapper(rootNode);
        ArrLevelWrapper rootLevelWrapper = rootAdapter.createLevelWrapper(rootNodeWrapper.getIdHolder());
        return addNode(rootNodeWrapper, rootLevelWrapper, importId, 0);
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

    interface SectionRootAdapter {

        ArrFund getFund();

        ArrNodeWrapper createNodeWrapper(ArrNode rootNode);

        ArrLevelWrapper createLevelWrapper(EntityIdHolder<ArrNode> rootNodeIdHolder);

        /**
         * Called when section is processed.
         */
        void onSectionClose();
    }
}
