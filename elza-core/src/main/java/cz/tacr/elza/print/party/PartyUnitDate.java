package cz.tacr.elza.print.party;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.print.UnitDate;

public class PartyUnitDate extends UnitDate {

    private String note;

    public PartyUnitDate(IUnitdate srcItemData) {
        super(srcItemData);
    }

    public String getNote() {
        return note;
    }
}
