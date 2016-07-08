package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Implementace {@link cz.tacr.elza.api.UsrGroupUser}.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Entity(name = "usr_group_user")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrGroupUser implements cz.tacr.elza.api.UsrGroupUser<UsrUser, UsrGroup>, Serializable {

    @Id
    @GeneratedValue
    private Integer groupUserId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrGroup.class)
    @JoinColumn(name = "groupId", nullable = false)
    private UsrGroup group;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = false)
    private UsrUser user;

    @Override
    public Integer getGroupUserId() {
        return groupUserId;
    }

    @Override
    public void setGroupUserId(final Integer groupUserId) {
        this.groupUserId = groupUserId;
    }

    @Override
    public UsrGroup getGroup() {
        return group;
    }

    @Override
    public void setGroup(final UsrGroup group) {
        this.group = group;
    }

    @Override
    public UsrUser getUser() {
        return user;
    }

    @Override
    public void setUser(final UsrUser user) {
        this.user = user;
    }
}
