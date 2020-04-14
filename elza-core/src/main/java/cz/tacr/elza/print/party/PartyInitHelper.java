package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.string.PrepareForCompare;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.UnitDate;

/**
 * Helper object for party initialization
 */
public class PartyInitHelper {

    private final Record ap;

    public PartyInitHelper(Record ap) {
        this.ap = Validate.notNull(ap);
    }

    public Record getAP() {
        return ap;
    }

}
