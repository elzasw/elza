package cz.tacr.elza.service.eventnotification.events;

import java.util.List;

/**
 * Událost pro změny v hodnot strukt. typu.
 *
 * @since 16.11.2017
 */
public class EventStructureDataChange extends AbstractEventSimple {

    /**
     * Identifikátor AS.
     */
    private Integer fundId;

    /**
     * Kód strukt. typu.
     */
    private String structureTypeCode;

    /**
     * Identifikátory dočasných hodnot strukt. typu.
     */
    private List<Integer> tempIds;

    /**
     * Identifikátory vytvořených hodnot strukt. typu.
     */
    private List<Integer> createIds;

    /**
     * Identifikátory upravených hodnot strukt. typu.
     */
    private List<Integer> updateIds;

    /**
     * Identifikátory smazaných hodnot strukt. typu.
     */
    private List<Integer> deleteIds;

    public EventStructureDataChange(final Integer fundId,
                                    final String structureTypeCode,
                                    final List<Integer> tempIds,
                                    final List<Integer> createIds,
                                    final List<Integer> updateIds,
                                    final List<Integer> deleteIds) {
        super(EventType.STRUCTURE_DATA_CHANGE);
        this.fundId = fundId;
        this.structureTypeCode = structureTypeCode;
        this.tempIds = tempIds;
        this.createIds = createIds;
        this.updateIds = updateIds;
        this.deleteIds = deleteIds;
    }

    public Integer getFundId() {
        return fundId;
    }

    public String getStructureTypeCode() {
        return structureTypeCode;
    }

    public void setStructureTypeCode(final String structureTypeCode) {
        this.structureTypeCode = structureTypeCode;
    }

    public List<Integer> getTempIds() {
        return tempIds;
    }

    public void setTempIds(final List<Integer> tempIds) {
        this.tempIds = tempIds;
    }

    public List<Integer> getCreateIds() {
        return createIds;
    }

    public void setCreateIds(final List<Integer> createIds) {
        this.createIds = createIds;
    }

    public List<Integer> getUpdateIds() {
        return updateIds;
    }

    public void setUpdateIds(final List<Integer> updateIds) {
        this.updateIds = updateIds;
    }

    public List<Integer> getDeleteIds() {
        return deleteIds;
    }

    public void setDeleteIds(final List<Integer> deleteIds) {
        this.deleteIds = deleteIds;
    }
}
