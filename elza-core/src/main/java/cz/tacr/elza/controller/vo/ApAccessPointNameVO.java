package cz.tacr.elza.controller.vo;

/**
 * Jméno přístupového bodu.
 *
 * @since 11.07.2018
 */
public class ApAccessPointNameVO {

    /**
     * Identifikátor záznamu
     */
    private Integer id;

    /**
     * Id rejstříkového hesla.
     */
    private Integer accessPointId;

    /**
     * Název.
     */
    private String name;

    /**
     * Doplněk.
     */
    private String complement;

    /**
     * Jedná se o preferované jméno?
     */
    private Boolean preferredName;

    /**
     * Kód jazyku jména.
     */
    private String languageCode;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(final Integer accessPointId) {
        this.accessPointId = accessPointId;
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

    public Boolean getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final Boolean preferredName) {
        this.preferredName = preferredName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(final String languageCode) {
        this.languageCode = languageCode;
    }
}
