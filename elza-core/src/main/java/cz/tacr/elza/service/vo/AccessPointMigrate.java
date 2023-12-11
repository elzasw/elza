package cz.tacr.elza.service.vo;

import org.apache.commons.lang3.Validate;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelový objekt pro groovy - AP migrace.
 */
public class AccessPointMigrate {

    /**
     * Seznam jmen AP.
     */
    private List<NameMigrate> names;

    /**
     * Identifikátor.
     */
    private Integer id;

    /**
     * UUID.
     */
    private String uuid;

    /**
     * Charakteristika.
     */
    private String description;

    /**
     * Seznam itemů AP.
     */
    private List<SimpleItem> items;

    public AccessPointMigrate(final List<NameMigrate> names, final Integer id, final String uuid, final String description) {
        this.names = names;
        this.id = id;
        this.uuid = uuid;
        this.description = description;
        this.items = new ArrayList<>();
    }

    /**
     * Přidání položky do seznamu atributů.
     *
     * @param type  kód typu
     * @param spec  specifikace
     * @param value hodnota
     */
    public void addItem(final String type, @Nullable final String spec, final String value) {
        Validate.notNull(type);
        Validate.notNull(value);
        SimpleItem si = new SimpleItem(null, type, spec, null, value);
        items.add(si);
    }

    public List<SimpleItem> getItems() {
        return items;
    }

    public List<NameMigrate> getNames() {
        return names;
    }

    public Integer getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getDescription() {
        return description;
    }
}
