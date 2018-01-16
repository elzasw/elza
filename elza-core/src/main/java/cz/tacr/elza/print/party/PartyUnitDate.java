package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.print.UnitDate;

public class PartyUnitDate extends UnitDate {

    private final String note;

    public PartyUnitDate(ParUnitdate srcItemData) {
        super(srcItemData);
        this.note = srcItemData.getNote();
    }

    public String getNote() {
        return note;
    }
}
