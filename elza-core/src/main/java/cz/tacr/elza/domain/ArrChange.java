package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Seznam provedených změn v archivních pomůckách.
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_change")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrChange implements cz.tacr.elza.api.ArrChange<UsrUser, ArrLevel> {

    @Id
    @GeneratedValue
    private Integer changeId;

    @Column(nullable = false)
    private LocalDateTime changeDate;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrLevel.class)
    @JoinColumn(name = "primaryLevelId", nullable = true)
    private ArrLevel primaryLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 25, nullable = true)
    private Type type;

    @Override
    public Integer getChangeId() {
        return changeId;
    }

    @Override
    public void setChangeId(Integer changeId) {
        this.changeId = changeId;
    }

    @Override
    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    @Override
    public void setChangeDate(LocalDateTime changeDate) {
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

    @Override
    public ArrLevel getPrimaryLevel() {
        return primaryLevel;
    }

    @Override
    public void setPrimaryLevel(final ArrLevel primaryLevel) {
        this.primaryLevel = primaryLevel;
    }

    @Override
    public String toString() {
        return "ArrChange pk=" + changeId;
    }
}
