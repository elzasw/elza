package cz.tacr.elza.controller.vo;

/**
 * Třída pro založení přístupového bodu.
 *
 * @since 11.07.2018
 */
public class ApAccessPointCreateVO {

    /**
     * Identifikátor typu AP.
     */
    private Integer typeId;

    /**
     * Identifikátor třídy.
     */
    private Integer scopeId;

    /**
     * Název přístupového bodu - preferované.
     */
    private String name;

    /**
     * Doplněk názvu přístupového bodu - preferované.
     */
    private String complement;

    /**
     * Kód jazyka jména přístupového bodu.
     */
    private String languageCode;

    /**
     * Popis přístupového bodu.
     */
    private String description;

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(final Integer typeId) {
        this.typeId = typeId;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(final String complement) {
        this.complement = complement;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(final String languageCode) {
        this.languageCode = languageCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
