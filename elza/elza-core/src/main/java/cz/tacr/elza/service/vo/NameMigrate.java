package cz.tacr.elza.service.vo;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelový objekt pro groovy - jméno AP - migrace.
 */
public class NameMigrate {

    /**
     * Identifikátor.
     */
    private Integer id;

    /**
     * Zdali se jedná o preferované jméno.
     */
    private boolean preferredName;

    /**
     * Vygenerované jméno.
     */
    private String name;

    /**
     * Vygenerovaný doplněk.
     */
    private String complement;

    /**
     * Vygenerované plné jméno.
     */
    private String fullName;

    /**
     * Jazyk.
     */
    private Language language;

    /**
     * Seznam itemů AP.
     */
    private List<SimpleItem> items;

    public NameMigrate(final Integer id, final boolean preferredName, final String name, final String complement, final String fullName, final Language language) {
        this.id = id;
        this.preferredName = preferredName;
        this.name = name;
        this.complement = complement;
        this.fullName = fullName;
        this.language = language;
        this.items = new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }

    public boolean isPreferredName() {
        return preferredName;
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

    public Language getLanguage() {
        return language;
    }

    public List<SimpleItem> getItems() {
        return items;
    }

    public void addItem(final String type, final String spec, final String value) {
        Validate.notNull(type);
        Validate.notNull(value);
        SimpleItem si = new SimpleItem(null, type, spec, null, value);
        items.add(si);
    }
}
