package cz.tacr.elza.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.UsrApiKey;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.UsrApiKeyRepository;
import cz.tacr.elza.security.apikey.ApiKeyProperties;

/**
 * Personal API keys: creation, listing, revocation, and the lookup used by the authentication
 * provider.
 *
 * The secret part of a token is generated here and returned once, then only its SHA-256 hash is
 * kept. Verification of the secret against that hash lives in the authentication provider so the
 * hash never leaves this module together with a user identity.
 */
@Service
public class ApiKeyService {

    /** Base62 alphabet — URL-safe, no separator collision with the token's underscore. */
    private static final String BASE62_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** Length of the public identifier. 62^12 ≈ 3·10²¹ possible values — unique index guards collisions. */
    private static final int KEY_ID_LENGTH = 12;

    /** Length of the secret in base62 chars — 43 chars give more than 256 bits of entropy. */
    private static final int SECRET_LENGTH = 43;

    /** How often the last-used timestamp is written back to the database, per key. */
    private static final Duration LAST_USED_WRITE_INTERVAL = Duration.ofMinutes(1);

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Per-key gate for {@link #touchLastUsed(Integer)} writes. Kept in memory only — in a
     * multi-node deployment each node writes at most once per minute per key, which is acceptable.
     */
    private final ConcurrentHashMap<Integer, Instant> lastUsedWrites = new ConcurrentHashMap<>();

    @Autowired
    private UsrApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyProperties properties;

    /** Result of {@link #create(UsrUser, String, OffsetDateTime)}: the stored entity plus the token shown once. */
    public record CreatedApiKey(UsrApiKey apiKey, String token) {}

    /**
     * Creates a new key for the given user and returns its full token.
     *
     * The token is emitted exactly once, in the return value. After this call the server keeps
     * only the SHA-256 hash of the secret part.
     *
     * @param user       owner of the key
     * @param name       user-facing name, non-blank, up to 250 chars
     * @param expireDate desired expiration; when {@code null}, {@link ApiKeyProperties#getDefaultValidityDays()}
     *                   is applied. Must lie in the future and no farther than
     *                   {@link ApiKeyProperties#getMaxValidityDays()} from now.
     */
    @Transactional
    public CreatedApiKey create(UsrUser user, String name, OffsetDateTime expireDate) {
        Objects.requireNonNull(user, "user");

        String trimmedName = StringUtils.trimToNull(name);
        if (trimmedName == null) {
            throw new SystemException("Název API klíče musí být vyplněn", BaseCode.PROPERTY_NOT_EXIST)
                    .set(BaseCode.PARAM_PROPERTY, "name");
        }
        if (trimmedName.length() > 250) {
            throw new SystemException("Název API klíče je příliš dlouhý (max 250 znaků)", BaseCode.INVALID_LENGTH)
                    .set(BaseCode.PARAM_PROPERTY, "name");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime effectiveExpire = (expireDate != null)
                ? expireDate
                : now.plusDays(properties.getDefaultValidityDays());
        OffsetDateTime maxExpire = now.plusDays(properties.getMaxValidityDays());
        if (!effectiveExpire.isAfter(now)) {
            throw new SystemException("Konec platnosti API klíče musí ležet v budoucnosti", BaseCode.PROPERTY_IS_INVALID)
                    .set(BaseCode.PARAM_PROPERTY, "expireDate");
        }
        if (effectiveExpire.isAfter(maxExpire)) {
            throw new SystemException("Konec platnosti API klíče přesahuje povolené maximum", BaseCode.PROPERTY_IS_INVALID)
                    .set(BaseCode.PARAM_PROPERTY, "expireDate");
        }

        String keyId = generateBase62(KEY_ID_LENGTH);
        String secret = generateBase62(SECRET_LENGTH);

        UsrApiKey entity = new UsrApiKey();
        entity.setUser(user);
        entity.setName(trimmedName);
        entity.setKeyId(keyId);
        entity.setSecretHash(sha256Hex(secret));
        entity.setCreateDate(now);
        entity.setExpireDate(effectiveExpire);
        apiKeyRepository.save(entity);

        return new CreatedApiKey(entity, "elza_" + keyId + "_" + secret);
    }

    /** Lists a user's own keys, newest first. */
    @Transactional(readOnly = true)
    public List<UsrApiKey> listByUser(UsrUser user) {
        Objects.requireNonNull(user, "user");
        return apiKeyRepository.findByUserOrderByCreateDateDesc(user);
    }

    /** Lists any user's keys by id — for the admin overview. */
    @Transactional(readOnly = true)
    public List<UsrApiKey> listByUserId(Integer userId) {
        Objects.requireNonNull(userId, "userId");
        return apiKeyRepository.findByUserUserIdOrderByCreateDateDesc(userId);
    }

    /**
     * Revokes the key. Idempotent — revoking an already-revoked key returns silently.
     *
     * @return the entity in its final state, or {@link Optional#empty()} if no such key exists
     */
    @Transactional
    public Optional<UsrApiKey> revoke(Integer apiKeyId, UsrUser revokedBy) {
        Objects.requireNonNull(apiKeyId, "apiKeyId");
        // revokedBy may be null when the built-in admin revokes someone else's key — no
        // usr_user row for the actor exists, and revoked_by_user_id is nullable in the schema.

        Optional<UsrApiKey> found = apiKeyRepository.findById(apiKeyId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        UsrApiKey entity = found.get();
        if (entity.getRevokedDate() != null) {
            return found;
        }
        entity.setRevokedDate(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setRevokedByUser(revokedBy);
        apiKeyRepository.save(entity);
        return Optional.of(entity);
    }

    /**
     * Looks the key row up for authentication. The caller (authentication provider) inspects
     * {@code revokedDate}, {@code expireDate}, verifies the secret hash and checks user activity.
     */
    @Transactional(readOnly = true)
    public Optional<UsrApiKey> findByKeyId(String keyId) {
        if (StringUtils.isBlank(keyId)) {
            return Optional.empty();
        }
        return apiKeyRepository.findByKeyId(keyId);
    }

    /**
     * Records that the key was just used successfully. Throttled to at most one database write
     * per key per {@link #LAST_USED_WRITE_INTERVAL}, so a busy integration does not amplify one
     * request into a stream of writes.
     */
    @Transactional
    public void touchLastUsed(Integer apiKeyId) {
        Objects.requireNonNull(apiKeyId, "apiKeyId");
        Instant now = Instant.now();
        Instant previous = lastUsedWrites.get(apiKeyId);
        if (previous != null && Duration.between(previous, now).compareTo(LAST_USED_WRITE_INTERVAL) < 0) {
            return;
        }
        lastUsedWrites.put(apiKeyId, now);
        apiKeyRepository.findById(apiKeyId).ifPresent(entity -> {
            entity.setLastUsedDate(OffsetDateTime.now(ZoneOffset.UTC));
            apiKeyRepository.save(entity);
        });
    }

    /** Loads a key by its internal id, or throws 404. */
    @Transactional(readOnly = true)
    public UsrApiKey getRequired(Integer apiKeyId) {
        return apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ObjectNotFoundException("API key not found", BaseCode.ID_NOT_EXIST)
                        .set(BaseCode.PARAM_PROPERTY, "apiKeyId"));
    }

    private String generateBase62(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_ALPHABET.charAt(secureRandom.nextInt(BASE62_ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Shared with the authentication provider so both sides hash the secret the same way. */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
