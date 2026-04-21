package cz.tacr.elza.cam.v2;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import cz.tacr.cam.v2.schema.cam.CodeXml;
import cz.tacr.cam.v2.schema.cam.EntityIdXml;
import cz.tacr.cam.v2.schema.cam.LongStringXml;
import cz.tacr.cam.v2.schema.cam.UserInfoXml;
import cz.tacr.cam.v2.schema.cam.UserRefXml;
import cz.tacr.cam.v2.schema.cam.UuidXml;
import cz.tacr.elza.cam.CamUserService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.ExternalSystemService;

/**
 * Per-batch registry of {@link UserInfoXml} instances so every distinct user
 * is declared once (with a stable {@code localId}) and later occurrences emit
 * a {@link UserRefXml} IDREF instead of redefining identity data.
 *
 * Scoped to a single {@code CamService.prepareUpload} call; callers create a
 * fresh registry per batch.
 */
class CamV2UserInfoRegistry {

    private static final String ANONYMOUS_LOCAL_ID = "u_sys";

    private final CamUserService camUserService;
    private final ApExternalSystem apExternalSystem;
    private final ExternalSystemService externalSystemService;

    private final Map<Integer, UserInfoXml> byUserId = new HashMap<>();
    private UserInfoXml anonymousUserInfo;

    CamV2UserInfoRegistry(CamUserService camUserService,
                          ApExternalSystem apExternalSystem,
                          ExternalSystemService externalSystemService) {
        this.camUserService = camUserService;
        this.apExternalSystem = apExternalSystem;
        this.externalSystemService = externalSystemService;
    }

    /**
     * Ensure the given user has an inline {@link UserInfoXml} in this batch
     * and return it. Intended for slots that require a concrete UserInfo
     * (e.g. {@code BatchInfoXml.sender}, which the schema types as UserInfoXml
     * and cannot hold a ref).
     */
    UserInfoXml ensureInline(UsrUser user, String template) {
        return find(user).orElseGet(() -> create(user, template));
    }

    /**
     * Return an object suitable for {@code ParticipantActivityXml.externalUser}:
     * an inline {@link UserInfoXml} on first use of the user in this batch,
     * otherwise a {@link UserRefXml} pointing at the previously registered
     * instance via XML IDREF.
     */
    Object inlineOrRef(UsrUser user, String template) {
        return find(user).<Object>map(UserRefXml::new)
                .orElseGet(() -> create(user, template));
    }

    private Optional<UserInfoXml> find(UsrUser user) {
        if (user == null) {
            return Optional.ofNullable(anonymousUserInfo);
        }
        return Optional.ofNullable(byUserId.get(user.getUserId()));
    }

    private UserInfoXml create(UsrUser user, String template) {
        String userId = user == null ? "0" : Integer.toString(user.getUserId());
        String localId = user == null ? ANONYMOUS_LOCAL_ID : ("u" + user.getUserId());
        CodeXml id = new CodeXml(userId);
        UuidXml uuid = resolveUserUuid(user);
        LongStringXml name = new LongStringXml(camUserService.buildUserInfo(template, user));
        EntityIdXml entityId = resolvePersonEntityId(user);
        UserInfoXml xml = new UserInfoXml(id, uuid, name, entityId, null, localId);
        if (user == null) {
            anonymousUserInfo = xml;
        } else {
            byUserId.put(user.getUserId(), xml);
        }
        return xml;
    }

    private UuidXml resolveUserUuid(UsrUser user) {
        if (user == null) {
            return null;
        }
        ApAccessPoint personAp = user.getAccessPoint();
        if (personAp == null || personAp.getUuid() == null) {
            return null;
        }
        return new UuidXml(personAp.getUuid());
    }

    /**
     * Emit the person AP's CAM {@code eid} only when the person AP is bound to
     * the same external system we are currently uploading to. The lookup
     * scopes to {@link #apExternalSystem}, so a hit implies the binding is
     * meaningful on the target.
     */
    private EntityIdXml resolvePersonEntityId(UsrUser user) {
        if (user == null) {
            return null;
        }
        ApAccessPoint personAp = user.getAccessPoint();
        if (personAp == null) {
            return null;
        }
        ApBindingState bindingState =
                externalSystemService.findByAccessPointAndExternalSystem(personAp, apExternalSystem);
        if (bindingState == null || bindingState.getBinding() == null) {
            return null;
        }
        String value = bindingState.getBinding().getValue();
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new EntityIdXml(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
