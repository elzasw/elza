package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;

import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrFindingAidVersionConformityInfo;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityInfo;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.FindingAidVersionConformityInfoRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorsRepository;
import cz.tacr.elza.repository.NodeConformityInfoRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 12. 2015
 */
@Service
public class ArrangementService {

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private FindingAidRepository findingAidRepository;

    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;

    @Autowired
    private NodeConformityInfoRepository nodeConformityInfoRepository;

    @Autowired
    private NodeConformityErrorsRepository nodeConformityErrorsRepository;

    @Autowired
    private NodeConformityMissingRepository nodeConformityMissingRepository;

    @Autowired
    private FindingAidVersionConformityInfoRepository findingAidVersionConformityInfoRepository;

    public ArrFindingAid findFindingAidByRootNodeUUID(String uuid) {
        Assert.notNull(uuid);

        return findingAidRepository.findFindingAidByRootNodeUUID(uuid);
    }

    public ArrFindingAid createFindingAid(String name, RulRuleSet ruleSet, RulArrangementType arrangementType,
            ArrChange change) {
        ArrFindingAid findingAid = new ArrFindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName(name);

        findingAid = findingAidRepository.save(findingAid);

//        Assert.isTrue(ruleSet.equals(arrangementType.getRuleSet()));

        ArrLevel rootNode = createLevel(change, null);
        createVersion(change, findingAid, arrangementType, ruleSet, rootNode);

        return findingAid;
    }

    public ArrFindingAidVersion createVersion(final ArrChange createChange, final ArrFindingAid findingAid,
            final RulArrangementType arrangementType, final RulRuleSet ruleSet, final ArrLevel rootNode) {
        ArrFindingAidVersion version = new ArrFindingAidVersion();
        version.setCreateChange(createChange);
        version.setArrangementType(arrangementType);
        version.setFindingAid(findingAid);
        version.setRuleSet(ruleSet);
        version.setRootLevel(rootNode);
        version.setLastChange(createChange);
        return findingAidVersionRepository.save(version);
    }

    public ArrLevel createLevel(final ArrChange createChange, final ArrNode parentNode) {
        ArrLevel level = new ArrLevel();
        level.setPosition(1);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode());
        return levelRepository.save(level);
    }

    public ArrLevel createLevel(final ArrChange createChange, final ArrNode parentNode, final Integer position) {
        Assert.notNull(createChange);

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode());
        return levelRepository.save(level);
    }

    public ArrLevel createLevel(ArrChange createChange, ArrNode node, ArrNode parentNode, int position) {
        Assert.notNull(createChange);

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(node);
        return levelRepository.save(level);
    }

    public ArrNode createNode() {
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        node.setUuid(UUID.randomUUID().toString());
        return nodeRepository.save(node);
    }

    public ArrNode createNode(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return createNode();
        }
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        node.setUuid(uuid);
        return nodeRepository.save(node);
    }

    public ArrChange createChange() {
        ArrChange change = new ArrChange();
        change.setChangeDate(LocalDateTime.now());

        return changeRepository.save(change);
    }

    /**
     * Smaže archivní pomůcku se zadaným id. Maže kompletní strukturu se všemi závislostmi.
     *
     * @param findingAidId id archivní pomůcky
     */
    public void deleteFindingAid(final Integer findingAidId) {
        Assert.notNull(findingAidId);

        if (!findingAidRepository.exists(findingAidId)) {
            return;
        }

//        bulkActionService.getBulkActionState(findingAidId).forEach(state -> {
//            if (state.getState() == State.RUNNING) {
//                throw new IllegalStateException("Archivní pomůcku nelze smazat protože běží hromadná akce.");
//            }
//        });

        ArrFindingAidVersion version = getOpenVersionByFindingAidId(findingAidId);

        ArrLevel rootLevel = version.getRootLevel();
        ArrNode node = rootLevel.getNode();

        findingAidVersionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId).forEach(deleteVersion ->
            deleteVersion(deleteVersion)
        );

        deleteLevelCascade(rootLevel);
        nodeRepository.delete(node);
        findingAidRepository.delete(findingAidId);
    }

    private void deleteVersion(ArrFindingAidVersion version) {
        Assert.notNull(version);

//        updateConformityInfoService.terminateWorkerInVersion(version);

        nodeConformityInfoRepository.findByFaVersion(version).forEach(conformityInfo -> {
            deleteConformityInfo(conformityInfo);
        });

        ArrFindingAidVersionConformityInfo versionConformityInfo = findingAidVersionConformityInfoRepository.findByFaVersion(version);
        if (versionConformityInfo != null) {
            findingAidVersionConformityInfoRepository.delete(versionConformityInfo);
        }

        findingAidVersionRepository.delete(version);
    }

    private void deleteConformityInfo(ArrNodeConformityInfo conformityInfo) {
        nodeConformityErrorsRepository.findByNodeConformityInfo(conformityInfo).forEach(error ->
            nodeConformityErrorsRepository.delete(error)
        );
        nodeConformityMissingRepository.findByNodeConformityInfo(conformityInfo).forEach(error ->
            nodeConformityMissingRepository.delete(error)
        );

        nodeConformityInfoRepository.delete(conformityInfo);
    }

    private void deleteLevelCascade(final ArrLevel level) {
        Set<ArrNode> nodes = new HashSet<>();
        ArrNode parentNode = level.getNode();
        for (ArrLevel childLevel : levelRepository.findByParentNode(parentNode)) {
            nodes.add(childLevel.getNode());
            deleteLevelCascade(childLevel);
        }

        for (ArrDescItem descItem : descItemRepository.findByNodeOrderByCreateChangeAsc(parentNode)) {
            deleteDescItemInner(descItem);
        }

        levelRepository.delete(level);
        nodes.forEach(node -> {
            deleteNode(node);
        });
    }

    private void deleteNode(ArrNode node) {
        Assert.notNull(node);

        nodeRegisterRepository.findByNode(node).forEach(relation -> {
            nodeRegisterRepository.delete(relation);
        });

        nodeConformityInfoRepository.findByNode(node).forEach(conformityInfo -> {
            deleteConformityInfo(conformityInfo);
        });

        nodeRepository.delete(node);
    }

    private void deleteDescItemInner(final ArrDescItem descItem) {
        Assert.notNull(descItem);

        dataRepository.findByDescItem(descItem).forEach(data -> dataRepository.delete(data));
        descItemRepository.delete(descItem);
    }

    /**
     * Načte neuzavřenou verzi archivní pomůcky.
     *
     * @param findingAidId      id archivní pomůcky
     * @return                  verze
     */
    public ArrFindingAidVersion getOpenVersionByFindingAidId(@RequestParam(value = "findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);
        ArrFindingAidVersion faVersion = findingAidVersionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);

        return faVersion;
    }

    public ArrLevel deleteLevelCascade(final ArrLevel level, final ArrChange deleteChange) {
        //pokud je level sdílený, smažeme pouze entitu, atributy ponecháme
        if (isLevelShared(level)) {
            return deleteLevelInner(level, deleteChange);
        }


        for (ArrLevel childLevel : levelRepository
                .findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(level.getNode())) {
            deleteLevelCascade(childLevel, deleteChange);
        }

        for (ArrDescItem descItem : descItemRepository.findByNodeAndDeleteChangeIsNull(level.getNode())) {
            deleteDescItemInner(descItem, deleteChange);
        }

        return deleteLevelInner(level, deleteChange);
    }

    private boolean isLevelShared(final ArrLevel level) {
        Assert.notNull(level);

        return levelRepository.countByNode(level.getNode()) > 1;
    }

    private ArrLevel deleteLevelInner(final ArrLevel level, final ArrChange deleteChange) {
        Assert.notNull(level);

        ArrNode node = level.getNode();
        node.setLastUpdate(LocalDateTime.now());
        nodeRepository.save(node);

        level.setDeleteChange(deleteChange);
        return levelRepository.save(level);
    }

    private void deleteDescItemInner(final ArrDescItem descItem, final ArrChange deleteChange) {
        Assert.notNull(descItem);

        descItem.setDeleteChange(deleteChange);
        ArrDescItem descItemTmp = new ArrDescItem();
        BeanUtils.copyProperties(descItem, descItemTmp);
        descItemRepository.save(descItemTmp);
    }

    /**
     * Vrací další identifikátor objektu pro atribut (oproti PK se zachovává při nové verzi)
     *
     * TODO:
     * Není dořešené, může dojít k přidělení stejného object_id dvěma různýmhodnotám atributu.
     * Řešit v budoucnu zrušením object_id (pravděpodobně GUID) nebo vytvořením nové entity,
     * kde bude object_id primární klíč a bude tak generován pomocí sekvencí hibernate.
     *
     * @return Identifikátor objektu
     */
    public Integer getNextDescItemObjectId() {
        Integer maxDescItemObjectId = descItemRepository.findMaxDescItemObjectId();
        if (maxDescItemObjectId == null) {
            maxDescItemObjectId = 0;
        }
        return maxDescItemObjectId + 1;
    }
}
