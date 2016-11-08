package cz.tacr.elza.service;

import cz.tacr.elza.api.ArrBulkActionRun;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.vo.Change;
import cz.tacr.elza.service.vo.ChangesResult;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Servisní třída pro práci s obnovou změn v archivní souboru - "UNDO".
 *
 * @author Martin Šlapa
 * @since 03.11.2016
 */
@Service
public class RevertingChangesService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserService userService;

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

        Integer fundId = fund.getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();
        Integer fromChangeId = fromChange.getChangeId();
        Integer toChangeId = toChange.getChangeId();

        Query updateEntityQuery;
        Query deleteEntityQuery;

        int changes = 0;

        updateEntityQuery = createUpdateEntityQuery(fundId, nodeId, "delete_change_id", "arr_level", toChangeId);
        deleteEntityQuery = createDeleteEntityQuery(fundId, nodeId, "create_change_id", "arr_level", toChangeId);
        changes += updateEntityQuery.executeUpdate();
        changes += deleteEntityQuery.executeUpdate();

        updateEntityQuery = createUpdateEntityQuery(fundId, nodeId, "delete_change_id", "arr_node_register", toChangeId);
        deleteEntityQuery = createDeleteEntityQuery(fundId, nodeId, "create_change_id", "arr_node_register", toChangeId);
        changes += updateEntityQuery.executeUpdate();
        changes += deleteEntityQuery.executeUpdate();

        updateEntityQuery = createExtendUpdateEntityQuery(fundId, nodeId, "item_id", "delete_change_id", "arr_desc_item", "arr_item", toChangeId);
        deleteEntityQuery = createExtendDeleteEntityQuery(fundId, nodeId, "item_id", "create_change_id", "arr_desc_item", "arr_item", toChangeId);
        changes += updateEntityQuery.executeUpdate();
        changes += deleteEntityQuery.executeUpdate();

        System.out.println("changes: " + changes);

    }

    private Query createUpdateEntityQuery(@NotNull final Integer fundId,
                                           @Nullable final Integer nodeId,
                                           @NotNull final String changeNameColumn,
                                           @NotNull final String table,
                                           @NotNull final Integer changeId) {
        String nodeIdsQuery = createFindChangesQuery(changeNameColumn, table, createSubNodeQuery(fundId, nodeId), changeId);
        String queryString = String.format("UPDATE %1$s SET %2$s = NULL WHERE %2$s IN (%3$s)", table, changeNameColumn, nodeIdsQuery);

        Query query = entityManager.createNativeQuery(queryString);

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }

        return query;
    }

    private Query createExtendUpdateEntityQuery(@NotNull final Integer fundId,
                                                @Nullable final Integer nodeId,
                                                @NotNull final String entityNameColumn,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                @NotNull final String tableJoin,
                                                @NotNull final Integer changeId) {
        String nodeIdsQuery = createFindChangesQuery(changeNameColumn, table, createSubNodeQuery(fundId, nodeId), changeId);
        String entityQuery = String.format("SELECT t.%4$s FROM %1$s t JOIN %2$s tj ON t.%4$s = tj.%4$s WHERE tj.%3$s IN (%5$s)",
                table, tableJoin, changeNameColumn, entityNameColumn, nodeIdsQuery);
        String queryString = String.format("UPDATE %1$s SET %2$s = NULL WHERE %3$s IN (%4$s)",
                tableJoin, changeNameColumn, entityNameColumn, entityQuery);

        Query query = entityManager.createNativeQuery(queryString);

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }

        return query;
    }

    private Query createDeleteEntityQuery(@NotNull final Integer fundId,
                                           @Nullable final Integer nodeId,
                                           @NotNull final String changeNameColumn,
                                           @NotNull final String table,
                                           @NotNull final Integer changeId) {
        String nodeIdsQuery = createFindChangesQuery(changeNameColumn, table, createSubNodeQuery(fundId, nodeId), changeId);
        String queryString = String.format("DELETE FROM %1$s WHERE %2$s IN (%3$s)", table, changeNameColumn, nodeIdsQuery);

        Query query = entityManager.createQuery(queryString);

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }

        return query;
    }

    private Query createExtendDeleteEntityQuery(@NotNull final Integer fundId,
                                                @Nullable final Integer nodeId,
                                                @NotNull final String entityNameColumn,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                @NotNull final String tableJoin,
                                                @NotNull final Integer changeId) {
        String nodeIdsQuery = createFindChangesQuery(changeNameColumn, table, createSubNodeQuery(fundId, nodeId), changeId);
        String entityQuery = String.format("SELECT t.%4$s FROM %1$s t JOIN %2$s tj ON t.%4$s = tj.%4$s WHERE tj.%3$s IN (%5$s)",
                table, tableJoin, changeNameColumn, entityNameColumn, nodeIdsQuery);
        String queryString = String.format("DELETE FROM %1$s WHERE %2$s IN (%3$s)",
                table, entityNameColumn, entityQuery);

        Query query = entityManager.createNativeQuery(queryString);

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }

        return query;
    }

    private String createFindChangesQuery(@NotNull final String changeNameColumn,
                                          @NotNull final String table,
                                          @NotNull final String inNodeId,
                                          @NotNull final Integer changeId) {
        return String.format("SELECT DISTINCT %1$s AS change_id FROM %2$s WHERE node_id IN (%3$s) AND %1$s >= %4$d",
                changeNameColumn, table, inNodeId, changeId);
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
            change.setChangeDate(changeResult.changeDate);
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
