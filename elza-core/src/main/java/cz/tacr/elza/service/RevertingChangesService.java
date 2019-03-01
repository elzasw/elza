package cz.tacr.elza.service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.config.ConfigView;
import cz.tacr.elza.config.view.ViewTitles;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.LockedValueRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.eventnotification.events.EventFunds;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.Change;
import cz.tacr.elza.service.vo.ChangesResult;
import cz.tacr.elza.service.vo.TitleItemsByType;

/**
 * Servisní třída pro práci s obnovou změn v archivní souboru - "UNDO".
 *
 * @author Martin Šlapa
 * @since 03.11.2016
 */
@Service
public class RevertingChangesService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserService userService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private StartupService startupService;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private ConfigView configView;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private LockedValueRepository usedValueRepository;

    @Autowired
    private StructuredItemRepository structuredItemRepository;

    @Autowired
    private StructuredObjectRepository structuredObjectRepository;

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS.
     *
     * @param fundVersion verze AS nad kterou provádím vyhledávání
     * @param node        JP omezující vyhledávání
     * @param maxSize     maximální počet záznamů
     * @param offset      počet přeskočených záznamů
     * @param fromChange  změna, vůči které chceme počítat offset (pokud není vyplněn, bere se vždy poslední)
     * @return výsledek hledání
     */
    public ChangesResult findChanges(@NotNull final ArrFundVersion fundVersion,
                                     @Nullable final ArrNode node,
                                     final int maxSize,
                                     final int offset,
                                     @Nullable final ArrChange fromChange) {
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");

        Integer fundId = fundVersion.getFund().getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();
        Integer fromChangeId = fromChange == null ? null : fromChange.getChangeId();
        boolean isNodeContext = node != null;

        // dotaz pro vyhledání
        Query query = createFindQuery(fundId, nodeId, maxSize, offset, fromChangeId);

        // dotaz pro celkový počet položek
        Query queryCount = createFindQueryCount(fundId, nodeId, fromChangeId);

        // dotaz pro zjištění poslední změny (pro nastavení parametru outdated)
        Query queryLastChange = createQueryLastChange(fundId, nodeId);

        Object queryResult = queryLastChange.getSingleResult();
        if (queryResult == null) {
            throw new BusinessException("Failed to find valid last change", ArrangementCode.DATA_NOT_FOUND)
                    .set("nodeId", nodeId);
        }
        ChangeResult lastChange = convertResult((Object[]) queryResult);
        Integer count = ((Number) queryCount.getSingleResult()).intValue();

        // nalezené změny
        List<ChangeResult> sqlResult = convertResults(query.getResultList());

        // typ oprávnění, podle kterého se určuje, zda-li je možné provést rozsáhlejší revert, nebo pouze své změny
        boolean fullRevertPermission = hasFullRevertPermission(fundVersion.getFund());

        // kontrola, že neexistuje předchozí změna od jiného uživatele (true - neexistuje a můžu provést revert)
        boolean canRevertByUserBefore = true;

        // pokud nemám vyšší oprávnění a načítám starší změny, kontroluji, že neexistuje předchozí změna jiného uživatele
        if (!fullRevertPermission && offset > 0) {
            Query findUserChangeQuery = createFindUserChangeQuery(fundId, nodeId, offset, fromChangeId);
            Integer otherUserChangeCount = ((BigInteger) findUserChangeQuery.getSingleResult()).intValue();
            canRevertByUserBefore = otherUserChangeCount == 0;
        }

        // kontrola, že neexistuje změna, který by uživateli znemožnila provést revert (true - neexistuje a můžu provést revert)
        boolean canReverBefore = true;

        if (offset > 0 && isNodeContext) {
            Query findQueryToChange = createFindQuery(fundId, nodeId, 1, offset, fromChangeId);
            ChangeResult toChangeResult = convertResult((Object[]) findQueryToChange.getSingleResult());
            Query findQueryCountBefore = createFindQueryCountBefore(fundId, nodeId, fromChangeId, toChangeResult.getChangeId());
            Integer countBefore = ((BigInteger) findQueryCountBefore.getSingleResult()).intValue();

            if (countBefore > 0) {
                canReverBefore = false;
            }
        }

        List<Change> changes = convertChangeResults(sqlResult, fundVersion, fullRevertPermission, canReverBefore, canRevertByUserBefore, isNodeContext);

        // sestavení odpovědi
        ChangesResult changesResult = new ChangesResult();
        changesResult.setMaxSize(maxSize);
        changesResult.setOffset(offset);
        changesResult.setOutdated(fromChange != null && !fromChange.getChangeId().equals(lastChange.getChangeId()));
        changesResult.setTotalCount(count);
        changesResult.setChanges(changes);

        return changesResult;
    }

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS v závislosti na zvoleném datumu.
     *
     * @param fundVersion verze AS nad kterou provádím vyhledávání
     * @param node        JP omezující vyhledávání
     * @param maxSize     maximální počet záznamů
     * @param fromDate    datum od kterého vyhledávám v historii
     * @param fromChange  změna, vůči které chceme počítat offset - v tomto případě musí být vyplněná
     * @return výsledek hledání
     */
    public ChangesResult findChangesByDate(@NotNull final ArrFundVersion fundVersion,
                                           @Nullable final ArrNode node,
                                           final int maxSize,
                                           @NotNull final LocalDateTime fromDate,
                                           @NotNull final ArrChange fromChange) {
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");
        Validate.notNull(fromDate, "Datum od musí být vyplněn");
        Validate.notNull(fromChange, "Změna musí být vyplněna");

        Integer fundId = fundVersion.getFund().getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();
        Integer fromChangeId = fromChange.getChangeId();

        // dotaz pro zjištění pozice v seznamu podle datumu
        Query queryIndex = createQueryIndex(fundId, nodeId, fromChangeId, fromDate);

        final Object singleResult = queryIndex.getSingleResult();

        Integer count = null;
        if (singleResult instanceof Integer) {
            count = (Integer) singleResult;
        } else if (singleResult instanceof BigInteger) {
            count = ((BigInteger) queryIndex.getSingleResult()).intValue();
        } else {
            throw new SystemException("Nedefinovaný typ výsledku dotazu. (" + (singleResult == null ? "null" : singleResult.getClass().getSimpleName()) + ")");
        }

        return findChanges(fundVersion, node, maxSize, count, fromChange);
    }

    /**
     * Provede revertování dat ke zvolené změně.
     *
     * @param fund       AS nad kterým provádím obnovu
     * @param node       JP omezující obnovu
     * @param fromChange změna od které se provádí revert (pouze pro kontrolu, že se jedná o poslední)
     * @param toChange   změna ke které se provádí revert (včetně)
     */
    public void revertChanges(@NotNull final ArrFund fund,
                              @Nullable final ArrNode node,
                              @NotNull final ArrChange fromChange,
                              @NotNull final ArrChange toChange) {
        Validate.notNull(fund, "AS musí být vyplněn");
        Validate.notNull(fromChange, "Změna od musí být vyplněna");
        Validate.notNull(toChange, "Změna do musí být vyplněna");

        Integer fundId = fund.getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();

        ArrFundVersion openFundVersion = null;
        for (ArrFundVersion fundVersion : fund.getVersions()) {
            if (fundVersion.getLockChange() == null) {
                openFundVersion = fundVersion;
                break;
            }
        }

        // provede validaci prováděného revertování
        revertChangesValidateAction(fromChange, toChange, fundId, nodeId);

        // zastavení probíhajících výpočtů pro validaci uzlů u verzí
        stopConformityInfFundVersions(fund);

        Query nodeIdsQuery = findChangeNodeIdsQuery(fund, node, toChange);
        Set<Integer> nodeIdsChange = new HashSet<>(nodeIdsQuery.getResultList());

        Query updateEntityQuery;
        Query deleteEntityQuery;

        deleteEntityQuery = createConformityDeleteForeignEntityQuery(fund, node, /*toChange,*/ "arr_node_conformity_error");
        deleteEntityQuery.executeUpdate();

        deleteEntityQuery = createConformityDeleteForeignEntityQuery(fund, node, /*toChange,*/ "arr_node_conformity_missing");
        deleteEntityQuery.executeUpdate();

        deleteEntityQuery = createConformityDeleteEntityQuery(fund, node/*, toChange*/);
        deleteEntityQuery.executeUpdate();

        // drop used/fixed values
        usedValueRepository.deleteToChange(fund, toChange.getChangeId());

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_level", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_level", toChange);
        updateEntityQuery.executeUpdate();
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_node_register", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_node_register", toChange);
        updateEntityQuery.executeUpdate();
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_node_extension", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_node_extension", toChange);
        updateEntityQuery.executeUpdate();
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_dao_link", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_dao_link", toChange);
        updateEntityQuery.executeUpdate();
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createExtendUpdateEntityQuery(fund, node, "deleteChange", "arr_desc_item", "arr_item", toChange);
        updateEntityQuery.executeUpdate();

        TypedQuery<ArrData> arrDataQuery = findChangeArrDataQuery(fund, node, toChange);
        Set<ArrData> arrDataList = new HashSet<>(arrDataQuery.getResultList());

        /*deleteEntityQuery = createDeleteForeignEntityQuery(fund, node, "createChange", "arr_desc_item", "item", "arr_data", toChange);
        deleteEntityQuery.executeUpdate();*/

        deleteEntityQuery = createExtendDeleteEntityQuery(fund, node, "createChange", "arr_desc_item", /*"item",*/ "arr_item", toChange);
        deleteEntityQuery.executeUpdate();

        dataRepository.delete(arrDataList);

        updateEntityQuery = createUpdateOutputQuery(fund, node, toChange);
        updateEntityQuery.executeUpdate();

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_node_output", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_node_output", toChange);
        updateEntityQuery.executeUpdate();
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createUpdateActionQuery(fund, node, toChange);
        updateEntityQuery.executeUpdate();

        deleteEntityQuery = createDeleteActionNodeQuery(fund, node, toChange);
        deleteEntityQuery.executeUpdate();

        deleteEntityQuery = createDeleteActionRunQuery(fund, toChange);
        deleteEntityQuery.executeUpdate();

        Query deleteNotUseChangesQuery = createDeleteNotUseChangesQuery();
        deleteNotUseChangesQuery.executeUpdate();

        Set<Integer> deleteNodeIds = getNodeIdsToDelete();
        nodeIdsChange.removeAll(deleteNodeIds);

        nodeCacheService.deleteNodes(deleteNodeIds);

        Query deleteNotUseNodesQuery = createDeleteNotUseNodesQuery();
        deleteNotUseNodesQuery.executeUpdate();

        nodeCacheService.syncNodes(nodeIdsChange);

        if (CollectionUtils.isNotEmpty(deleteNodeIds) && openFundVersion != null) {
            eventNotificationService.publishEvent(new EventIdsInVersion(EventType.DELETE_NODES, openFundVersion.getFundVersionId(),
                    deleteNodeIds.toArray(new Integer[deleteNodeIds.size()])));
        }

        if (node == null) {
            Set<Integer> fundVersionIds = fund.getVersions().stream().map(ArrFundVersion::getFundVersionId).collect(Collectors.toSet());
            eventNotificationService.publishEvent(new EventFunds(EventType.FUND_INVALID, Collections.singleton(fundId), fundVersionIds));
        } else {
            if (openFundVersion != null) {
                eventNotificationService.publishEvent(new EventIdsInVersion(EventType.NODES_CHANGE, openFundVersion.getFundVersionId(), node.getNodeId()));
            }
        }

        levelTreeCacheService.invalidateFundVersion(fund);
        startupService.startNodeValidation();
    }

    private TypedQuery<ArrData> findChangeArrDataQuery(final ArrFund fund, final ArrNode node, final ArrChange change) {

        String changeNameColumn = "createChange";
        String table = "arr_desc_item";
        String subTable = "arr_item";

        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));

        String hql = String.format("SELECT i.data FROM %1$s i WHERE i.%2$s IN (%3$s)", subTable, changeNameColumn, nodesHql);
        TypedQuery<ArrData> query = entityManager.createQuery(hql, ArrData.class);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    /**
     * Vytvoří dotaz pro zjištění počtu položek, po kterých nelze provést revert v rámci JP.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @param toChangeId   identifikátor změny, vůči které provádíme vyhledávání
     * @return query objekt
     */
    private Query createFindQueryCountBefore(@NotNull final Integer fundId,
                                             @Nullable final Integer nodeId,
                                             @Nullable final Integer fromChangeId,
                                             @NotNull final Integer toChangeId) {
        String querySkeleton = createFindQuerySkeleton();

        String selectParams = "COUNT(*)";
        String querySpecification = "";

        List<String> wheres = new ArrayList<>();

        if (fromChangeId != null) {
            wheres.add("ch.change_id <= :fromChangeId");
        }

        wheres.add("ch.change_id >= :toChangeId");

        wheres.add("ch.type NOT IN (:types)");

        if (wheres.size() > 0) {
            querySpecification = "WHERE " + String.join(" AND ", wheres) + " " + querySpecification;
        }

        String queryString = String.format(querySkeleton, selectParams, createSubNodeQuery(fundId, nodeId), querySpecification);

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("types", Arrays.asList(ArrChange.Type.ADD_RECORD_NODE.name(),
                ArrChange.Type.DELETE_RECORD_NODE.name(),
                ArrChange.Type.UPDATE_DESC_ITEM.name(),
                ArrChange.Type.ADD_DESC_ITEM.name(),
                ArrChange.Type.DELETE_DESC_ITEM.name()));

        query.setParameter("fundId", fundId);

        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }

        query.setParameter("toChangeId", toChangeId);

        if (fromChangeId != null) {
            query.setParameter("fromChangeId", fromChangeId);
        }
        return query;
    }

    /**
     * Provádí validaci akce pro revertování změn.
     *
     * @param fromChange změna od které se provádí revert (pouze pro kontrolu, že se jedná o poslední)
     * @param toChange   změna ke které se provádí revert
     * @param fundId     identifikátor AS
     * @param nodeId     identifikátor JP
     */
    private void revertChangesValidateAction(final @NotNull ArrChange fromChange,
                                             final @NotNull ArrChange toChange,
                                             final @NotNull Integer fundId,
                                             final @Nullable Integer nodeId) {
        // dotaz pro zjištění poslední změny
        Query queryLastChange = createQueryLastChange(fundId, nodeId);

        Object queryResult = queryLastChange.getSingleResult();
        if (queryResult == null) {
            throw new BusinessException("Failed to find valid last change", ArrangementCode.DATA_NOT_FOUND)
                    .set("nodeId", nodeId);
        }
        ChangeResult lastChange = convertResult((Object[]) queryResult);

        if (!fromChange.getChangeId().equals(lastChange.getChangeId())) {
            throw new BusinessException("Existuje novější verze", ArrangementCode.EXISTS_NEWER_CHANGE);
        }

        if (toChange.getType() != null && toChange.getType().equals(ArrChange.Type.CREATE_AS)) {
            throw new BusinessException("Existuje blokující změna v JP", ArrangementCode.EXISTS_BLOCKING_CHANGE);
        }

        if (nodeId != null) {
            Query findQueryCountBefore = createFindQueryCountBefore(fundId, nodeId, fromChange.getChangeId(), toChange.getChangeId());
            Integer countBefore = ((BigInteger) findQueryCountBefore.getSingleResult()).intValue();
            if (countBefore > 0) {
                throw new BusinessException("Existuje blokující změna v JP", ArrangementCode.EXISTS_BLOCKING_CHANGE);
            }
        }
        // check if change includes structured types
        // unsupported operation till 1.1 (MT11)
        int itemsCnt = structuredItemRepository.countItemsWithinChangeRange(fundId, fromChange.getChangeId(),
                                                                            toChange.getChangeId());
        if (itemsCnt > 0) {
            throw new BusinessException("Změna ve strukturovaném objektu, změnu bude možné vrátit až od verze 1.1",
                    ArrangementCode.EXISTS_BLOCKING_CHANGE);
        }
        itemsCnt = structuredObjectRepository.countItemsWithinChangeRange(fundId, fromChange.getChangeId(),
                                                                          toChange.getChangeId());
        if (itemsCnt > 0) {
            throw new BusinessException("Změna ve strukturovaném objektu, změnu bude možné vrátit až od verze 1.1",
                    ArrangementCode.EXISTS_BLOCKING_CHANGE);
        }
    }

    private Query createUpdateOutputQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("UPDATE arr_output_definition d SET d.state = :stateNew WHERE d.state IN (:stateOld) AND d IN (" +
                "SELECT no.outputDefinition FROM arr_node_output no WHERE no.node IN (" + createHqlSubNodeQuery(fund, node) + ") AND no.createChange >= :change OR no.deleteChange >= :change" +
                ")");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        query.setParameter("stateNew", ArrOutputDefinition.OutputState.OUTDATED);
        query.setParameter("stateOld", Collections.singletonList(ArrOutputDefinition.OutputState.FINISHED));
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    /**
     * Smazání návazných entity hromadné akce.
     *
     * @param fund     AS nad kterým provádím obnovu
     * @param node     JP omezující obnovu
     * @param toChange změna ke které se provádí revert
     * @return
     */
    private Query createDeleteActionNodeQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("DELETE FROM arr_bulk_action_node n WHERE n.bulkActionRun IN (" +
                "SELECT rn.bulkActionRun FROM arr_bulk_action_node rn JOIN rn.bulkActionRun rs WHERE rn.node IN (" + createHqlSubNodeQuery(fund, node) + ") AND rs.change >= :change" +
                ")");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    /**
     * Smazání samotných hromadných akcí.
     *
     * @param fund     AS nad kterým provádím obnovu
     * @param toChange změna ke které se provádí revert
     * @return
     */
    private Query createDeleteActionRunQuery(final @NotNull ArrFund fund, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("DELETE FROM arr_bulk_action_run rd WHERE rd IN (SELECT r FROM arr_bulk_action_run r JOIN r.fundVersion v WHERE v.fund = :fund AND r.change >= :change)");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        return query;
    }

    private Query createUpdateActionQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("UPDATE arr_bulk_action_run r SET r.state = :stateNew WHERE r IN (" +
                "SELECT rs FROM arr_bulk_action_run rs JOIN rs.arrBulkActionNodes rn WHERE rn.node IN (" + createHqlSubNodeQuery(fund, node) + ") AND rs.change >= :change" +
                ") AND r.state = :stateOld");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        query.setParameter("stateNew", ArrBulkActionRun.State.OUTDATED);
        query.setParameter("stateOld", ArrBulkActionRun.State.FINISHED);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    /**
     * Zastavení probíhajících výpočtů pro validaci uzlů u verzí.
     *
     * @param fund AS nad kterým provádím zastavení
     */
    private void stopConformityInfFundVersions(final @NotNull ArrFund fund) {
        for (ArrFundVersion fundVersion : fund.getVersions()) {
            updateConformityInfoService.terminateWorkerInVersionAndWait(fundVersion.getFundVersionId());
        }
    }

    private Query createConformityDeleteEntityQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node/*, final @NotNull ArrChange toChange*/) {
        //String nodesHql = createHQLFindChanges("createChange", "arr_level", );
        //String hqlSubSelect = String.format("SELECT i.node FROM %1$s i WHERE %2$s IN (%3$s)", "arr_level", "createChange", nodesHql);
        String hql = String.format("DELETE FROM arr_node_conformity WHERE node IN (%1$s)", createHqlSubNodeQuery(fund, node));
        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        //query.setParameter("change", toChange);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    private Query createConformityDeleteForeignEntityQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, /*final @NotNull ArrChange toChange,*/ final @NotNull String table) {
        //String nodesHql = createHQLFindChanges("createChange", "arr_level", createHqlSubNodeQuery(fund, node));
        //String hqlSubSelect = String.format("SELECT i.node FROM %1$s i WHERE %2$s IN (%3$s)", "arr_level", "createChange", nodesHql);
        String hql = String.format("DELETE FROM %1$s ncx WHERE ncx.nodeConformity IN (SELECT nc FROM arr_node_conformity nc WHERE node IN (%2$s))", table, createHqlSubNodeQuery(fund, node));
        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        //query.setParameter("change", toChange);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    private Query createDeleteNotUseNodesQuery() {
        List<String[]> nodesTables = getNodesTables();
        List<String> unionPart = new ArrayList<>();
        for (String[] nodeTable : nodesTables) {
            unionPart.add(String.format("n.nodeId NOT IN (SELECT i.%2$s.nodeId FROM %1$s i WHERE i.%2$s IS NOT NULL)", nodeTable[0], nodeTable[1]));
        }
        String changesHql = String.join("\nAND\n", unionPart);

        String hql = String.format("DELETE FROM arr_node n WHERE %1$s", changesHql);

        return entityManager.createQuery(hql);
    }

    private Set<Integer> getNodeIdsToDelete() {
        List<String[]> nodesTables = getNodesTables();
        List<String> unionPart = new ArrayList<>();
        for (String[] nodeTable : nodesTables) {
            unionPart.add(String.format("n.nodeId NOT IN (SELECT i.%2$s.nodeId FROM %1$s i WHERE i.%2$s IS NOT NULL)", nodeTable[0], nodeTable[1]));
        }
        String changesHql = String.join("\nAND\n", unionPart);
        String hql = String.format("SELECT n.nodeId FROM arr_node n WHERE %1$s", changesHql);
        Query query = entityManager.createQuery(hql);
        return new HashSet<>(query.getResultList());
    }

    private List<String[]> getNodesTables() {
        String[][] configUnionTables = new String[][]{
            {"arr_level", "node"},
            {"arr_level", "nodeParent"},
            {"arr_node_register", "node"},
            {"arr_node_extension", "node"},
            {"arr_node_conformity", "node"},
            {"arr_fund_version", "rootNode"},
            {"ui_visible_policy", "node"},
            {"arr_node_output", "node"},
            {"arr_bulk_action_node", "node"},
            {"arr_desc_item", "node"},
            {"arr_change", "primaryNode"},
            {"arr_dao_link", "node"},
            {"arr_digitization_request_node", "node"},
        };

        return Arrays.asList(configUnionTables);
    }

    /**
     * Delete from ARR_CHANGE unused change_ids
     * 
     * @return
     */
    public Query createDeleteNotUseChangesQuery() {

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("DELETE FROM arr_change c WHERE c.change_id NOT IN (");

        String[][] configUnionTables = new String[][]{
                { ArrLevel.TABLE_NAME, ArrLevel.FIELD_CREATE_CHANGE_ID },
                { ArrLevel.TABLE_NAME, ArrLevel.FIELD_DELETE_CHANGE_ID },
                { ArrItem.TABLE_NAME, ArrItem.FIELD_CREATE_CHANGE_ID },
                { ArrItem.TABLE_NAME, ArrItem.FIELD_DELETE_CHANGE_ID },
                { ArrNodeRegister.TABLE_NAME, ArrNodeRegister.FIELD_CREATE_CHANGE_ID },
                { ArrNodeRegister.TABLE_NAME, ArrNodeRegister.FIELD_DELETE_CHANGE_ID },
                { ArrNodeExtension.TABLE_NAME, ArrNodeExtension.FIELD_CREATE_CHANGE_ID },
                { ArrNodeExtension.TABLE_NAME, ArrNodeExtension.FIELD_DELETE_CHANGE_ID },
                
                { ArrFundVersion.TABLE_NAME, ArrFundVersion.FIELD_CREATE_CHANGE_ID },
                { ArrFundVersion.TABLE_NAME, ArrFundVersion.FIELD_LOCK_CHANGE_ID },
                { ArrBulkActionRun.TABLE_NAME, ArrBulkActionRun.FIELD_CHANGE_ID },
                { ArrOutput.TABLE_NAME, ArrOutput.FIELD_CREATE_CHANGE_ID },
                { ArrOutput.TABLE_NAME, ArrOutput.FIELD_LOCK_CHANGE_ID },
                { ArrNodeOutput.TABLE_NAME, ArrNodeOutput.FIELD_CREATE_CHANGE_ID },
                { ArrNodeOutput.TABLE_NAME, ArrNodeOutput.FIELD_DELETE_CHANGE_ID },
                { ArrOutputResult.TABLE_NAME, ArrOutputResult.FIELD_CHANGE_ID },
                
                { ArrDaoLink.TABLE_NAME, ArrDaoLink.FIELD_CREATE_CHANGE_ID },
                { ArrDaoLink.TABLE_NAME, ArrDaoLink.FIELD_DELETE_CHANGE_ID },
                
                { ArrRequestQueueItem.TABLE_NAME, ArrRequestQueueItem.FIELD_CREATE_CHANGE_ID },
                { ArrRequest.TABLE_NAME, ArrRequest.FIELD_CREATE_CHANGE_ID },

                { ArrStructuredObject.TABLE_NAME, ArrStructuredObject.FIELD_CREATE_CHANGE_ID },
                { ArrStructuredObject.TABLE_NAME, ArrStructuredObject.FIELD_DELETE_CHANGE_ID },

                { ArrFundStructureExtension.TABLE_NAME, ArrFundStructureExtension.CREATE_CHANGE_ID },
                { ArrFundStructureExtension.TABLE_NAME, ArrFundStructureExtension.DELETE_CHANGE_ID }
        };

        NamingStrategy ins = ImprovedNamingStrategy.INSTANCE;

        List<String> unionPart = new ArrayList<>(configUnionTables.length);
        for (int index = 0; index < configUnionTables.length; index++) {
            String[] changeTable = configUnionTables[index];

            String columnName = ins.columnName(changeTable[1]);

            unionPart.add(String.format("SELECT distinct t%1$d.%3$s FROM %2$s t%1$d", index, changeTable[0],
                                        columnName));
        }
        sqlBuilder.append(String.join("\nUNION\n", unionPart));

        sqlBuilder.append(")");
        String sql = sqlBuilder.toString();

        return entityManager.createNativeQuery(sql);
    }

    private Query createExtendUpdateEntityQuery(@NotNull final ArrFund fund,
                                                @Nullable final ArrNode node,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                @NotNull final String subTable,
                                                @NotNull final ArrChange change) {
        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));

        String hqlSubSelect = String.format("SELECT i.%2$s FROM %1$s i WHERE %2$s IN (%3$s)", subTable, changeNameColumn, nodesHql);
        String hql = String.format("UPDATE %1$s SET %2$s = NULL WHERE %2$s IN (%3$s)", table, changeNameColumn, hqlSubSelect);
        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private Query createDeleteForeignEntityQuery(@NotNull final ArrFund fund,
                                                 @Nullable final ArrNode node,
                                                 @NotNull final String changeNameColumn,
                                                 @NotNull final String table,
                                                 @NotNull final String joinNameColumn,
                                                 @NotNull final String subTable,
                                                 @NotNull final ArrChange change) {
        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));

        String hqlSubSelect = String.format("SELECT i FROM %1$s i WHERE %2$s IN (%3$s)", table, changeNameColumn, nodesHql);
        String hql = String.format("DELETE FROM %1$s WHERE %2$s IN (%3$s)", subTable, joinNameColumn, hqlSubSelect);

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private Query findChangeNodeIdsQuery(@NotNull final ArrFund fund,
                                         @Nullable final ArrNode node,
                                         @NotNull final ArrChange change) {
        String[][] tables = new String[][]{
            {"arr_level", "node"},
            {"arr_node_register", "node"},
            {"arr_node_extension", "node"},
            {"arr_dao_link", "node"},
            {"arr_desc_item", "node"},
        };

        List<String> hqls = new ArrayList<>();
        for (String[] table : tables) {
            String nodesHql = createHQLFindChanges("createChange", table[0], createHqlSubNodeQuery(fund, node));
            String hql = String.format("SELECT i.nodeId FROM %1$s i WHERE %2$s IN (%3$s)", table[0], "createChange", nodesHql);
            hqls.add(hql);
        }

        String hql = "SELECT DISTINCT n.nodeId FROM arr_node n WHERE n.nodeId IN (" + String.join(") OR n.nodeId IN (", hqls) + ")";

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private Query createExtendDeleteEntityQuery(@NotNull final ArrFund fund,
                                                @Nullable final ArrNode node,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                /*@NotNull final String joinNameColumn,*/
                                                @NotNull final String subTable,
                                                @NotNull final ArrChange change) {
        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));

        String hqlSubSelect = String.format("SELECT i.%2$s FROM %1$s i WHERE %2$s IN (%3$s)", subTable, changeNameColumn, nodesHql);
        String hql = String.format("DELETE FROM %1$s WHERE %2$s IN (%3$s)", table, changeNameColumn, hqlSubSelect);

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private Query createSimpleUpdateEntityQuery(@NotNull final ArrFund fund,
                                                @Nullable final ArrNode node,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                @NotNull final ArrChange change) {
        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));
        String hql = String.format("UPDATE %1$s SET %2$s = NULL WHERE %2$s IN (%3$s)", table, changeNameColumn, nodesHql);

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private Query createSimpleDeleteEntityQuery(@NotNull final ArrFund fund,
                                                @Nullable final ArrNode node,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                @NotNull final ArrChange change) {
        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));
        String hql = String.format("DELETE FROM %1$s WHERE %2$s IN (%3$s)", table, changeNameColumn, nodesHql);

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }


    private String createHQLFindChanges(@NotNull final String changeNameColumn,
                                        @NotNull final String table,
                                        @NotNull final String hqlNodes) {
        return String.format("SELECT DISTINCT c.%1$s FROM %2$s c WHERE c.node IN (%3$s) AND %1$s >= :change",
                changeNameColumn, table, hqlNodes);
    }

    /**
     * Zjistí, jestli má oprávnění na provedení všech změn.
     *
     * @param fund AS u kterého kontrolujeme oprávnění
     * @return má oprávnění provádět obnovu všech změn
     */
    private boolean hasFullRevertPermission(@NotNull final ArrFund fund) {
        return userService.hasPermission(UsrPermission.Permission.ADMIN)
                || userService.hasPermission(UsrPermission.Permission.FUND_ADMIN)
                || userService.hasPermission(UsrPermission.Permission.FUND_VER_WR, fund.getFundId());
    }

    /**
     * Převedení změn z databázového dotazu na změny pro odpověď.
     *
     * @param sqlResult             změny z dotazu
     * @param fundVersion           verze AS
     * @param fullRevertPermission  má úplné oprávnění?
     * @param isNodeContext         změny jsou v kontextu JP
     * @param canReverBefore        může se revertovat změna? (true - předchozí můžou)
     * @param canRevertByUserBefore může se revertovat změna - uživatel? (true - předchozí provedl stejný uživatel)
     * @return seznam změn pro odpověď
     */
    private List<Change> convertChangeResults(final List<ChangeResult> sqlResult,
                                              final ArrFundVersion fundVersion,
                                              final boolean fullRevertPermission,
                                              final boolean canReverBefore,
                                              final boolean canRevertByUserBefore,
                                              final boolean isNodeContext) {
        UsrUser loggedUser = userService.getLoggedUser();

        boolean canRevert = canReverBefore;
        boolean canRevertByUser = canRevertByUserBefore;

        List<Change> changes = new ArrayList<>(sqlResult.size());

        HashMap<Integer, Integer> changeIdNodeIdMap = new HashMap<>();

        Set<Integer> userIds = new HashSet<>();

        for (ChangeResult changeResult : sqlResult) {
            Integer primaryNodeId = changeResult.getPrimaryNodeId();
            if (primaryNodeId != null) {
                changeIdNodeIdMap.put(changeResult.changeId, changeResult.primaryNodeId);
            }
            Integer userId = changeResult.getUserId();
            if (userId != null) {
                userIds.add(userId);
            }
        }

        ViewTitles viewTitles = configView.getViewTitles(fundVersion.getRuleSetId(),
                                                                    fundVersion.getFundId());
        Set<Integer> descItemTypeCodes = viewTitles.getTreeItemIds() == null ? Collections.emptySet()
                : new LinkedHashSet<>(viewTitles.getTreeItemIds());

        List<RulItemType> descItemTypes = Collections.emptyList();
        if (!descItemTypeCodes.isEmpty()) {
            descItemTypes = itemTypeRepository.findAll(descItemTypeCodes);
            if (descItemTypes.size() != descItemTypeCodes.size()) {
                List<String> foundCodes = descItemTypes.stream().map(RulItemType::getCode).collect(Collectors.toList());
                Collection<Integer> missingCodes = new HashSet<>(descItemTypeCodes);
                missingCodes.removeAll(foundCodes);

                logger.warn("Nepodařilo se nalézt typy atributů s kódy " + org.apache.commons.lang.StringUtils.join(missingCodes, ", ") + ". Změňte kódy v"
                        + " konfiguraci.");
            }

        }

        HashMap<Map.Entry<Integer, Integer>, String> changeNodeMap = createNodeLabels(changeIdNodeIdMap, descItemTypes,
                                                                                      fundVersion.getRuleSetId(),
                                                                                      fundVersion.getFund()
                                                                                              .getFundId());

        Map<Integer, UsrUser> users = userService.findUserMap(userIds);

        for (ChangeResult changeResult : sqlResult) {
            Change change = new Change();
            change.setChangeId(changeResult.changeId);
            change.setNodeChanges(changeResult.nodeChanges == null ? null : changeResult.nodeChanges.intValue());
            change.setChangeDate(Date.from(changeResult.changeDate.atZone(ZoneId.systemDefault()).toInstant()));
            change.setPrimaryNodeId(changeResult.primaryNodeId);
            change.setType(StringUtils.isEmpty(changeResult.type) ? null : ArrChange.Type.valueOf(changeResult.type));
            change.setUserId(changeResult.userId);

            // nemůžu revertovat vytvoření AS
            if (change.getType() == null || change.getType().equals(ArrChange.Type.CREATE_AS)) {
                canRevert = false;
            }

            if (isNodeContext) {
                if ((change.getType() != null &&
                        !Arrays.asList(ArrChange.Type.ADD_RECORD_NODE,
                                ArrChange.Type.DELETE_RECORD_NODE,
                                ArrChange.Type.UPDATE_DESC_ITEM,
                                ArrChange.Type.ADD_DESC_ITEM,
                                ArrChange.Type.DELETE_DESC_ITEM).contains(change.getType())) || change.getNodeChanges() > 1) {
                    canRevert = false;
                }
            }

            UsrUser usrUser = null;
            if (changeResult.userId != null) {
                usrUser = users.get(changeResult.userId);
                change.setUsername(usrUser.getUsername());
            }

            // pokud nemám plné oprávnění vracet změny a pokud nejsem uživatel provedené změny, nemůžu provést změny
            if (!fullRevertPermission && !loggedUser.equals(usrUser)) {
                canRevertByUser = false;
            }

            change.setRevert(canRevert && canRevertByUser);
            change.setLabel(changeNodeMap.get(new AbstractMap.SimpleImmutableEntry<>(changeResult.changeId, changeResult.primaryNodeId)));
            changes.add(change);
        }
        return changes;
    }

    /**
     * Vytvoření mapy popisků JP podle identifikátoru změny/JP.
     *
     * @param changeIdNodeIdMap
     *            mapa změny/JP
     * @param itemTypes
     *            seznam typů atributů
     * @param ruleSetId
     *            identifikátor pravidel
     * @param fundId
     *            @return mapa popisků
     */
    private HashMap<Map.Entry<Integer, Integer>, String> createNodeLabels(final HashMap<Integer, Integer> changeIdNodeIdMap,
                                                                          final List<RulItemType> itemTypes,
                                                                          final Integer ruleSetId,
                                                                          final Integer fundId) {
        HashMap<Map.Entry<Integer, Integer>, String> result = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : changeIdNodeIdMap.entrySet()) {
            Map<Integer, TitleItemsByType> nodeValuesMap = descriptionItemService
                    .createNodeValuesByItemTypeCodeMap(Collections.singleton(entry.getValue()), itemTypes, entry.getKey(), null);
            TitleItemsByType items = nodeValuesMap.get(entry.getValue());
            if (items != null) {
                List<String> titles = new ArrayList<>();
                for (RulItemType itemType : itemTypes) {
                    titles.addAll(items.getValues(itemType.getItemTypeId()));
                }
                result.put(entry, String.join(" ", titles));
            } else {
                ViewTitles viewTitles = configView.getViewTitles(ruleSetId, fundId);
                String defaultTitle = viewTitles.getDefaultTitle();
                defaultTitle = StringUtils.isEmpty(defaultTitle) ? "JP <" + entry.getValue() + ">" : defaultTitle;
                result.put(entry, defaultTitle);
            }
        }

        return result;
    }

    /**
     * Konverze výsledků z databázového dotazu na typovaný seznam změn.
     *
     * @param inputList seznam z databázového dotazu
     * @return typovaný seznam z databázového dotazu
     */
    private List<ChangeResult> convertResults(final List<Object[]> inputList) {
        List<ChangeResult> result = new ArrayList<>(inputList.size());
        for (Object[] o : inputList) {
            result.add(convertResult(o));
        }
        return result;
    }

    /**
     * Konverze výsledku z databázového dotazu na třídu změny.
     *
     * @param o pole parametrů z dotazu
     * @return převedený objekt
     */
    private @NotNull ChangeResult convertResult(@NotNull final Object[] o) {
        ChangeResult change = new ChangeResult();
        change.setChangeId((Integer) o[0]);
        change.setChangeDate(((Timestamp) o[1]).toLocalDateTime());
        change.setUserId((Integer) o[2]);
        change.setType(o[3] == null ? null : ((String) o[3]).trim());
        change.setPrimaryNodeId((Integer) o[4]);
        // pokud je váha (weights) rovna nule, nebyl ovlivněna žádná JP
        change.setNodeChanges(((Number) o[6]).intValue() == 0 ? BigInteger.ZERO : (BigInteger.valueOf(((Number) o[5]).intValue())));
        return change;
    }

    /**
     * Sestavení řetězce pro vnořený dotaz, který vrací seznam JP omezený AS nebo JP.
     *
     * @param fund AS
     * @param node JP
     * @return HQL řetězec
     */
    private String createHqlSubNodeQuery(@NotNull final ArrFund fund,
                                         @Nullable final ArrNode node) {
        Validate.notNull(fund, "AS musí být vyplněn");
        String query = "SELECT n FROM arr_node n WHERE fund = :fund";
        if (node != null) {
            query += " AND n = :node";
        }
        return query;
    }

    /**
     * Sestavení řetězce pro vnořený dotaz, který vrací seznam JP omezený AS nebo JP.
     *
     * @param fundId identifikátor AS
     * @param nodeId identifikátor JP
     * @return SQL řetězec
     */
    private String createSubNodeQuery(@NotNull final Integer fundId,
                                      @Nullable final Integer nodeId) {
        Validate.notNull(fundId, "Identifikátor AS musí být vyplněn");
        String query = "SELECT node_id FROM arr_node WHERE fund_id = :fundId";
        if (nodeId != null) {
            query += " AND node_id = :nodeId";
        }
        return query;
    }

    /**
     * Sestavení řetězce pro základní dotaz vyhledávání změn.
     *
     * @return SQL řetězec
     */
    private String createFindQuerySkeleton() {
        return "SELECT  \n" +
                "%1$s\n" +
                "FROM\n" +
                "  arr_change ch\n" +
                "JOIN \n" +
                "(\n" +
                "  SELECT change_id, COUNT(*) AS node_changes, SUM(weight) AS weights FROM (\n" + // provádním agregaci přes součet vah a počtu unikátnách změn
                "    SELECT DISTINCT change_id, node_id AS nodes, weight FROM (\n" + // provádím union změn z požadovaných tabulek, u hromadných akcí má změna váhu 0, protože upravené změny se počítají přes hodnoty atributů
                "      SELECT create_change_id AS change_id, node_id, 1 AS weight FROM arr_level WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id AS change_id, node_id, 1 AS weight FROM arr_level WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_desc_item di JOIN arr_item i ON i.item_id = di.item_id WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_desc_item di JOIN arr_item i ON i.item_id = di.item_id WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_node_register WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_node_register WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_node_extension WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_node_extension WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_node_output WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_node_output WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_dao_link WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_dao_link WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT change_id, null, 0 AS weight FROM arr_bulk_action_run r JOIN arr_fund_version v ON r.fund_version_id = v.fund_version_id WHERE v.fund_id = :fundId AND r.state = '" + ArrBulkActionRun.State.FINISHED + "'\n" +
                //                "    ) chlx ORDER BY change_id DESC\n" +
                "    ) chlx \n" +
                "  ) chlxx GROUP BY change_id \n" +
                ") chl\n" +
                "ON\n" +
                "  ch.change_id = chl.change_id\n" +
                "%3$s";
    }

    /**
     * Sestavení dotazu na vyhledání změn.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param maxSize      maximální počet záznamů
     * @param offset       počet přeskočených záznamů
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @return query objekt
     */
    private Query createFindQuery(final Integer fundId,
                                  final Integer nodeId,
                                  final int maxSize,
                                  final int offset,
                                  final Integer fromChangeId) {
        String querySkeleton = createFindQuerySkeleton();

        // doplňující parametry dotazu
        String selectParams = "ch.change_id, ch.change_date, ch.user_id, ch.type, ch.primary_node_id, chl.node_changes, chl.weights";
        String querySpecification = "GROUP BY ch.change_id, ch.change_date, ch.user_id, ch.type, ch.primary_node_id, chl.node_changes, chl.weights ORDER BY ch.change_id DESC";
        if (fromChangeId != null) {
            querySpecification = "WHERE ch.change_id <= :fromChangeId " + querySpecification;
        }

        // vnoření parametrů a vytvoření query objektu
        String queryString = String.format(querySkeleton, selectParams, createSubNodeQuery(fundId, nodeId), querySpecification);
        Query query = entityManager.createNativeQuery(queryString);

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        if (fromChangeId != null) {
            query.setParameter("fromChangeId", fromChangeId);
        }
        query.setMaxResults(maxSize);
        query.setFirstResult(offset);

        return query;
    }

    /**
     * Vyhledá počet změn, které nevytvořil přihlášený uživatel.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @return query objekt
     */
    private Query createFindUserChangeQuery(final Integer fundId,
                                            final Integer nodeId,
                                            final int maxSize,
                                            final Integer fromChangeId) {
        String querySkeleton = createFindQuerySkeleton();

        UsrUser loggedUser = userService.getLoggedUser();

        // doplňující parametry dotazu
        String selectParams = "COUNT(ch.change_id)";
        String querySpecification = "GROUP BY ch.change_id";
        List<String> wheres = new ArrayList<>();
        if (fromChangeId != null) {
            wheres.add("ch.change_id <= :fromChangeId");
        }

        if (loggedUser.getUserId() != null) {
            wheres.add("ch.user_id <> :userId");
        }

        if (wheres.size() > 0) {
            querySpecification = "WHERE " + String.join(" AND ", wheres) + " " + querySpecification;
        }

        // vnoření parametrů a vytvoření query objektu
        String queryString = String.format(querySkeleton, selectParams, createSubNodeQuery(fundId, nodeId), querySpecification);
        Query query = entityManager.createNativeQuery(queryString);

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        if (fromChangeId != null) {
            query.setParameter("fromChangeId", fromChangeId);
        }
        if (loggedUser.getUserId() != null) {
            query.setParameter("userId", loggedUser.getUserId());
        }
        query.setMaxResults(maxSize);

        return query;
    }

    /**
     * Sestavení dotazu pro zjištění počtu změn, které se mují přeskočit na základě vyhledání podle datumu.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @param fromDate     datum podle kterého počítám změny k přeskočení
     * @return query objekt
     */
    private Query createQueryIndex(final Integer fundId,
                                   final Integer nodeId,
                                   final Integer fromChangeId,
                                   final LocalDateTime fromDate) {
        String querySkeleton = createFindQuerySkeleton();

        // doplňující parametry dotazu
        String selectParams = "COUNT(*)";
        String querySpecification = "WHERE ch.change_id < :fromChangeId AND ch.change_date >= :changeDate";

        // vnoření parametrů a vytvoření query objektu
        String queryString = String.format(querySkeleton, selectParams, createSubNodeQuery(fundId, nodeId), querySpecification);
        Query query = entityManager.createNativeQuery(queryString);

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        query.setParameter("fromChangeId", fromChangeId);
        query.setParameter("changeDate", Timestamp.valueOf(fromDate), TemporalType.TIMESTAMP);

        return query;
    }

    /**
     * Sestavení dotazu pro zjištění poslední provedené změny.
     *
     * @param fundId identifikátor AS
     * @param nodeId identifikátor JP
     * @return query objekt
     */
    private Query createQueryLastChange(final Integer fundId, final Integer nodeId) {
        return createFindQuery(fundId, nodeId, 1, 0, null);
    }

    /**
     * Sestavení dotazu pro zjištění celkového počtu změn.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @return query objekt
     */
    private Query createFindQueryCount(final Integer fundId, final Integer nodeId, final Integer fromChangeId) {
        String querySkeleton = createFindQuerySkeleton();

        String selectParams = "COUNT(*)";
        String querySpecification = "";

        if (fromChangeId != null) {
            querySpecification = "WHERE ch.change_id <= :fromChangeId " + querySpecification;
        }

        String queryString = String.format(querySkeleton, selectParams, createSubNodeQuery(fundId, nodeId), querySpecification);

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        if (fromChangeId != null) {
            query.setParameter("fromChangeId", fromChangeId);
        }
        return query;
    }

    /**
     * Pomocná struktura změn získaných z DB.
     */
    private class ChangeResult {

        /**
         * Identifikátor změny.
         */
        private Integer changeId;

        /**
         * Datum a čas změny.
         */
        private LocalDateTime changeDate;

        /**
         * Identifikátor uživatele, který změnu provedl.
         */
        private Integer userId;

        /**
         * Textový zápis typu změny.
         */
        private String type;

        /**
         * Identifikátor JP se kterou změna souvisí.
         */
        private Integer primaryNodeId;

        /**
         * Počet změněných JP způsobené změnou.
         */
        private BigInteger nodeChanges;

        public Integer getChangeId() {
            return changeId;
        }

        public void setChangeId(final Integer changeId) {
            this.changeId = changeId;
        }

        public LocalDateTime getChangeDate() {
            return changeDate;
        }

        public void setChangeDate(final LocalDateTime changeDate) {
            this.changeDate = changeDate;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(final Integer userId) {
            this.userId = userId;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public Integer getPrimaryNodeId() {
            return primaryNodeId;
        }

        public void setPrimaryNodeId(final Integer primaryNodeId) {
            this.primaryNodeId = primaryNodeId;
        }

        public BigInteger getNodeChanges() {
            return nodeChanges;
        }

        public void setNodeChanges(final BigInteger nodeChanges) {
            this.nodeChanges = nodeChanges;
        }

    }

}
