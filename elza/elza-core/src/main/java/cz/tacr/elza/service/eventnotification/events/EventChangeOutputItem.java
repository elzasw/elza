package cz.tacr.elza.service.eventnotification.events;

/**
 * Údálost volána při změně hodnoty atributu.
 *
 * @author Martin Šlapa
 * @since 2.2.2016
 */
public class EventChangeOutputItem extends EventVersion {

    /**
     * Object id hodnoty atributu.
     */
    private Integer itemObjectId;

    /**
     * Idetifikator uzlu.
     */
    private Integer outputId;
    /**
     * verze uzlu.
     */
    private Integer version;

    public EventChangeOutputItem(final EventType eventType, final Integer versionId, final Integer itemObjectId,
                                 final Integer outputId, final Integer version) {
        super(eventType, versionId);
        this.itemObjectId = itemObjectId;
        this.outputId = outputId;
        this.version = version;
    }

    public Integer getItemObjectId() {
        return itemObjectId;
    }

    public void setItemObjectId(final Integer itemObjectId) {
        this.itemObjectId = itemObjectId;
    }

    public Integer getOutputId() {
        return outputId;
    }

    public void setOutputId(final Integer outputId) {
        this.outputId = outputId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }
}
