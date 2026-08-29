package cz.tacr.elza.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.api.DaAipActionType;
import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * One action a user asked to be carried out over a set of AIPs.
 *
 * It exists so that an action has an outcome the user can be shown. Both ways an action is
 * carried out - by ELZA alone, or through the synchronization queue of the digital archive -
 * report into this record, so the user is told about them the same way and does not have to
 * know which of the two happened.
 *
 * The progress of the action is not stored on it: it is derived from the items, which are
 * finished independently of each other and, once the actions run in parallel, on different
 * threads. Counters kept here would have to be updated concurrently and could drift.
 */
@Entity(name = "da_aip_action")
public class DaAipAction {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer aipActionId;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private DaAipActionType actionType;

    /**
     * Who asked for the action, so it can be reported back to them. Null for an action nobody
     * asked for - one started by the synchronization of a repository.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "user_id")
    private UsrUser user;

    @Column(nullable = false)
    private OffsetDateTime createDate;

    /**
     * When the last item of the action was finished, or null while any of them is outstanding.
     */
    @Column
    private OffsetDateTime finishDate;

    // No orphanRemoval: items are never taken off an action, and it would make replacing the
    // collection on a managed action a flush-time failure.
    @OneToMany(mappedBy = "aipAction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<DaAipActionItem> items = new ArrayList<>();

    public Integer getAipActionId() {
        return aipActionId;
    }

    public void setAipActionId(Integer aipActionId) {
        this.aipActionId = aipActionId;
    }

    public DaAipActionType getActionType() {
        return actionType;
    }

    public void setActionType(DaAipActionType actionType) {
        this.actionType = actionType;
    }

    public UsrUser getUser() {
        return user;
    }

    public void setUser(UsrUser user) {
        this.user = user;
    }

    public OffsetDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(OffsetDateTime createDate) {
        this.createDate = createDate;
    }

    public OffsetDateTime getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(OffsetDateTime finishDate) {
        this.finishDate = finishDate;
    }

    public List<DaAipActionItem> getItems() {
        return items;
    }

    public void setItems(List<DaAipActionItem> items) {
        this.items = items;
    }
}
