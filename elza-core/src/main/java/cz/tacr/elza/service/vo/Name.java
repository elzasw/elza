package cz.tacr.elza.service.vo;

import java.util.List;

/**
 * Modelový objekt pro groovy - jméno AP.
 */
public class Name {

    /**
     * Identifikátor.
     */
    private Integer id;

    /**
     * Zdali se jedná o preferované jméno.
     */
    private boolean preferredName;

    /**
     * Seznam itemů jména.
     */
    private List<SimpleItem> items;

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

    public Name(final Integer id, final boolean preferredName, final List<SimpleItem> items, final Language language) {
        this.id = id;
        this.preferredName = preferredName;
        this.items = items;
        this.language = language;
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

    public Language getLanguage() {
        return language;
    }
}
