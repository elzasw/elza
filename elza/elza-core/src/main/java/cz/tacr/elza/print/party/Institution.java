package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParInstitutionType;

/**
 * Institution
 */
public class Institution {

    private final String code;

    private final String type;

    private final String typeCode;

    private PartyGroup partyGroup;

    public Institution(String code, ParInstitutionType institutionType) {
        this.code = code;
        this.type = institutionType.getName();
        this.typeCode = institutionType.getCode();
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public PartyGroup getPartyGroup() {
        return partyGroup;
    }

    public void setPartyGroup(final PartyGroup partyGroup) {
        this.partyGroup = partyGroup;
    }
}
