package cz.tacr.elza.service.vo;

import java.util.List;

/**
 * Modelový objekt pro groovy - AP.
 */
public class AccessPoint {

    /**
     * Identifikátor.
     */
    private Integer id;

    /**
     * UUID.
     */
    private String uuid;

    /**
     * Seznam itemů AP.
     */
    private List<SimpleItem> items;

    /**
     * Seznam jmen AP.
     */
    private List<Name> names;

    /**
     * Vygenerovaná charakteristika.
     */
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
