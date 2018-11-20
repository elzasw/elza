package cz.tacr.elza.controller.vo;

/**
 * Základní údaje o protokolu (viz seznam oprávnění).
 */
public class WfIssueListBaseVO {

    // --- fields ---

    /**
     * Indentifikátor protokolu
     */
    private Integer id;

    /**
     * Název protokolu
     */
    private String name;

    // --- getters/setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
