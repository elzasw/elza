package cz.tacr.elza.controller.vo;

/**
 * @author <a href="mailto:stepan.marek@marbes.cz">Stepan Marek</a>
 */
public class WfIssueListBaseVO {

    // --- fields ---

    private Integer id;
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
