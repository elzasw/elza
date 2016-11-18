package cz.tacr.elza.service;

import cz.tacr.elza.api.ArrBulkActionRun;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.Change;
import cz.tacr.elza.service.vo.ChangesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servisní třída pro práci s obnovou změn v archivní souboru - "UNDO".
 *
 * TODO: invalidace výstupů, funkcí, ... na klientu
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
        Assert.notNull(fundVersion);

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

        ChangeResult lastChange = convertResult((Object[]) queryLastChange.getSingleResult());
        Integer count = ((BigInteger) queryCount.getSingleResult()).intValue();

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
        Assert.notNull(fundVersion);
        Assert.notNull(fromDate);
        Assert.notNull(fromChange);

        Integer fundId = fundVersion.getFund().getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();
        Integer fromChangeId = fromChange.getChangeId();

        // dotaz pro zjištění pozice v seznamu podle datumu
        Query queryIndex = createQueryIndex(fundId, nodeId, fromChangeId, fromDate);

        Integer count = ((BigInteger) queryIndex.getSingleResult()).intValue();

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
        Assert.notNull(fund);
        Assert.notNull(fromChange);
        Assert.notNull(toChange);

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

        Query updateEntityQuery;
        Query deleteEntityQuery;

        deleteEntityQuery = createConformityDeleteForeignEntityQuery(fund, node, /*toChange,*/ "arr_node_conformity_error");
        deleteEntityQuery.executeUpdate();

        deleteEntityQuery = createConformityDeleteForeignEntityQuery(fund, node, /*toChange,*/ "arr_node_conformity_missing");
        deleteEntityQuery.executeUpdate();

        deleteEntityQuery = createConformityDeleteEntityQuery(fund, node/*, toChange*/);
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_level", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_level", toChange);
        updateEntityQuery.executeUpdate();
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_node_register", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_node_register", toChange);
        updateEntityQuery.executeUpdate();
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createExtendUpdateEntityQuery(fund, node, "deleteChange", "arr_desc_item", "arr_item", toChange);
        updateEntityQuery.executeUpdate();

        deleteEntityQuery = createDeleteForeignEntityQuery(fund, node, "createChange", "arr_desc_item", "item", "arr_data", toChange);
        deleteEntityQuery.executeUpdate();

        deleteEntityQuery = createExtendDeleteEntityQuery(fund, node, "createChange", "arr_desc_item", /*"item",*/ "arr_item", toChange);
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createUpdateOutputQuery(fund, node, toChange);
        updateEntityQuery.executeUpdate();

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_node_output", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_node_output", toChange);
        updateEntityQuery.executeUpdate();
        deleteEntityQuery.executeUpdate();

        updateEntityQuery = createUpdateActionQuery(fund, node, toChange);
        updateEntityQuery.executeUpdate();

        deleteEntityQuery = createDeleteActionQuery(fund, node, toChange);
        deleteEntityQuery.executeUpdate();

        Query deleteNotUseChangesQuery = createDeleteNotUseChangesQuery();
        deleteNotUseChangesQuery.executeUpdate();

        Query deleteNotUseNodesQuery = createDeleteNotUseNodesQuery();
        deleteNotUseNodesQuery.executeUpdate();

        if (node == null) {
            // TODO: dopsat aktualizaci celého stromu AS
        } else {
            if (openFundVersion != null) {
                eventNotificationService.publishEvent(new EventIdsInVersion(EventType.NODES_CHANGE, openFundVersion.getFundVersionId(), node.getNodeId()));
            }
        }

        levelTreeCacheService.invalidateFundVersion(fund);
        startupService.revalidateNodes();
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

        ChangeResult lastChange = convertResult((Object[]) queryLastChange.getSingleResult());

        if (!fromChange.getChangeId().equals(lastChange.getChangeId())) {
            throw new BusinessException(ArrangementCode.EXISTS_NEWER_CHANGE);
        }

        if (toChange.getType() != null && toChange.getType().equals(ArrChange.Type.CREATE_AS)) {
            throw new BusinessException(ArrangementCode.EXISTS_BLOCKING_CHANGE);
        }

        if (nodeId != null) {
            Query findQueryCountBefore = createFindQueryCountBefore(fundId, nodeId, fromChange.getChangeId(), toChange.getChangeId());
            Integer countBefore = ((BigInteger) findQueryCountBefore.getSingleResult()).intValue();
            if (countBefore > 0) {
                throw new BusinessException(ArrangementCode.EXISTS_BLOCKING_CHANGE);
            }
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

    private Query createDeleteActionQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("DELETE FROM arr_bulk_action_node n WHERE n IN (" +
                "SELECT rn FROM arr_bulk_action_node rn JOIN rn.bulkActionRun rs WHERE rn.node IN (" + createHqlSubNodeQuery(fund, node) + ") AND rs.change >= :change" +
                ")");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        if (node != null) {
            query.setParameter("node", node);
        }
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
            updateConformityInfoService.terminateWorkerInVersionAndWait(fundVersion);
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
        String[][] configUnionTables = new String[][]{
                {"arr_level", "node"},
                {"arr_level", "nodeParent"},
                {"arr_node_register", "node"},
                {"arr_node_conformity", "node"},
                {"arr_fund_version", "rootNode"},
                {"ui_visible_policy", "node"},
                {"arr_node_output", "node"},
                {"arr_bulk_action_node", "node"},
                {"arr_desc_item", "node"},
                {"arr_change", "primaryNode"},
        };

        List<String[]> nodesTables = Arrays.asList(configUnionTables);
        List<String> unionPart = new ArrayList<>();
        for (String[] nodeTable : nodesTables) {
            unionPart.add(String.format("n.nodeId NOT IN (SELECT i.%2$s.nodeId FROM %1$s i WHERE i.%2$s IS NOT NULL)", nodeTable[0], nodeTable[1]));
        }
        String changesHql = String.join("\nAND\n", unionPart);

        String hql = String.format("DELETE FROM arr_node n WHERE %1$s", changesHql);

        return entityManager.createQuery(hql);
    }

    private Query createDeleteNotUseChangesQuery() {

        String[][] configUnionTables = new String[][]{
                {"arr_level", "createChange"},
                {"arr_level", "deleteChange"},
                {"arr_item", "createChange"},
                {"arr_item", "deleteChange"},
                {"arr_node_register", "createChange"},
                {"arr_node_register", "deleteChange"},

                {"arr_fund_version", "createChange"},
                {"arr_fund_version", "lockChange"},
                {"arr_bulk_action_run", "change"},
                {"arr_output", "createChange"},
                {"arr_output", "lockChange"},
                {"arr_node_output", "createChange"},
                {"arr_node_output", "deleteChange"},
                {"arr_output_result", "change"}
        };

        List<String[]> changeTables = Arrays.asList(configUnionTables);
        List<String> unionPart = new ArrayList<>();
        for (String[] changeTable : changeTables) {
            unionPart.add(String.format("c.changeId NOT IN (SELECT i.%2$s.changeId FROM %1$s i WHERE i.%2$s IS NOT NULL)", changeTable[0], changeTable[1]));
        }
        String changesHql = String.join("\nAND\n", unionPart);

        String hql = String.format("DELETE FROM arr_change c WHERE %1$s", changesHql);

        return entityManager.createQuery(hql);
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

        Set<Integer> nodeIds = new HashSet<>();
        Set<Integer> userIds = new HashSet<>();

        for (ChangeResult changeResult : sqlResult) {
            Integer primaryNodeId = changeResult.getPrimaryNodeId();
            if (primaryNodeId != null) {
                nodeIds.add(primaryNodeId);
            }
            Integer userId = changeResult.getUserId();
            if (userId != null) {
                userIds.add(userId);
            }
        }

        Map<Integer, UsrUser> users = userService.findUserMap(userIds);
        List<TreeNodeClient> nodesByIds = levelTreeCacheService.getNodesByIds(nodeIds, fundVersion.getFundVersionId());

        Map<Integer, TreeNodeClient> nodeMap = new HashMap<>();
        for (TreeNodeClient nodesById : nodesByIds) {
            nodeMap.put(nodesById.getId(), nodesById);
        }

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

            String description;
            if (changeResult.primaryNodeId != null && nodeMap.get(changeResult.primaryNodeId) != null) {
                TreeNodeClient treeNodeClient = nodeMap.get(changeResult.primaryNodeId);
                description = treeNodeClient.getName();
            } else {

                if (change.getType() == null) {
                    // TODO: dopsat popis
                    description = StringUtils.isEmpty(changeResult.type) ? "neznámý typ" : ArrChange.Type.valueOf(changeResult.type).getDescription();
                    description += ", primaryNodeId: " + (changeResult.primaryNodeId == null ? "?" : changeResult.primaryNodeId);
                    description += ", changeId: " + changeResult.changeId;
                    description += ", changeDate: " + changeResult.changeDate;

                } else {
                    description = createDescriptionNode(changeResult, change);
                }
            }

            change.setDescription(description);
            changes.add(change);
        }
        return changes;
    }

    /**
     * Sestaví popis změny/JP.
     *
     * @param changeResult změna z DB
     * @param change       změna pro klienta
     * @return výsledný popis změny
     */
    private String createDescriptionNode(final ChangeResult changeResult, final Change change) {
        String description;
        switch (change.getType()) {

            case BULK_ACTION: {
                description = "Funkce (Ovlivněno JP: " + change.getNodeChanges() + ")";
                break;
            }

            case ADD_NODES_OUTPUT: {
                description = "Připojení JP (" + change.getNodeChanges() + ") k výstupu";
                break;
            }

            case REMOVE_NODES_OUTPUT: {
                description = "Odpojení JP (" + change.getNodeChanges() + ") od výstupu";
                break;
            }

            case CREATE_AS: {
                description = "Vytvoření archivního souboru";
                break;
            }

            case BATCH_CHANGE_DESC_ITEM: {
                description = "Hromadná úprava hodnot atributů";
                break;
            }

            case BATCH_DELETE_DESC_ITEM: {
                description = "Hromadný výmaz hodnot atributů";
                break;
            }

            case IMPORT: {
                description = "Import do AS";
                break;
            }

            default: {
                description = StringUtils.isEmpty(changeResult.type) ? "neznámý typ" : ArrChange.Type.valueOf(changeResult.type).getDescription();
                description += ", primaryNodeId: " + (changeResult.primaryNodeId == null ? "?" : changeResult.primaryNodeId);
                description += ", changeId: " + changeResult.changeId;
                description += ", changeDate: " + changeResult.changeDate;
            }

        }
        return description;
    }

    /**
     * Konverze výsledků z databázového dotazu na typovaný seznam změn.
     *
     * @param inputList seznam z databázového dotazu
     * @return typovaný seznam z databázového dotazu
     */
    private List<ChangeResult> convertResults(final List inputList) {
        List<ChangeResult> result = new ArrayList<>(inputList.size());
        for (Object[] o : (List<Object[]>) inputList) {
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
    private ChangeResult convertResult(final Object[] o) {
        if (o == null) {
            return null;
        }
        ChangeResult change = new ChangeResult();
        change.setChangeId((Integer) o[0]);
        change.setChangeDate(((Timestamp) o[1]).toLocalDateTime());
        change.setUserId((Integer) o[2]);
        change.setType(o[3] == null ? null : ((String) o[3]).trim());
        change.setPrimaryNodeId((Integer) o[4]);
        // pokud je váha (weights) rovna nule, nebyl ovlivněna žádná JP
        change.setNodeChanges(((BigInteger) o[6]).intValue() == 0 ? BigInteger.ZERO : ((BigInteger) o[5]));
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
        Assert.notNull(fund, "AS musí být vyplněn");
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
        Assert.notNull(fundId, "Identifikátor AS musí být vyplněn");
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
        return "SELECT\n" +
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
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_node_output WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_node_output WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT change_id, null, 0 AS weight FROM arr_bulk_action_run r JOIN arr_fund_version v ON r.fund_version_id = v.fund_version_id WHERE v.fund_id = :fundId AND r.state = '" + ArrBulkActionRun.State.FINISHED + "'\n" +
                "    ) chlx ORDER BY change_id DESC\n" +
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
        String querySpecification = "GROUP BY ch.change_id, chl.node_changes, chl.weights ORDER BY ch.change_id DESC";
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
        String querySpecification = "WHERE ch.change_id <= :fromChangeId AND ch.change_date > :changeDate";

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
