package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Personal API key used to authenticate a machine client as one specific user.
 *
 * Only the SHA-256 hash of the secret part is stored; the full token is shown to the user
 * once at creation and cannot be retrieved after. Multiple keys per user are allowed so
 * integrations can be rotated independently.
 */
@Entity(name = "usr_api_key")
public class UsrApiKey {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer apiKeyId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "user_id", nullable = false)
    private UsrUser user;

    @Column(length = 250, nullable = false)
    private String name;

    /**
     * Public identifier of the key, 12 chars base62. Used to locate the row before the secret
     * is verified; shown in UI and audit as {@code elza_<keyId>}.
     */
    @Column(name = "key_id", length = 50, nullable = false)
    private String keyId;

    /** SHA-256 of the secret part, hex-encoded (64 chars). */
    @Column(length = 64, nullable = false)
    private String secretHash;

    @Column(nullable = false)
    private OffsetDateTime createDate;

    @Column(nullable = false)
    private OffsetDateTime expireDate;

    /** Last successful authentication; updated at most once per minute to avoid write amplification. */
    @Column
    private OffsetDateTime lastUsedDate;

    /** When the key was revoked; {@code null} means the key has not been revoked. */
    @Column
    private OffsetDateTime revokedDate;

    /** Who revoked the key (owner or an administrator). */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "revoked_by_user_id")
    private UsrUser revokedByUser;

    public Integer getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Integer apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public UsrUser getUser() {
        return user;
    }

    public void setUser(UsrUser user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public void setSecretHash(String secretHash) {
        this.secretHash = secretHash;
    }

    public OffsetDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(OffsetDateTime createDate) {
        this.createDate = createDate;
    }

    public OffsetDateTime getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(OffsetDateTime expireDate) {
        this.expireDate = expireDate;
    }

    public OffsetDateTime getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(OffsetDateTime lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public OffsetDateTime getRevokedDate() {
        return revokedDate;
    }

    public void setRevokedDate(OffsetDateTime revokedDate) {
        this.revokedDate = revokedDate;
    }

    public UsrUser getRevokedByUser() {
        return revokedByUser;
    }

    public void setRevokedByUser(UsrUser revokedByUser) {
        this.revokedByUser = revokedByUser;
    }
}
