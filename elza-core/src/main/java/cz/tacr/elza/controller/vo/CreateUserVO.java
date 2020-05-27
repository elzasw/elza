package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.UsrAuthentication;

import java.util.Map;

public class CreateUserVO {

    /**
     * Identifikátor osoby
     */
    private Integer accessPointId;

    /**
     * Uživatelské jméno
     */
    private String username;

    /**
     * Mapa typů přihlášení s hodnotama
     */
    private Map<UsrAuthentication.AuthType, String> valuesMap;

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(final Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Map<UsrAuthentication.AuthType, String> getValuesMap() {
        return valuesMap;
    }

    public void setValuesMap(final Map<UsrAuthentication.AuthType, String> valuesMap) {
        this.valuesMap = valuesMap;
    }
}
