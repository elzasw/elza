package cz.tacr.elza.controller.vo;

/**
 * Působnost osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParPartyTimeRangeVO {

    private Integer partyTimeRangeId;

    private Integer partyId;

    private ParUnitdateVO from;

    private ParUnitdateVO to;

    public Integer getPartyTimeRangeId() {
        return partyTimeRangeId;
    }

    public void setPartyTimeRangeId(final Integer partyTimeRangeId) {
        this.partyTimeRangeId = partyTimeRangeId;
    }

    public ParUnitdateVO getFrom() {
        return from;
    }

    public void setFrom(final ParUnitdateVO from) {
        this.from = from;
    }

    public ParUnitdateVO getTo() {
        return to;
    }

    public void setTo(final ParUnitdateVO to) {
        this.to = to;
    }

    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }
}
