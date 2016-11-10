package cz.tacr.elza.service;

import cz.tacr.elza.api.ArrBulkActionRun;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.vo.Change;
import cz.tacr.elza.service.vo.ChangesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import java.util.Date;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS.
     *
     * @param fund       AS nad kterým provádím vyhledávání
     * @param node       JP omezující vyhledávání
     * @param maxSize    maximální počet záznamů
     * @param offset     počet přeskočených záznamů
     * @param fromChange změna, vůči které chceme počítat offset (pokud není vyplněn, bere se vždy poslední)
     * @return výsledek hledání
     */
    public ChangesResult findChanges(@NotNull final ArrFund fund,
                                     @Nullable final ArrNode node,
                                     final int maxSize,
                                     final int offset,
                                     @Nullable final ArrChange fromChange) {
        Assert.notNull(fund);

        Integer fundId = fund.getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();
        Integer fromChangeId = fromChange == null ? null : fromChange.getChangeId();

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
        boolean fullRevertPermission = hasFullRevertPermission(fund);

        List<Change> changes = convertChangeResults(sqlResult, fullRevertPermission);

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
     * @param fund       AS nad kterým provádím vyhledávání
     * @param node       JP omezující vyhledávání
     * @param maxSize    maximální počet záznamů
     * @param fromDate   datum od kterého vyhledávám v historii
     * @param fromChange změna, vůči které chceme počítat offset - v tomto případě musí být vyplněná
     * @return výsledek hledání
     */
    public ChangesResult findChangesByDate(@NotNull final ArrFund fund,
                                           @Nullable final ArrNode node,
                                           final int maxSize,
                                           @NotNull final LocalDateTime fromDate,
                                           @NotNull final ArrChange fromChange) {
        Assert.notNull(fund);
        Assert.notNull(fromDate);
        Assert.notNull(fromChange);

        Integer fundId = fund.getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();
        Integer fromChangeId = fromChange.getChangeId();

        // dotaz pro zjištění pozice v seznamu podle datumu
        Query queryIndex = createQueryIndex(fundId, nodeId, fromChangeId, fromDate);

        Integer count = ((BigInteger) queryIndex.getSingleResult()).intValue();

        return findChanges(fund, node, maxSize, count, fromChange);
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

        // TODO: oprávnění, kontrola změny, ...

        // zastavení probíhajících výpočtů pro validaci uzlů u verzí
        stopConformityInfFundVersions(fund);

        Query updateEntityQuery;
        Query deleteEntityQuery;

        int changes = 0;
        int partChange;

        deleteEntityQuery = createConformityDeleteForeignEntityQuery(fund, node, toChange, "arr_node_conformity_error");
        partChange = deleteEntityQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_node_conformity_error");
        changes += partChange;

        deleteEntityQuery = createConformityDeleteForeignEntityQuery(fund, node, toChange, "arr_node_conformity_missing");
        partChange += deleteEntityQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_node_conformity_missing");
        changes += partChange;

        deleteEntityQuery = createConformityDeleteEntityQuery(fund, node, toChange);
        partChange += deleteEntityQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_node_conformity");
        changes += partChange;

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_level", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_level", toChange);
        partChange += updateEntityQuery.executeUpdate();
        logger.info("Upraveno " + partChange + " záznamů z arr_level");
        changes += partChange;
        partChange += deleteEntityQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_level");
        changes += partChange;

        updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_node_register", toChange);
        deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_node_register", toChange);
        partChange += updateEntityQuery.executeUpdate();
        logger.info("Upraveno " + partChange + " záznamů z arr_node_register");
        changes += partChange;
        partChange += deleteEntityQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_node_register");
        changes += partChange;

        updateEntityQuery = createExtendUpdateEntityQuery(fund, node, "deleteChange", "arr_desc_item", "arr_item", toChange);
        partChange += updateEntityQuery.executeUpdate();
        logger.info("Upraveno " + partChange + " záznamů z arr_desc_item");
        changes += partChange;

        deleteEntityQuery = createDeleteForeignEntityQuery(fund, node, "createChange", "arr_desc_item", "item", "arr_data", toChange);
        partChange += deleteEntityQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_data");
        changes += partChange;

        // TODO: ověřit, proč se tady musím fakticky mazat znovu arr_node_conformity_error
        // - musí se vyřešit předchozí mazání conformity info, pak nebude třeba
        deleteEntityQuery = createDeleteForeignEntityQuery(fund, node, "createChange", "arr_desc_item", "descItem", "arr_node_conformity_error", toChange);
        partChange += deleteEntityQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_node_conformity_error");
        changes += partChange;

        deleteEntityQuery = createExtendDeleteEntityQuery(fund, node, "createChange", "arr_desc_item", "item", "arr_item", toChange);
        partChange += deleteEntityQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_desc_item");
        changes += partChange;

        Query deleteNotUseChangesQuery = createDeleteNotUseChangesQuery();
        partChange += deleteNotUseChangesQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_change");
        changes += partChange;

        Query deleteNotUseNodesQuery = createDeleteNotUseNodesQuery();
        partChange += deleteNotUseNodesQuery.executeUpdate();
        logger.info("Odstraněno " + partChange + " záznamů z arr_node");
        changes += partChange;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override
            public void suspend() {
            }

            @Override
            public void resume() {
            }

            @Override
            public void flush() {
            }

            @Override
            public void beforeCommit(final boolean b) {
            }

            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCommit() {

                if (node == null) {
                    // invalidace
                    levelTreeCacheService.invalidateFundVersion(fund);
                }

                // přidá do fronty JP, které je potřeba přepočítat
                startupService.revalidateNodes();
            }

            @Override
            public void afterCompletion(final int i) {
            }
        });

        logger.info("changes: " + changes);

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

    // TODO: vyřešit správně
    private Query createConformityDeleteEntityQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, final @NotNull ArrChange toChange) {
        String nodesHql = createHQLFindChanges("createChange", "arr_level", createHqlSubNodeQuery(fund, node));
        String hqlSubSelect = String.format("SELECT i.node FROM %1$s i WHERE %2$s IN (%3$s)", "arr_level", "createChange", nodesHql);
        String hql = String.format("DELETE FROM arr_node_conformity WHERE node IN (%1$s)", hqlSubSelect);
        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    // TODO: vyřešit správně
    private Query createConformityDeleteForeignEntityQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, final @NotNull ArrChange toChange, final @NotNull String table) {
        String nodesHql = createHQLFindChanges("createChange", "arr_level", createHqlSubNodeQuery(fund, node));
        String hqlSubSelect = String.format("SELECT i.node FROM %1$s i WHERE %2$s IN (%3$s)", "arr_level", "createChange", nodesHql);
        String hql = String.format("DELETE FROM %1$s ncx WHERE ncx.nodeConformity IN (SELECT nc FROM arr_node_conformity nc WHERE node IN (%2$s))", table, hqlSubSelect);
        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    private Query createDeleteNotUseNodesQuery() {
        String[][] configUnionTables = new String[][] {
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

        String[][] configUnionTables = new String[][] {
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
                                                @NotNull final String joinNameColumn,
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
     * @param sqlResult            změny z dotazu
     * @param fullRevertPermission má úplné oprávnění?
     * @return seznam změn pro odpověď
     */
    private List<Change> convertChangeResults(final List<ChangeResult> sqlResult,
                                              final boolean fullRevertPermission) {
        List<Change> changes = new ArrayList<>(sqlResult.size());
        for (ChangeResult changeResult : sqlResult) {
            Change change = new Change();
            change.setChangeId(changeResult.changeId);
            change.setNodeChanges(changeResult.nodeChanges == null ? null : changeResult.nodeChanges.intValue());
            change.setChangeDate(Date.from(changeResult.changeDate.atZone(ZoneId.systemDefault()).toInstant()));
            change.setPrimaryNodeId(changeResult.primaryNodeId);
            change.setType(StringUtils.isEmpty(changeResult.type) ? null : ArrChange.Type.valueOf(changeResult.type));
            change.setUserId(changeResult.userId);

            // TODO: dopsat popis
            String description = StringUtils.isEmpty(changeResult.type) ? "neznámý typ" : ArrChange.Type.valueOf(changeResult.type).getDescription();
            description += ", primaryNodeId: " + (changeResult.primaryNodeId == null ? "?" : changeResult.primaryNodeId);
            description += ", changeId: " + changeResult.changeId;
            description += ", changeDate: " + changeResult.changeDate;

            change.setDescription(description);

            // TODO: dopsat úplnou logiku vyhodnocení
            change.setRevert(fullRevertPermission);

            changes.add(change);
        }
        return changes;
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
            query += " AND node = :node";
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
