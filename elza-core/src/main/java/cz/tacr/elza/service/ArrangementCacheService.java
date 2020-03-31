package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.arrangement.BatchChangeContext;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;

/**
 * Serviska pro operace nad JP, která současně propisuje změny do cache.
 *
 * 
 */
@Service
public class ArrangementCacheService {

    @Autowired
    private NodeCacheService nodeCacheService;

    /**
     * Smazání JP.
     *
     * @param nodeId identifikátor JP
     */
    public void deleteNode(final Integer nodeId) {
        nodeCacheService.deleteNodes(Collections.singletonList(nodeId));
    }

    /**
     * Smazání více JP.
     *
     * @param nodeIds identifikátory JP
     */
    public void deleteNodes(final Collection<Integer> nodeIds) {
        nodeCacheService.deleteNodes(nodeIds);
    }

    /**
     * Odstranení vazby mezi nodem a digitalizátem.
     *
     * @param nodeId    identifikátor JP
     * @param daoLinkId identifikátor mazaného záznamu
     */
    public void deleteDaoLink(final Integer nodeId, final Integer daoLinkId) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrDaoLink> daoLinks = cachedNode.getDaoLinks();
        if (daoLinks == null) {
            throw new ObjectNotFoundException("Seznam je prázdný, nelze z něj odebírat navázané položky z digitálních entit", BaseCode.ID_NOT_EXIST);
        }
        Iterator<ArrDaoLink> iterator = daoLinks.iterator();
        while (iterator.hasNext()) {
            ArrDaoLink item = iterator.next();
            if (daoLinkId.equals(item.getDaoLinkId())) {
                iterator.remove();
                nodeCacheService.saveNode(cachedNode, true);
                return;
            }
        }
        throw new ObjectNotFoundException("Záznam nebyl nalezen v seznamu objektů uložených v cache", BaseCode.ID_NOT_EXIST);
    }

    /**
     * Vytvoření vazby mezi nodem a digitalizátem.
     *
     * @param nodeId  identifikátor JP
     * @param daoLink identifikátor vazby
     */
    public void createDaoLink(final Integer nodeId, final ArrDaoLink daoLink) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrDaoLink> daoLinks = cachedNode.getDaoLinks();
        if (daoLinks == null) {
            daoLinks = new ArrayList<>();
            cachedNode.setDaoLinks(daoLinks);
        }
        daoLinks.add(daoLink);
        nodeCacheService.saveNode(cachedNode, true);
    }

    /**
     * Aktualizace vazeb mezi nodem a digitalizátem.
     *
     * @param nodeDaoLinkMap mapa identifikátor JP -> seznam vazeb
     */
    public void updateDaoLinks(Collection<Integer> nodeIds, Collection<ArrDaoLink> daoLinks) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return;
        }
        Map<Integer, List<ArrDaoLink>> nodeDaoLinkMap = daoLinks != null
                ? daoLinks.stream().collect(Collectors.groupingBy(daoLink -> daoLink.getNodeId()))
                : Collections.emptyMap();
        Collection<RestoredNode> cachedNodes = nodeCacheService.getNodes(nodeIds).values();
        for (RestoredNode cachedNode : cachedNodes) {
            cachedNode.setDaoLinks(nodeDaoLinkMap.get(cachedNode.getNodeId()));
        }
        nodeCacheService.saveNodes(cachedNodes, true);
    }

    /**
     * Zrušení všech vazeb mezi nodem a digitalizátem.
     *
     * @param nodeId  identifikátor JP
     */
    public void clearDaoLinks(final Integer nodeId) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        cachedNode.setDaoLinks(null);
        nodeCacheService.saveNode(cachedNode, true);
    }

    /**
     * Vytvoření hodnoty atributu u nodu.
     *
     * @param nodeId
     *            identifikátor JP
     * @param descItem
     *            vytvářený hodnota atributu
     * @param flush
     *            priznak na provedeni flush
     */
    public void createDescItem(final Integer nodeId, final ArrDescItem descItem, BatchChangeContext changeContext) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
		cachedNode.addDescItem(descItem);
        nodeCacheService.saveNode(cachedNode, changeContext.getFlushNodeCache());
    }

    /**
     * Mazaná hodnota atributu u nodu.
     *
     * @param nodeId           identifikátor JP
     * @param descItemObjectId identifikátor hodnoty atributu
     */
    public void deleteDescItem(final Integer nodeId, final Integer descItemObjectId, BatchChangeContext changeContext) {
        deleteDescItems(nodeId, Collections.singletonList(descItemObjectId), changeContext);
    }

    /**
     * Hromadné mazání hodnoty atributu u nodu.
     *
     * @param nodeId            identifikátor JP
     * @param descItemObjectIds identifikátory hodnot atributů
     */
    public void deleteDescItems(final Integer nodeId, final List<Integer> descItemObjectIds,
                                BatchChangeContext changeContext) {
        Set<Integer> objIdsToRemove = new HashSet<>(descItemObjectIds);

        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrDescItem> descItems = cachedNode.getDescItems();
        if (descItems == null) {
            throw new ObjectNotFoundException("Seznam je prázdný, nelze z něj odebírat navázané položky z hodnot atributů", BaseCode.ID_NOT_EXIST);
        }
        Iterator<ArrDescItem> iterator = descItems.iterator();
        while (iterator.hasNext()) {
            ArrDescItem item = iterator.next();
            if (objIdsToRemove.contains(item.getDescItemObjectId())) {
                objIdsToRemove.remove(item.getDescItemObjectId());
                iterator.remove();
                if (objIdsToRemove.size() == 0) {
                    break;
                }
            }
        }

        if (objIdsToRemove.size() > 0) {
            throw new ObjectNotFoundException("Záznam nebyl nalezen v seznamu objektů uložených v cache",
                    BaseCode.ID_NOT_EXIST)
                            .set("nodeId", nodeId)
                            .set("descItemObjectIdsToRemove", objIdsToRemove);
        }

        nodeCacheService.saveNode(cachedNode, changeContext.getFlushNodeCache());
    }

    /**
     * Změna hodnoty atributu u nodu.
     *
     * @param nodeId
     *            identifikátor JP
     * @param descItem
     *            změněná hodnota atributu
     * @param move
     *            jedná se o přesun?
     *            true - nebudou se měnit data, pouze se naváže obalovaný objekt
     *            false - provede se úplné nahrazení hodnoty atributu
     * @param flush
     *            priznak pro flush zmen
     */
    public void changeDescItem(final Integer nodeId, final ArrDescItem descItem,
                               final boolean move, BatchChangeContext changeContext) {
        changeDescItems(nodeId, Collections.singletonList(descItem), move, changeContext);
    }

    /**
     * Změna hodnoty atributu u nodu.
     *
     * @param nodeId
     *            identifikátor JP
     * @param descItemList
     *            měněné hodnoty atributů
     * @param move
     *            jedná se o přesun?
     *            true - nebudou se měnit data, pouze se naváže obalovaný objekt
     *            false - provede se úplné nahrazení hodnoty atributu
     * @param flush
     *            priznak pro flush zmen
     */
    public void changeDescItems(final Integer nodeId, final Collection<ArrDescItem> descItemList,
                                final boolean move,
                                BatchChangeContext changeContext) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrDescItem> descItems = cachedNode.getDescItems();
        if (descItems == null) {
            throw new ObjectNotFoundException("Seznam je prázdný, nelze v něm měnit navázané prvky popisu, nodeId:"+nodeId, BaseCode.ID_NOT_EXIST);
        }
        for (ArrDescItem descItem : descItemList) {
            int index = -1;
            for (int i = 0; i < descItems.size(); i++) {
                ArrDescItem descItemLocal = descItems.get(i);
                if (descItemLocal.getDescItemObjectId().equals(descItem.getDescItemObjectId())) {
                    if (move) {
                        descItem.setData(descItemLocal.getData());
                    }
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                throw new ObjectNotFoundException("Záznam nebyl nalezen v seznamu objektů uložených v cache", BaseCode.ID_NOT_EXIST);
            }
            descItems.set(index, descItem);
        }
        nodeCacheService.saveNode(cachedNode, changeContext.getFlushNodeCache());
    }

    /**
     * Vytvoření vazby mezi JP a definicí řídících pravidel.
     *
     * @param nodeId        identifikátor JP
     * @param nodeExtension přidáváná vazba
     */
    public void createNodeExtension(final Integer nodeId, final ArrNodeExtension nodeExtension) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrNodeExtension> nodeExtensions = cachedNode.getNodeExtensions();
        if (nodeExtensions == null) {
            nodeExtensions = new ArrayList<>();
            cachedNode.setNodeExtensions(nodeExtensions);
        }
        nodeExtensions.add(nodeExtension);
        nodeCacheService.saveNode(cachedNode, true);
    }

    /**
     * Odstranění vazby mezi JP a definicí řídících pravidel.
     *
     * @param nodeId         identifikátor JP
     * @param nodeExtensionId identifikátor mazaného záznamu
     */
    public void deleteNodeExtension(final Integer nodeId, final Integer nodeExtensionId) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrNodeExtension> nodeExtensions = cachedNode.getNodeExtensions();
        if (nodeExtensions == null) {
            throw new ObjectNotFoundException("Seznam je prázdný, nelze z něj odebírat navázané položky z definic řídících pravidel", BaseCode.ID_NOT_EXIST);
        }
        Iterator<ArrNodeExtension> iterator = nodeExtensions.iterator();
        while (iterator.hasNext()) {
            ArrNodeExtension item = iterator.next();
            if (nodeExtensionId.equals(item.getNodeExtensionId())) {
                iterator.remove();
                nodeCacheService.saveNode(cachedNode, true);
                return;
            }
        }
        throw new ObjectNotFoundException("Záznam nebyl nalezen v seznamu objektů uložených v cache", BaseCode.ID_NOT_EXIST);
    }
}
