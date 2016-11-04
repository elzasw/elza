package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.ChangeRepository;
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
    private ChangeRepository changeRepository;

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS.
     *
     * @param fund       AS nad kterým provádím vyhledávání
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
        Integer fundId = fund.getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();

        Query query = createQuery(fundId, nodeId, maxSize, offset);
        Query queryCount = createQueryCount(fundId, nodeId);
        Query queryLastChange = createQueryLastChange(fundId, nodeId);

        ChangeResult lastChange = convertResult((Object[]) queryLastChange.getSingleResult());

        Integer count = ((BigInteger) queryCount.getSingleResult()).intValue();

        List<ChangeResult> sqlResult = convertResults(query.getResultList());
        List<Change> changes = convertChangeResults(sqlResult);

        ChangesResult changesResult = new ChangesResult();
        changesResult.setMaxSize(maxSize);
        changesResult.setOffset(offset);
        changesResult.setOutdated(fromChange != null && !fromChange.getChangeId().equals(lastChange.getChangeId()));
        changesResult.setTotalCount(count);
        changesResult.setChanges(changes);

        return changesResult;
    }

    private List<Change> convertChangeResults(final List<ChangeResult> sqlResult) {
        List<Change> changes = new ArrayList<>(sqlResult.size());
        for (ChangeResult changeResult : sqlResult) {
            Change change = new Change();
            change.setChangeId(changeResult.changeId);
            change.setNodeChanges(changeResult.nodeChanges == null ? null : changeResult.nodeChanges.intValue());
            change.setChangeDate(changeResult.changeDate);
            change.setPrimaryNodeId(changeResult.primaryNodeId);
            change.setType(StringUtils.isEmpty(changeResult.type) ? null : ArrChange.Type.valueOf(changeResult.type));
            change.setUserId(changeResult.userId);
            changes.add(change);
        }
        return changes;
    }

    private List<ChangeResult> convertResults(final List inputList) {
        List<ChangeResult> result = new ArrayList<>(inputList.size());
        for (Object[] o : (List<Object[]>) inputList) {
            result.add(convertResult(o));
        }
        return result;
    }

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
        change.setNodeChanges((BigInteger) o[5]);
        return change;
    }

    private String createSubNodeQuery(@NotNull final Integer fundId,
                                      @Nullable final Integer nodeId) {
        Assert.notNull(fundId, "Identifikátor AS musí být vyplněn");
        String query = "SELECT node_id FROM arr_node WHERE fund_id = :fundId";
        if (nodeId != null) {
            query += " AND node_id = :nodeId";
        }
        return query;
    }

    private String createQuerySkeleton() {
        return "SELECT\n" +
                "%1$s\n" +
                "FROM\n" +
                "  arr_change ch\n" +
                "JOIN \n" +
                "(\n" +
                "  SELECT change_id, COUNT(*) AS node_changes FROM (\n" +
                "    SELECT DISTINCT change_id, node_id AS nodes FROM (\n" +
                "      SELECT create_change_id AS change_id, node_id FROM arr_level WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id AS change_id, node_id FROM arr_level WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id FROM arr_desc_item di JOIN arr_item i ON i.item_id = di.item_id WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id FROM arr_desc_item di JOIN arr_item i ON i.item_id = di.item_id WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id FROM arr_node_register WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id FROM arr_node_register WHERE node_id IN (%2$s)\n" +
                "    ) chlx ORDER BY change_id DESC\n" +
                "  ) chlxx GROUP BY change_id\n" +
                ") chl\n" +
                "ON\n" +
                "  ch.change_id = chl.change_id\n" +
                "%3$s";
    }

    private Query createQuery(final Integer fundId,
                              final Integer nodeId,
                              final int maxSize,
                              final int offset) {
        String querySkeleton = createQuerySkeleton();

        String selectParams = "ch.change_id, ch.change_date, ch.user_id, ch.type, ch.primary_node_id, chl.node_changes";
        String querySpecification = "GROUP BY ch.change_id, chl.node_changes ORDER BY ch.change_date DESC";
        String queryString = String.format(querySkeleton, selectParams, createSubNodeQuery(fundId, nodeId), querySpecification);

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        query.setMaxResults(maxSize);
        query.setFirstResult(offset);
        return query;
    }

    private Query createQueryLastChange(final Integer fundId, final Integer nodeId) {
        return createQuery(fundId, nodeId, 1, 0);
    }

    private Query createQueryCount(final Integer fundId, final Integer nodeId) {
        String querySkeleton = createQuerySkeleton();

        String selectParams = "COUNT(*)";
        String queryString = String.format(querySkeleton, selectParams, createSubNodeQuery(fundId, nodeId), "");

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        return query;
    }

    public class ChangeResult {

        private Integer changeId;

        private LocalDateTime changeDate;

        private Integer userId;

        private String type;

        private Integer primaryNodeId;

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
