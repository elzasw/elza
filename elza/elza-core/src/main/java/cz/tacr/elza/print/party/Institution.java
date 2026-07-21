package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.print.Record;

/**
 * Institution
 */
public class Institution {

    private final String code;

    private final String type;

    private final String typeCode;

    private Record record;

    public Institution(String code, ParInstitutionType institutionType) {
        this.code = code;
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

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }
}
