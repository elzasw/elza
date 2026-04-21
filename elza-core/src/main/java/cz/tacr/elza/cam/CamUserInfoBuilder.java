package cz.tacr.elza.cam;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static cz.tacr.elza.groovy.GroovyResult.SHORT_NAME;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

/**
 * Build user info string from a template shared by all CAM API versions.
 *
 * Supported template placeholders:
 * <ul>
 *   <li>{@code %i} - user id</li>
 *   <li>{@code %u} - user login (username)</li>
 *   <li>{@code %n} - preferred (display) name</li>
 *   <li>{@code %s} - short name</li>
 * </ul>
 */
@Component
public class CamUserInfoBuilder {

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    /**
     * Render the user info template for the given user.
     *
     * When {@code template} is {@code null}, the user login is returned.
     *
     * @param template user info template (may be {@code null})
     * @param user sending user (may be {@code null} for system calls)
     * @return rendered user info string
     */
    public String buildUserInfo(String template, UsrUser user) {
        String userName;
        String userId;
        String prefName;
        String shortName;
        if (user == null) {
            userName = "admin";
            userId = "0";
            prefName = "Admin";
            shortName = prefName;
        } else {
            prefName = shortName = userName = user.getUsername();
            userId = Integer.toString(user.getUserId());
            CachedAccessPoint cachedAp = accessPointCacheService.findCachedAccessPoint(user.getAccessPointId());
            Objects.requireNonNull(cachedAp);
            CachedPart prefPart = cachedAp.getPart(cachedAp.getPreferredPartId());
            Objects.requireNonNull(prefPart);
            for (ApIndex index : prefPart.getIndices()) {
                if (index.getIndexType().equals(DISPLAY_NAME)) {
                    prefName = index.getIndexValue();
                } else if (index.getIndexType().equals(SHORT_NAME)) {
                    shortName = index.getIndexValue();
                }
            }
        }
        if (template == null) {
            return userName;
        }
        return template.replaceAll("%i", userId)
                .replaceAll("%u", userName)
                .replaceAll("%n", prefName)
                .replaceAll("%s", shortName);
    }
}
