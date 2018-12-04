package cz.tacr.elza.controller.vo;

/**
 * Připomínka - zjednodušené VO
 */
public class WfSimpleIssueVO {

    // --- fields ---

    /**
     * Indentifikátor připomínky
     */
    private Integer id;

    /**
     * Číslo připomínky v rámci protokolu
     */
    private int number;

    /**
     * Text připomínky
     */
    private String description;

    // --- getters/setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
