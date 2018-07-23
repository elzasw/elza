package cz.tacr.elza.service.vo;

public class SimpleItem {

    private Integer id;

    private String type;

    private String spec;

    private Integer position;

    private String value;

    public SimpleItem(final Integer id,
                      final String type,
                      final String spec,
                      final Integer position,
                      final String value) {
        this.id = id;
        this.type = type;
        this.spec = spec;
        this.position = position;
        this.value = value;
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

    public Integer getPosition() {
        return position;
    }

    public String getValue() {
        return value;
    }
}
