package cz.tacr.elza.cam.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * One entry in {@code ExtSyncsQueueItem.uuidMap} — pairs a CAM UUID generated
 * during batch upload with the ELZA part or item it represents.
 *
 * Exactly one of {@link #partId} / {@link #itemId} is set. Stored as a flat
 * list in JSON so the map can be extended (e.g. a future {@code entityId}
 * column) without restructuring existing entries.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UuidMapping {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<UuidMapping>> LIST_TYPE = new TypeReference<List<UuidMapping>>() {};

    private Integer partId;
    private Integer itemId;
    private String uuid;

    public UuidMapping() {
    }

    public static UuidMapping forPart(int partId, String uuid) {
        UuidMapping m = new UuidMapping();
        m.partId = partId;
        m.uuid = uuid;
        return m;
    }

    public static UuidMapping forItem(int itemId, String uuid) {
        UuidMapping m = new UuidMapping();
        m.itemId = itemId;
        m.uuid = uuid;
        return m;
    }

    public Integer getPartId() {
        return partId;
    }

    public void setPartId(Integer partId) {
        this.partId = partId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Serialize the two upload-side maps ({@code partId -> uuid}, {@code itemId -> uuid})
     * into the flat JSON list stored on {@code ExtSyncsQueueItem.uuidMap}. Returns
     * {@code null} when both maps are empty so the column stays {@code null} rather than
     * holding an empty array.
     */
    public static String serialize(Map<Integer, String> partUuidMap, Map<Integer, String> itemUuidMap) {
        if ((partUuidMap == null || partUuidMap.isEmpty())
                && (itemUuidMap == null || itemUuidMap.isEmpty())) {
            return null;
        }
        List<UuidMapping> entries = new ArrayList<>(
                (partUuidMap != null ? partUuidMap.size() : 0)
                        + (itemUuidMap != null ? itemUuidMap.size() : 0));
        if (partUuidMap != null) {
            partUuidMap.forEach((id, uuid) -> entries.add(forPart(id, uuid)));
        }
        if (itemUuidMap != null) {
            itemUuidMap.forEach((id, uuid) -> entries.add(forItem(id, uuid)));
        }
        try {
            return MAPPER.writeValueAsString(entries);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize UuidMapping list", e);
        }
    }

    /** Deserialize the list produced by {@link #serialize}; empty list for null/blank input. */
    public static List<UuidMapping> deserialize(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize UuidMapping list", e);
        }
    }
}
