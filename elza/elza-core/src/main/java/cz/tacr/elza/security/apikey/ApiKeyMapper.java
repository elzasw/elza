package cz.tacr.elza.security.apikey;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import cz.tacr.elza.controller.vo.ApiKeyInfo;
import cz.tacr.elza.controller.vo.ApiKeyState;
import cz.tacr.elza.domain.UsrApiKey;

/**
 * Converts stored {@link UsrApiKey} rows into the {@link ApiKeyInfo} DTO the API returns.
 * The displayed {@code keyId} is prefixed with {@code elza_} so audit logs, UI and API responses
 * all speak the same, easily recognisable identifier; the secret part is never included.
 */
public final class ApiKeyMapper {

    private ApiKeyMapper() {
    }

    public static ApiKeyInfo toInfo(UsrApiKey entity) {
        ApiKeyInfo info = new ApiKeyInfo(
                entity.getApiKeyId(),
                entity.getName(),
                "elza_" + entity.getKeyId(),
                entity.getCreateDate(),
                entity.getExpireDate(),
                computeState(entity));
        info.setLastUsedDate(entity.getLastUsedDate());
        info.setRevokedDate(entity.getRevokedDate());
        return info;
    }

    private static ApiKeyState computeState(UsrApiKey entity) {
        if (entity.getRevokedDate() != null) {
            return ApiKeyState.REVOKED;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (!entity.getExpireDate().isAfter(now)) {
            return ApiKeyState.EXPIRED;
        }
        return ApiKeyState.ACTIVE;
    }
}
