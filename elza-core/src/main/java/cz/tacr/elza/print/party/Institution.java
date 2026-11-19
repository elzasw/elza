package cz.tacr.elza.print.party;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.part.Part;

/**
 * Institution
 */
public class Institution {

    /**
     * Item type with main part of the name
     */
    private static final String ITEM_TYPE_NAME_MAIN = "NM_MAIN";

    private final String code;

    private final String type;

    private final String typeCode;

    /**
     * Name of the institution (Administration -> Institutions: Name)
     */
    private final String name;

    /**
     * Short name of the institution (Administration -> Institutions: Short name)
     */
    private final String shortName;

    private Record record;

    public Institution(String code, ParInstitutionType institutionType, String name, String shortName) {
        this.code = code;
        this.name = name;
        this.shortName = shortName;
        if (institutionType != null) {
        	type = institutionType.getName();
        	typeCode = institutionType.getCode();
        } else {
        	type = null;
        	typeCode = null;
        }
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

    /**
     * Return name of the institution as set in the administration.
     *
     * If the name is not set, the main part of the preferred name
     * of the institution's access point (without complements) is returned.
     *
     * @return name of the institution, empty string if not available
     */
    public String getName() {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        if (record != null) {
            Part preferredPart = record.getPreferredPart();
            if (preferredPart != null) {
                return preferredPart.getSingleItemValue(ITEM_TYPE_NAME_MAIN);
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Return short name of the institution as set in the administration.
     *
     * @return short name, might be null
     */
    public String getShortName() {
        return shortName;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }
}
