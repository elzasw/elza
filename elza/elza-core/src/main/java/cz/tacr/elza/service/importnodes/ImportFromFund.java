package cz.tacr.elza.service.importnodes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.service.importnodes.vo.descitems.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;
import com.vividsolutions.jts.geom.Geometry;

import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.importnodes.vo.ChangeDeep;
import cz.tacr.elza.service.importnodes.vo.DeepCallback;
import cz.tacr.elza.service.importnodes.vo.ImportSource;
import cz.tacr.elza.service.importnodes.vo.Node;

/**
 * Implementace importu z jiného archivního souboru.
 *
 * @since 19.07.2017
 */
@Component
@org.springframework.context.annotation.Scope("prototype")
public class ImportFromFund implements ImportSource {

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private StructuredObjectRepository structureDataRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private NodeCacheService nodeCacheService;

    private Set<Integer> nodeIds;
    private Iterator<Integer> nodeIdsIterator;
    private boolean ignoreRootNodes;

    private Integer nodeId = null;
    private LevelIterator levelsIterator;
    private ArrNode node;
    private ArrNode nodePrev;
    private Stack<ArrNode> nodeParents = new Stack<>();
    private boolean reset = false;

    /**
     * Inicializace zdroje.
     *
     * @param sourceNodes       uzly, které prohledáváme
     * @param ignoreRootNodes   ignorují je vrcholové uzly, které prohledáváme
     */
	public void init(final Collection<ArrNode> sourceNodes, final boolean ignoreRootNodes) {
        this.nodeIds = sourceNodes.stream().map(ArrNode::getNodeId).collect(Collectors.toCollection(TreeSet::new));
        this.nodeIdsIterator = this.nodeIds.iterator();
        this.ignoreRootNodes = ignoreRootNodes;
    }

    @Override
	public List<ArrFile> getFiles() {
        List<ArrFile> files = fundFileRepository.findFilesBySubtreeNodeIds(nodeIds, ignoreRootNodes);
		return files;
    }

    @Override
    public List<ArrStructuredObject> getStructuredList() {
        return structureDataRepository.findStructureDataBySubtreeNodeIds(nodeIds, null, ignoreRootNodes);
    }

    @Override
    public boolean hasNext() {
        while (true) {
            if (nodeId == null) {
                if (nodeIdsIterator.hasNext()) {
                    nodeId = nodeIdsIterator.next();
                    levelsIterator = new LevelIterator(levelRepository, nodeCacheService, nodeId);
                } else {
                    break;
                }
            }
            if (levelsIterator.hasNext()) {
                return true;
            } else {
                nodeId = null;
            }
        }
        return false;
    }

    /**
     * Iterátor pro postupné získávání uzlů ve stromu/podstromu.
     *
     * Iterátor prochází strom do hloubky (DFS)!
     */
    public class LevelIterator implements Iterator<ArrLevel> {

        /**
         * Postup
         */
        private int offset = 0;

        /**
         * Maximální počet uzlů, které lze načíst na jediný dotaz z DB.
         */
        private final int MAX = 1000;

        /**
         * Identifikátor uzlu od kterého se prohledává strom.
         */
        private final Integer nodeId;

        /**
         * Iterátor načtených uzlů.
         */
        private Iterator<ArrLevel> iterator = null;

        /**
         * Načtené levely iterátoru.
         */
        private List<ArrLevel> levelsSubtree = null;

        /**
         * Naštené uzly iterátoru.
         */
		private Map<Integer, RestoredNode> cachedNodes = null;

        private final LevelRepository levelRepository;

        private final NodeCacheService nodeCacheService;

        public LevelIterator(final LevelRepository levelRepository, final NodeCacheService nodeCacheService, final Integer nodeId) {
            this.levelRepository = levelRepository;
            this.nodeCacheService = nodeCacheService;
            this.nodeId = nodeId;
        }

		/**
		 * Fetch next nodes from DB
		 */
		private void readNext() {
			levelsSubtree = levelRepository.findLevelsSubtree(nodeId, offset, MAX, ignoreRootNodes);
			// shift offset
			offset += levelsSubtree.size();
			// read nodes from cache
			List<Integer> nodeIds = levelsSubtree.stream().map(ArrLevel::getNodeId).collect(Collectors.toList());
			if (!nodeIds.isEmpty()) {
				cachedNodes = nodeCacheService.getNodes(nodeIds);
			}
			// prepare iterator
			iterator = levelsSubtree.iterator();
		}

        @Override
        public boolean hasNext() {
            if (iterator == null) { // pokud není nic načtené, načteme první část do bufferu
				readNext();
			} else
            if (!iterator.hasNext()) { // pokud už nemáme v buffer, posuneme offset a načteme další část
				readNext();
            }
            return iterator.hasNext();
        }

        @Override
        public ArrLevel next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            return iterator.next();
        }

        /**
         * Získání JP z cache.
         *
         * @param nodeId identifikátor JP
         * @return JP
         */
        public CachedNode getNode(final Integer nodeId) {
            if (cachedNodes == null) {
                throw new IllegalStateException();
            }
            CachedNode cachedNode = cachedNodes.get(nodeId);
            if (cachedNode == null) {
                throw new BusinessException("Missing node in cache", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("nodeId", nodeId);
            }
            return cachedNode;
        }
    }

    @Override
    public Node getNext(final DeepCallback changeDeep) {
        if (!hasNext()) {
            throw new IllegalStateException();
        }

        ArrLevel level = levelsIterator.next();
        nodePrev = node;
        node = level.getNode();

        if ((!reset && nodeParents.empty())) {
            reset = true;
            changeDeep.call(ChangeDeep.RESET);
        } else if (!nodeParents.empty() && Objects.equal(level.getNodeParent(), nodeParents.peek())) {
            changeDeep.call(ChangeDeep.NONE);
        } else if (level.getNodeParent() != null && level.getNodeParent().equals(nodePrev)) {
            nodeParents.push(level.getNodeParent());
            changeDeep.call(ChangeDeep.DOWN);
        } else {
            if (nodeParents.isEmpty()) {
                changeDeep.call(ChangeDeep.NONE);
            } else {
                do {
                    nodeParents.pop();
                    changeDeep.call(ChangeDeep.UP);
                } while (!nodeParents.isEmpty() && !Objects.equal(level.getNodeParent(), nodeParents.peek()));
            }
        }

        CachedNode cachedNode = levelsIterator.getNode(node.getNodeId());

        if (!levelsIterator.hasNext()) {
            reset = false;
            nodeParents.clear();
        }

        return new Node() {
            @Override
            public String getUuid() {
                return null; // bude vygenerováno nové
            }

            @Override
            public Collection<? extends Item> getItems() {
                List<ArrDescItem> descItems = cachedNode.getDescItems();

                if (descItems == null) {
                    return null;
                }

                List<Item> result = new ArrayList<>(descItems.size());

                for (ArrDescItem descItem : descItems) {
                    result.add(convertDescItem(descItem));
                }

                return result;
            }
        };
    }

    /**
     * Konverze atributu na item z rozhraní.
     *
     * @param item konvertovaný item
     * @return vytvořený item
     */
    private Item convertDescItem(final ArrDescItem item) {
        Item result;

        ArrData itemData = item.getData();

        if (itemData instanceof ArrDataNull) {
            result = new ItemEnumImpl(item);
        } else if (itemData instanceof ArrDataString) {
            result = new ItemStringImpl(item, (ArrDataString) itemData);
        } else if (itemData instanceof ArrDataText && item.getItemType().getDataType().getCode().equals("TEXT")) {
            result = new ItemTextImpl(item, (ArrDataText) itemData);
        } else if (itemData instanceof ArrDataText && item.getItemType().getDataType().getCode().equals("FORMATTED_TEXT")) {
            result = new ItemFormattedTextImpl(item, (ArrDataText) itemData);
        } else if (itemData instanceof ArrDataInteger) {
            result = new ItemIntImpl(item, (ArrDataInteger) itemData);
        } else if (itemData instanceof ArrDataDecimal) {
            result = new ItemDecimalImpl(item, (ArrDataDecimal) itemData);
        } else if (itemData instanceof ArrDataUnitid) {
            result = new ItemUnitidImpl(item, (ArrDataUnitid) itemData);
        } else if (itemData instanceof ArrDataJsonTable) {
            result = new ItemJsonTableImpl(item, (ArrDataJsonTable) itemData);
        } else if (itemData instanceof ArrDataUnitdate) {
            result = new ItemUnitdateImpl(item, (ArrDataUnitdate) itemData);
        } else if (itemData instanceof ArrDataFileRef) {
            result = new ItemFileRefImpl(item, (ArrDataFileRef) itemData);
        } else if (itemData instanceof ArrDataPartyRef) {
            result = new ItemPartyRefImpl(item, (ArrDataPartyRef) itemData);
        } else if (itemData instanceof ArrDataRecordRef) {
            result = new ItemRecordRefImpl(item, (ArrDataRecordRef) itemData);
        } else if (itemData instanceof ArrDataStructureRef) {
            result = new ItemStructureRefImpl(item, (ArrDataStructureRef) itemData);
        } else if (itemData instanceof ArrDataCoordinates) {
            result = new ItemCoordinatesRefImpl(item, (ArrDataCoordinates) itemData);
        } else if (itemData instanceof ArrDataUriRef) {
            result = new ItemUriRefImpl(item, (ArrDataUriRef) itemData);
        } else {
            result = new ItemImpl(item);
        }

        return result;
    }

    private class ItemImpl implements Item {

        private final String typeCode;

        private final String specCode;

        public ItemImpl(final ArrDescItem item) {
            typeCode = item.getItemType().getCode();
            specCode = item.getItemSpec() == null ? null : item.getItemSpec().getCode();
        }

        @Override
        public String getTypeCode() {
            return typeCode;
        }

        @Override
        public String getSpecCode() {
            return specCode;
        }
    }

    private class ItemEnumImpl extends ItemImpl implements ItemEnum {
        public ItemEnumImpl(final ArrDescItem item) {
            super(item);
        }
    }

    private class ItemStringImpl extends ItemImpl implements ItemString {

        private final String value;

        public ItemStringImpl(final ArrDescItem item, final ArrDataString itemData) {
            super(item);
            value = itemData.getValue();
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    private class ItemTextImpl extends ItemImpl implements ItemText {
        private final String value;

        public ItemTextImpl(final ArrDescItem item, final ArrDataText itemData) {
            super(item);
            value = itemData.getValue();
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    private class ItemFormattedTextImpl extends ItemImpl implements ItemFormattedText {
        private final String value;

        public ItemFormattedTextImpl(final ArrDescItem item, final ArrDataText itemData) {
            super(item);
            value = itemData.getValue();
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    private class ItemIntImpl extends ItemImpl implements ItemInt {
        private final Integer value;

        public ItemIntImpl(final ArrDescItem item, final ArrDataInteger itemData) {
            super(item);
            value = itemData.getValue();
        }

        @Override
        public Integer getValue() {
            return value;
        }
    }

    private class ItemDecimalImpl extends ItemImpl implements ItemDecimal {
        private final BigDecimal value;

        public ItemDecimalImpl(final ArrDescItem item, final ArrDataDecimal itemData) {
            super(item);
            value = itemData.getValue();
        }

        @Override
        public BigDecimal getValue() {
            return value;
        }
    }

    private class ItemUnitidImpl extends ItemImpl implements ItemUnitid {
        private final String value;

        public ItemUnitidImpl(final ArrDescItem item, final ArrDataUnitid itemData) {
            super(item);
            value = itemData.getUnitId();
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    private class ItemJsonTableImpl extends ItemImpl implements ItemJsonTable {

        private final ElzaTable value;

        public ItemJsonTableImpl(final ArrDescItem item, final ArrDataJsonTable itemData) {
            super(item);
            value = itemData.getValue();
        }

        @Override
        public ElzaTable getValue() {
            return value;
        }
    }

    private class ItemUnitdateImpl extends ItemImpl implements ItemUnitdate {

        private final String value;

        private final String calendarTypeCode;

        public ItemUnitdateImpl(final ArrDescItem item, final ArrDataUnitdate itemData) {
            super(item);
            calendarTypeCode = itemData.getCalendarType().getCode();
            value = UnitDateConvertor.convertToString(itemData);
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getCalendarTypeCode() {
            return calendarTypeCode;
        }
    }

    private class ItemFileRefImpl extends ItemImpl implements ItemFileRef {

        private final Integer fileId;

        public ItemFileRefImpl(final ArrDescItem item, final ArrDataFileRef itemData) {
            super(item);
            fileId = itemData.getFileId();
        }

        @Override
        public Integer getFileId() {
            return fileId;
        }
    }

    private class ItemStructureRefImpl extends ItemImpl implements ItemStructureRef {

        private final Integer structureDataId;

        public ItemStructureRefImpl(final ArrDescItem item, final ArrDataStructureRef itemData) {
            super(item);
            structureDataId = itemData.getStructuredObjectId();
        }

        @Override
        public Integer getStructureDataId() {
            return structureDataId;
        }
    }

    private class ItemPartyRefImpl extends ItemImpl implements ItemPartyRef {

        private final Integer partyId;

        public ItemPartyRefImpl(final ArrDescItem item, final ArrDataPartyRef itemData) {
            super(item);
            partyId = itemData.getPartyId();
        }

        public Integer getPartyId() {
            return partyId;
        }
    }

    private class ItemUriRefImpl extends ItemImpl implements ItemUriRef {

        private final String schema;

        private final String value;

        private final String description;

        private final Integer nodeId;

        public ItemUriRefImpl(final ArrDescItem item, final ArrDataUriRef itemData) {
            super(item);
            nodeId = itemData.getNodeId();
            schema = itemData.getSchema();
            value = itemData.getValue();
            description = itemData.getDescription();
        }

        public String getSchema() {
            return schema;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public Integer getNodeId() {
            return nodeId;
        }
    }

    private class ItemRecordRefImpl extends ItemImpl implements ItemRecordRef {

        private final Integer recordId;

        public ItemRecordRefImpl(final ArrDescItem item, final ArrDataRecordRef itemData) {
            super(item);
            recordId = itemData.getRecordId();
        }

        public Integer getRecordId() {
            return recordId;
        }
    }

    private class ItemCoordinatesRefImpl  extends ItemImpl implements ItemCoordinates {

        private final Geometry geometry;

        public ItemCoordinatesRefImpl(final ArrDescItem item, final ArrDataCoordinates itemData) {
            super(item);
            geometry = itemData.getValue();
        }

        @Override
        public Geometry getGeometry() {
            return geometry;
        }
    }

	@Override
	public List<ApScope> getScopes() {
		return scopeRepository.findScopesBySubtreeNodeIds(nodeIds, ignoreRootNodes);
	}
}
