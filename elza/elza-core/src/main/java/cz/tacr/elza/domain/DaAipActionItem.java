package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * What an action did to one AIP.
 *
 * The action is recorded per AIP because that is the granularity at which it succeeds or fails:
 * one AIP of a selection can be rebuilt while the next one has nothing to rebuild from. A single
 * outcome for the whole action would have to choose which of them to report.
 */
@Entity(name = "da_aip_action_item")
public class DaAipActionItem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer aipActionItemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaAipAction.class)
    @JoinColumn(name = "aip_action_id", nullable = false)
    private DaAipAction aipAction;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaAip.class)
    @JoinColumn(name = "aip_id", nullable = false)
    private DaAip aip;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private DaAipActionItemState state;

    /**
     * What happened to this AIP, in the words the user reads. Expected for the outcomes that
     * changed nothing - a failure and a skip are only useful when they say why.
     */
    @Column
    private String message;

    @Column
    private OffsetDateTime finishDate;

    public Integer getAipActionItemId() {
        return aipActionItemId;
    }

    public void setAipActionItemId(Integer aipActionItemId) {
        this.aipActionItemId = aipActionItemId;
    }

    public DaAipAction getAipAction() {
        return aipAction;
    }

    public void setAipAction(DaAipAction aipAction) {
        this.aipAction = aipAction;
    }

    public DaAip getAip() {
        return aip;
    }

    public void setAip(DaAip aip) {
        this.aip = aip;
    }

    public DaAipActionItemState getState() {
        return state;
    }

    public void setState(DaAipActionItemState state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(OffsetDateTime finishDate) {
        this.finishDate = finishDate;
    }
}
