package cz.tacr.elza.security.apikey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.UsrApiKey;
import cz.tacr.elza.security.AuthenticationMethod;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ApiKeyService;
import cz.tacr.elza.service.UserService;

/**
 * Validates an {@link ApiKeyAuthenticationToken} against the stored row:
 * key exists → not revoked → not expired → secret hash matches (constant-time) → user is active.
 *
 * On success the resulting {@link UsernamePasswordAuthenticationToken} carries a {@link UserDetail}
 * marked with {@link AuthenticationMethod#API_KEY} so downstream operations can refuse actions
 * that must not be reachable through a machine credential.
 */
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final UserService userService;
    private final PlatformTransactionManager txManager;
    private final SiemAuditLogger siemAuditLogger;

    public ApiKeyAuthenticationProvider(ApiKeyService apiKeyService, UserService userService,
                                        PlatformTransactionManager txManager,
                                        SiemAuditLogger siemAuditLogger) {
        this.apiKeyService = apiKeyService;
        this.userService = userService;
        this.txManager = txManager;
        this.siemAuditLogger = siemAuditLogger;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken carrier = (ApiKeyAuthenticationToken) authentication;
        String keyId = carrier.getKeyId();
        String secret = (String) carrier.getCredentials();
        String sourceIp = sourceIp(carrier);

        try {
            UsernamePasswordAuthenticationToken result = new TransactionTemplate(txManager).execute(status -> {
                UsrApiKey key = apiKeyService.findByKeyId(keyId)
                        .orElseThrow(() -> new ApiKeyAuthenticationException(ApiKeyFailure.UNKNOWN_KEY, keyId));

                if (key.getRevokedDate() != null) {
                    throw new ApiKeyAuthenticationException(ApiKeyFailure.REVOKED, keyId);
                }
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                if (!key.getExpireDate().isAfter(now)) {
                    throw new ApiKeyAuthenticationException(ApiKeyFailure.EXPIRED, keyId);
                }
                if (!secretMatches(secret, key.getSecretHash())) {
                    throw new ApiKeyAuthenticationException(ApiKeyFailure.INVALID_SECRET, keyId);
                }

                UsernamePasswordAuthenticationToken auth;
                try {
                    auth = userService.createAuthentication(key.getUser());
                } catch (LockedException e) {
                    throw new ApiKeyAuthenticationException(ApiKeyFailure.USER_INACTIVE, keyId);
                }
                if (auth.getDetails() instanceof UserDetail userDetail) {
                    userDetail.setAuthenticationMethod(AuthenticationMethod.API_KEY);
                }
                apiKeyService.touchLastUsed(key.getApiKeyId());
                return auth;
            });

            siemAuditLogger.apiKeyLoginSuccess(result.getName(), sourceIp, keyId);
            return result;
        } catch (ApiKeyAuthenticationException ex) {
            siemAuditLogger.apiKeyLoginFailed(sourceIp, ex.getKeyId(), ex.getFailure().name());
            throw ex;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static String sourceIp(Authentication auth) {
        return (auth.getDetails() instanceof WebAuthenticationDetails details)
                ? details.getRemoteAddress()
                : null;
    }

    private static boolean secretMatches(String secret, String storedHash) {
        String computed = ApiKeyService.sha256Hex(secret);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
