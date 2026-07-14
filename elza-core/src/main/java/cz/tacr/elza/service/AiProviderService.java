package cz.tacr.elza.service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import cz.tacr.elza.aiprovider.client.ElzaAiApi;
import cz.tacr.elza.aiprovider.client.vo.AiServiceInfo;
import cz.tacr.elza.connector.ApiClientAiProvider;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.SysExternalSystemProperty;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.repository.AiExternalSystemRepository;
import cz.tacr.elza.repository.SysExternalSystemPropertyRepository;

/**
 * Communication with AI providers over the "Elza AI Provider API"
 * (contract: elza-development/typespec-ai; generated client
 * {@code cz.tacr.elza.aiprovider.client}). Requests are signed by
 * {@link ApiClientAiProvider}.
 *
 * <p><b>Key selection</b> (accounts &amp; credits proposal in
 * elza-ai-provider.git, decisions Q3/Q6): a user's tasks are signed with the
 * user's <em>personal</em> key when one is stored, billing to that user's
 * personal account (seat). Without one, the instance-wide key of the external
 * system is used (the shared organizational account). No key at all = AI is not
 * available. KeyIds are opaque strings issued by the provider's account
 * authority — never interpreted here.
 *
 * <p>A personal key is a pair of per-user rows in
 * {@code sys_external_system_property} named {@code apiKeyId}/{@code apiKeyValue}
 * — the same convention CAM and other external systems use, so the shared
 * per-user "API keys" settings ({@code ExternalsystemsApi}) manage the AI key
 * too; there is no AI-specific key endpoint.
 *
 * <p><b>Caching</b>: both the signed API client (connector) and the provider's
 * {@code /info} catalog are cached per access key. A user with a personal key
 * therefore reads the task types, profiles and limits of their own
 * subscription, over their own connector; the catalog is reused for
 * {@link #INFO_CACHE_TTL} so repeated UI loads don't re-call the provider.
 */
@Service
public class AiProviderService {

    /** Per-user property: personal signing KeyId (shared external-system convention). */
    private static final String PROPERTY_API_KEY_ID = "apiKeyId";

    /** Per-user property: personal signing secret (shared external-system convention). */
    private static final String PROPERTY_API_KEY_VALUE = "apiKeyValue";

    /** How long a provider {@code /info} response is reused before a re-fetch (per access key). */
    private static final Duration INFO_CACHE_TTL = Duration.ofMinutes(5);

    @Autowired
    private AiExternalSystemRepository aiExternalSystemRepository;

    @Autowired
    private SysExternalSystemPropertyRepository sysExtSysPropertyRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ElzaLocale elzaLocale;

    /** Identity of an access key within a provider: (external system, KeyId). */
    private record CacheKey(Integer externalSystemId, String keyId) { }

    /** Resolved signing credentials (personal or instance-wide). */
    private record Credentials(String keyId, String secret) { }

    /** A cached connector together with the url/secret it was built for. */
    private record Connector(String url, String secret, ElzaAiApi api) { }

    /** A cached {@code /info} response with the instant it was fetched. */
    private record CachedInfo(AiServiceInfo info, Instant fetchedAt) { }

    /** Signed API clients reused per access key; rebuilt on a url/secret change. */
    private final Map<CacheKey, Connector> connectorCache = new ConcurrentHashMap<>();

    /** Provider {@code /info} responses cached per access key for {@link #INFO_CACHE_TTL}. */
    private final Map<CacheKey, CachedInfo> infoCache = new ConcurrentHashMap<>();

    /**
     * Finds an AI provider external system by numeric id or code.
     */
    @Transactional
    public AiExternalSystem findAiSystemByCodeOrId(final String codeOrId) {
        return aiExternalSystemRepository.findAll().stream()
                .filter(es -> codeOrId.equals(es.getCode())
                        || codeOrId.equals(String.valueOf(es.getExternalSystemId())))
                .findFirst()
                .orElseThrow(() -> new ObjectNotFoundException("AI provider not found: " + codeOrId,
                        BaseCode.ID_NOT_EXIST).setId(codeOrId));
    }

    /**
     * Reads the provider's service info ({@code GET /info}) — protocol version,
     * supported task types, profiles, limits — for the logged user's access
     * key. The catalog is the user's own (their personal key's subscription
     * when set, else the instance-wide one) and doubles as the connection test:
     * a successful call proves reachability, credentials and an active
     * subscription. Cached per access key, see {@link #INFO_CACHE_TTL}.
     */
    @Transactional
    public AiServiceInfo getInfo(final AiExternalSystem extSystem) {
        return serviceInfo(extSystem, loggedUser().getUserId());
    }

    /**
     * Provider service info for the given user, without a permission check, for
     * internal use during that user's task submission (matching the task's
     * declared parameters). Same per-user key selection and caching as
     * {@link #getInfo}.
     *
     * @param userId owner of the tasks (conversation owner)
     */
    public AiServiceInfo fetchServiceInfo(final AiExternalSystem extSystem, final Integer userId) {
        return serviceInfo(extSystem, userId);
    }

    /**
     * Signed API client billing to the given user's account: the user's
     * personal key when stored, else the instance-wide key (the shared
     * organizational account). The client is reused across calls (see
     * {@link #connector}).
     *
     * @param userId owner of the tasks (conversation owner), not necessarily
     *            the logged user — the poller runs with no user context
     */
    public ElzaAiApi createApi(final AiExternalSystem extSystem, final Integer userId) {
        return connector(extSystem, resolveCredentials(extSystem, userId));
    }

    /**
     * Provider capabilities for the given user's access key, cached per access
     * key for {@link #INFO_CACHE_TTL}.
     */
    private AiServiceInfo serviceInfo(final AiExternalSystem extSystem, final Integer userId) {
        Credentials credentials = resolveCredentials(extSystem, userId);
        CacheKey cacheKey = new CacheKey(extSystem.getExternalSystemId(), credentials.keyId());
        CachedInfo cached = infoCache.get(cacheKey);
        if (cached != null && Instant.now().isBefore(cached.fetchedAt().plus(INFO_CACHE_TTL))) {
            return cached.info();
        }
        AiServiceInfo info = fetchInfo(extSystem, credentials);
        infoCache.put(cacheKey, new CachedInfo(info, Instant.now()));
        return info;
    }

    private AiServiceInfo fetchInfo(final AiExternalSystem extSystem, final Credentials credentials) {
        ElzaAiApi api = connector(extSystem, credentials);
        try {
            return api.getInfo(OffsetDateTime.now(), acceptLanguage());
        } catch (RestClientResponseException e) {
            throw new SystemException("AI provider call failed: HTTP " + e.getStatusCode().value(), e,
                    ExternalCode.EXTERNAL_SYSTEM_ERROR)
                            .set("externalSystem", extSystem.getCode())
                            .set("responseBody", e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new SystemException("AI provider call failed: " + e.getMessage(), e,
                    ExternalCode.EXTERNAL_SYSTEM_ERROR)
                            .set("externalSystem", extSystem.getCode());
        }
    }

    /**
     * The signed API client for the given access key, reused across calls. A
     * client is rebuilt when the provider url or the key's secret changes (e.g.
     * the instance-wide key was rotated), so a cached client never signs with a
     * stale secret.
     */
    private ElzaAiApi connector(final AiExternalSystem extSystem, final Credentials credentials) {
        CacheKey cacheKey = new CacheKey(extSystem.getExternalSystemId(), credentials.keyId());
        Connector cached = connectorCache.get(cacheKey);
        if (cached != null && cached.url().equals(extSystem.getUrl())
                && cached.secret().equals(credentials.secret())) {
            return cached.api();
        }
        ElzaAiApi api = new ElzaAiApi(new ApiClientAiProvider(extSystem.getUrl(),
                credentials.keyId(), credentials.secret()));
        connectorCache.put(cacheKey, new Connector(extSystem.getUrl(), credentials.secret(), api));
        return api;
    }

    /**
     * The signing credentials that bill to the given user: the user's personal
     * key when stored (per-user rows in {@code sys_external_system_property}),
     * else the instance-wide key. No key at all = AI is not available.
     */
    private Credentials resolveCredentials(final AiExternalSystem extSystem, final Integer userId) {
        String keyId = null;
        String secret = null;
        if (userId != null) {
            for (SysExternalSystemProperty property : sysExtSysPropertyRepository
                    .findByExternalSystemIdAndUserId(extSystem.getExternalSystemId(), userId)) {
                if (PROPERTY_API_KEY_ID.equals(property.getName())) {
                    keyId = property.getValue();
                } else if (PROPERTY_API_KEY_VALUE.equals(property.getName())) {
                    secret = property.getValue();
                }
            }
        }
        if (StringUtils.isBlank(keyId) || StringUtils.isBlank(secret)) {
            keyId = extSystem.getApiKeyId();
            secret = extSystem.getApiKeyValue();
        }
        if (StringUtils.isBlank(keyId) || StringUtils.isBlank(secret)) {
            throw new BusinessException(
                    "AI services are not available: no signing key (neither personal nor instance-wide)",
                    BaseCode.INVALID_STATE).set("externalSystem", extSystem.getCode());
        }
        return new Credentials(keyId, secret);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private UsrUser loggedUser() {
        UsrUser user = userService.getLoggedUser();
        if (user == null) {
            throw new BusinessException("User not logged in", BaseCode.INSUFFICIENT_PERMISSIONS);
        }
        return user;
    }

    /**
     * The deployment-wide locale as a BCP-47 {@code Accept-Language} value, so the
     * provider localizes the human-readable catalog labels (task-type and profile
     * names/descriptions). Elza has no per-user server locale — the client renders
     * its own UI chrome — so the single {@code elza.locale} is the best hint
     * available. Returns {@code null} for an undefined locale (header omitted).
     */
    private String acceptLanguage() {
        String tag = elzaLocale.getLocale().toLanguageTag();
        return StringUtils.isBlank(tag) || "und".equals(tag) ? null : tag;
    }
}
