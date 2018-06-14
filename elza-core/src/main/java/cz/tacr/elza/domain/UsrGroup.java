package cz.tacr.elza.domain;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Uživatelká skupina.
 *
 */
@Entity(name = "usr_group")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrGroup {

	/* Konstanty pro vazby a fieldy. */
	public static final String GROUP_ID = "groupId";
	public static final String CODE = "code";
	public static final String NAME = "name";
	public static final String DESCRIPTION = "description";
	public static final String USERS = "users";

	@Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer groupId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 250, nullable = true)
    private String description;

	@OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
	private List<UsrGroupUser> users;

    /**
     * @return identifikátor entity
     */
    public Integer getGroupId() {
        return groupId;
    }

    /**
     * @param groupId identifikátor entity
     */
    public void setGroupId(final Integer groupId) {
        this.groupId = groupId;
    }

    /**
     * @return kód skupiny
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code kód skupiny
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * @return název skupiny
     */
    public String getName() {
        return name;
    }

    /**
     * @param name název skupiny
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return popis skupiny
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description popis skupiny
     */
    public void setDescription(final String description) {
        this.description = description;
    }

	public List<UsrGroupUser> getUsers() {
		return users;
	}

	public void setUsers(List<UsrGroupUser> users) {
		this.users = users;
	}
}
