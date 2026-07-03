package cz.tacr.elza.service;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.aiprovider.ApiException;
import cz.tacr.elza.aiprovider.client.ElzaAiApi;
import cz.tacr.elza.aiprovider.client.vo.AiServiceInfo;
import cz.tacr.elza.connector.ApiClientAiProvider;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.repository.AiExternalSystemRepository;

/**
 * Communication with AI providers over the "Elza AI Provider API"
 * (contract: elza-development/typespec-ai; generated client
 * {@code cz.tacr.elza.aiprovider.client}). Requests are signed by
 * {@link ApiClientAiProvider}.
 */
@Service
public class AiProviderService {

    @Autowired
    private AiExternalSystemRepository aiExternalSystemRepository;

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
     * subscription.
     */
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
    @Transactional
    public AiServiceInfo getInfo(final AiExternalSystem extSystem) {
        ElzaAiApi api = new ElzaAiApi(createClient(extSystem));
        try {
            return api.getInfo(OffsetDateTime.now());
        } catch (ApiException e) {
            throw new SystemException("AI provider call failed: HTTP " + e.getCode(), e,
                    ExternalCode.EXTERNAL_SYSTEM_ERROR)
                            .set("externalSystem", extSystem.getCode())
                            .set("responseBody", e.getResponseBody());
        }
    }

    /** Signed API client for the given provider (see {@link ApiClientAiProvider}). */
    public ElzaAiApi createApi(final AiExternalSystem extSystem) {
        return new ElzaAiApi(createClient(extSystem));
    }

    private ApiClientAiProvider createClient(final AiExternalSystem extSystem) {
        return new ApiClientAiProvider(extSystem.getUrl(), extSystem.getApiKeyId(),
                extSystem.getApiKeyValue());
    }
}
