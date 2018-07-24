package cz.tacr.elza.service.vo;

import java.util.List;

public class AccessPoint {

    private Integer id;

    private String uuid;

    private List<SimpleItem> items;

    private List<Name> names;

    private String description;

    public AccessPoint(final Integer id, final String uuid, final List<SimpleItem> items, final List<Name> names) {
        this.id = id;
        this.uuid = uuid;
        this.items = items;
        this.names = names;
    }

    public void setResult(final String description) {
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public List<SimpleItem> getItems() {
        return items;
    }

    public List<Name> getNames() {
        return names;
    }

    public String getDescription() {
        return description;
    }
}
