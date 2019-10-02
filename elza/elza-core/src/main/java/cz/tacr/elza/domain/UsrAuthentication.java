package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

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
    private String value;

    /* Konstanty pro vazby a fieldy. */
    public static final String FIELD_AUTHENTICATION_ID = "authenticationId";
    public static final String FIELD_USER = "user";
    public static final String FIELD_AUTH_TYPE = "authType";
    public static final String FIELD_VALUE = "value";

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

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
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
         * SSO shibboleth v podporované verzi.
         */
        SHIBBOLETH

    }
}
