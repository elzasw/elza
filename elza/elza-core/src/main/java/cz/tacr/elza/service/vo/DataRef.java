package cz.tacr.elza.service.vo;

import org.apache.commons.lang3.Validate;

public class DataRef {

    private String uuid;

    /**
     * Value of identifier in another system
     */
    private String value;

    public DataRef(String uuid, String value) {
        Validate.notNull(uuid);
        Validate.notNull(value);

        this.uuid = uuid;
        this.value = value;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
