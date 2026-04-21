package cz.tacr.elza.cam;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static cz.tacr.elza.groovy.GroovyResult.SHORT_NAME;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApChangeRepository.ParticipantRow;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

/**
 * CAM-shared user helper used across all API versions:
 * <ul>
 *   <li>render the user info template string for a single user (batch sender,
 *       participant inline definition);</li>
 *   <li>collect {@link ParticipantRecord} entries (EDITORs + APPROVERs) for
 *       an access point, optionally filtered to a delta window.</li>
 * </ul>
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
public class CamUserService {

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private ApChangeRepository apChangeRepository;

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

    /**
     * Collect {@code (user, role, lastChange)} tuples describing everyone who
     * participated on {@code ap} — EDITORs (users who changed parts or items)
     * and APPROVERs (users who created an ApState with stateApproval =
     * APPROVED).
     *
     * @param ap access point to inspect
     * @param sinceChangeIdExclusive only include activity whose change id is
     *        strictly greater than this value; pass {@code null} to walk the
     *        full history (first-ever upload of the AP).
     * @return one record per (user, role) pair, aggregated to the user's
     *         most recent change timestamp in that role.
     */
    public List<ParticipantRecord> collectParticipants(ApAccessPoint ap, Integer sinceChangeIdExclusive) {
        List<ParticipantRecord> out = new ArrayList<>();
        for (Object[] row : apChangeRepository.findEditorParticipants(ap, sinceChangeIdExclusive)) {
            ParticipantRow pr = ParticipantRow.from(row);
            out.add(new ParticipantRecord(pr.user(), ParticipantRole.EDITOR, pr.lastChange()));
        }
        for (Object[] row : apChangeRepository.findApproverParticipants(ap, sinceChangeIdExclusive)) {
            ParticipantRow pr = ParticipantRow.from(row);
            out.add(new ParticipantRecord(pr.user(), ParticipantRole.APPROVER, pr.lastChange()));
        }
        return out;
    }

    public enum ParticipantRole {
        EDITOR,
        APPROVER
    }

    public record ParticipantRecord(UsrUser user, ParticipantRole role, OffsetDateTime lastChange) {
    }
}
