package cz.tacr.elza.service.vo;

import cz.tacr.elza.domain.ArrChange;

import java.time.LocalDateTime;

/**
 * TODO: vyplnit popis třídy
 *
 * @author Martin Šlapa
 * @since 03.11.2016
 */
public class Change {

    /**
     * Identifikátor změny.
     */
    private Integer changeId;

    /**
     * Datum změny.
     */
    private LocalDateTime changeDate;

    /**
     * Identifikátor uživatele, který změnu provedl.
     */
    private Integer userId;

    /**
     * Typ změny.
     */
    private ArrChange.Type type;

    /**
     * Identifikátor JP nad kterým se změna provedla.
     */
    private Integer primaryNodeId;

    /**
     * Počet JP, které změna ovlivnila.
     */
    private Integer nodeChanges;

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

    public ArrChange.Type getType() {
        return type;
    }

    public void setType(final ArrChange.Type type) {
        this.type = type;
    }

    public Integer getPrimaryNodeId() {
        return primaryNodeId;
    }

    public void setPrimaryNodeId(final Integer primaryNodeId) {
        this.primaryNodeId = primaryNodeId;
    }

    public Integer getNodeChanges() {
        return nodeChanges;
    }

    public void setNodeChanges(final Integer nodeChanges) {
        this.nodeChanges = nodeChanges;
    }
}
