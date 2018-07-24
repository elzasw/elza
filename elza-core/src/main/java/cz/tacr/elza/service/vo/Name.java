package cz.tacr.elza.service.vo;

import java.util.List;

public class Name {

    private Integer id;

    private boolean preferredName;

    private List<SimpleItem> items;

    private String name;

    private String complement;

    private String fullName;

    public Name(final Integer id, final boolean preferredName, final List<SimpleItem> items) {
        this.id = id;
        this.preferredName = preferredName;
        this.items = items;
    }

    public void setResult(final String name, final String complement, final String fullName) {
        this.name = name;
        this.complement = complement;
        this.fullName = fullName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public boolean isPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final boolean preferredName) {
        this.preferredName = preferredName;
    }

    public List<SimpleItem> getItems() {
        return items;
    }

    public void setItems(final List<SimpleItem> items) {
        this.items = items;
    }

    public String getName() {
        return name;
    }

    public String getComplement() {
        return complement;
    }

    public String getFullName() {
        return fullName;
    }
}
