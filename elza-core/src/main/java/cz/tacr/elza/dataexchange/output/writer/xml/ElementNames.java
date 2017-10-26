package cz.tacr.elza.dataexchange.output.writer.xml;

import javax.xml.namespace.QName;

import cz.tacr.elza.schema.v2.Event;
import cz.tacr.elza.schema.v2.Family;
import cz.tacr.elza.schema.v2.Party;
import cz.tacr.elza.schema.v2.PartyGroup;
import cz.tacr.elza.schema.v2.Person;

public class ElementNames {

    public static final QName ROOT = new QName("http://elza.tacr.cz/schema/v2", "edx", "ns2");

    public static final String SECTIONS = "s";

    public static final String ACCESS_POINTS = "aps";

    public static final String PARTIES = "pars";

    public static final String SECTION = "fs";

    public static final String PARTY = "ap";

    public static final String RULE_SET_CODE = "rule";

    public static final String FUND_INFO = "fi";

    public static final String LEVELS = "lvls";

    public static final String LEVEL = "lvl";

    public static final String PACKETS = "pcks";

    public static final String PACKET = "pck";

    public static final String getPartyName(Party party) {
        Class<? extends Party> partyType = party.getClass();
        if (partyType == Person.class) {
            return "per";
        }
        if (partyType == Family.class) {
            return "famy";
        }
        if (partyType == PartyGroup.class) {
            return "pg";
        }
        if (partyType == Event.class) {
            return "evnt";
        }
        throw new IllegalStateException("Uknow party type:" + partyType);
    }
}
