package cz.tacr.elza.print;

import cz.tacr.elza.domain.RegRegisterType;

/**
 * Class to hold Record type info
 *
 */
public class RecordType {

    private final String name;

    private final String code;

    private final RecordType parentType;

    private RecordType(RecordType parentType, String code, String name) {
        this.parentType = parentType;
        this.code = code;
        this.name = name;
    }

    public RecordType getParentType() {
        return parentType;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * Return new instance of RecordType
     *
     * @param registerType
     * @return
     */
    public static RecordType newInstance(RecordType parentType, RegRegisterType dbRegisterType) {
        RecordType recordType = new RecordType(parentType, dbRegisterType.getCode(), dbRegisterType.getName());
        return recordType;
    }
}
