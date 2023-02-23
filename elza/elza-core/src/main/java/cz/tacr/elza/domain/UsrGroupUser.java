package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Seznam uživatelů ve skupině.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Entity(name = "usr_group_user")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrGroupUser {

    public static final String FIELD_GROUP_USER_ID = "groupUserId";
    public static final String FIELD_GROUP = "group";
    public static final String FIELD_USER = "user";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_GROUP_ID = "groupId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer groupUserId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrGroup.class)
    @JoinColumn(name = "groupId", nullable = false)
    private UsrGroup group;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = false)
    private UsrUser user;

    @Column(name = "groupId", updatable = false, insertable = false)
    private Integer groupId;

    @Column(name = "userId", updatable = false, insertable = false)
    private Integer userId;

    /**
     * @return identifikátor entity
     */
    public Integer getGroupUserId() {
        return groupUserId;
    }

    /**
     * @param groupUserId identifikátor entity
     */
    public void setGroupUserId(final Integer groupUserId) {
        this.groupUserId = groupUserId;
    }

    /**
     * @return skupina
     */
    public UsrGroup getGroup() {
        return group;
    }

    /**
     * @param group skupina
     */
    public void setGroup(final UsrGroup group) {
        this.group = group;
        this.groupId = group == null ? null : group.getGroupId();
    }

	public Integer getGroupId() {
		return groupId;
	}

    /**
     * @return uživatel ve skupině
     */
    public UsrUser getUser() {
        return user;
    }

    /**
     * @param user uživatel ve skupině
     */
    public void setUser(final UsrUser user) {
        this.user = user;
        this.userId = user == null ? null : user.getUserId();
    }

	public Integer getUserId() {
		return userId;
	}
}
