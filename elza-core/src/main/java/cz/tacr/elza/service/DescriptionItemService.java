package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.NodeRepository;


/**
 * Serviska pro správu hodnot atributů.
 *
 * @author Martin Šlapa
 * @since 13. 1. 2016
 */
@Service
public class DescriptionItemService {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    /**
     * Kontrola otevřené verze.
     *
     * @param version verze
     */
    private void checkFindingAidVersionLock(final ArrFindingAidVersion version) {
        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze provést verzovanou změnu v uzavřené verzi.");
        }
    }

    /**
     * Uložení uzlu - optimistické zámky
     *
     * @param node uzel
     * @return uložený uzel
     */
    private ArrNode saveNode(final ArrNode node) {
        node.setLastUpdate(LocalDateTime.now());
        // TODO: pokud je v node změněná verze ručně, hibernate to ignoruje
        return nodeRepository.save(node);
    }

    /**
     * Smaže hodnotu atributu.
     * - s kontrolou verze uzlu
     * - se spuštěním validace uzlu
     *
     * @param descItemObjectId    identifikátor hodnoty atributu
     * @param nodeVersion         verze uzlu (optimistické zámky)
     * @param findingAidVersionId identifikátor verze archivní pomůcky
     */
    public void deleteDescriptionItem(final Integer descItemObjectId,
                                      final Integer nodeVersion,
                                      final Integer findingAidVersionId) {
        Assert.notNull(descItemObjectId);
        Assert.notNull(nodeVersion);
        Assert.notNull(findingAidVersionId);

        ArrChange change = arrangementService.createChange();
        ArrFindingAidVersion findingAidVersion = findingAidVersionRepository.findOne(findingAidVersionId);
        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItemObjectId);

        if (descItems.size() > 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        } else if (descItems.size() == 0) {
            throw new IllegalStateException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }

        ArrDescItem descItem = descItems.get(0);
        descItem.getNode().setVersion(nodeVersion);

        // uložení uzlu (kontrola optimistických zámků)
        saveNode(descItem.getNode());

        deleteDescriptionItem(descItem, findingAidVersion, change);

        // uložení poslední uživatelské změny nad AP k verzi AP
        arrangementService.saveLastChangeFaVersion(change, findingAidVersion);

        // validace uzlu
        ruleService.conformityInfo(findingAidVersionId, Arrays.asList(descItem.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, null, null, Arrays.asList(descItem));
    }


    /**
     * Vytvoření hodnoty atributu.
     * - s kontrolou verze uzlu
     * - se spuštěním validace uzlu
     *
     * @param descItem              hodnota atributu
     * @param nodeId                identifikátor uzlu
     * @param nodeVersion           verze uzlu (optimistické zámky)
     * @param findingAidVersionId   identifikátor verze archivní pomůcky
     */
    public void createDescriptionItem(final ArrDescItem descItem,
                                      final Integer nodeId,
                                      final Integer nodeVersion,
                                      final Integer findingAidVersionId) {
        Assert.notNull(descItem);
        Assert.notNull(nodeId);
        Assert.notNull(nodeVersion);
        Assert.notNull(findingAidVersionId);

        ArrChange change = arrangementService.createChange();
        ArrFindingAidVersion findingAidVersion = findingAidVersionRepository.findOne(findingAidVersionId);

        ArrNode node = nodeRepository.findOne(nodeId);

        // uložení uzlu (kontrola optimistických zámků)
        saveNode(node);

        descItem.setNode(node);
        descItem.setCreateChange(change);
        descItem.setDeleteChange(null);
        descItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

        createDescriptionItemWithData(descItem, findingAidVersion, change);

        // uložení poslední uživatelské změny nad AP k verzi AP
        arrangementService.saveLastChangeFaVersion(change, findingAidVersion);

        // validace uzlu
        ruleService.conformityInfo(findingAidVersionId, Arrays.asList(descItem.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, null, null, Arrays.asList(descItem));
    }

    /**
     * Vytvoření hodnoty atributu s daty.
     *
     * @param descItem hodnota atributu
     * @param version  verze archivní pomůcky
     * @param change   změna operace
     */
    public void createDescriptionItemWithData(final ArrDescItem descItem,
                                      final ArrFindingAidVersion version,
                                      final ArrChange change) {
        Assert.notNull(descItem);
        Assert.notNull(version);
        Assert.notNull(change);

        // pro vytváření musí být verze otevřená
        checkFindingAidVersionLock(version);

        // kontrola validity typu a specifikace
        checkValidTypeAndSpec(descItem);

        int position = 0;

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsAfterPosition(
                descItem.getDescItemType(),
                descItem.getNode(),
                0);

        for (ArrDescItem item : descItems) {
            if (item.getPosition() > position) {
                position = item.getPosition();
            }
        }

        if (descItem.getPosition() == null || (descItem.getPosition() > position)) {
            descItem.setPosition(position + 1);
        }

        // načtení hodnot, které je potřeba přesunout níž
        descItems = descItemRepository.findOpenDescItemsAfterPosition(
                descItem.getDescItemType(),
                descItem.getNode(),
                descItem.getPosition() - 1);

        for (ArrDescItem descItemMove : descItems) {

            descItemMove.setDeleteChange(change);
            descItemRepository.save(descItemMove);

            ArrDescItem descItemNew = new ArrDescItem();

            BeanUtils.copyProperties(descItemMove, descItemNew);
            descItemNew.setDescItemId(null);
            descItemNew.setDeleteChange(null);
            descItemNew.setCreateChange(change);
            descItemNew.setPosition(descItemMove.getPosition() + 1);

            descItemRepository.save(descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyDescItemData(descItemMove, descItemNew);
        }

        descItem.setCreateChange(change);
        descItemFactory.saveDescItemWithData(descItem, true);
    }

    /**
     * Kontrola typu a specifikace.
     *
     * @param descItem hodnota atributu
     */
    private void checkValidTypeAndSpec(final ArrDescItem descItem) {
        RulDescItemType descItemType = descItem.getDescItemType();
        RulDescItemSpec descItemSpec = descItem.getDescItemSpec();

        Assert.notNull(descItemType, "Hodnota atributu musí mít vyplněný typ");

        if (descItemType.getUseSpecification()) {
            Assert.notNull(descItemSpec, "Pro typ atributu je specifikace povinná");
        }

        if (descItemSpec != null) {
            List<RulDescItemSpec> descItemSpecs = descItemSpecRepository.findByDescItemType(descItemType);
            if (!descItemSpecs.contains(descItemType)) {
                throw new IllegalStateException("Specifikace neodpovídá typu hodnoty atributu");
            }
        }
    }

    /**
     * Smaže hodnotu atributu.
     *
     * @param descItem hodnota atributu
     * @param version  verze archivní pomůcky
     * @param change   změna operace
     */
    public void deleteDescriptionItem(final ArrDescItem descItem,
                                      final ArrFindingAidVersion version,
                                      final ArrChange change) {
        Assert.notNull(descItem);
        Assert.notNull(version);
        Assert.notNull(change);

        // pro mazání musí být verze otevřená
        checkFindingAidVersionLock(version);

        // načtení hodnot, které je potřeba přesunout výš
        List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsAfterPosition(
                descItem.getDescItemType(),
                descItem.getNode(),
                descItem.getPosition());

        for (ArrDescItem descItemMove : descItems) {

            descItemMove.setDeleteChange(change);
            descItemRepository.save(descItemMove);

            ArrDescItem descItemNew = new ArrDescItem();

            BeanUtils.copyProperties(descItemMove, descItemNew);
            descItemNew.setDescItemId(null);
            descItemNew.setDeleteChange(null);
            descItemNew.setCreateChange(change);
            descItemNew.setPosition(descItemMove.getPosition() - 1);

            descItemRepository.save(descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyDescItemData(descItemMove, descItemNew);
        }

        descItem.setDeleteChange(change);
        descItemRepository.save(descItem);
    }

    /**
     * Provede kopii dat mezi hodnotama atributů.
     *
     * @param descItemFrom z hodnoty atributu
     * @param descItemTo   do hodnoty atributu
     */
    private void copyDescItemData(final ArrDescItem descItemFrom, final ArrDescItem descItemTo) {
        List<ArrData> dataList = dataRepository.findByDescItem(descItemFrom);

        if (dataList.size() != 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        }

        ArrData data = dataList.get(0);

        try {
            ArrData dataNew = data.getClass().getConstructor().newInstance();

            BeanUtils.copyProperties(data, dataNew);
            dataNew.setDataId(null);
            dataNew.setDescItem(descItemTo);

            dataRepository.save(dataNew);
        } catch (Exception e) {
            throw new IllegalStateException(e.getCause());
        }
    }

}
