package cz.tacr.elza.drools.model.item;

public abstract class AbstractItem {

    private Integer id;
    private String type;
    private String spec;
    private String dataType;

    AbstractItem(final Integer id, final String type, final String spec, final String dataType) {
        this.id = id;
        this.type = type;
        this.spec = spec;
        this.dataType = dataType;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSpec() {
        return spec;
    }

    public String getDataType() {
        return dataType;
    }
}
