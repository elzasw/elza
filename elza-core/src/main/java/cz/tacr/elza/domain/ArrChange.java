package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Seznam provedených změn v archivních pomůckách.
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_change")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrChange implements cz.tacr.elza.api.ArrChange<UsrUser> {

    @Id
    @GeneratedValue
    private Integer changeId;

    @Column(nullable = false)
    private LocalDateTime changeDate;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "primaryNodeId", nullable = true)
    private ArrNode primaryNode;

    @Enumerated(EnumType.STRING)
    @Column(length = 25, nullable = true)
    private Type type;

    @Override
    public Integer getChangeId() {
        return changeId;
    }

    @Override
    public void setChangeId(final Integer changeId) {
        this.changeId = changeId;
    }

    @Override
    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    @Override
    public void setChangeDate(final LocalDateTime changeDate) {
        this.changeDate = changeDate;
    }

    @Override
    public UsrUser getUser() {
        return user;
    }

    @Override
    public void setUser(final UsrUser user) {
        this.user = user;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setType(final Type type) {
        this.type = type;
    }

    public ArrNode getPrimaryNode() {
        return primaryNode;
    }

    public void setPrimaryNode(final ArrNode primaryNode) {
        this.primaryNode = primaryNode;
    }

    @Override
    public String toString() {
        return "ArrChange pk=" + changeId;
    }
}
