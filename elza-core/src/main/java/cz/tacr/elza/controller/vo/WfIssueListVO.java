package cz.tacr.elza.controller.vo;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Protokol
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Integer> rdUserIds;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Integer> wrUserIds;

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

    public List<Integer> getRdUserIds() {
        return rdUserIds;
    }

    public void setRdUserIds(List<Integer> rdUserIds) {
        this.rdUserIds = rdUserIds;
    }

    public List<Integer> getWrUserIds() {
        return wrUserIds;
    }

    public void setWrUserIds(List<Integer> wrUserIds) {
        this.wrUserIds = wrUserIds;
    }
}
