package cz.tacr.elza.controller.vo;

/**
 * Působnost osoby.
 */
public class ParPartyTimeRangeEditVO {

    private Integer partyTimeRangeId;

    private Integer partyId;

    private ParUnitdateEditVO from;

    private ParUnitdateEditVO to;


    public Integer getPartyTimeRangeId() {
        return partyTimeRangeId;
    }

    public void setPartyTimeRangeId(final Integer partyTimeRangeId) {
        this.partyTimeRangeId = partyTimeRangeId;
    }

    public ParUnitdateEditVO getFrom() {
        return from;
    }

    public void setFrom(final ParUnitdateEditVO from) {
        this.from = from;
    }

    public ParUnitdateEditVO getTo() {
        return to;
    }

    public void setTo(final ParUnitdateEditVO to) {
        this.to = to;
    }

    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }
}
