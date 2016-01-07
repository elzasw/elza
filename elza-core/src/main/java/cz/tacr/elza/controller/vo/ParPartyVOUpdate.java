package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;


/**
 * Osoba pro update.
 */
public class ParPartyVOUpdate extends ParPartyNameVOSave {

    /**
     * Seznam působností osoby.
     */
    private List<ParPartyTimeRangeVO> timeRanges;

    public List<ParPartyTimeRangeVO> getTimeRanges() {
        return timeRanges;
    }

    public void setTimeRanges(final List<ParPartyTimeRangeVO> timeRanges) {
        this.timeRanges = timeRanges;
    }

    public void addPartyTimeRange(final ParPartyTimeRangeVO partyTimeRange) {
        if (timeRanges == null) {
            timeRanges = new LinkedList<>();
        }
        timeRanges.add(partyTimeRange);
    }
}

