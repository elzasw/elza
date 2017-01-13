package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost, která se vztahuje k AS.
 *
 * @author Martin Šlapa
 * @since 10.05.2016
 */
public class EventFund extends AbstractEventSimple {

    /**
     * Id verze stromu.
     */
    private Integer versionId;

    /**
     * Id souboru.
     */
    private Integer fundId;

    public EventFund(final EventType eventType, final Integer fundId, final Integer versionId) {
        super(eventType);
        this.versionId = versionId;
        this.fundId = fundId;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public Integer getFundId() {
        return fundId;
    }
}
