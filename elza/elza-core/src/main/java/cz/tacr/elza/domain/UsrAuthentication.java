package cz.tacr.elza.domain;

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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Autorizace uživatele.
 *
 * @since 12.06.2019
 */
@Entity(name = "usr_authentication")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrAuthentication {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer authenticationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = false)
    private UsrUser user;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private AuthType authType;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String authValue;

    /* Konstanty pro vazby a fieldy. */
    public static final String FIELD_AUTHENTICATION_ID = "authenticationId";
    public static final String FIELD_USER = "user";
    public static final String FIELD_AUTH_TYPE = "authType";
    public static final String FIELD_AUTH_VALUE = "authValue";

    public Integer getAuthenticationId() {
        return authenticationId;
    }

    public void setAuthenticationId(final Integer authenticationId) {
        this.authenticationId = authenticationId;
    }

    public UsrUser getUser() {
        return user;
    }

    public void setUser(final UsrUser user) {
        this.user = user;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(final AuthType authType) {
        this.authType = authType;
    }

    public String getAuthValue() {
        return authValue;
    }

    public void setAuthValue(String authValue) {
        this.authValue = authValue;
    }

    /**
     * Typ autentizace.
     */
    public enum AuthType {

        /**
         * Hash hesla (SHA256).
         */
        PASSWORD,

        /**
         * SSO pomocí SAML2 protokolu
         */
        SAML2

    }
}
