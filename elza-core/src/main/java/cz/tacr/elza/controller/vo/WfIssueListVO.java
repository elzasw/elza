package cz.tacr.elza.controller.vo;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

/**
 * @author <a href="mailto:stepan.marek@marbes.cz">Stepan Marek</a>
 */
public class WfIssueListVO {

    // --- fields ---

    private Integer id;
    @NotNull
    private Integer fundId;
    @NotBlank
    private String name;
    @NotNull
    private Boolean open;

    // --- getters/setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFundId() {
        return fundId;
    }

    public void setFundId(Integer fundId) {
        this.fundId = fundId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }
}
