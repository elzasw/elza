package cz.tacr.elza.print;

import cz.tacr.elza.domain.ParUnitdate;

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
