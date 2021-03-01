package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import cz.tacr.elza.service.cache.AccessPointCacheSerializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Uživatel.
 *
 * @since 11.04.2016
 */
@Entity(name = "usr_user")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrUser implements AccessPointCacheSerializable {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId", nullable = false)
    private ApAccessPoint accessPoint;

    @Column(length = 250, nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private Boolean active;

    @Column(length = 250)
    private String description;

    /* Konstanty pro vazby a fieldy. */
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_ACCESS_POINT = "accessPoint";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_ACTIVE = "active";

    /**
     * @return identifikátor entity
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * @param userId identifikátor entity
     */
    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    /**
     * @return uživatelské jméno
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username uživatelské jméno
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * @return je účet aktivní?
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * @param active je účet aktivní?
     */
    public void setActive(final Boolean active) {
        this.active = active;
    }

    /**
     * @return poznámka u uživateli
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description poznámka u uživateli
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }
}
