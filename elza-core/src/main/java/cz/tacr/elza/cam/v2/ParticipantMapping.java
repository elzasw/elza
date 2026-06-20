package cz.tacr.elza.cam.v2;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One participant sent in the batch as an {@code addParticipant} action, mirrored
 * into {@code ap_binding_participant} once CAM confirms the upload.
 *
 * One element kind of the {@link UploadMapping} payload. {@link #lastChange} is kept
 * as an ISO-8601 string so the bare {@code ObjectMapper} needs no JSR-310 module.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParticipantMapping {

    private String role;
    private String name;
    private String lastChange;

    public ParticipantMapping() {
    }

    /**
     * @param role the {@code ApBindingParticipant.Role} name, already mapped from the
     *        CAM direction (e.g. EDITOR -&gt; AUTHOR)
     * @param name the rendered user-info name that went on the wire
     * @param lastChange timestamp of the participant's activity
     */
    public static ParticipantMapping of(String role, String name, OffsetDateTime lastChange) {
        ParticipantMapping m = new ParticipantMapping();
        m.role = role;
        m.name = name;
        m.lastChange = lastChange == null ? null : lastChange.toString();
        return m;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastChange() {
        return lastChange;
    }

    public void setLastChange(String lastChange) {
        this.lastChange = lastChange;
    }
}
