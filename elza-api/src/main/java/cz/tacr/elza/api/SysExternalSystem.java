package cz.tacr.elza.api;

/**
 * Číselník externích systémů.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2016
 */
public interface SysExternalSystem {

    Integer getExternalSystemId();

    void setExternalSystemId(Integer externalSystemId);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);

    String getUrl();

    void setUrl(String url);

    String getUsername();

    void setUsername(String username);

    String getPassword();

    void setPassword(String password);

}