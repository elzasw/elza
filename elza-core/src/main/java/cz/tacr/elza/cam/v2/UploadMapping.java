package cz.tacr.elza.cam.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Transient payload for the in-flight CAM upload, stored as JSON on
 * {@code ExtSyncsQueueItem.uploadMap}. Groups the specialized mappings collected
 * while the batch is built and consumed by the confirm processor once CAM accepts:
 * <ul>
 *   <li>{@link #uuidMappings} — part/item ↔ CAM UUID pairs, to resolve
 *       {@code IssueXml.partRef}/{@code itemRef} back to ELZA ids;</li>
 *   <li>{@link #participants} — people sent in the batch, mirrored into
 *       {@code ap_binding_participant} on confirmation.</li>
 * </ul>
 * Populated when the batch is posted to CAM; cleared on terminal states.
 */
public class UploadMapping {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private List<UuidMapping> uuidMappings = new ArrayList<>();
    private List<ParticipantMapping> participants = new ArrayList<>();

    public List<UuidMapping> getUuidMappings() {
        return uuidMappings;
    }

    public void setUuidMappings(List<UuidMapping> uuidMappings) {
        this.uuidMappings = uuidMappings;
    }

    public List<ParticipantMapping> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantMapping> participants) {
        this.participants = participants;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return uuidMappings.isEmpty() && participants.isEmpty();
    }

    /**
     * Build the payload from the upload-side maps ({@code partId -> uuid},
     * {@code itemId -> uuid}) and participants, and serialize it to the JSON stored on
     * {@code ExtSyncsQueueItem.uploadMap}. Returns {@code null} when everything is empty
     * so the column stays {@code null} rather than holding an empty payload.
     */
    public static String serialize(Map<Integer, String> partUuidMap,
                                   Map<Integer, String> itemUuidMap,
                                   List<ParticipantMapping> participants) {
        UploadMapping payload = new UploadMapping();
        if (partUuidMap != null) {
            partUuidMap.forEach((id, uuid) -> payload.uuidMappings.add(UuidMapping.forPart(id, uuid)));
        }
        if (itemUuidMap != null) {
            itemUuidMap.forEach((id, uuid) -> payload.uuidMappings.add(UuidMapping.forItem(id, uuid)));
        }
        if (participants != null) {
            payload.participants.addAll(participants);
        }
        if (payload.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize UploadMapping", e);
        }
    }

    /** Deserialize the payload produced by {@link #serialize}; an empty payload for null/blank input. */
    public static UploadMapping deserialize(String json) {
        if (json == null || json.isBlank()) {
            return new UploadMapping();
        }
        try {
            return MAPPER.readValue(json, UploadMapping.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize UploadMapping", e);
        }
    }
}
