package cz.tacr.elza.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.ApiKeyCreateRequest;
import cz.tacr.elza.controller.vo.ApiKeyCreated;
import cz.tacr.elza.controller.vo.ApiKeyInfo;
import cz.tacr.elza.domain.UsrApiKey;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.apikey.ApiKeyMapper;
import cz.tacr.elza.service.ApiKeyService;
import cz.tacr.elza.service.ApiKeyService.CreatedApiKey;
import cz.tacr.elza.service.UserService;

/**
 * REST endpoints under {@code /user/my/api-keys} — a logged-in user's own personal API keys.
 * Every operation refuses a request that was itself authenticated with an API key, so a key
 * cannot be used to escalate into permanent access by minting further keys.
 */
@RestController
@RequestMapping("/api/v1")
public class ApiKeyController implements UserApi {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private UserService userService;

    @Autowired
    private SiemAuditLogger siemAuditLogger;

    @Override
    public ResponseEntity<List<ApiKeyInfo>> apiKeysList() {
        userService.requireInteractiveAuth();
        // The built-in admin (elza.security.allowDefaultUser) has no usr_user row and
        // therefore no keys — show an empty list rather than failing with 403.
        UsrUser user = userService.getLoggedUser();
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }
        List<ApiKeyInfo> result = apiKeyService.listByUser(user).stream()
                .map(ApiKeyMapper::toInfo)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<ApiKeyCreated> apiKeysCreate(ApiKeyCreateRequest request) {
        userService.requireInteractiveAuth();
        UsrUser user = requireLoggedUser();

        CreatedApiKey created = apiKeyService.create(user, request.getName(), request.getExpireDate());
        UsrApiKey entity = created.apiKey();

        siemAuditLogger.apiKeyCreated(user.getUsername(), user.getUsername(), entity.getKeyId(), entity.getExpireDate());

        ApiKeyCreated response = new ApiKeyCreated(ApiKeyMapper.toInfo(entity), created.token());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> apiKeysRevoke(Integer id) {
        userService.requireInteractiveAuth();
        UsrUser user = requireLoggedUser();

        UsrApiKey key = apiKeyService.getRequired(id);
        // A key that belongs to another user is reported as not-found so a user cannot probe
        // for the existence of someone else's keys.
        if (!key.getUser().getUserId().equals(user.getUserId())) {
            throw new ObjectNotFoundException("API key not found", BaseCode.ID_NOT_EXIST).set(BaseCode.PARAM_PROPERTY, "id");
        }

        boolean alreadyRevoked = key.getRevokedDate() != null;
        apiKeyService.revoke(id, user);
        if (!alreadyRevoked) {
            siemAuditLogger.apiKeyRevoked(user.getUsername(), user.getUsername(), key.getKeyId());
        }
        return ResponseEntity.ok().build();
    }

    private UsrUser requireLoggedUser() {
        UsrUser user = userService.getLoggedUser();
        if (user == null) {
            // The built-in admin (elza.security.allowDefaultUser) is authenticated but has no
            // usr_user row, so a personal key cannot be bound to them.
            throw new AccessDeniedException(
                    "Vestavěný účet admin nemůže mít vlastní API klíče.",
                    Collections.emptyList());
        }
        return user;
    }
}
