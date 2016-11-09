package cz.tacr.elza.service.vo;

import cz.tacr.elza.domain.ArrChange;
import org.exolab.castor.types.DateTime;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Value objekt konkrétní změny.
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
    private Date changeDate;

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

    /**
     * Textový popis změny.
     */
    private String description;

    /**
     * Může se vrátit?
     */
    private Boolean revert;

    public Integer getChangeId() {
        return changeId;
    }

    public void setChangeId(final Integer changeId) {
        this.changeId = changeId;
    }

    public Date getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(final Date changeDate) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Boolean getRevert() {
        return revert;
    }

    public void setRevert(final Boolean revert) {
        this.revert = revert;
    }
}
