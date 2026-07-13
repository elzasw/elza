package cz.tacr.elza.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import cz.tacr.elza.aiprovider.client.ElzaAiApi;
import cz.tacr.elza.aiprovider.client.vo.AiServiceInfo;
import cz.tacr.elza.connector.ApiClientAiProvider;
import cz.tacr.elza.controller.vo.AiMyKeyUpdateVO;
import cz.tacr.elza.controller.vo.AiMyKeyVO;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.SysExternalSystemProperty;
import cz.tacr.elza.domain.UsrPermission;
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
 * user's <em>personal</em> key when one is stored (per-user rows in
 * {@code sys_external_system_property}: {@code ai.keyId}/{@code ai.secret} —
 * the CAM pattern), billing to that user's personal account (seat). Without
 * one, the instance-wide key of the external system is used (the shared
 * organizational account). No key at all = AI is not available. KeyIds are
 * opaque strings issued by the provider's account authority — never
 * interpreted here.
 */
@Service
public class AiProviderService {

    /** Per-user property: KeyId of the user's personal AI signing key. */
    public static final String PROPERTY_KEY_ID = "ai.keyId";

    /** Per-user property: HMAC secret of the user's personal AI signing key. */
    public static final String PROPERTY_SECRET = "ai.secret";

    @Autowired
    private AiExternalSystemRepository aiExternalSystemRepository;

    @Autowired
    private SysExternalSystemPropertyRepository sysExtSysPropertyRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ElzaLocale elzaLocale;

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
     * supported task types, profiles, limits. Doubles as the connection test:
     * a successful call proves reachability, credentials and an active
     * subscription. Uses the instance-wide key (admin scope).
     */
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
    @Transactional
    public AiServiceInfo getInfo(final AiExternalSystem extSystem) {
        return fetchServiceInfo(extSystem);
    }

    /**
     * Reads the provider's service info without the admin permission check, for
     * internal use during a user's task submission (matching the task's declared
     * parameters). Uses the instance-wide signing key, like {@link #getInfo}.
     */
    public AiServiceInfo fetchServiceInfo(final AiExternalSystem extSystem) {
        ElzaAiApi api = new ElzaAiApi(createClient(extSystem));
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
     * Signed API client billing to the given user's account: the user's
     * personal key when stored, else the instance-wide key (the shared
     * organizational account).
     *
     * @param userId owner of the tasks (conversation owner), not necessarily
     *            the logged user — the poller runs with no user context
     */
    public ElzaAiApi createApi(final AiExternalSystem extSystem, final Integer userId) {
        String keyId = null;
        String secret = null;
        if (userId != null) {
            for (SysExternalSystemProperty property : sysExtSysPropertyRepository
                    .findByExternalSystemIdAndUserId(extSystem.getExternalSystemId(), userId)) {
                if (PROPERTY_KEY_ID.equals(property.getName())) {
                    keyId = property.getValue();
                } else if (PROPERTY_SECRET.equals(property.getName())) {
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
        return new ElzaAiApi(new ApiClientAiProvider(extSystem.getUrl(), keyId, secret));
    }

    // -----------------------------------------------------------------------
    // The current user's personal key (my-key endpoints)
    // -----------------------------------------------------------------------

    /** Presence of the logged user's personal key; the secret is never returned. */
    @Transactional
    public AiMyKeyVO getMyKey(final AiExternalSystem extSystem) {
        UsrUser user = loggedUser();
        AiMyKeyVO vo = new AiMyKeyVO();
        vo.setKeyId(findProperty(extSystem, user.getUserId(), PROPERTY_KEY_ID)
                .map(SysExternalSystemProperty::getValue).orElse(null));
        return vo;
    }

    /** Sets/replaces the logged user's personal key (KeyId + secret). */
    @Transactional
    public AiMyKeyVO setMyKey(final AiExternalSystem extSystem, final AiMyKeyUpdateVO update) {
        if (StringUtils.isBlank(update.getKeyId()) || StringUtils.isBlank(update.getSecret())) {
            throw new BusinessException("Both keyId and secret are required",
                    BaseCode.PROPERTY_IS_INVALID);
        }
        UsrUser user = loggedUser();
        storeProperty(extSystem, user, PROPERTY_KEY_ID, update.getKeyId().trim());
        storeProperty(extSystem, user, PROPERTY_SECRET, update.getSecret().trim());
        AiMyKeyVO vo = new AiMyKeyVO();
        vo.setKeyId(update.getKeyId().trim());
        return vo;
    }

    /** Removes the logged user's personal key; tasks fall back to the instance-wide key. */
    @Transactional
    public void deleteMyKey(final AiExternalSystem extSystem) {
        UsrUser user = loggedUser();
        for (SysExternalSystemProperty property : sysExtSysPropertyRepository
                .findByExternalSystemIdAndUserId(extSystem.getExternalSystemId(), user.getUserId())) {
            if (PROPERTY_KEY_ID.equals(property.getName()) || PROPERTY_SECRET.equals(property.getName())) {
                sysExtSysPropertyRepository.delete(property);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Optional<SysExternalSystemProperty> findProperty(final AiExternalSystem extSystem,
                                                             final Integer userId,
                                                             final String name) {
        return sysExtSysPropertyRepository
                .findByExternalSystemIdAndUserId(extSystem.getExternalSystemId(), userId).stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst();
    }

    private void storeProperty(final AiExternalSystem extSystem, final UsrUser user,
                               final String name, final String value) {
        List<SysExternalSystemProperty> properties = sysExtSysPropertyRepository
                .findByExternalSystemIdAndUserId(extSystem.getExternalSystemId(), user.getUserId());
        SysExternalSystemProperty property = properties.stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst().orElse(null);
        if (property == null) {
            property = new SysExternalSystemProperty();
            property.setExternalSystem(extSystem);
            property.setUser(user);
            property.setName(name);
        }
        property.setValue(value);
        sysExtSysPropertyRepository.save(property);
    }

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

    private ApiClientAiProvider createClient(final AiExternalSystem extSystem) {
        if (StringUtils.isBlank(extSystem.getApiKeyId()) || StringUtils.isBlank(extSystem.getApiKeyValue())) {
            throw new BusinessException("AI external system has no instance-wide signing key",
                    BaseCode.INVALID_STATE).set("externalSystem", extSystem.getCode());
        }
        return new ApiClientAiProvider(extSystem.getUrl(), extSystem.getApiKeyId(),
                extSystem.getApiKeyValue());
    }
}
