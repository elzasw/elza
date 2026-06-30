package cz.tacr.elza.cam.v2;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Pairs a CAM UUID generated during batch upload with the ELZA part or item it
 * represents. Exactly one of {@link #partId} / {@link #itemId} is set.
 *
 * One element kind of the {@link UploadMapping} payload, used by the confirm
 * processor to resolve {@code IssueXml.partRef}/{@code itemRef} back to ELZA ids.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UuidMapping {

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
}
