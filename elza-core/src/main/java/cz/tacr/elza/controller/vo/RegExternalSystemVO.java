package cz.tacr.elza.controller.vo;

/**
 * VO pro Číselník externích zdrojů rejstříkových hesel.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class RegExternalSystemVO {

    /**
     * Id
     */
    private Integer externalSystemId;
    /**
     * Kod zdroje.
     */
    private String code;
    /**
     * Název zdroje.
     */
    private String name;

    private String url;

    private String username;

    private String password;

    public Integer getExternalSystemId() {
        return externalSystemId;
    }

    public void setExternalSystemId(final Integer externalSystemId) {
        this.externalSystemId = externalSystemId;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
