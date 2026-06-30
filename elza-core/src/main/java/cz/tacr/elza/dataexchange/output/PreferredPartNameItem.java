package cz.tacr.elza.dataexchange.output;

/**
 * Lightweight projection of an {@code ApItem} that lives on the preferred part of an access point.
 *
 * Used by the access-point CSV export to avoid loading the full {@code ArrData} entity hierarchy.
 * The string value referenced by {@link #getDataId()} is fetched separately via
 * {@code DataStringRepository.findValuesByDataIdIn(...)}.
 */
public class PreferredPartNameItem {

    private final Integer accessPointId;
    private final Integer itemTypeId;
    private final Integer dataId;

    public PreferredPartNameItem(Integer accessPointId, Integer itemTypeId, Integer dataId) {
        this.accessPointId = accessPointId;
        this.itemTypeId = itemTypeId;
        this.dataId = dataId;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public Integer getDataId() {
        return dataId;
    }
}
