package cz.tacr.elza.service.vo;

import java.util.Date;

import cz.tacr.elza.domain.ArrChange;

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
     * Uživatelské jméno osoby, která změnu proveda.
     */
    private String username;

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
     * Popisek změny.
     */
    private String label;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public Boolean getRevert() {
        return revert;
    }

    public void setRevert(final Boolean revert) {
        this.revert = revert;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Change{" +
                "changeId=" + changeId +
                ", changeDate=" + changeDate +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", type=" + type +
                ", primaryNodeId=" + primaryNodeId +
                ", nodeChanges=" + nodeChanges +
                ", label='" + label + '\'' +
                ", revert=" + revert +
                '}';
    }
}
