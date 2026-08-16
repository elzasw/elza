package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.output.loaders.AbstractBatchLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.DaoInfo;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrFsLink;
import cz.tacr.elza.domain.ArrLegacyDaoLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;

/**
 * Reads supplied levels from node cache and pass them to {@link SectionOutputStream}.
 */
public class LevelInfoLoader extends AbstractBatchLoader<ArrLevel, LevelInfoImpl> {

    private final NodeCacheService nodeCacheService;

    private boolean firstBatch = true;

    private final ArrDaLinkRepository daLinkRepository;

    private final DaoLoader daoLoader;

    private final boolean includeAccessPoints;

    private final boolean includeUuid;

    private final boolean includeDaos;

    /**
     * descItemObjectIds of desc items dropped by the export filter on each processed node.
     * Populated only when {@link #includeAccessPoints} is false.
     */
    private final Map<Integer, Set<Integer>> removedDescObjectIdsByNode = new HashMap<>();

    /**
     * Parent of each processed node, used to walk the tree up when filtering inhibited items.
     * Populated only when {@link #includeAccessPoints} is false.
     */
    private final Map<Integer, Integer> parentNodeByNode = new HashMap<>();

    public LevelInfoLoader(final EntityManager em,
                           final int batchSize,
                           final NodeCacheService nodeCacheService,
                           final ArrDaLinkRepository daLinkRepository,
                           final boolean includeAccessPoints,
                           final boolean includeUuid,
                           final boolean includeDaos) {
        super(batchSize);
        this.daLinkRepository = daLinkRepository;
        this.daoLoader = new DaoLoader(em, batchSize);
        this.nodeCacheService = nodeCacheService;
        this.includeAccessPoints = includeAccessPoints;
        this.includeUuid = includeUuid;
        this.includeDaos = includeDaos;
    }

    @Override
    protected void processBatch(List<BatchEntry> entries) {
        List<Integer> nodeIds = getNodeIds(entries);
        Map<Integer, RestoredNode> cachedNodes = nodeCacheService.getNodes(nodeIds);

        // connected daos (only when requested)
        Map<Integer, ArrDao> daoMap = Collections.emptyMap();
        Map<Integer, List<DaoInfo>> aipDaoInfos = Collections.emptyMap();
        if (includeDaos) {
            // native daos linked from the node cache
            daoMap = loadDaos(cachedNodes);
            // links to an AIP (or a selected part of it) carry no ArrDao, so they are not
            // cached and must be loaded straight from the database
            aipDaoInfos = loadAipDaoInfos(nodeIds);
        }

        for (int i = 0; i < entries.size(); i++) {
            BatchEntry entry = entries.get(i);

            ArrLevel level = entry.getRequest();
            // remove parent for first node (section root node)
            Integer parentNodeId = firstBatch && i == 0 ? null : level.getNodeIdParent();
            // cached node from prepared map
            RestoredNode cachedNode = cachedNodes.get(level.getNodeId());

            // remember real parent (independent of section-root nulling) so inhibited
            // refs can be checked against ancestor levels processed earlier
            if (!includeAccessPoints) {
                parentNodeByNode.put(level.getNodeId(), level.getNodeIdParent());
            }

            LevelInfoImpl levelInfo = createLevelInfo(level.getNodeId(), parentNodeId, cachedNode, daoMap, aipDaoInfos);
            entry.setResult(levelInfo);
        }

        firstBatch = false;
    }

    /**
     *
     * @param cachedNodes
     * @return Map of DAOs.
     */
    private Map<Integer, ArrDao> loadDaos(Map<Integer, RestoredNode> cachedNodes) {
        Map<Integer, ArrDao> daoMap = new HashMap<>();
        LoadDispatcher<ArrDao> daoDispatcher = new LoadDispatcher<ArrDao>() {

            @Override
            public void onLoadBegin() {
                // NOP
            }

            @Override
            public void onLoad(ArrDao result) {
                daoMap.put(result.getDaoId(), result);
            }

            @Override
            public void onLoadEnd() {
                // NOP
            }

        };

        cachedNodes.forEach((nodeId, restoredNode) -> {
            List<ArrDaoLink> daoLinks = restoredNode.getDaoLinks();
            if (daoLinks != null) {
                daoLinks.forEach(daoLink -> {
                    if (daoLink instanceof ArrLegacyDaoLink legacyLink) {
                        daoLoader.addRequest(legacyLink.getDaoId(), daoDispatcher);
                    }
                });
            }
        });

        // fetch DAOs from DB
        daoLoader.flush();

        return daoMap;
    }

    /**
     * Loads DAO references that link a node to an AIP, or to a selected part of an AIP.
     * These links carry no {@link ArrDao} (so they are absent from the node cache) and are
     * resolved from the {@link DaAip} - its repository code and the AIP code - plus, for
     * part links, the code of the selected {@link DaDao}.
     *
     * @param nodeIds processed node ids
     * @return DAO references keyed by node id
     */
    private Map<Integer, List<DaoInfo>> loadAipDaoInfos(Collection<Integer> nodeIds) {
        Map<Integer, List<DaoInfo>> daoInfosByNode = new HashMap<>();
        ObjectListIterator.forEachPage(nodeIds, page -> {
            List<ArrDaLink> daoLinks = daLinkRepository.findAipLinksByNodeIdsAndFetchAip(page);
            for (ArrDaLink daoLink : daoLinks) {
                DaAip aip = daoLink.getAip();
                DaDao daDao = daoLink.getDaDao();
                DaoInfo daoInfo = new DaoInfo(
                        aip.getDigitalRepository().getCode(),
                        aip.getCode(),
                        daDao != null ? daDao.getCode() : null);
                daoInfosByNode.computeIfAbsent(daoLink.getNodeId(), k -> new ArrayList<>()).add(daoInfo);
            }
        });

        return daoInfosByNode;
    }

    private static List<Integer> getNodeIds(List<BatchEntry> entries) {
        List<Integer> nodeIds = new ArrayList<>(entries.size());
        for (BatchEntry entry : entries) {
            nodeIds.add(entry.getRequest().getNodeId());
        }
        return nodeIds;
    }

    private LevelInfoImpl createLevelInfo(Integer nodeId, Integer parentNodeId, CachedNode cachedNode,
                                          Map<Integer, ArrDao> daoMap, Map<Integer, List<DaoInfo>> aipDaoInfos) {
    	Objects.requireNonNull(nodeId);
    	Objects.requireNonNull(cachedNode);

        LevelInfoImpl levelInfo = new LevelInfoImpl(nodeId, parentNodeId);
        // show UUID by condition
        if (includeUuid) {
            levelInfo.setNodeUuid(cachedNode.getUuid());
        }

        // add desc items
        List<ArrDescItem> descItems = cachedNode.getDescItems();
        if (descItems != null) {
            // partition: keep items that pass the filter; remember dropped descItemObjectIds
            // so inhibited references from descendants can be cleaned up
            Set<Integer> droppedAtThisNode = null;
            List<ArrDescItem> kept = new ArrayList<>(descItems.size());
            for (ArrDescItem item : descItems) {
                if (isItemIncluded(item)) {
                    kept.add(item);
                } else {
                    if (droppedAtThisNode == null) {
                        droppedAtThisNode = new HashSet<>();
                    }
                    droppedAtThisNode.add(item.getDescItemObjectId());
                }
            }
            if (droppedAtThisNode != null) {
                removedDescObjectIdsByNode.put(nodeId, droppedAtThisNode);
            }
            kept.sort(this::compareItems);
            kept.forEach(levelInfo::addItem);
        }

        // add inhibited items, dropping refs whose target item was removed from any ancestor
        List<ArrInhibitedItem> inhibitedItems = cachedNode.getInhibitedItems();
        if (inhibitedItems != null) {
            Collection<ArrInhibitedItem> filtered = filterInhibitedItems(inhibitedItems, nodeId);
            if (!filtered.isEmpty()) {
                levelInfo.addInhibitedItems(filtered);
            }
        }

        // add daos (only when requested)
        if (includeDaos) {
            // native daos linked from the node cache: no part, object identified by its own code
            List<ArrDaoLink> daoLinks = cachedNode.getDaoLinks();
            if (daoLinks != null) {
                daoLinks.forEach(daoLink -> {
                    if (daoLink instanceof ArrLegacyDaoLink legacyLink) {
                        ArrDao dao = daoMap.get(legacyLink.getDaoId());
                        Objects.requireNonNull(dao, "Missing dao: " + legacyLink.getDaoId());
                        levelInfo.addDao(new DaoInfo(
                                dao.getDaoPackage().getDigitalRepository().getCode(),
                                dao.getCode(),
                                null));
                    } else if (daoLink instanceof ArrFsLink fsLink) {
                        // filesystem link: the repository-relative path is the object's code
                        levelInfo.addDao(new DaoInfo(
                                fsLink.getDigitalRepository().getCode(),
                                fsLink.getPath() != null ? fsLink.getPath() : "",
                                null));
                    }
                });
            }
            // daos referencing an AIP or a selected part of it (loaded from the database)
            List<DaoInfo> aipDaos = aipDaoInfos.get(nodeId);
            if (aipDaos != null) {
                aipDaos.forEach(levelInfo::addDao);
            }
        }

        return levelInfo;
    }

    /**
     * Compare/Order items
     *
     * @param item1
     * @param item2
     * @return
     */
    private int compareItems(ArrDescItem item1, ArrDescItem item2) {
        RulItemType itemType1 = item1.getItemType();
        RulItemType itemType2 = item2.getItemType();
        int cmp = itemType1.getViewOrder().compareTo(itemType2.getViewOrder());
        if (cmp == 0) {
            if (itemType1.getUseSpecification() && itemType2.getUseSpecification()) {
                RulItemSpec itemSpec1 = item1.getItemSpec();
                RulItemSpec itemSpec2 = item2.getItemSpec();
                cmp = itemSpec1.getViewOrder().compareTo(itemSpec2.getViewOrder());
                if (cmp == 0) {
                    cmp = item1.getPosition().compareTo(item2.getPosition());
                }
            } else {
                cmp = item1.getPosition().compareTo(item2.getPosition());
            }
        }
        return cmp;
    }

    private boolean isItemIncluded(ArrDescItem item) {
        if (!includeAccessPoints) {
            // filter out AccessPoints
            if (item.getData() != null && DataType.fromId(item.getData().getDataTypeId()) == DataType.RECORD_REF) {
                return false;
            }
        }
        return true;
    }

    private Collection<ArrInhibitedItem> filterInhibitedItems(List<ArrInhibitedItem> inhibitedItems,
                                                              Integer currentNodeId) {
        if (includeAccessPoints || removedDescObjectIdsByNode.isEmpty()) {
            return inhibitedItems;
        }
        List<ArrInhibitedItem> result = new ArrayList<>(inhibitedItems.size());
        for (ArrInhibitedItem inh : inhibitedItems) {
            if (isInhibitedItemIncluded(inh, currentNodeId)) {
                result.add(inh);
            }
        }
        return result;
    }

    /**
     * An inhibited item targets a description item on some ancestor level. Walk the parent
     * chain and look for a node where that target was filtered out — if found, the inhibition
     * is meaningless in the export and must be omitted.
     */
    private boolean isInhibitedItemIncluded(ArrInhibitedItem inh, Integer currentNodeId) {
        Integer refId = inh.getDescItemObjectId();
        Integer ancestorId = parentNodeByNode.get(currentNodeId);
        while (ancestorId != null) {
            Set<Integer> dropped = removedDescObjectIdsByNode.get(ancestorId);
            if (dropped != null && dropped.contains(refId)) {
                return false;
            }
            ancestorId = parentNodeByNode.get(ancestorId);
        }
        return true;
    }
}
