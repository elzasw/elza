package cz.tacr.elza.controller.vo;

public class UserVO {

    /**
     * Identifikátor
     */
    private Integer id;

    /**
     * Zobrazované jméno
     */
    private String displayName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
