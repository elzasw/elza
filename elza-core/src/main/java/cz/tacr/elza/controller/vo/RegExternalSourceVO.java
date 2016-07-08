package cz.tacr.elza.controller.vo;

/**
 * VO pro Číselník externích zdrojů rejstříkových hesel.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class RegExternalSourceVO {

    /**
     * Id
     */
    private Integer externalSourceId;
    /**
     * Kod zdroje.
     */
    private String code;
    /**
     * Název zdroje.
     */
    private String name;

    public Integer getExternalSourceId() {
        return externalSourceId;
    }

    public void setExternalSourceId(final Integer externalSourceId) {
        this.externalSourceId = externalSourceId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
