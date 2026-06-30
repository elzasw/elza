package cz.tacr.elza.cam.v2;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.cam.v2.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.v2.schema.cam.UserInfoXml;
import cz.tacr.cam.v2.schema.cam.UserRefXml;
import cz.tacr.cam.v2.schema.cam.UuidXml;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * CAM Schema helper methods
 *
 */
public class CamHelper {

    private static final Logger log = LoggerFactory.getLogger(CamHelper.class);

    /**
     * Resolve the {@code ExternalUser} choice ({@link UserInfoXml} or
     * {@link UserRefXml} IDREF) into a displayable user name.
     *
     * @param externalUser value returned by {@code getExternalUser()}; may be {@code null}
     * @return {@link UserInfoXml#getName()} value, or {@code null} if {@code externalUser} is {@code null}
     */
    public static String getExternalUserName(Object externalUser) {
        UserInfoXml userInfo = resolveUserInfo(externalUser);
        if (userInfo == null) {
            return null;
        }
        if (userInfo.getName() == null || userInfo.getName().getValue() == null) {
            log.error("UserInfoXml is missing required name: id={}", userInfo.getId());
            throw new SystemException("UserInfoXml is missing required name", BaseCode.INVALID_STATE);
        }
        return userInfo.getName().getValue();
    }

    /**
     * Resolve the {@code ExternalUser} choice ({@link UserInfoXml} or
     * {@link UserRefXml} IDREF) into its {@link UserInfoXml}.
     *
     * @param externalUser value returned by {@code getExternalUser()}; may be {@code null}
     * @return the resolved {@link UserInfoXml}, or {@code null} if {@code externalUser} is {@code null}
     */
    public static UserInfoXml resolveUserInfo(Object externalUser) {
        if (externalUser == null) {
            return null;
        }
        if (externalUser instanceof UserInfoXml info) {
            return info;
        }
        if (externalUser instanceof UserRefXml ref) {
            // @XmlIDREF: JAXB resolves the ref to the target UserInfoXml during unmarshal
            Object target = ref.getValue();
            if (!(target instanceof UserInfoXml resolved)) {
                log.error("UserRefXml IDREF did not resolve to UserInfoXml: target={}", target);
                throw new SystemException("UserRefXml IDREF did not resolve to UserInfoXml", BaseCode.INVALID_STATE)
                        .set("targetType", target == null ? null : target.getClass().getName());
            }
            return resolved;
        }
        log.error("Unexpected externalUser type: {}", externalUser.getClass().getName());
        throw new SystemException("Unexpected externalUser type", BaseCode.INVALID_STATE)
                .set("type", externalUser.getClass().getName());
    }

    /**
     * Return list of IDs
     *
     * @param entities
     * @return
     */
    public static List<String> getEids(List<EntityXml> entities) {
        List<String> values = new ArrayList<>();
        for (EntityXml entityXml : entities) {
            values.add(String.valueOf(entityXml.getEntityId().getValue()));
        }
        return values;
    }

    /**
     * Return list of UUIDs
     * 
     * @param entities
     * @return
     */
    public static List<String> getEuids(List<EntityXml> entities) {
        List<String> values = new ArrayList<>();
        for (EntityXml entityXml : entities) {
            values.add(String.valueOf(entityXml.getEntityUuid().getValue()));
        }
        return values;
    }

    public static String getUuid(UuidXml uuid) {
        return uuid != null ? uuid.getValue() : null;
    }

    public static String getEntityId(EntityXml entityXml) {
        return String.valueOf(entityXml.getEntityId().getValue());
    }

    public static String getEntityUuid(EntityXml entityXml) {
        return entityXml.getEntityUuid().getValue();
    }

    public static String getEntityIdorUuid(ItemEntityRefXml itemEntityRef) {
        EntityRecordRefXml entityRecordRef = (EntityRecordRefXml) itemEntityRef.getRef();
        return getEntityIdorUuid(entityRecordRef);
    }

    public static String getEntityIdorUuid(EntityRecordRefXml entityRef) {
        // prepare external ID
        String extIdent = null;
        if (entityRef.getEntityId() != null) {
            extIdent = Long.toString(entityRef.getEntityId().getValue());
        }
        if (extIdent == null && entityRef.getEntityUuid() != null) {
            extIdent = entityRef.getEntityUuid().getValue();
        }
        if (extIdent == null) {
            throw new BusinessException("External ID is empty. UUID or ID has to be provided.", BaseCode.ID_NOT_EXIST);
        }
        return extIdent;
    }
}
