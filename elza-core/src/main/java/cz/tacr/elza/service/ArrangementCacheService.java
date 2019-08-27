package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;

/**
 * Serviska pro operace nad JP, která současně propisuje změny do cache.
 *
 * @author Martin Šlapa
 * @since 31.01.2017
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
     * Vytvoření vazby mezi nodem a rejstříkovým heslem.
     *
     * @param nodeId       identifikátor JP
     * @param nodeRegister přiřazení rejstříkových hesel
     * @return upravený cache záznam
     */
    public void createNodeRegister(final Integer nodeId, final ArrNodeRegister nodeRegister) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrNodeRegister> nodeRegisters = cachedNode.getNodeRegisters();
        if (nodeRegisters == null) {
            nodeRegisters = new ArrayList<>();
            cachedNode.setNodeRegisters(nodeRegisters);
        }
        nodeRegisters.add(nodeRegister);
        nodeCacheService.saveNode(cachedNode);
    }

    /**
     * Změna vazby mezi nodem a rejstříkovým heslem.
     *
     * @param nodeId          identifikátor JP
     * @param nodeRegisterOld původní záznam
     * @param nodeRegisterNew nový záznam
     * @return upravený cache záznam
     */
    public void changeNodeRegister(final Integer nodeId, final ArrNodeRegister nodeRegisterOld, final ArrNodeRegister nodeRegisterNew) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrNodeRegister> nodeRegisters = cachedNode.getNodeRegisters();
        if (nodeRegisters == null) {
            throw new ObjectNotFoundException("Seznam je prázdný, nelze v něj měnit navázané položky z rejstříků", BaseCode.ID_NOT_EXIST);
        }
        int index = -1;
        for (int i = 0; i < nodeRegisters.size(); i++) {
            if (nodeRegisters.get(i).getNodeRegisterId().equals(nodeRegisterOld.getNodeRegisterId())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ObjectNotFoundException("Záznam nebyl nalezen v seznamu objektů uložených v cache", BaseCode.ID_NOT_EXIST);
        }
        nodeRegisters.set(index, nodeRegisterNew);
        nodeCacheService.saveNode(cachedNode);
    }

    /**
     * Odstranění vazby mezi nodem a rejstříkovým heslem.
     *
     * @param nodeId         identifikátor JP
     * @param nodeRegisterId identifikátor mazaného záznamu
     */
    public void deleteNodeRegister(final Integer nodeId, final Integer nodeRegisterId) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrNodeRegister> nodeRegisters = cachedNode.getNodeRegisters();
        if (nodeRegisters == null) {
            throw new ObjectNotFoundException("Seznam je prázdný, nelze z něj odebírat navázané položky z rejstříků", BaseCode.ID_NOT_EXIST);
        }
        Iterator<ArrNodeRegister> iterator = nodeRegisters.iterator();
        while (iterator.hasNext()) {
            ArrNodeRegister item = iterator.next();
            if (nodeRegisterId.equals(item.getNodeRegisterId())) {
                iterator.remove();
                nodeCacheService.saveNode(cachedNode);
                return;
            }
        }
        throw new ObjectNotFoundException("Záznam nebyl nalezen v seznamu objektů uložených v cache", BaseCode.ID_NOT_EXIST);
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
                nodeCacheService.saveNode(cachedNode);
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
        nodeCacheService.saveNode(cachedNode);
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
        nodeCacheService.saveNodes(cachedNodes);
    }

    /**
     * Zrušení všech vazeb mezi nodem a digitalizátem.
     *
     * @param nodeId  identifikátor JP
     */
    public void clearDaoLinks(final Integer nodeId) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        cachedNode.setDaoLinks(null);
        nodeCacheService.saveNode(cachedNode);
    }

    /**
     * Vytvoření hodnoty atributu u nodu.
     *
     * @param nodeId   identifikátor JP
     * @param descItem vytvářený hodnota atributu
     */
    public void createDescItem(final Integer nodeId, final ArrDescItem descItem) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
		cachedNode.addDescItem(descItem);
        nodeCacheService.saveNode(cachedNode);
    }

    /**
     * Vytvoření hodnot atributu u nodu.
     *
     * @param nodeId   identifikátor JP
     * @param newDescItems vytvářené hodnoty atributu
     */
    public void createDescItems(final Integer nodeId, final Collection<ArrDescItem> newDescItems) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
		cachedNode.addDescItems(newDescItems);
        nodeCacheService.saveNode(cachedNode);
    }

    /**
     * Mazaná hodnota atributu u nodu.
     *
     * @param nodeId           identifikátor JP
     * @param descItemObjectId identifikátor hodnoty atributu
     */
    public void deleteDescItem(final Integer nodeId, final Integer descItemObjectId) {
        deleteDescItems(nodeId, Collections.singletonList(descItemObjectId));
    }

    /**
     * Hromadné mazání hodnoty atributu u nodu.
     *
     * @param nodeId            identifikátor JP
     * @param descItemObjectIds identifikátory hodnot atributů
     */
    public void deleteDescItems(final Integer nodeId, final List<Integer> descItemObjectIds) {
        CachedNode cachedNode = nodeCacheService.getNode(nodeId);
        List<ArrDescItem> descItems = cachedNode.getDescItems();
        if (descItems == null) {
            throw new ObjectNotFoundException("Seznam je prázdný, nelze z něj odebírat navázané položky z hodnot atributů", BaseCode.ID_NOT_EXIST);
        }
        Iterator<ArrDescItem> iterator = descItems.iterator();
        List<Integer> descItemObjectIdsToRemove = new ArrayList<>(descItemObjectIds);
        while (iterator.hasNext()) {
            ArrDescItem item = iterator.next();
            if (descItemObjectIdsToRemove.contains(item.getDescItemObjectId())) {
                descItemObjectIdsToRemove.remove(item.getDescItemObjectId());
                iterator.remove();
            }
            if (descItemObjectIdsToRemove.size() == 0) {
                nodeCacheService.saveNode(cachedNode);
                return;
            }
        }

        throw new ObjectNotFoundException("Záznam nebyl nalezen v seznamu objektů uložených v cache",
                BaseCode.ID_NOT_EXIST)
                        .set("nodeId", nodeId)
                        .set("descItemObjectIdsToRemove", descItemObjectIdsToRemove);
    }

    /**
     * Změna hodnoty atributu u nodu.
     *
     * @param nodeId   identifikátor JP
     * @param descItem změněná hodnota atributu
     * @param move     jedná se o přesun?
     *                 true - nebudou se měnit data, pouze se naváže obalovaný objekt
     *                 false - provede se úplné nahrazení hodnoty atributu
     */
    public void changeDescItem(final Integer nodeId, final ArrDescItem descItem, final boolean move) {
        changeDescItems(nodeId, Collections.singletonList(descItem), move);
    }

    /**
     * Změna hodnoty atributu u nodu.
     *
     * @param nodeId       identifikátor JP
     * @param descItemList měněné hodnoty atributů
     * @param move         jedná se o přesun?
     *                     true - nebudou se měnit data, pouze se naváže obalovaný objekt
     *                     false - provede se úplné nahrazení hodnoty atributu
     */
    public void changeDescItems(final Integer nodeId, final Collection<ArrDescItem> descItemList, final boolean move) {
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
        nodeCacheService.saveNode(cachedNode);
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
        nodeCacheService.saveNode(cachedNode);
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
                nodeCacheService.saveNode(cachedNode);
                return;
            }
        }
        throw new ObjectNotFoundException("Záznam nebyl nalezen v seznamu objektů uložených v cache", BaseCode.ID_NOT_EXIST);
    }
}
