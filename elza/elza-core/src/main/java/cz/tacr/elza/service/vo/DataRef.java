package cz.tacr.elza.service.vo;

public class DataRef {

    private String uuid;

    private Long value;

    public DataRef(String uuid, Long value) {
        this.uuid = uuid;
        this.value = value;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}
