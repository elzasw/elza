package cz.tacr.elza.utils;

import org.springframework.util.Assert;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 15. 8. 2016
 */
public enum PartyType {

    PERSON("PERSON"),

    DYNASTY("DYNASTY"),

    EVENT("EVENT"),

    PARTY_GROUP("GROUP_PARTY");

    private String code;

    private PartyType(final String typeCode) {
        Assert.notNull(typeCode);

        this.code = typeCode;
    }

    public String getCode() {
        return code;
    }

    public static PartyType getByCode(final String partyTypeCode) {
        for (PartyType type : values()) {
            if (type.getCode().equalsIgnoreCase(partyTypeCode)) {
                return type;
            }
        }

        throw new IllegalStateException("Neznámý typ osoby " + partyTypeCode);
    }
}
